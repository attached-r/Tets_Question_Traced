package com.rj.issuehelper.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 字典项：类型下拉、状态筛选都用它，避免前端硬编码中文文案。
 */
@Data
@AllArgsConstructor
public class OptionVO {

    private String code;
    private String label;
}
