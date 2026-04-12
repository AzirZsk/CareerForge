package com.landit.auth.controller;

import com.landit.auth.dto.LoginRequest;
import com.landit.auth.dto.LoginResponse;
import com.landit.auth.dto.RegisterRequest;
import com.landit.auth.dto.RegisterResponse;
import com.landit.auth.service.AuthService;
import com.landit.common.response.ApiResponse;
import com.landit.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 认证控制器
 * 提供用户注册、登录、登出 API 接口
 *
 * @author Azir
 */
@Tag(name = "认证接口", description = "用户注册、登录、登出")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @param httpServletRequest HTTP 请求
     * @return 注册响应
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpServletRequest
    ) {
        RegisterResponse response = authService.register(request, httpServletRequest);
        return ApiResponse.success(response);
    }

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应（包含 Token 和用户信息）
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success(response);
    }

    /**
     * 用户登出
     *
     * @return 成功响应
     */
    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        String userId = SecurityUtils.getCurrentUserId();
        authService.logout(userId);
        return ApiResponse.success();
    }
}
