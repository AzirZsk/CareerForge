package com.landit.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 提示词配置类
 * 从 application.yml 中读取 AI 相关的提示词配置
 *
 * @author Azir
 */
@Data
@Component
@ConfigurationProperties(prefix = "landit.ai.prompt")
public class AIPromptProperties {

    /**
     * 简历解析提示词
     */
    private ResumePrompt resume = new ResumePrompt();

    @Data
    public static class ResumePrompt {
        /**
         * 从图片/文件解析简历的提示词
         */
        private String parse = """
                你是一个简历解析专家。请仔细分析图片中的简历内容，完成以下任务：

                1. 识别简历中的姓名和性别
                2. 将简历的全部内容原封不动地转换为Markdown格式（保留原有结构和信息，不要添加或删减任何内容）

                请以JSON格式返回，格式如下：
                {
                    "name": "姓名",
                    "gender": "性别（男/女/未知）",
                    "markdownContent": "markdown格式的简历完整内容"
                }

                注意事项：
                - markdownContent 必须包含简历中的所有文字信息，不得遗漏
                - 保持简历原有的层级结构和排版顺序
                - 使用适当的Markdown语法（标题、列表、加粗等）来还原简历的视觉层次
                - 如果无法识别姓名或性别，对应字段填"未知"
                - 只返回JSON，不要返回其他内容""";
    }

}
