package com.rj.issuehelper.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TypeUpdateDTO {

    @NotBlank(message = "请选择反馈类型")
    private String typeCode;

    @NotNull(message = "版本号不能为空")
    private Integer version;
}
