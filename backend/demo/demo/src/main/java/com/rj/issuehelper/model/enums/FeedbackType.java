package com.rj.issuehelper.model.enums;

import com.rj.issuehelper.common.ResultCode;
import com.rj.issuehelper.exception.BusinessException;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 反馈类型：五选一，由提交者在前端显式选择，后端只校验合法性。
 * <p>
 * 为什么不自动分类后落库：报的是故障还是想加需求，只有提交者知道；
 * 系统猜错会把故障淹没在需求池里。系统最多给"建议"，落库的一定是人的选择，
 * 这样也满足"不得捏造缺失信息"——说不清就选 UNDETERMINED。
 */
@Getter
public enum FeedbackType {

    SYSTEM_FAULT("系统故障", "本该成功的事失败、报错、卡住或变慢"),
    PERMISSION("权限申请", "需要开通访问权、角色或审批"),
    FEATURE("功能建议", "希望新增或改进功能"),
    CONSULT("使用咨询", "询问怎么操作、在哪里配置"),
    UNDETERMINED("待确认", "说不清、指向不明或信息不足");

    private final String label;
    private final String hint;

    FeedbackType(String label, String hint) {
        this.label = label;
        this.hint = hint;
    }

    public String getCode() {
        return name();
    }

    public static FeedbackType of(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择反馈类型");
        }
        return Arrays.stream(values())
                .filter(type -> type.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST, "未知的反馈类型: " + code));
    }

    public static String labelOf(String code) {
        return Arrays.stream(values())
                .filter(type -> type.name().equals(code))
                .map(FeedbackType::getLabel)
                .findFirst()
                .orElse(null);
    }

    public static List<FeedbackType> ordered() {
        return List.of(values());
    }
}
