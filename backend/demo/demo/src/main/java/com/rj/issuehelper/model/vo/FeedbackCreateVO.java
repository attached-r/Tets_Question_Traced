package com.rj.issuehelper.model.vo;

import com.rj.issuehelper.model.enums.FeedbackStatus;
import com.rj.issuehelper.model.enums.FeedbackType;
import com.rj.issuehelper.model.pojo.Feedback;
import com.rj.issuehelper.util.TimeUtil;
import lombok.Data;

/**
 * 提交成功后的返回。
 * <p>
 * 只回必要字段：调用方拿到 id / 编号 / 初始状态就够，
 * 不把整条记录回显出去（列表与详情另有接口）。
 */
@Data
public class FeedbackCreateVO {

    private Long id;
    private String requestId;
    private String typeCode;
    private String typeLabel;
    private Integer status;
    private String statusLabel;
    private String createTime;

    public static FeedbackCreateVO of(Feedback feedback) {
        FeedbackCreateVO vo = new FeedbackCreateVO();
        vo.setId(feedback.getId());
        vo.setRequestId(feedback.getRequestId());
        vo.setTypeCode(feedback.getTypeCode());
        vo.setTypeLabel(FeedbackType.labelOf(feedback.getTypeCode()));
        vo.setStatus(feedback.getStatus());
        vo.setStatusLabel(FeedbackStatus.labelOf(feedback.getStatus()));
        vo.setCreateTime(TimeUtil.format(feedback.getCreateTime()));
        return vo;
    }
}
