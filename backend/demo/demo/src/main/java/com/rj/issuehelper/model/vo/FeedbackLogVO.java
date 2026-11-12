package com.rj.issuehelper.model.vo;

import com.rj.issuehelper.model.enums.OperateType;
import com.rj.issuehelper.model.enums.FeedbackStatus;
import com.rj.issuehelper.model.pojo.FeedbackLog;
import com.rj.issuehelper.util.TimeUtil;
import lombok.Data;

/**
 * 时间线节点。
 * <p>
 * {@code remark} 是内部操作说明：对员工角色组装时传 {@code false}，
 * 该字段保持 null，且 Jackson 配置了 non_null，不会出现在响应 JSON 里。
 */
@Data
public class FeedbackLogVO {

    private Long id;
    private String operatorName;
    private String operatorRole;
    private Integer operateType;
    private String operateTypeLabel;
    private String fromStatusLabel;
    private String toStatusLabel;
    private String fromTypeLabel;
    private String toTypeLabel;
    private String remark;
    private String createTime;

    public static FeedbackLogVO of(FeedbackLog log, String operatorName, boolean includeInternalRemark) {
        FeedbackLogVO vo = new FeedbackLogVO();
        vo.setId(log.getId());
        vo.setOperatorName(operatorName);
        vo.setOperatorRole(log.getOperatorRole());
        vo.setOperateType(log.getOperateType());
        vo.setOperateTypeLabel(OperateType.labelOf(log.getOperateType()));
        vo.setFromStatusLabel(FeedbackStatus.labelOf(log.getFromStatus()));
        vo.setToStatusLabel(FeedbackStatus.labelOf(log.getToStatus()));
        vo.setFromTypeLabel(com.rj.issuehelper.model.enums.FeedbackType.labelOf(log.getFromType()));
        vo.setToTypeLabel(com.rj.issuehelper.model.enums.FeedbackType.labelOf(log.getToType()));
        vo.setRemark(includeInternalRemark ? log.getRemark() : null);
        vo.setCreateTime(TimeUtil.format(log.getCreateTime()));
        return vo;
    }
}
