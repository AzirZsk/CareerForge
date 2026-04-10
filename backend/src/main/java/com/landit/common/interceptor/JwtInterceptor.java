package com.landit.common.interceptor;

import com.landit.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 认证拦截器
 * 验证请求头中的 Token，并将用户ID设置到 Request Attribute
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 跨域预检请求放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 从 Header 获取 Token
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            send401Response(response, "未登录或登录已过期");
            return false;
        }

        try {
            // 提取 Token（去掉 "Bearer " 前缀）
            String token = authorizationHeader.substring(7);
            // 验证 Token 并获取用户ID
            String userId = jwtUtil.verifyToken(token);
            // 设置用户ID到 Request Attribute，供后续业务使用
            request.setAttribute("currentUserId", userId);
            return true;
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage());
            send401Response(response, "Token验证失败");
            return false;
        }
    }

    /**
     * 发送 401 未授权响应
     *
     * @param response HTTP 响应
     * @param message 错误消息
     */
    private void send401Response(HttpServletResponse response, String message) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
    }
}
