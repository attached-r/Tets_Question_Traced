package com.rj.issuehelper.model.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表 sys_user。
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 密文，任何情况下都不对外返回 */
    private String password;

    private String realName;

    /** 角色码：EMPLOYEE / SUPPORT / ADMIN */
    private String roleCode;

    /** 0 禁用 1 启用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
