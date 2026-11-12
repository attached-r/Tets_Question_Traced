package com.rj.issuehelper.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NoteUpdateDTO {

    /** 内部处理备注；传空串表示清空 */
    @Size(max = 1000, message = "备注不能超过 1000 字")
    private String note;

    @NotNull(message = "版本号不能为空")
    private Integer version;
}
