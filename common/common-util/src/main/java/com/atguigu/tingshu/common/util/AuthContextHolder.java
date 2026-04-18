package com.atguigu.tingshu.common.util;

/**
 * 获取当前用户信息帮助类
 */
public class AuthContextHolder {

    private static ThreadLocal<Long> userId = new ThreadLocal<Long>();

    public static void setUserId(Long _userId) {
        userId.set(_userId);
    }

    public static Long getUserId() {
        //Todo 获取用户ID
        return 114514L;
//        return userId.get();
    }

    public static void removeUserId() {
        userId.remove();
    }

}
