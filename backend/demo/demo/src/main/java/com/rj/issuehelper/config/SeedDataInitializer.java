package com.rj.issuehelper.config;

import com.rj.issuehelper.mapper.FeedbackLogMapper;
import com.rj.issuehelper.mapper.FeedbackMapper;
import com.rj.issuehelper.mapper.SysUserMapper;
import com.rj.issuehelper.model.enums.FeedbackStatus;
import com.rj.issuehelper.model.enums.FeedbackType;
import com.rj.issuehelper.model.enums.OperateType;
import com.rj.issuehelper.model.enums.Role;
import com.rj.issuehelper.model.pojo.Feedback;
import com.rj.issuehelper.model.pojo.FeedbackLog;
import com.rj.issuehelper.model.pojo.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 演示数据初始化。
 * <p>
 * 只在 sys_user 为空时执行，重复启动不会重复插入。
 * 密码用 BCrypt 现场加密，不在 SQL 里硬编码 hash——手写 hash 极易出错且无法验证。
 * 生产环境应关闭（issuehelper.seed.enabled=false）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeedDataInitializer implements CommandLineRunner {

    private static final String DEFAULT_PASSWORD = "123456";

    private final SysUserMapper sysUserMapper;
    private final FeedbackMapper feedbackMapper;
    private final FeedbackLogMapper feedbackLogMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${issuehelper.seed.enabled:true}")
    private boolean enabled;

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        Long existing = sysUserMapper.selectCount(null);
        if (existing != null && existing > 0) {
            log.info("已存在用户数据，跳过演示数据灌入");
            return;
        }

        log.info("开始灌入演示账号与样例反馈 ...");
        Map<String, SysUser> users = new LinkedHashMap<>();

        // 7 名员工（对应任务书模拟数据）
        users.put("xli", createUser("xli", "小李", Role.EMPLOYEE_CODE));
        users.put("xzhou", createUser("xzhou", "小周", Role.EMPLOYEE_CODE));
        users.put("xchen", createUser("xchen", "小陈", Role.EMPLOYEE_CODE));
        users.put("xwang", createUser("xwang", "小王", Role.EMPLOYEE_CODE));
        users.put("xzhang", createUser("xzhang", "小张", Role.EMPLOYEE_CODE));
        users.put("xzhao", createUser("xzhao", "小赵", Role.EMPLOYEE_CODE));
        users.put("xsun", createUser("xsun", "小孙", Role.EMPLOYEE_CODE));

        // 2 名技术支持
        users.put("support01", createUser("support01", "张工", Role.SUPPORT_CODE));
        users.put("support02", createUser("support02", "李工", Role.SUPPORT_CODE));

        users.values().forEach(sysUserMapper::insert);

        SysUser support01 = users.get("support01");
        SysUser support02 = users.get("support02");
        LocalDateTime now = LocalDateTime.now();

        // R001 登录转圈 —— 系统故障，待处理
        seed(users.get("xli"), "R001", FeedbackType.SYSTEM_FAULT,
                "今天系统登录页面一直转圈，我进不去。",
                now.minusHours(8), FeedbackStatus.PENDING, null, null, null);

        // R002 申请查看权限 —— 权限申请，待处理
        seed(users.get("xzhou"), "R002", FeedbackType.PERMISSION,
                "帮我申请一下项目资料的查看权限，现在提示无权限。",
                now.minusHours(7), FeedbackStatus.PENDING, null, null, null);

        // R003 保存筛选条件 —— 功能建议，待处理
        seed(users.get("xchen"), "R003", FeedbackType.FEATURE,
                "希望查询页面能保存上次的筛选条件，每次都得重新选。",
                now.minusHours(6), FeedbackStatus.PENDING, null, null, null);

        // R004 导出无文件 —— 系统故障，处理中（带内部备注，用于验证字段边界）
        seed(users.get("xli"), "R004", FeedbackType.SYSTEM_FAULT,
                "点击导出后没有文件，试了两次都不行。",
                now.minusHours(5), FeedbackStatus.PROCESSING, support01,
                "疑似对象存储权限问题，正在核对导出任务日志", null);

        // R005 重置密码 —— 使用咨询，已完成
        seed(users.get("xwang"), "R005", FeedbackType.CONSULT,
                "请问怎么重置自己的登录密码？",
                now.minusHours(4), FeedbackStatus.DONE, support01,
                "已电话指导：个人中心 - 安全设置 - 重置密码，用户确认可用",
                now.minusHours(3).withMinute(30));

        // R006 系统很慢 —— 系统故障，处理中
        seed(users.get("xzhang"), "R006", FeedbackType.SYSTEM_FAULT,
                "系统特别慢，今天工作都受影响了。",
                now.minusHours(3), FeedbackStatus.PROCESSING, support02,
                "已采样网关耗时，待确认是否为报表任务并发导致", null);

        // R007 想加报表 —— 功能建议，待处理
        seed(users.get("xzhao"), "R007", FeedbackType.FEATURE,
                "想加个报表功能，最好下周能用。",
                now.minusHours(2), FeedbackStatus.PENDING, null, null, null);

        // R008 指向不明 —— 待确认（不推断的样例）
        seed(users.get("xsun"), "R008", FeedbackType.UNDETERMINED,
                "那个先不用了。",
                now.minusHours(1), FeedbackStatus.PENDING, null, null, null);

        log.info("演示数据灌入完成：{} 个账号，8 条样例反馈", users.size());
    }

    private SysUser createUser(String username, String realName, String roleCode) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setRealName(realName);
        user.setRoleCode(roleCode);
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        return user;
    }

    private void seed(SysUser submitter, String requestId, FeedbackType type, String content,
                      LocalDateTime createTime, FeedbackStatus status, SysUser handler,
                      String handlerNote, LocalDateTime finishTime) {
        Feedback feedback = new Feedback();
        feedback.setRequestId(requestId);
        feedback.setSubmitterId(submitter.getId());
        feedback.setContent(content);
        feedback.setTypeCode(type.getCode());
        feedback.setTypeSource(1);
        feedback.setStatus(status.getCode());
        feedback.setHandlerId(status == FeedbackStatus.PENDING ? null : handler.getId());
        feedback.setHandlerNote(handlerNote);
        feedback.setFinishTime(finishTime);
        feedback.setVersion(0);
        feedback.setDeleted(0);
        feedback.setCreateTime(createTime);
        feedback.setUpdateTime(finishTime != null ? finishTime : createTime);
        feedbackMapper.insert(feedback);

        // 提交日志
        writeLog(feedback.getId(), submitter, OperateType.SUBMIT,
                null, FeedbackStatus.PENDING.getCode(), null, type.getCode(), "提交反馈", createTime);

        if (status != FeedbackStatus.PENDING) {
            // 领取日志
            writeLog(feedback.getId(), handler, OperateType.START,
                    FeedbackStatus.PENDING.getCode(), FeedbackStatus.PROCESSING.getCode(),
                    null, null, null, createTime.plusMinutes(10));
        }
        if (status == FeedbackStatus.DONE) {
            writeLog(feedback.getId(), handler, OperateType.FINISH,
                    FeedbackStatus.PROCESSING.getCode(), FeedbackStatus.DONE.getCode(),
                    null, null, "已答复并结案", finishTime);
        }
    }

    private void writeLog(Long feedbackId, SysUser operator, OperateType operateType,
                          Integer fromStatus, Integer toStatus, String fromType, String toType,
                          String remark, LocalDateTime createTime) {
        FeedbackLog log = new FeedbackLog();
        log.setFeedbackId(feedbackId);
        log.setOperatorId(operator.getId());
        log.setOperatorRole(operator.getRoleCode());
        log.setOperateType(operateType.getCode());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setFromType(fromType);
        log.setToType(toType);
        log.setRemark(remark);
        log.setCreateTime(createTime);
        feedbackLogMapper.insert(log);
    }
}
