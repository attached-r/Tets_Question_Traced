package com.rj.issuehelper.model.enums;

import com.rj.issuehelper.common.ResultCode;
import com.rj.issuehelper.exception.BusinessException;
import lombok.Getter;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 反馈状态。允许的迁移集中在 {@link #ALLOWED} 一张表里，
 * 新增或调整流转只改这一处，不会散落在各个 Service 方法中。
 *
 * <pre>
 * 待处理(0) → 处理中(1) → 已完成(2)
 * 待处理(0) → 已完成(2)    咨询类答复即结案
 * 已完成(2) 为终态，不可再变更
 * </pre>
 */
@Getter
public enum FeedbackStatus {

    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    DONE(2, "已完成");

    private final int code;
    private final String label;

    FeedbackStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    /** 允许的迁移：key 为当前状态，value 为可到达的目标状态 */
    private static final Map<FeedbackStatus, Set<FeedbackStatus>> ALLOWED = Map.of(
            PENDING, EnumSet.of(PROCESSING, DONE),
            PROCESSING, EnumSet.of(DONE),
            DONE, EnumSet.noneOf(FeedbackStatus.class)
    );

    public boolean canGoTo(FeedbackStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }

    public static FeedbackStatus of(Integer code) {
        if (code == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态不能为空");
        }
        for (FeedbackStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new BusinessException(ResultCode.BAD_REQUEST, "未知状态: " + code);
    }

    /** 找不到时返回 null，用于给历史脏数据兜底展示，不抛异常 */
    public static FeedbackStatus ofNullable(Integer code) {
        if (code == null) {
            return null;
        }
        for (FeedbackStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    public static String labelOf(Integer code) {
        FeedbackStatus status = ofNullable(code);
        return status == null ? null : status.label;
    }
}
