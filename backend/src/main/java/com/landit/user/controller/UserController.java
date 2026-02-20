package com.landit.user.controller;

import com.landit.common.response.ApiResponse;
import com.landit.user.dto.AvatarUploadResponse;
import com.landit.user.dto.UserExistsResponse;
import com.landit.user.dto.UserInitRequest;
import com.landit.user.dto.UserUpdateRequest;
import com.landit.user.entity.User;
import com.landit.user.service.UserService;
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
 *
 * @author Azir
 */
@Tag(name = "user", description = "用户管理")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "检查用户是否存在")
    @GetMapping("/exists")
    public ApiResponse<UserExistsResponse> checkUserExists() {
        return ApiResponse.success(userService.checkUserExists());
    }

    @Operation(summary = "初始化用户")
    @PostMapping("/init")
    public ApiResponse<User> initUser(@Valid @RequestBody UserInitRequest request) {
        return ApiResponse.success(userService.initUser(request));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/profile")
    public ApiResponse<User> getUserProfile() {
        return ApiResponse.success(userService.getUserProfile());
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/profile")
    public ApiResponse<User> updateUserProfile(@Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userService.updateUserProfile(request));
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public ApiResponse<AvatarUploadResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(userService.uploadAvatar(file));
    }

}
