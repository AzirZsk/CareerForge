package com.careerforge.common.util;

import com.careerforge.common.exception.BusinessException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 安全工具类
 * 提供获取当前登录用户ID的方法
 *
 * @author Azir
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户ID
     * 从 Request Attribute 中获取（由 JwtAuthenticationFilter 设置）
     *
     * @return 用户ID
     * @throws BusinessException 用户未登录
     */
    public static String getCurrentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BusinessException("用户未登录");
        }
        HttpServletRequest request = attributes.getRequest();
        String userId = (String) request.getAttribute("currentUserId");
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        return userId;
    }

    /**
     * 检查用户是否已登录
     *
     * @return true 如果已登录，否则 false
     */
    public static boolean isLoggedIn() {
        try {
            getCurrentUserId();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
