package com.rj.issuehelper.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rj.issuehelper.common.PageResult;
import com.rj.issuehelper.common.ResultCode;
import com.rj.issuehelper.common.UserAuth;
import com.rj.issuehelper.common.UserContext;
import com.rj.issuehelper.exception.BusinessException;
import com.rj.issuehelper.mapper.FeedbackLogMapper;
import com.rj.issuehelper.mapper.FeedbackMapper;
import com.rj.issuehelper.mapper.SysUserMapper;
import com.rj.issuehelper.model.dto.FeedbackCreateDTO;
import com.rj.issuehelper.model.dto.FeedbackQueryDTO;
import com.rj.issuehelper.model.dto.NoteUpdateDTO;
import com.rj.issuehelper.model.dto.StatusUpdateDTO;
import com.rj.issuehelper.model.dto.TypeUpdateDTO;
import com.rj.issuehelper.model.enums.FeedbackStatus;
import com.rj.issuehelper.model.enums.FeedbackType;
import com.rj.issuehelper.model.enums.OperateType;
import com.rj.issuehelper.model.pojo.Feedback;
import com.rj.issuehelper.model.pojo.FeedbackLog;
import com.rj.issuehelper.model.pojo.SysUser;
import com.rj.issuehelper.model.vo.FeedbackCreateVO;
import com.rj.issuehelper.model.vo.FeedbackDetailVO;
import com.rj.issuehelper.model.vo.FeedbackListItemVO;
import com.rj.issuehelper.model.vo.FeedbackLogVO;
import com.rj.issuehelper.service.IFeedbackService;
import com.rj.issuehelper.util.TextGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 反馈业务实现。
 * <p>
 * 三条硬规则都落在这一层：
 * <ol>
 *   <li>幂等：靠 uk_request_id 唯一索引，捕获 DuplicateKeyException 转 40901；</li>
 *   <li>状态机：靠 FeedbackStatus.canGoTo 迁移表，非法流转 40902；</li>
 *   <li>边界：数据归属校验贴着数据的唯一出口（buildDetail）写，不写在 Controller。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService implements IFeedbackService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int TYPE_SOURCE_SUBMITTER = 1;
    private static final int TYPE_SOURCE_SUPPORT = 2;

    private final FeedbackMapper feedbackMapper;
    private final FeedbackLogMapper feedbackLogMapper;
    private final SysUserMapper sysUserMapper;

    // ------------------------------------------------------------------
    // 提交
    // ------------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FeedbackCreateVO create(FeedbackCreateDTO dto) {
        TextGuard.assertRequestId(dto.getRequestId());
        // 返回的是原文：规范化只用于判空，不落库
        String rawContent = TextGuard.assertContent(dto.getContent());
        FeedbackType type = FeedbackType.of(dto.getTypeCode());
        UserAuth me = UserContext.require();

        LocalDateTime now = LocalDateTime.now();
        Feedback entity = new Feedback();
        entity.setRequestId(dto.getRequestId().trim());
        entity.setSubmitterId(me.getUserId());
        entity.setContent(rawContent);
        entity.setTypeCode(type.getCode());
        entity.setTypeSource(TYPE_SOURCE_SUBMITTER);
        entity.setStatus(FeedbackStatus.PENDING.getCode());
        entity.setVersion(0);
        entity.setDeleted(0);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        try {
            feedbackMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            // 唯一索引是幂等的最终防线：并发重复提交时只有一个请求能插入成功。
            // 这里不区分"是否本人重复"，统一 40901，避免把他人记录的存在性暴露出去。
            log.warn("[幂等] 提交编号已存在 requestId={} operatorId={}", entity.getRequestId(), me.getUserId());
            throw new BusinessException(ResultCode.DUPLICATE_REQUEST_ID);
        }

        writeLog(entity.getId(), me, OperateType.SUBMIT, null, entity.getStatus(),
                null, type.getCode(), "提交反馈");

        // 只记长度和类型，不记原文：原文可能含内部系统名、报错堆栈甚至账号信息
        log.info("[提交] feedbackId={} requestId={} type={} contentLen={} operatorId={}",
                entity.getId(), entity.getRequestId(), type.getCode(), rawContent.length(), me.getUserId());

        return FeedbackCreateVO.of(entity);
    }

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    @Override
    public PageResult<FeedbackListItemVO> myList(FeedbackQueryDTO query) {
        return toPageResult(queryPage(query, true));
    }

    @Override
    public PageResult<FeedbackListItemVO> board(FeedbackQueryDTO query) {
        // 注解已经拦过一次，这里再挡一次：Service 被别处调用时也不能漏
        if (!UserContext.isSupport()) {
            log.warn("[越权] 非支持人员访问看板 operatorId={} role={}",
                    UserContext.getUserId(), UserContext.getRoleCode());
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问反馈看板");
        }
        return toPageResult(queryPage(query, false));
    }

    @Override
    public FeedbackDetailVO detail(Long id) {
        return buildDetail(getOrThrow(id));
    }

    @Override
    public FeedbackDetailVO byRequestId(String requestId) {
        if (requestId == null || requestId.isBlank() || requestId.length() > 64) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "提交编号格式非法");
        }
        Feedback feedback = feedbackMapper.selectOne(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getRequestId, requestId.trim()));
        if (feedback == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return buildDetail(feedback);
    }

    // ------------------------------------------------------------------
    // 状态 / 类型 / 备注
    // ------------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FeedbackDetailVO updateStatus(Long id, StatusUpdateDTO dto) {
        Feedback db = getOrThrow(id);

        // 先判版本再判状态机：版本过期说明别人已经改过，此时让客户端先刷新拿最新状态，
        // 提示"已被他人修改"比提示"当前状态不允许"更准确。
        // 反过来先判状态机的话，并发中的后到请求会读到已变更的状态，返回 40902 而不是 40903，
        // 用户看到的原因就是错的（TC-06 覆盖这一点）。
        checkVersion(id, db.getVersion(), dto.getVersion());

        FeedbackStatus from = FeedbackStatus.of(db.getStatus());
        FeedbackStatus to = FeedbackStatus.of(dto.getTargetStatus());
        if (!from.canGoTo(to)) {
            log.warn("[状态] 非法流转 feedbackId={} {} -> {} operatorId={}",
                    id, from.getLabel(), to.getLabel(), UserContext.getUserId());
            throw new BusinessException(ResultCode.ILLEGAL_TRANSITION,
                    String.format("当前状态不允许该操作（%s → %s）", from.getLabel(), to.getLabel()));
        }

        UserAuth me = UserContext.require();
        Feedback update = new Feedback();
        update.setId(id);
        update.setStatus(to.getCode());
        update.setVersion(db.getVersion());
        if (to == FeedbackStatus.PROCESSING) {
            update.setHandlerId(me.getUserId());
        }
        if (to == FeedbackStatus.DONE) {
            update.setHandlerId(db.getHandlerId() != null ? db.getHandlerId() : me.getUserId());
            update.setFinishTime(LocalDateTime.now());
        }

        // @Version 生效后 SQL 为 WHERE id=? AND version=?，影响行数 0 说明被他人抢先改过
        int rows = feedbackMapper.updateById(update);
        if (rows == 0) {
            log.warn("[并发] 更新影响行数为 0，版本冲突 feedbackId={}", id);
            throw new BusinessException(ResultCode.VERSION_CONFLICT);
        }

        OperateType operateType = to == FeedbackStatus.DONE ? OperateType.FINISH : OperateType.START;
        writeLog(id, me, operateType, db.getStatus(), to.getCode(), null, null, dto.getRemark());

        log.info("[状态] feedbackId={} {} -> {} operatorId={} 备注={}",
                id, from.getLabel(), to.getLabel(), me.getUserId(),
                dto.getRemark() == null ? "" : "有");

        return detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FeedbackDetailVO updateType(Long id, TypeUpdateDTO dto) {
        Feedback db = getOrThrow(id);
        // 同样先判版本：过期请求先刷新，避免拿旧数据去触发业务规则判断
        checkVersion(id, db.getVersion(), dto.getVersion());

        if (Objects.equals(db.getStatus(), FeedbackStatus.DONE.getCode())) {
            throw new BusinessException(ResultCode.TYPE_LOCKED);
        }
        FeedbackType to = FeedbackType.of(dto.getTypeCode());
        if (to.getCode().equals(db.getTypeCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "类型未发生变化");
        }

        Feedback update = new Feedback();
        update.setId(id);
        update.setTypeCode(to.getCode());
        update.setTypeSource(TYPE_SOURCE_SUPPORT);
        update.setVersion(db.getVersion());

        int rows = feedbackMapper.updateById(update);
        if (rows == 0) {
            log.warn("[并发] 更新影响行数为 0，版本冲突 feedbackId={}", id);
            throw new BusinessException(ResultCode.VERSION_CONFLICT);
        }

        writeLog(id, UserContext.require(), OperateType.RETYPE, null, null,
                db.getTypeCode(), to.getCode(), "类型复判");

        log.info("[类型] feedbackId={} {} -> {} operatorId={}",
                id, db.getTypeCode(), to.getCode(), UserContext.getUserId());

        return detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FeedbackDetailVO updateNote(Long id, NoteUpdateDTO dto) {
        Feedback db = getOrThrow(id);
        checkVersion(id, db.getVersion(), dto.getVersion());

        String note = dto.getNote() == null ? null : dto.getNote().trim();
        if (note != null && note.isEmpty()) {
            note = null;   // 传空串表示清空备注
        }
        TextGuard.assertOptionalLength(note, 1000, "备注");

        // 用 UpdateWrapper 显式 set：实体 updateById 在字段为 null 时会跳过该列，清空不掉
        LambdaUpdateWrapper<Feedback> wrapper = new LambdaUpdateWrapper<Feedback>()
                .eq(Feedback::getId, id)
                .eq(Feedback::getVersion, db.getVersion())
                .set(Feedback::getHandlerNote, note)
                .setSql("version = version + 1");

        int rows = feedbackMapper.update(null, wrapper);
        if (rows == 0) {
            log.warn("[并发] 更新影响行数为 0，版本冲突 feedbackId={}", id);
            throw new BusinessException(ResultCode.VERSION_CONFLICT);
        }

        writeLog(id, UserContext.require(), OperateType.UPDATE_NOTE, null, null,
                null, null, note == null ? "清空内部备注" : "更新内部备注");

        // 备注是内部信息，只记长度不记内容
        log.info("[备注] feedbackId={} noteLen={} cleared={} operatorId={}",
                id, note == null ? 0 : note.length(), note == null, UserContext.getUserId());

        return detail(id);
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private Feedback getOrThrow(Long id) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return feedback;
    }

    private void checkVersion(Long feedbackId, Integer current, Integer expected) {
        if (expected == null || !Objects.equals(current, expected)) {
            log.warn("[并发] 版本不一致 feedbackId={} expected={} actual={}", feedbackId, expected, current);
            throw new BusinessException(ResultCode.VERSION_CONFLICT);
        }
    }

    private boolean isSupportRole(UserAuth me) {
        return "SUPPORT".equals(me.getRoleCode()) || "ADMIN".equals(me.getRoleCode());
    }

    /**
     * 详情组装：数据归属与字段可见性都在这里判定。
     * <p>
     * 详情被「详情接口」「按编号回查」「状态更新后返回」三处复用，
     * 校验写在这里才不会漏掉后两处。
     */
    private FeedbackDetailVO buildDetail(Feedback feedback) {
        UserAuth me = UserContext.require();
        boolean support = isSupportRole(me);
        boolean owner = Objects.equals(feedback.getSubmitterId(), me.getUserId());

        if (!support && !owner) {
            log.warn("[越权] 查看他人反馈 feedbackId={} ownerId={} operatorId={} role={}",
                    feedback.getId(), feedback.getSubmitterId(), me.getUserId(), me.getRoleCode());
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看该反馈");
        }

        List<FeedbackLog> logs = feedbackLogMapper.selectList(new LambdaQueryWrapper<FeedbackLog>()
                .eq(FeedbackLog::getFeedbackId, feedback.getId())
                .orderByAsc(FeedbackLog::getCreateTime)
                .orderByAsc(FeedbackLog::getId));

        Set<Long> ids = new HashSet<>();
        ids.add(feedback.getSubmitterId());
        ids.add(feedback.getHandlerId());
        logs.forEach(log -> ids.add(log.getOperatorId()));
        Map<Long, String> names = loadNames(ids);

        FeedbackDetailVO vo = FeedbackDetailVO.of(feedback,
                names.get(feedback.getSubmitterId()),
                feedback.getHandlerId() == null ? null : names.get(feedback.getHandlerId()));
        if (support) {
            vo.setHandlerNote(feedback.getHandlerNote());
        }

        List<FeedbackLogVO> logVos = new ArrayList<>();
        for (FeedbackLog log : logs) {
            // 员工侧不下发 remark（内部操作说明）
            logVos.add(FeedbackLogVO.of(log, names.get(log.getOperatorId()), support));
        }
        vo.setLogs(logVos);
        return vo;
    }

    private Page<Feedback> queryPage(FeedbackQueryDTO query, boolean onlyMine) {
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int size = query.getSize() == null || query.getSize() < 1 ? 10
                : Math.min(query.getSize(), MAX_PAGE_SIZE);

        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        if (onlyMine) {
            wrapper.eq(Feedback::getSubmitterId, UserContext.getUserId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Feedback::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getTypeCode())) {
            wrapper.eq(Feedback::getTypeCode, query.getTypeCode().trim());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(Feedback::getContent, keyword)
                    .or().like(Feedback::getRequestId, keyword));
        }
        wrapper.orderByDesc(Feedback::getCreateTime).orderByDesc(Feedback::getId);

        return feedbackMapper.selectPage(new Page<>(page, size), wrapper);
    }

    private PageResult<FeedbackListItemVO> toPageResult(Page<Feedback> page) {
        List<Feedback> rows = page.getRecords();
        Set<Long> ids = new HashSet<>();
        for (Feedback feedback : rows) {
            ids.add(feedback.getSubmitterId());
            ids.add(feedback.getHandlerId());
        }
        Map<Long, String> names = loadNames(ids);

        List<FeedbackListItemVO> list = rows.stream()
                .map(feedback -> FeedbackListItemVO.of(feedback,
                        names.get(feedback.getSubmitterId()),
                        feedback.getHandlerId() == null ? null : names.get(feedback.getHandlerId())))
                .toList();

        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), list);
    }

    /** 批量取姓名，避免列表 N+1 查询 */
    private Map<Long, String> loadNames(Collection<Long> ids) {
        Set<Long> distinct = new HashSet<>();
        for (Long id : ids) {
            if (id != null) {
                distinct.add(id);
            }
        }
        if (distinct.isEmpty()) {
            return Map.of();
        }
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getId, distinct));
        Map<Long, String> names = new HashMap<>();
        for (SysUser user : users) {
            names.put(user.getId(), user.getRealName());
        }
        return names;
    }

    private void writeLog(Long feedbackId, UserAuth operator, OperateType operateType,
                          Integer fromStatus, Integer toStatus,
                          String fromType, String toType, String remark) {
        FeedbackLog log = new FeedbackLog();
        log.setFeedbackId(feedbackId);
        log.setOperatorId(operator.getUserId());
        log.setOperatorRole(operator.getRoleCode());
        log.setOperateType(operateType.getCode());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setFromType(fromType);
        log.setToType(toType);
        log.setRemark(remark);
        log.setCreateTime(LocalDateTime.now());
        feedbackLogMapper.insert(log);
    }
}
