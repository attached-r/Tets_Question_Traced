package com.rj.issuehelper.model.vo;

import com.rj.issuehelper.model.pojo.SysUser;
import lombok.Data;

@Data
public class LoginVO {

    private String token;

    /** 有效期，单位秒 */
    private long expiresIn;

    private UserVO user;

    public static LoginVO of(String token, long expiresIn, SysUser user) {
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setExpiresIn(expiresIn);
        vo.setUser(UserVO.of(user));
        return vo;
    }
}
