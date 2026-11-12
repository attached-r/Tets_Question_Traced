package com.rj.issuehelper.common;

import com.rj.issuehelper.common.ResultCode;
import com.rj.issuehelper.exception.BusinessException;

/**
 * 当前登录用户上下文，基于 ThreadLocal。
 * <p>
 * 由 {@code AuthInterceptor} 在校验通过后写入，请求结束时必须清理：
 * Web 容器线程会复用，不清理会造成串号（A 的请求读到 B 的身份）。
 */
public final class UserContext {

    private static final ThreadLocal<UserAuth> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(UserAuth userAuth) {
        CURRENT_USER.set(userAuth);
    }

    /** 当前登录用户快照，未登录时为 null */
    public static UserAuth get() {
        return CURRENT_USER.get();
    }

    /** 当前登录用户快照，未登录时抛 401（正常链路不会走到，属于兜底） */
    public static UserAuth require() {
        UserAuth auth = CURRENT_USER.get();
        if (auth == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return auth;
    }

    public static Long getUserId() {
        return require().getUserId();
    }

    public static String getRoleCode() {
        return require().getRoleCode();
    }

    public static boolean isSupport() {
        UserAuth auth = CURRENT_USER.get();
        return auth != null && "SUPPORT".equals(auth.getRoleCode());
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
