package com.rj.issuehelper.controller;

import com.rj.issuehelper.common.Result;
import com.rj.issuehelper.model.dto.LoginDTO;
import com.rj.issuehelper.model.vo.LoginVO;
import com.rj.issuehelper.model.vo.UserVO;
import com.rj.issuehelper.service.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    /** 登录换 token（拦截器已排除该路径） */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    /** 前端刷新页面时用它恢复身份，避免把用户信息只存在 localStorage 里 */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(authService.me());
    }
}
