package com.landit.user.handler;

import com.landit.common.exception.BusinessException;
import com.landit.common.service.AIService;
import com.landit.common.service.FileToImageService;
import com.landit.resume.dto.ResumeStructuredData;
import com.landit.resume.service.ResumeService;
import com.landit.user.dto.UserCreateRequest;
import com.landit.user.dto.UserInitResponse;
import com.landit.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final UserService userService;
    private final AIService aiService;
    private final FileToImageService fileToImageService;
    private final ResumeService resumeService;


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

        ResumeStructuredData parsedResume = aiService.parseResumeFromFile(file);

        UserCreateRequest createRequest = UserCreateRequest.builder()
                .name(parsedResume.getBasicInfo().getName())
                .build();

        UserInitResponse response = userService.createUser(createRequest);
        log.info("用户初始化完成: {}", response.getName());

        resumeService.createResumeFromParsedData(response.getId(), parsedResume);

        return response;
    }

    // ==================== Private Helper Methods ====================

    private void validateFileType(MultipartFile file) {
        if (!fileToImageService.isSupported(file.getContentType(), file.getOriginalFilename())) {
            throw new BusinessException(UNSUPPORTED_FILE_TYPE_MSG);
        }
    }


}
