package com.rj.issuehelper.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户的鉴权快照。
 * <p>
 * 由 JWT 载荷还原而来，只携带身份与角色，不携带权限列表：
 * 本项目权限点很少，按角色判断比逐条比对权限码更快也更直白。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAuth {

    private Long userId;
    private String username;
    private String realName;
    private String roleCode;
}
