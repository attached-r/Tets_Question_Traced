package com.rj.issuehelper.service;

import com.rj.issuehelper.model.dto.LoginDTO;
import com.rj.issuehelper.model.vo.LoginVO;
import com.rj.issuehelper.model.vo.UserVO;

public interface IAuthService {

    LoginVO login(LoginDTO dto);

    /** 当前登录用户信息（前端刷新页面时恢复身份） */
    UserVO me();
}
