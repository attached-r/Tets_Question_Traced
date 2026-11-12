package com.rj.issuehelper.exception;

import com.rj.issuehelper.common.ResultCode;

/**
 * 业务异常：携带业务码与可直接展示给用户的提示语。
 * <p>
 * 抛出后由 {@code GlobalExceptionHandler} 统一转换为 {@code Result.fail(code, message)}。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /** 覆盖默认提示语，用于补充上下文（如"当前状态不允许该操作（已完成 → 处理中）"） */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public int getCode() {
        return code;
    }
}
