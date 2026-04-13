package com.landit.common.filter;

import com.landit.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * JWT 认证过滤器
 * 在 Servlet 容器层面拦截请求，验证 Token 并设置用户ID到 Request Attribute
 * 比 Interceptor 更早介入，能拦截所有请求（包括 forward、error dispatch 等）
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    // 不需要认证的路径前缀
    private static final Set<String> EXCLUDED_PATH_PREFIXES = Set.of(
            "/landit/auth/",
            "/landit/swagger-ui/",
            "/landit/v3/api-docs/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 跨域预检请求放行
        if ("OPTIONS".equals(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestURI = request.getRequestURI();

        // 不需要认证的路径直接放行
        if (isExcludedPath(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 从 Header 获取 Token
        String authorizationHeader = request.getHeader("Authorization");
        String token = null;

        // 优先从 Header 获取（普通HTTP请求）
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
        } else {
            // EventSource 和 WebSocket 不支持自定义 Header，从 URL 参数获取
            token = request.getParameter("token");
        }

        if (token == null || token.isEmpty()) {
            sendUnauthorizedResponse(response, "未登录或登录已过期");
            return;
        }

        try {
            // 验证 Token 并获取用户ID
            String userId = jwtUtil.verifyToken(token);
            // 设置用户ID到 Request Attribute，供后续业务使用
            request.setAttribute("currentUserId", userId);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage());
            sendUnauthorizedResponse(response, "Token验证失败");
        }
    }

    /**
     * 判断请求路径是否在排除列表中
     */
    private boolean isExcludedPath(String requestURI) {
        for (String prefix : EXCLUDED_PATH_PREFIXES) {
            if (requestURI.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 发送 401 未授权响应
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
    }
}
