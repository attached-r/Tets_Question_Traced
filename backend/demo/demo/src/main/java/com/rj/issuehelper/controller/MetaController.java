package com.rj.issuehelper.controller;

import com.rj.issuehelper.common.Result;
import com.rj.issuehelper.model.dto.TypeSuggestDTO;
import com.rj.issuehelper.model.vo.DictVO;
import com.rj.issuehelper.model.vo.TypeSuggestVO;
import com.rj.issuehelper.util.RuleTypeClassifier;
import com.rj.issuehelper.util.TextGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 字典与辅助能力。
 */
@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
public class MetaController {

    /** 类型/状态字典，前端不硬编码中文文案 */
    @GetMapping("/dict")
    public Result<DictVO> dict() {
        return Result.success(DictVO.build());
    }

    /**
     * 类型建议：纯规则打分，不落库。
     * 没有付费模型 Key 也能演示；接模型时替换 RuleTypeClassifier 即可。
     */
    @PostMapping("/type-suggest")
    public Result<TypeSuggestVO> suggestType(@RequestBody TypeSuggestDTO dto) {
        TextGuard.assertOptionalLength(dto.getContent(), TextGuard.CONTENT_MAX_LENGTH, "文本");
        return Result.success(TypeSuggestVO.of(RuleTypeClassifier.suggest(dto.getContent())));
    }
}
