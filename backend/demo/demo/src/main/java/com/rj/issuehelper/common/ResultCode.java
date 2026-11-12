package com.rj.issuehelper.common;

import lombok.Getter;

/**
 * 业务状态码。
 * <p>
 * 写入响应体的 {@code code} 字段，与 HTTP 状态码相互独立：
 * 本项目约定 HTTP 恒为 200，业务结果只看这里的 code，前端只需一个拦截器。
 * <p>
 * 409xx 是细分冲突码，便于测试用例直接断言具体原因，而不是去匹配 message 文案。
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权访问该资源"),
    NOT_FOUND(404, "记录不存在"),

    /** 幂等冲突：相同提交编号重复提交 */
    DUPLICATE_REQUEST_ID(40901, "该提交编号已存在，请勿重复提交"),
    /** 状态机冲突：当前状态不允许目标状态 */
    ILLEGAL_TRANSITION(40902, "当前状态不允许该操作"),
    /** 并发冲突：乐观锁版本不一致或影响行数为 0 */
    VERSION_CONFLICT(40903, "该记录已被他人修改，请刷新后重试"),
    /** 终态锁定：已完成记录不允许复判类型 */
    TYPE_LOCKED(40904, "反馈已完成，类型不可再修改"),

    INTERNAL_ERROR(500, "系统异常，请稍后再试");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
