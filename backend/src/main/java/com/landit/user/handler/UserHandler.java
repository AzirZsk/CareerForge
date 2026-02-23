package com.landit.user.handler;

import com.landit.common.exception.BusinessException;
import com.landit.common.service.AIService;
import com.landit.common.service.FileToImageService;
import com.landit.resume.dto.ResumeParseResult;
import com.landit.resume.service.ResumeService;
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

    private static final String UNSUPPORTED_FILE_TYPE_MSG = "不支持的文件格式，请上传图片（jpg/png）、PDF 或 Word 文档";
    private static final String USER_ALREADY_EXISTS_MSG = "用户已存在，无法重复初始化";
    private static final String CONVERSION_FAILED_MSG = "文件转换失败，无法提取简历内容";

    private final UserService userService;
    private final AIService aiService;
    private final FileToImageService fileToImageService;
    private final ResumeService resumeService;

    @Value("${app.upload.avatar-path:./data/avatars}")
    private String avatarUploadPath;

    /**
     * 初始化用户（上传简历文件解析）
     * 涉及：文件验证 -> AI解析 -> 创建用户 -> 创建简历记录
     *
     * @param file 上传的简历文件（图片或PDF）
     * @return 用户初始化响应
     */
    @Transactional(rollbackFor = Exception.class)
    public UserInitResponse initUser(MultipartFile file) {
        validateFileType(file);

        ResumeParseResult parsedResume = aiService.parseResumeFromFile(file);

        UserCreateRequest createRequest = UserCreateRequest.builder()
                .name(parsedResume.getName())
                .gender(parsedResume.getGender())
                .build();

        UserInitResponse response = userService.createUser(createRequest);
        log.info("用户初始化完成: {}", response.getName());

        resumeService.createResumeFromParsedData(response.getId(), parsedResume);

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
        if (userService.isUserInitialized()) {
            throw new BusinessException(USER_ALREADY_EXISTS_MSG);
        }

        validateFileType(file);

        List<String> images = fileToImageService.convertToImages(file);
        if (images.isEmpty()) {
            throw new BusinessException(CONVERSION_FAILED_MSG);
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
        User user = userService.getUserProfile();
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        Path uploadDir = ensureDirectoryExists(Paths.get(avatarUploadPath));
        String filename = generateFilename(file.getOriginalFilename());
        Path filePath = uploadDir.resolve(filename);

        saveFile(file, filePath);

        String avatarUrl = "/landit/avatars/" + filename;
        userService.updateUserAvatar(avatarUrl);

        log.info("头像上传成功: {}", avatarUrl);
        return AvatarUploadResponse.builder().avatarUrl(avatarUrl).build();
    }

    // ==================== Private Helper Methods ====================

    private void validateFileType(MultipartFile file) {
        if (!fileToImageService.isSupported(file.getContentType(), file.getOriginalFilename())) {
            throw new BusinessException(UNSUPPORTED_FILE_TYPE_MSG);
        }
    }

    private Path ensureDirectoryExists(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            return path;
        } catch (IOException e) {
            log.error("创建上传目录失败: {}", path, e);
            throw BusinessException.serverError("创建上传目录失败");
        }
    }

    private String generateFilename(String originalFilename) {
        String extension = ".png";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    private void saveFile(MultipartFile file, Path path) {
        try {
            file.transferTo(path.toFile());
        } catch (IOException e) {
            log.error("保存文件失败: {}", path, e);
            throw BusinessException.serverError("保存文件失败");
        }
    }

}
