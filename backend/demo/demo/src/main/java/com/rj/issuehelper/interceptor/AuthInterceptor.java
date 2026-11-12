package com.rj.issuehelper.interceptor;

import com.rj.issuehelper.common.ResultCode;
import com.rj.issuehelper.common.UserAuth;
import com.rj.issuehelper.common.UserContext;
import com.rj.issuehelper.common.annotation.RequiresRole;
import com.rj.issuehelper.exception.BusinessException;
import com.rj.issuehelper.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * 登录与角色校验拦截器。
 * <p>
 * 做法要点：
 * <ol>
 *   <li>异常直接抛出，交给 {@code GlobalExceptionHandler} 序列化，避免在这里手拼 JSON；</li>
 *   <li>先完成全部校验再写 UserContext，保证抛异常时上下文不会残留在复用线程上；</li>
 *   <li>校验通过后把 userId / role 写进 MDC，后续日志行都会带上，便于按人排查；</li>
 *   <li>afterCompletion 一定清理 ThreadLocal。</li>
 * </ol>
 * 注意：任何情况下都不打印 token 原文。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 预检请求直接放行，CORS 由 WebMvcConfig 处理
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 非 Controller 方法（静态资源、错误页）不校验
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String token = extractToken(request);
        if (token == null) {
            log.warn("[鉴权] 缺少 token：{} {}", request.getMethod(), request.getRequestURI());
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        UserAuth auth;
        try {
            auth = jwtUtil.parse(token);
        } catch (BusinessException e) {
            log.warn("[鉴权] token 无效或已过期：{} {}", request.getMethod(), request.getRequestURI());
            throw e;
        }

        RequiresRole requiresRole = handlerMethod.getMethodAnnotation(RequiresRole.class);
        if (requiresRole == null) {
            requiresRole = handlerMethod.getBeanType().getAnnotation(RequiresRole.class);
        }
        if (requiresRole != null && !Arrays.asList(requiresRole.value()).contains(auth.getRoleCode())) {
            log.warn("[鉴权] 角色不足：path={} role={} required={}",
                    request.getRequestURI(), auth.getRoleCode(), Arrays.toString(requiresRole.value()));
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        UserContext.set(auth);
        MDC.put("userId", String.valueOf(auth.getUserId()));
        MDC.put("role", auth.getRoleCode());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 容器线程会复用，不清理会造成串号
        UserContext.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
