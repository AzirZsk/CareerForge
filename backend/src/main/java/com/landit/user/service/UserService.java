package com.landit.user.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.common.enums.Gender;
import com.landit.user.dto.AvatarUploadResponse;
import com.landit.user.dto.UserExistsResponse;
import com.landit.user.dto.UserInitRequest;
import com.landit.user.dto.UserUpdateRequest;
import com.landit.user.entity.User;
import com.landit.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Value("${app.upload.avatar-path:./data/avatars}")
    private String avatarUploadPath;

    /**
     * 检查用户是否存在
     */
    public UserExistsResponse checkUserExists() {
        User user = getById(SINGLE_USER_ID);
        return UserExistsResponse.of(user != null && user.getName() != null && !user.getName().isEmpty());
    }

    /**
     * 初始化用户
     */
    public User initUser(UserInitRequest request) {
        User user = new User();
        user.setId(SINGLE_USER_ID);
        user.setName(request.getName());
        user.setGender(request.getGender());
        save(user);
        return user;
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
