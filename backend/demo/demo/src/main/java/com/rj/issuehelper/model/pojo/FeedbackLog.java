package com.rj.issuehelper.model.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志表 feedback_operate_log，只增不改。
 * <p>
 * 记录"谁在什么时候把状态/类型从什么改成什么"，
 * 员工端时间线与支持端审计都依赖它。
 */
@Data
@TableName("feedback_operate_log")
public class FeedbackLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long feedbackId;

    private Long operatorId;

    /** 操作人角色快照，便于事后判断操作来源 */
    private String operatorRole;

    /** 1 提交 2 开始处理 3 完成 4 类型复判 5 更新备注 */
    private Integer operateType;

    private Integer fromStatus;

    private Integer toStatus;

    private String fromType;

    private String toType;

    /** 操作说明，属于内部信息，对员工不下发 */
    private String remark;

    private LocalDateTime createTime;
}
