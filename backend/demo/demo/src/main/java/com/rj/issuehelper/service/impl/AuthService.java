package com.rj.issuehelper.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rj.issuehelper.common.ResultCode;
import com.rj.issuehelper.common.UserAuth;
import com.rj.issuehelper.common.UserContext;
import com.rj.issuehelper.exception.BusinessException;
import com.rj.issuehelper.mapper.SysUserMapper;
import com.rj.issuehelper.model.dto.LoginDTO;
import com.rj.issuehelper.model.pojo.SysUser;
import com.rj.issuehelper.model.vo.LoginVO;
import com.rj.issuehelper.model.vo.UserVO;
import com.rj.issuehelper.service.IAuthService;
import com.rj.issuehelper.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public LoginVO login(LoginDTO dto) {
        String username = dto.getUsername() == null ? "" : dto.getUsername().trim();

        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));

        // 账号不存在与密码错误返回同一提示，避免账号枚举；日志里也只记账号，不记密码
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            log.warn("[登录] 失败：账号或密码错误 username={}", username);
            throw new BusinessException(ResultCode.BAD_REQUEST, "账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            log.warn("[登录] 失败：账号已禁用 username={} userId={}", username, user.getId());
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }

        UserAuth auth = new UserAuth(user.getId(), user.getUsername(), user.getRealName(), user.getRoleCode());
        log.info("[登录] 成功 username={} userId={} role={}", user.getUsername(), user.getId(), user.getRoleCode());
        return LoginVO.of(jwtUtil.createToken(auth), jwtUtil.getTimeout(), user);
    }

    @Override
    public UserVO me() {
        // 每次回查数据库：token 里的角色是签发时的快照，这里给前端最新状态
        SysUser user = sysUserMapper.selectById(UserContext.getUserId());
        if (user == null) {
            log.warn("[登录] 当前 token 对应的用户不存在 userId={}", UserContext.getUserId());
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return UserVO.of(user);
    }
}
