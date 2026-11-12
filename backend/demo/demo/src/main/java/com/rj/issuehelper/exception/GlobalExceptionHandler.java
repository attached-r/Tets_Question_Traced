package com.rj.issuehelper.exception;

import com.rj.issuehelper.common.Result;
import com.rj.issuehelper.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 全局异常处理器：统一转换为 {@link Result}。
 * <p>
 * 约定：HTTP 状态码恒为 200，业务结果看响应体 {@code code}。
 * 由于 HTTP 层看不出失败，这里会把业务码写到 request 属性上（{@link #BIZ_CODE}），
 * 由 AccessLogFilter 打进访问日志 —— 这样一行日志就能看出"这次请求是 40901 还是成功"。
 * <p>
 * 日志分级：可纠正的业务冲突打 WARN（不打堆栈），未预期异常打 ERROR（打全堆栈）。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务码属性名，AccessLogFilter 读取 */
    public static final String BIZ_CODE = "biz.code";

    /** 业务提示属性名，AccessLogFilter 读取 */
    public static final String BIZ_MESSAGE = "biz.message";

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        markBusinessFailure(request, e.getCode(), e.getMessage());
        log.warn("[业务异常] code={} message={} path={} {}",
                e.getCode(), e.getMessage(), request.getMethod(), request.getRequestURI());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** @RequestBody + @Valid 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                     HttpServletRequest request) {
        String message = firstFieldError(e.getBindingResult().getFieldErrors());
        markBusinessFailure(request, ResultCode.BAD_REQUEST.getCode(), message);
        log.warn("[参数校验] {} {}：{}", request.getMethod(), request.getRequestURI(), message);
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    /** 表单/查询参数绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        String message = firstFieldError(e.getBindingResult().getFieldErrors());
        markBusinessFailure(request, ResultCode.BAD_REQUEST.getCode(), message);
        log.warn("[参数校验] {} {}：{}", request.getMethod(), request.getRequestURI(), message);
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    /** @RequestParam / @PathVariable 上的约束校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e,
                                                  HttpServletRequest request) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse(ResultCode.BAD_REQUEST.getMessage());
        markBusinessFailure(request, ResultCode.BAD_REQUEST.getCode(), message);
        log.warn("[参数校验] {} {}：{}", request.getMethod(), request.getRequestURI(), message);
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    /** 请求体为空或 JSON 格式错误 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
                                                     HttpServletRequest request) {
        markBusinessFailure(request, ResultCode.BAD_REQUEST.getCode(), "请求参数格式错误");
        log.warn("[请求体] 解析失败 {} {}：{}", request.getMethod(), request.getRequestURI(),
                e.getMostSpecificCause().getMessage());
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), "请求参数格式错误");
    }

    /** 路径变量类型不匹配，如 /api/feedback/abc */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                           HttpServletRequest request) {
        markBusinessFailure(request, ResultCode.BAD_REQUEST.getCode(), "参数类型错误");
        log.warn("[参数类型] {} {}：{}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), "参数类型错误");
    }

    /** 唯一索引冲突兜底（提交编号冲突已在 Service 内单独处理） */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e, HttpServletRequest request) {
        markBusinessFailure(request, ResultCode.DUPLICATE_REQUEST_ID.getCode(),
                ResultCode.DUPLICATE_REQUEST_ID.getMessage());
        log.warn("[唯一约束] 冲突 {} {}：{}", request.getMethod(), request.getRequestURI(),
                e.getMostSpecificCause().getMessage());
        return Result.fail(ResultCode.DUPLICATE_REQUEST_ID);
    }

    /**
     * 路径不存在（含浏览器自动请求的 /favicon.ico）。
     * <p>
     * 单独处理，否则会落到兜底 handler 变成 500 + ERROR 堆栈，
     * 日志里全是噪音，真正的问题反而被淹掉。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFound(NoResourceFoundException e, HttpServletRequest request) {
        markBusinessFailure(request, ResultCode.NOT_FOUND.getCode(), "接口不存在");
        log.debug("[路径] 未找到 {}", request.getRequestURI());
        return Result.fail(ResultCode.NOT_FOUND.getCode(), "接口不存在");
    }

    /** 兜底：不向前端泄露内部细节，细节留在日志里 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        markBusinessFailure(request, ResultCode.INTERNAL_ERROR.getCode(),
                ResultCode.INTERNAL_ERROR.getMessage());
        log.error("[未处理异常] {} {}", request.getMethod(), request.getRequestURI(), e);
        return Result.fail(ResultCode.INTERNAL_ERROR);
    }

    /**
     * 把业务结果写到 request 上，供 AccessLogFilter 输出到访问日志。
     * HTTP 恒为 200 的前提下，这是让"一行访问日志"能反映业务成败的关键。
     */
    private void markBusinessFailure(HttpServletRequest request, int code, String message) {
        if (request == null) {
            return;
        }
        request.setAttribute(BIZ_CODE, code);
        request.setAttribute(BIZ_MESSAGE, message);
    }

    private String firstFieldError(List<FieldError> errors) {
        return errors.stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse(ResultCode.BAD_REQUEST.getMessage());
    }
}
