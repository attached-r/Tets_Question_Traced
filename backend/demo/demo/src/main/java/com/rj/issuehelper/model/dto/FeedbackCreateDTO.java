package com.rj.issuehelper.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 提交反馈入参。
 * <p>
 * {@code content} 不挂 {@code @NotBlank}：空白与长度校验交给 TextGuard，
 * 因为它还要处理全角空格、零宽字符这些 {@code strip()} 管不到的情况，
 * 并且要给出比 Hibernate Validator 更精确的中文提示。
 */
@Data
public class FeedbackCreateDTO {

    @NotBlank(message = "提交编号不能为空")
    private String requestId;

    @NotBlank(message = "请选择反馈类型")
    private String typeCode;

    private String content;
}
