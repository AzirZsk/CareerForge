package com.landit.user.controller;

import com.landit.common.response.ApiResponse;
import com.landit.common.service.AIService;
import com.landit.common.service.FileToImageService;
import com.landit.resume.dto.ResumeParseResult;
import com.landit.user.dto.AvatarUploadResponse;
import com.landit.user.dto.ResumeImagesResponse;
import com.landit.user.dto.UserCreateRequest;
import com.landit.user.dto.UserStatusResponse;
import com.landit.user.dto.UserInitResponse;
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
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AIService aiService;
    private final FileToImageService fileToImageService;

    @Operation(summary = "检查用户状态")
    @GetMapping("/status")
    public ApiResponse<UserStatusResponse> getUserStatus() {
        return ApiResponse.success(userService.getUserStatus());
    }

    @Operation(summary = "初始化用户（上传简历文件解析）")
    @PostMapping("/init")
    public ApiResponse<UserInitResponse> initUser(@RequestParam("file") MultipartFile file) {
        // 验证文件类型：仅支持图片和PDF
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        if (!fileToImageService.isSupported(contentType, filename)) {
            throw new IllegalArgumentException("仅支持图片（jpg/png/gif等）和PDF格式的文件");
        }
        // 调用AI服务解析简历
        ResumeParseResult parsedResume = aiService.parseResumeFromFile(file);
        // 创建用户
        UserCreateRequest createRequest = UserCreateRequest.builder()
                .name(parsedResume.getName())
                .gender(parsedResume.getGender())
                .build();
        UserInitResponse response = userService.createUser(createRequest);
        return ApiResponse.success(response);
    }

    @Operation(summary = "解析简历为图片列表（用于初始化用户）")
    @PostMapping("/parse-resume")
    public ApiResponse<ResumeImagesResponse> parseResumeForInit(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(userService.parseResumeForInit(file));
    }

    @Operation(summary = "创建用户（AI解析简历后调用）")
    @PostMapping("/create")
    public ApiResponse<UserInitResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userService.createUser(request));
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
