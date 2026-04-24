package com.careerforge.common.config.prompt;

import lombok.Data;

/**
 * 提示词配置基类
 * 用于前缀缓存优化，将提示词拆分为固定部分和动态部分
 *
 * @author Azir
 */
@Data
public class PromptConfig {
    /**
     * 系统提示词（固定部分，可被缓存）
     * 包含：角色定义、任务说明、评估维度、输出格式、要求列表
     */
    private String systemPrompt;

    /**
     * 用户提示词模板（动态部分）
     * 包含：目标岗位、简历内容等动态变量
     * 使用 {variableName} 格式的占位符
     */
    private String userPromptTemplate;

    /**
     * 如果未配置值，则应用默认提示词
     */
    public static PromptConfig ensureDefaults(PromptConfig config, String defaultSystem, String defaultTemplate) {
        if (config.getSystemPrompt() == null || config.getSystemPrompt().isBlank()) {
            config.setSystemPrompt(defaultSystem);
        }
        if (config.getUserPromptTemplate() == null || config.getUserPromptTemplate().isBlank()) {
            config.setUserPromptTemplate(defaultTemplate);
        }
        return config;
    }
}
