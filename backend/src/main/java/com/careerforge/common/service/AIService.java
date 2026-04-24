package com.careerforge.common.service;

import com.careerforge.common.config.prompt.ResumePromptProperties;
import com.careerforge.common.util.ChatClientHelper;
import com.careerforge.common.util.JsonParseHelper;
import com.careerforge.resume.dto.ResumeStructuredData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * AI 服务
 * 基于 Spring AI OpenAI 实现大模型对话能力
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final ChatClient chatClient;
    private final FileToImageService fileToImageService;
    private final ResumePromptProperties promptProperties;

    // ==================== 简历解析相关方法 ====================

    /**
     * 从文件中解析简历信息
     * 支持图片、PDF等格式的简历文件，使用大模型进行多模态识别
     * 使用 ChatClientHelper 统一调用，自动重试 + Schema 约束
     *
     * @param file 上传的简历文件（图片或PDF）
     * @return 解析后的简历信息
     */
    public ResumeStructuredData parseResumeFromFile(MultipartFile file) {
        log.info("开始解析简历文件: {}", file.getOriginalFilename());
        // 将文件转换为Media列表
        List<Media> mediaList = fileToImageService.convertToMedia(file);
        // 从配置中获取解析提示词
        String parsePrompt = promptProperties.getParse();
        // 统一调用大模型（多模态版本）
        ResumeStructuredData result = ChatClientHelper.callAndParse(
                chatClient, null, parsePrompt, mediaList, ResumeStructuredData.class);
        log.info("简历文件解析完成:{}", JsonParseHelper.toJsonString(result));
        return result;
    }

}
