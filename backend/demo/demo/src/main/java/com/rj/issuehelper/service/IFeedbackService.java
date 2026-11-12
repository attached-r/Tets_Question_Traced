package com.rj.issuehelper.service;

import com.rj.issuehelper.common.PageResult;
import com.rj.issuehelper.model.dto.FeedbackCreateDTO;
import com.rj.issuehelper.model.dto.FeedbackQueryDTO;
import com.rj.issuehelper.model.dto.NoteUpdateDTO;
import com.rj.issuehelper.model.dto.StatusUpdateDTO;
import com.rj.issuehelper.model.dto.TypeUpdateDTO;
import com.rj.issuehelper.model.vo.FeedbackCreateVO;
import com.rj.issuehelper.model.vo.FeedbackDetailVO;
import com.rj.issuehelper.model.vo.FeedbackListItemVO;

public interface IFeedbackService {

    FeedbackCreateVO create(FeedbackCreateDTO dto);

    /** 员工端：只看自己的 */
    PageResult<FeedbackListItemVO> myList(FeedbackQueryDTO query);

    /** 支持端：看全部，可按状态/类型/关键字筛选 */
    PageResult<FeedbackListItemVO> board(FeedbackQueryDTO query);

    FeedbackDetailVO detail(Long id);

    /** 按提交编号回查（重复提交后的补偿入口，仅本人或支持人员） */
    FeedbackDetailVO byRequestId(String requestId);

    FeedbackDetailVO updateStatus(Long id, StatusUpdateDTO dto);

    FeedbackDetailVO updateType(Long id, TypeUpdateDTO dto);

    FeedbackDetailVO updateNote(Long id, NoteUpdateDTO dto);
}
