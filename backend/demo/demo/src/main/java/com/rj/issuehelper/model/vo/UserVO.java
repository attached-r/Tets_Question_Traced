package com.rj.issuehelper.model.vo;

import com.rj.issuehelper.model.enums.Role;
import com.rj.issuehelper.model.pojo.SysUser;
import lombok.Data;

/**
 * 用户信息（对外）。永远不包含 password。
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String roleCode;
    private String roleLabel;

    public static UserVO of(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRoleCode(user.getRoleCode());
        vo.setRoleLabel(Role.labelOf(user.getRoleCode()));
        return vo;
    }
}
