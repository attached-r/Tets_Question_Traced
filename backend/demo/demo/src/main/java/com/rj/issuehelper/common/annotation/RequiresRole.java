package com.rj.issuehelper.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口角色鉴权：标注在 Controller 方法或类上。
 * <p>
 * 由 AuthInterceptor 读取，要求当前登录用户的角色在 value 中，
 * 否则直接拒绝（403），不进入业务方法。方法上的注解优先于类上的注解。
 * <p>
 * 注意：这里只做"角色级"拦截；"这条记录是不是我的"这类数据归属校验
 * 必须写在 Service 里（贴着数据的唯一出口），否则新增调用方就会漏掉。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {

    /** 允许的角色码，如 SUPPORT；多个时任一个满足即可 */
    String[] value();
}
