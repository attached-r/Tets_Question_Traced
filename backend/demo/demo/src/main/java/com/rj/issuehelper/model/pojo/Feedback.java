package com.rj.issuehelper.model.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 反馈主表 feedback。
 * <p>
 * {@code content} 保存提交时的原文，任何流程都不改写它；
 * {@code version} 配合 OptimisticLockerInnerInterceptor 做并发控制。
 */
@Data
@TableName("feedback")
public class Feedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提交编号，全局唯一（数据库唯一索引兜底幂等） */
    private String requestId;

    private Long submitterId;

    /** 反馈原文，原样保存 */
    private String content;

    /** SYSTEM_FAULT / PERMISSION / FEATURE / CONSULT / UNDETERMINED */
    private String typeCode;

    /** 1 提交者选择 2 处理人复判 */
    private Integer typeSource;

    /** 0 待处理 1 处理中 2 已完成 */
    private Integer status;

    private Long handlerId;

    /** 内部处理备注，仅 SUPPORT / ADMIN 可见 */
    private String handlerNote;

    private LocalDateTime finishTime;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
