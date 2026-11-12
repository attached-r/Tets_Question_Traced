package com.rj.issuehelper.model.dto;

import lombok.Data;

/**
 * 列表查询条件。
 * <p>
 * {@code keyword} 只做 {@code LIKE} 匹配，参数化由 MyBatis 保证，
 * 不存在拼接 SQL 的注入风险（见测试用例 TC-12）。
 */
@Data
public class FeedbackQueryDTO {

    private Integer page = 1;

    private Integer size = 10;

    /** 0 待处理 1 处理中 2 已完成；null = 全部 */
    private Integer status;

    /** 类型码；null = 全部 */
    private String typeCode;

    /** 关键字：匹配原文或提交编号 */
    private String keyword;
}
