package com.rj.issuehelper.model.vo;

import com.rj.issuehelper.util.RuleTypeClassifier;
import lombok.Data;

import java.util.List;

/**
 * 类型建议结果。
 * <p>
 * {@code confident=false} 时 {@code typeCode} 固定为 UNDETERMINED：
 * 信息不足就不给结论，这是"不得捏造缺失信息"的接口层体现。
 */
@Data
public class TypeSuggestVO {

    private String typeCode;
    private String typeLabel;
    /** HIGH / MEDIUM / LOW */
    private String confidence;
    private boolean confident;
    private int score;
    private List<String> matchedKeywords;
    private String tip;

    public static TypeSuggestVO of(RuleTypeClassifier.Suggestion suggestion) {
        TypeSuggestVO vo = new TypeSuggestVO();
        vo.setTypeCode(suggestion.typeCode());
        vo.setTypeLabel(suggestion.typeLabel());
        vo.setConfidence(suggestion.confidence());
        vo.setConfident(suggestion.confident());
        vo.setScore(suggestion.score());
        vo.setMatchedKeywords(suggestion.matchedKeywords());
        vo.setTip(suggestion.tip());
        return vo;
    }
}
