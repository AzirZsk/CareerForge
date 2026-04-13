package com.careerforge.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Web 相关工具类
 *
 * @author Azir
 */
public class WebUtils {

    /**
     * 获取客户端真实IP
     * 支持多级代理，优先从 X-Forwarded-For 获取
     *
     * @param request HTTP 请求
     * @return IP 地址，无法获取时返回 "unknown"
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多级代理的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }
}
