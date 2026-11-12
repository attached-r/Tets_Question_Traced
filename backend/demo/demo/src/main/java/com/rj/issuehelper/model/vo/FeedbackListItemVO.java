package com.rj.issuehelper.model.vo;

import com.rj.issuehelper.model.enums.FeedbackStatus;
import com.rj.issuehelper.model.enums.FeedbackType;
import com.rj.issuehelper.model.pojo.Feedback;
import com.rj.issuehelper.util.TextGuard;
import com.rj.issuehelper.util.TimeUtil;
import lombok.Data;

/**
 * 列表项。
 * <p>
 * 刻意只给 {@code contentPreview}（截断 80 字）而不是全文：
 * 列表接口是信息暴露面最大的地方，全文只在详情接口下发。
 */
@Data
public class FeedbackListItemVO {

    private Long id;
    private String requestId;
    private String typeCode;
    private String typeLabel;
    private Integer status;
    private String statusLabel;
    private String submitterName;
    private String handlerName;
    private String contentPreview;
    private String createTime;

    public static FeedbackListItemVO of(Feedback feedback, String submitterName, String handlerName) {
        FeedbackListItemVO vo = new FeedbackListItemVO();
        vo.setId(feedback.getId());
        vo.setRequestId(feedback.getRequestId());
        vo.setTypeCode(feedback.getTypeCode());
        vo.setTypeLabel(FeedbackType.labelOf(feedback.getTypeCode()));
        vo.setStatus(feedback.getStatus());
        vo.setStatusLabel(FeedbackStatus.labelOf(feedback.getStatus()));
        vo.setSubmitterName(submitterName);
        vo.setHandlerName(handlerName);
        vo.setContentPreview(TextGuard.preview(feedback.getContent()));
        vo.setCreateTime(TimeUtil.format(feedback.getCreateTime()));
        return vo;
    }
}
