package com.caishen.foodlab.security;

import com.caishen.foodlab.common.BusinessException;
import com.caishen.foodlab.common.ResultCode;

/**
 * 当前登录身份（线程级）
 */
public final class AuthContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> ADMIN_ID = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void setUserId(Long id) {
        USER_ID.set(id);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static Long requireUserId() {
        Long id = USER_ID.get();
        if (id == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        return id;
    }

    public static void setAdminId(Long id) {
        ADMIN_ID.set(id);
    }

    public static Long getAdminId() {
        return ADMIN_ID.get();
    }

    public static Long requireAdminId() {
        Long id = ADMIN_ID.get();
        if (id == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录管理后台");
        }
        return id;
    }

    public static void clear() {
        USER_ID.remove();
        ADMIN_ID.remove();
    }
}
