package com.careerforge.common.util;

/**
 * 用户上下文持有者
 * 通过 ThreadLocal 在线程间传播用户ID
 * 配合 Schedulers.onScheduleHook 将 userId 从 HTTP 线程传播到 Reactor 异步线程
 *
 * @author Azir
 */
public final class UserContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    private UserContext() {}

    public static void setUserId(String userId) {
        USER_ID.set(userId);
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
