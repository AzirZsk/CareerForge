package com.landit.user.handler;

import com.landit.common.service.AIService;
import com.landit.common.service.FileToImageService;
import com.landit.resume.dto.ResumeParseResult;
import com.landit.user.dto.AvatarUploadResponse;
import com.landit.user.dto.ResumeImagesResponse;
import com.landit.user.dto.UserCreateRequest;
import com.landit.user.dto.UserInitResponse;
import com.landit.user.entity.User;
import com.landit.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * 用户业务编排处理器
 * 负责处理涉及跨服务调用、外部服务集成、文件处理等聚合操作
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserHandler {

    private final UserService userService;
    private final AIService aiService;
    private final FileToImageService fileToImageService;

    @Value("${app.upload.avatar-path:./data/avatars}")
    private String avatarUploadPath;

    /**
     * 初始化用户（上传简历文件解析）
     * 涉及：文件验证 -> AI解析 -> 创建用户
     *
     * @param file 上传的简历文件（图片或PDF）
     * @return 用户初始化响应
     */
    @Transactional(rollbackFor = Exception.class)
    public UserInitResponse initUser(MultipartFile file) {
        // 验证文件类型：仅支持图片和PDF
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        if (!fileToImageService.isSupported(contentType, filename)) {
            throw new IllegalArgumentException("仅支持图片（jpg/png/gif等）和PDF格式的文件");
        }

        // 调用 AI 服务解析简历
        ResumeParseResult parsedResume = aiService.parseResumeFromFile(file);

        // 创建用户
        UserCreateRequest createRequest = UserCreateRequest.builder()
                .name(parsedResume.getName())
                .gender(parsedResume.getGender())
                .build();

        UserInitResponse response = userService.createUser(createRequest);
        log.info("用户初始化完成: {}", response.getName());
        return response;
    }

    /**
     * 解析简历文件为图片列表
     * 用于初始化用户前的简历解析，返回图片列表供视觉模型提取用户信息
     *
     * @param file 上传的简历文件
     * @return 图片列表（Base64格式）
     */
    public ResumeImagesResponse parseResumeForInit(MultipartFile file) {
        // 检查用户是否已存在
        if (userService.isUserInitialized()) {
            throw new RuntimeException("用户已存在，无法重复初始化");
        }

        // 验证文件类型
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        if (!fileToImageService.isSupported(contentType, filename)) {
            throw new RuntimeException("不支持的文件格式，请上传图片（jpg/png）、PDF 或 Word 文档");
        }

        // 将文件转换为图片
        List<String> images = fileToImageService.convertToImages(file);
        if (images.isEmpty()) {
            throw new RuntimeException("文件转换失败，无法提取简历内容");
        }

        log.info("简历文件转换为 {} 张图片", images.size());
        return ResumeImagesResponse.of(images);
    }

    /**
     * 上传头像
     * 涉及：用户验证 -> 文件存储 -> 更新用户信息
     *
     * @param file 上传的头像文件
     * @return 头像上传响应
     */
    @Transactional(rollbackFor = Exception.class)
    public AvatarUploadResponse uploadAvatar(MultipartFile file) {
        // 获取当前用户
        User user = userService.getUserProfile();
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
            log.error("创建上传目录失败: {}", avatarUploadPath, e);
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
            log.error("保存头像文件失败: {}", filename, e);
            throw new RuntimeException("保存头像文件失败", e);
        }

        // 更新用户头像URL
        String avatarUrl = "/landit/avatars/" + filename;
        userService.updateUserAvatar(avatarUrl);

        log.info("头像上传成功: {}", avatarUrl);
        return AvatarUploadResponse.builder().avatarUrl(avatarUrl).build();
    }

}
