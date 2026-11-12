package com.rj.issuehelper.model.dto;

import lombok.Data;

/**
 * 智能识别入参：只传文本，不落库。
 */
@Data
public class TypeSuggestDTO {

    private String content;
}
