package com.rj.issuehelper.model.vo;

import com.rj.issuehelper.model.enums.FeedbackStatus;
import com.rj.issuehelper.model.enums.FeedbackType;
import lombok.Data;

import java.util.Arrays;
import java.util.List;

/**
 * 前端初始化所需的字典：类型与状态。
 */
@Data
public class DictVO {

    private List<OptionVO> types;
    private List<OptionVO> statuses;

    public static DictVO build() {
        DictVO vo = new DictVO();
        vo.setTypes(Arrays.stream(FeedbackType.values())
                .map(type -> new OptionVO(type.getCode(), type.getLabel()))
                .toList());
        vo.setStatuses(Arrays.stream(FeedbackStatus.values())
                .map(status -> new OptionVO(String.valueOf(status.getCode()), status.getLabel()))
                .toList());
        return vo;
    }
}
