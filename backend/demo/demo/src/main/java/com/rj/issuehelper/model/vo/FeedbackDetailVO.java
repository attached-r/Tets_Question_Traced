package com.rj.issuehelper.model.vo;

import com.rj.issuehelper.model.enums.FeedbackStatus;
import com.rj.issuehelper.model.enums.FeedbackType;
import com.rj.issuehelper.model.pojo.Feedback;
import com.rj.issuehelper.util.TimeUtil;
import lombok.Data;

import java.util.List;

/**
 * 反馈详情。
 * <p>
 * {@code handlerNote} 是内部备注，只有 SUPPORT / ADMIN 会被赋值；
 * 员工侧保持 null，配合 non_null 序列化策略，字段不会出现在响应里
 * —— 信息边界靠后端裁剪，而不是前端 v-if。
 */
@Data
public class FeedbackDetailVO {

    private Long id;
    private String requestId;
    private String typeCode;
    private String typeLabel;
    private Integer typeSource;
    private Integer status;
    private String statusLabel;
    private String submitterName;
    private String handlerName;
    private String content;
    private String handlerNote;
    private String finishTime;
    private Integer version;
    private String createTime;
    private String updateTime;
    private List<FeedbackLogVO> logs;

    public static FeedbackDetailVO of(Feedback feedback, String submitterName, String handlerName) {
        FeedbackDetailVO vo = new FeedbackDetailVO();
        vo.setId(feedback.getId());
        vo.setRequestId(feedback.getRequestId());
        vo.setTypeCode(feedback.getTypeCode());
        vo.setTypeLabel(FeedbackType.labelOf(feedback.getTypeCode()));
        vo.setTypeSource(feedback.getTypeSource());
        vo.setStatus(feedback.getStatus());
        vo.setStatusLabel(FeedbackStatus.labelOf(feedback.getStatus()));
        vo.setSubmitterName(submitterName);
        vo.setHandlerName(handlerName);
        vo.setContent(feedback.getContent());
        vo.setFinishTime(TimeUtil.format(feedback.getFinishTime()));
        vo.setVersion(feedback.getVersion());
        vo.setCreateTime(TimeUtil.format(feedback.getCreateTime()));
        vo.setUpdateTime(TimeUtil.format(feedback.getUpdateTime()));
        return vo;
    }
}
