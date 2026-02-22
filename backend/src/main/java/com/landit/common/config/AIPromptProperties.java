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
                你是一个专业的简历解析专家。请仔细分析图片中的简历内容，提取以下结构化信息：

                ## 需要提取的信息

                ### 1. 基本信息 (basicInfo)
                - name: 姓名
                - gender: 性别（男/女/未知）
                - phone: 联系电话
                - email: 邮箱地址
                - targetPosition: 求职意向/目标岗位
                - summary: 个人简介/自我评价

                ### 2. 教育经历 (education) - 数组
                - school: 学校名称
                - degree: 学历（本科/硕士/博士/大专/高中等）
                - major: 专业
                - period: 时间段（如：2020.09-2024.06）

                ### 3. 工作经历 (work) - 数组
                - company: 公司名称
                - position: 职位
                - period: 时间段
                - description: 工作描述/主要职责

                ### 4. 项目经验 (projects) - 数组
                - name: 项目名称
                - role: 在项目中的角色
                - period: 时间段
                - description: 项目描述
                - achievements: 项目成果/亮点（数组）

                ### 5. 技能 (skills) - 字符串数组
                列出简历中提到的所有专业技能，如编程语言、框架、工具等

                ### 6. 证书/荣誉 (certificates) - 数组
                - name: 证书/荣誉名称
                - date: 获得日期

                ### 7. 原始内容 (markdownContent)
                将简历的全部内容原封不动地转换为Markdown格式

                ## 注意事项
                - 如果某个字段在简历中未找到，设为空字符串或空数组
                - 日期格式尽量保持原样
                - 保持简历原有的层级结构和排版顺序
                - markdownContent 必须包含简历中的所有文字信息
                - 只返回JSON，不要返回其他内容""";
    }

}
