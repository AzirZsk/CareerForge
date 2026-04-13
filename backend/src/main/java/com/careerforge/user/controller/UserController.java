package com.careerforge.user.controller;

import com.careerforge.common.response.ApiResponse;
import com.careerforge.user.dto.AvatarUploadResponse;
import com.careerforge.user.dto.ResumeImagesResponse;
import com.careerforge.user.dto.UserCreateRequest;
import com.careerforge.user.dto.UserStatusResponse;
import com.careerforge.user.dto.UserInitResponse;
import com.careerforge.user.dto.UserUpdateRequest;
import com.careerforge.user.entity.User;
import com.careerforge.user.handler.UserHandler;
import com.careerforge.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户管理控制器
 * 仅负责接收 HTTP 请求和返回响应，业务逻辑由 Handler 处理
 *
 * @author Azir
 */
@Tag(name = "user", description = "用户管理")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserHandler userHandler;

    @Operation(summary = "检查用户状态")
    @GetMapping("/status")
    public ApiResponse<UserStatusResponse> getUserStatus() {
        return ApiResponse.success(userService.getUserStatus());
    }

    @Operation(summary = "初始化用户（上传简历文件解析）")
    @PostMapping("/init")
    public ApiResponse<UserInitResponse> initUser(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(userHandler.initUser(file));
    }

}
