package com.rj.issuehelper.model.enums;

import com.rj.issuehelper.common.ResultCode;
import com.rj.issuehelper.exception.BusinessException;
import lombok.Getter;

/**
 * 使用者角色。
 * <p>
 * 单角色模型：不做多对多。ADMIN 本迭代只在枚举与权限矩阵中预留，
 * 不提供任何管理员页面（有意的范围控制）。
 */
@Getter
public enum Role {

    EMPLOYEE("EMPLOYEE", "员工"),
    SUPPORT("SUPPORT", "技术支持"),
    ADMIN("ADMIN", "管理员");

    /** 供注解使用（注解属性要求编译期常量） */
    public static final String EMPLOYEE_CODE = "EMPLOYEE";
    public static final String SUPPORT_CODE = "SUPPORT";
    public static final String ADMIN_CODE = "ADMIN";

    private final String code;
    private final String label;

    Role(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static Role of(String code) {
        for (Role role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        throw new BusinessException(ResultCode.FORBIDDEN, "未知角色: " + code);
    }

    public static String labelOf(String code) {
        for (Role role : values()) {
            if (role.code.equals(code)) {
                return role.label;
            }
        }
        return code;
    }
}
