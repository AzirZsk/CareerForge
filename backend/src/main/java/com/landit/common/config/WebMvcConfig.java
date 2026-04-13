package com.landit.common.config;

import com.landit.common.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 * 注册 JWT 认证过滤器（Servlet Filter 层面，比 Interceptor 更早拦截）
 * 静态资源由 Nginx 直接 serve，Spring Boot 只处理 API 请求
 *
 * @author Azir
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 注册 JWT 认证过滤器
     * 拦截所有请求，在 Servlet 容器层面进行认证校验
     * URL Pattern 相对于 context-path，所以用 /* 而不是 /landit/*
     * 路径排除逻辑在 Filter 内部处理
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration() {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(jwtAuthenticationFilter);
        registration.addUrlPatterns("/*");  // 相对于 context-path /landit，实际匹配 /landit/*
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("jwtAuthenticationFilter");
        return registration;
    }
}
