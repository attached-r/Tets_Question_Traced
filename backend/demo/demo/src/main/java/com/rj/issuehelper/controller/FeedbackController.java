package com.rj.issuehelper.controller;

import com.rj.issuehelper.common.PageResult;
import com.rj.issuehelper.common.Result;
import com.rj.issuehelper.common.annotation.RequiresRole;
import com.rj.issuehelper.model.dto.FeedbackCreateDTO;
import com.rj.issuehelper.model.dto.FeedbackQueryDTO;
import com.rj.issuehelper.model.dto.NoteUpdateDTO;
import com.rj.issuehelper.model.dto.StatusUpdateDTO;
import com.rj.issuehelper.model.dto.TypeUpdateDTO;
import com.rj.issuehelper.model.enums.Role;
import com.rj.issuehelper.model.vo.FeedbackCreateVO;
import com.rj.issuehelper.model.vo.FeedbackDetailVO;
import com.rj.issuehelper.model.vo.FeedbackListItemVO;
import com.rj.issuehelper.service.IFeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 反馈接口。
 * <p>
 * 权限分两层：
 * <ul>
 *   <li>角色级：{@code @RequiresRole} 由拦截器统一拦截（看板、改状态、改类型、写备注）；</li>
 *   <li>数据级：员工只能看/查到自己的记录，由 Service 内 buildDetail 判定——
 *       详情接口本身不标注解，因为"登录用户"包含员工，但员工只能看自己那条。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final IFeedbackService feedbackService;

    /** 提交反馈：任何登录用户都可以提交，提交人即当前登录用户 */
    @PostMapping
    public Result<FeedbackCreateVO> create(@RequestBody @Valid FeedbackCreateDTO dto) {
        return Result.success(feedbackService.create(dto));
    }

    /** 我的反馈（员工端） */
    @GetMapping("/my")
    public Result<PageResult<FeedbackListItemVO>> myList(FeedbackQueryDTO query) {
        return Result.success(feedbackService.myList(query));
    }

    /** 全部反馈看板（仅技术支持） */
    @GetMapping
    @RequiresRole(Role.SUPPORT_CODE)
    public Result<PageResult<FeedbackListItemVO>> board(FeedbackQueryDTO query) {
        return Result.success(feedbackService.board(query));
    }

    /** 按提交编号回查：重复提交后的补偿入口 */
    @GetMapping("/by-request/{requestId}")
    public Result<FeedbackDetailVO> byRequestId(@PathVariable String requestId) {
        return Result.success(feedbackService.byRequestId(requestId));
    }

    /** 详情：本人或技术支持 */
    @GetMapping("/{id}")
    public Result<FeedbackDetailVO> detail(@PathVariable Long id) {
        return Result.success(feedbackService.detail(id));
    }

    /** 更新状态（仅技术支持） */
    @PatchMapping("/{id}/status")
    @RequiresRole(Role.SUPPORT_CODE)
    public Result<FeedbackDetailVO> updateStatus(@PathVariable Long id,
                                                 @RequestBody @Valid StatusUpdateDTO dto) {
        return Result.success(feedbackService.updateStatus(id, dto));
    }

    /** 类型复判（仅技术支持，已完成不可改） */
    @PatchMapping("/{id}/type")
    @RequiresRole(Role.SUPPORT_CODE)
    public Result<FeedbackDetailVO> updateType(@PathVariable Long id,
                                                @RequestBody @Valid TypeUpdateDTO dto) {
        return Result.success(feedbackService.updateType(id, dto));
    }

    /** 写内部备注（仅技术支持） */
    @PatchMapping("/{id}/note")
    @RequiresRole(Role.SUPPORT_CODE)
    public Result<FeedbackDetailVO> updateNote(@PathVariable Long id,
                                               @RequestBody @Valid NoteUpdateDTO dto) {
        return Result.success(feedbackService.updateNote(id, dto));
    }
}
