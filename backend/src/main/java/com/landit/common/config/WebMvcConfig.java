package com.landit.common.config;

import com.landit.common.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 * 注册 JWT 拦截器并配置放行路径
 * 静态资源由 Nginx 直接 serve，Spring Boot 只处理 API 请求
 *
 * @author Azir
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    /**
     * 注册 JWT 拦截器
     * 拦截所有 /landit/** 路径，排除认证接口和 Swagger 文档
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/landit/**")
                .excludePathPatterns(
                        "/landit/auth/**",           // 认证接口放行
                        "/landit/swagger-ui/**",     // Swagger UI 放行
                        "/landit/v3/api-docs/**"     // API 文档放行
                );
    }
}
