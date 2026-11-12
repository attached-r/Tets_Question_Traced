package com.rj.issuehelper.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StatusUpdateDTO {

    /** 目标状态：0 待处理 1 处理中 2 已完成 */
    @NotNull(message = "目标状态不能为空")
    private Integer targetStatus;

    /** 前端读到的版本号，用于乐观锁 */
    @NotNull(message = "版本号不能为空")
    private Integer version;

    /** 操作说明，写入日志，对员工不下发 */
    @Size(max = 200, message = "操作说明不能超过 200 字")
    private String remark;
}
