package com.landit.user.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.common.enums.Gender;
import com.landit.resume.service.ResumeService;
import com.landit.user.dto.AvatarUploadResponse;
import com.landit.user.dto.ResumeImagesResponse;
import com.landit.user.dto.UserCreateRequest;
import com.landit.user.dto.UserStatusResponse;
import com.landit.user.dto.UserInitResponse;
import com.landit.user.dto.UserUpdateRequest;
import com.landit.user.entity.User;
import com.landit.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * 用户服务实现类
 *
 * @author Azir
 */
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    private static final Long SINGLE_USER_ID = 1L;

    private final ResumeService resumeService;

    @Value("${app.upload.avatar-path:./data/avatars}")
    private String avatarUploadPath;

    /**
     * 获取用户状态
     */
    public UserStatusResponse getUserStatus() {
        User user = getById(SINGLE_USER_ID);
        // 用户不存在或未初始化
        if (user == null || user.getName() == null || user.getName().isEmpty()) {
            return UserStatusResponse.notExists();
        }
        // 用户存在，返回用户信息
        String genderValue = user.getGender() != null ? user.getGender().name() : null;
        return UserStatusResponse.exists(user.getId(), user.getName(), genderValue, user.getAvatar());
    }

    /**
     * 解析简历文件为图片列表
     * 用于初始化用户前的简历解析，返回图片列表供视觉模型提取用户信息
     */
    public ResumeImagesResponse parseResumeForInit(MultipartFile file) {
        // 检查用户是否已存在
        User existingUser = getById(SINGLE_USER_ID);
        if (existingUser != null && existingUser.getName() != null && !existingUser.getName().isEmpty()) {
            throw new RuntimeException("用户已存在，无法重复初始化");
        }
        // 将简历转换为图片列表
        List<String> images = resumeService.parseResumeToImages(file);
        return ResumeImagesResponse.of(images);
    }

    /**
     * 创建用户
     * 在 AI 解析简历获取用户信息后调用此方法创建用户
     */
    public UserInitResponse createUser(UserCreateRequest request) {
        // 检查用户是否已存在
        User existingUser = getById(SINGLE_USER_ID);
        if (existingUser != null && existingUser.getName() != null && !existingUser.getName().isEmpty()) {
            throw new RuntimeException("用户已存在，无法重复初始化");
        }
        // 创建用户
        User user = new User();
        user.setId(SINGLE_USER_ID);
        user.setName(request.getName());
        user.setGender(request.getGender());
        save(user);
        return UserInitResponse.builder()
                .name(user.getName())
                .gender(user.getGender())
                .build();
    }

    /**
     * 获取当前用户信息
     */
    public User getUserProfile() {
        return getById(SINGLE_USER_ID);
    }

    /**
     * 更新用户信息
     */
    public User updateUserProfile(UserUpdateRequest request) {
        User user = getById(SINGLE_USER_ID);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setName(request.getName());
        user.setGender(request.getGender());
        updateById(user);
        return user;
    }

    /**
     * 上传头像
     */
    public AvatarUploadResponse uploadAvatar(MultipartFile file) {
        User user = getById(SINGLE_USER_ID);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 创建上传目录
        Path uploadDir = Paths.get(avatarUploadPath);
        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("创建上传目录失败", e);
        }
        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".png";
        String filename = UUID.randomUUID().toString() + extension;
        // 保存文件
        Path filePath = uploadDir.resolve(filename);
        try {
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            throw new RuntimeException("保存头像文件失败", e);
        }
        // 更新用户头像URL
        String avatarUrl = "/landit/avatars/" + filename;
        user.setAvatar(avatarUrl);
        updateById(user);
        return AvatarUploadResponse.builder().avatarUrl(avatarUrl).build();
    }

}
