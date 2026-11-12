package com.rj.issuehelper.model.enums;

import lombok.Getter;

/**
 * 操作事件类型，写入 feedback_operate_log.operate_type。
 */
@Getter
public enum OperateType {

    SUBMIT(1, "提交"),
    START(2, "开始处理"),
    FINISH(3, "完成"),
    RETYPE(4, "类型复判"),
    UPDATE_NOTE(5, "更新备注");

    private final int code;
    private final String label;

    OperateType(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static String labelOf(Integer code) {
        if (code == null) {
            return null;
        }
        for (OperateType type : values()) {
            if (type.code == code) {
                return type.label;
            }
        }
        return String.valueOf(code);
    }
}
