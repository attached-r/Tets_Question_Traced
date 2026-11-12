package com.rj.issuehelper.util;

import com.rj.issuehelper.model.enums.FeedbackType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 类型建议器：本地关键词打分，纯函数、可单测、不依赖任何模型 Key。
 * <p>
 * 定位是"建议"而不是"识别结果"——返回值只用于前端回填，
 * 落库的 type_code 永远是用户确认过的值。置信度不够时直接返回"待确认"，
 * 因为猜测错误会让故障被淹没在需求池里。
 * <p>
 * 判定门槛：
 * <ol>
 *   <li>最高分 ≥ 2</li>
 *   <li>与第二名的分差 ≥ 1（两类分数接近时不猜）</li>
 * </ol>
 */
public final class RuleTypeClassifier {

    /** 建议结果 */
    public record Suggestion(
            String typeCode,
            String typeLabel,
            String confidence,
            boolean confident,
            int score,
            List<String> matchedKeywords,
            String tip
    ) {
    }

    private record Keyword(String word, int weight) {
    }

    private static final Keyword kw(String word, int weight) {
        return new Keyword(word, weight);
    }

    /** 分类规则表：只给四类具体类型建规则，UNDETERMINED 是兜底，不需要关键词 */
    private static final Map<FeedbackType, List<Keyword>> RULES = new LinkedHashMap<>();

    static {
        RULES.put(FeedbackType.SYSTEM_FAULT, List.of(
                kw("转圈", 2), kw("进不去", 2), kw("打不开", 2), kw("报错", 2),
                kw("异常", 2), kw("失败", 2), kw("崩溃", 2), kw("闪退", 2),
                kw("卡死", 2), kw("慢", 2), kw("没反应", 2), kw("没有文件", 2),
                kw("不行", 2), kw("无法", 2), kw("加载", 2),
                kw("系统", 1), kw("页面", 1), kw("登录", 1), kw("导出", 1)
        ));
        RULES.put(FeedbackType.PERMISSION, List.of(
                kw("权限", 2), kw("无权限", 2), kw("申请", 2), kw("开通", 2),
                kw("授权", 2), kw("角色", 2), kw("白名单", 2), kw("审批", 2),
                kw("访问", 1), kw("查看", 1)
        ));
        RULES.put(FeedbackType.FEATURE, List.of(
                kw("希望", 2), kw("建议", 2), kw("能不能", 2), kw("加个", 2),
                kw("增加", 2), kw("新增", 2), kw("优化", 2), kw("最好", 2),
                kw("需求", 2), kw("功能", 1), kw("支持", 1)
        ));
        RULES.put(FeedbackType.CONSULT, List.of(
                kw("请问", 2), kw("怎么", 2), kw("如何", 2), kw("在哪", 2),
                kw("哪里", 2), kw("教程", 2), kw("步骤", 2), kw("重置密码", 2)
        ));
    }

    private RuleTypeClassifier() {
    }

    public static Suggestion suggest(String content) {
        String text = content == null ? "" : content;

        Map<FeedbackType, Integer> scores = new EnumMap<>(FeedbackType.class);
        Map<FeedbackType, List<String>> matched = new EnumMap<>(FeedbackType.class);

        RULES.forEach((type, keywords) -> {
            int score = 0;
            List<String> hits = new ArrayList<>();
            for (Keyword keyword : keywords) {
                if (text.contains(keyword.word())) {
                    score += keyword.weight();
                    hits.add(keyword.word());
                }
            }
            scores.put(type, score);
            matched.put(type, hits);
        });

        List<Map.Entry<FeedbackType, Integer>> ranked = scores.entrySet().stream()
                .sorted(Comparator.<Map.Entry<FeedbackType, Integer>>comparingInt(Map.Entry::getValue).reversed())
                .toList();

        Map.Entry<FeedbackType, Integer> top = ranked.get(0);
        int topScore = top.getValue();
        int secondScore = ranked.size() > 1 ? ranked.get(1).getValue() : 0;
        int gap = topScore - secondScore;

        // 门槛不满足 → 不猜，返回待确认
        if (topScore < 2 || gap < 1) {
            return new Suggestion(
                    FeedbackType.UNDETERMINED.getCode(),
                    FeedbackType.UNDETERMINED.getLabel(),
                    "LOW",
                    false,
                    topScore,
                    List.of(),
                    "文本信息不足，无法判断类型，建议选择“待确认”由技术支持复核"
            );
        }

        String confidence = (topScore >= 4 && gap >= 2) ? "HIGH" : "MEDIUM";
        return new Suggestion(
                top.getKey().getCode(),
                top.getKey().getLabel(),
                confidence,
                true,
                topScore,
                List.copyOf(matched.get(top.getKey())),
                "仅为关键词规则建议，最终类型以你的选择为准"
        );
    }
}
