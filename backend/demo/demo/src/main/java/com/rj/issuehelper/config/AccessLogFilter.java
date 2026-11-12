package com.rj.issuehelper.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 请求级日志：给每个请求分配 traceId，并输出一行访问日志。
 * <p>
 * 为什么需要它：本项目 HTTP 状态码恒为 200，业务结果在响应体的 code 里，
 * 容器访问日志看不出任何失败。这里把 {@code biz.code}（由 GlobalExceptionHandler 写入）
 * 一并打印，才能一眼区分"成功"和"40901 重复提交"。
 * <p>
 * traceId 写入 MDC，会出现在同一请求的所有日志行里（见 logback-spring.xml 的 pattern），
 * 排查时按 traceId 过滤即可串起一次请求的全部日志。
 * <p>
 * 注意：MDC 基于 ThreadLocal，线程池复用，finally 里必须清理，否则 traceId 会串到下一个请求。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AccessLogFilter extends OncePerRequestFilter {

    /** 客户端可以带这个头来串联前后端日志；没带就由服务端生成 */
    private static final String TRACE_HEADER = "X-Request-Id";

    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_ROLE = "role";

    /** 超过这个耗时就按 WARN 打，便于发现慢接口 */
    private static final long SLOW_THRESHOLD_MS = 1000L;

    /**
     * 客户端传入的 traceId 属于不可信输入：只接受白名单字符与长度，
     * 其余一律丢弃并重新生成。否则一个带空白/控制字符的值就能把日志行切碎，
     * 让 grep 失效甚至伪造日志。
     */
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{4,32}$");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 预检和静态资源不记，避免日志噪音
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator") || "/favicon.ico".equals(uri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String traceId = resolveTraceId(request);
        MDC.put(MDC_TRACE_ID, traceId);
        response.setHeader(TRACE_HEADER, traceId);

        long startNanos = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            // 能走到这里的异常说明没被 GlobalExceptionHandler 接住，属于兜底
            log.error("[请求] 未捕获异常 {} {}", request.getMethod(), request.getRequestURI(), e);
            throw e;
        } finally {
            long costMs = (System.nanoTime() - startNanos) / 1_000_000;
            writeAccessLog(request, response, costMs);
            MDC.remove(MDC_TRACE_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_ROLE);
        }
    }

    private void writeAccessLog(HttpServletRequest request, HttpServletResponse response, long costMs) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();

        // GlobalExceptionHandler 在抛业务异常时写入，用来让访问日志带上业务结果
        Object bizCode = request.getAttribute("biz.code");
        Object bizMessage = request.getAttribute("biz.message");

        if (bizCode != null) {
            log.warn("[请求] {} {} -> {} ({}ms) biz={} {}", method, uri, status, costMs, bizCode,
                    bizMessage == null ? "" : bizMessage);
            return;
        }

        if (costMs >= SLOW_THRESHOLD_MS) {
            log.warn("[请求] {} {} -> {} ({}ms) 慢请求", method, uri, status, costMs);
            return;
        }

        log.info("[请求] {} {} -> {} ({}ms)", method, uri, status, costMs);
    }

    private String resolveTraceId(HttpServletRequest request) {
        String candidate = request.getHeader(TRACE_HEADER);
        if (candidate != null) {
            candidate = candidate.trim();
            if (TRACE_ID_PATTERN.matcher(candidate).matches()) {
                return candidate;
            }
            // 不打印候选值本身：它可能是超长串或垃圾内容，只记长度
            log.debug("[请求] traceId 格式非法，改用服务端生成，传入长度={}", candidate.length());
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
