package com.landit.common.schema;

import com.landit.common.enums.SectionType;
import com.landit.common.util.JsonSchemaBuilder;
import com.landit.common.util.SchemaGenerator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Section Schema 注册表
 * 根据 SectionType 枚举动态组装完整简历 Schema
 *
 * @author Azir
 */
@Component
public class SectionSchemaRegistry {

    /**
     * 构建完整的简历解析 Schema
     * 包含根级字段和各模块字段
     *
     * @return 完整的简历 JSON Schema
     */
    public Map<String, Object> buildResumeSchema() {
        return JsonSchemaBuilder.objectSchema(
                JsonSchemaBuilder.props()
                        // 根级必填字段
                        .put("name", JsonSchemaBuilder.stringSchema("姓名"))
                        .put("gender", JsonSchemaBuilder.enumSchema("性别", List.of("男", "女", "未知")))
                        .put("markdownContent", JsonSchemaBuilder.stringSchema("markdown格式的简历完整内容"))
                        // 基本信息模块
                        .put("basicInfo", buildBasicInfoSchema())
                        // 教育经历模块
                        .put("education", buildArraySchema(SectionType.EDUCATION))
                        // 工作经历模块
                        .put("work", buildArraySchema(SectionType.WORK))
                        // 项目经历模块
                        .put("projects", buildArraySchema(SectionType.PROJECT))
                        // 技能模块（字符串数组）
                        .put("skills", JsonSchemaBuilder.arraySchema(JsonSchemaBuilder.stringSchema("技能")))
                        // 证书模块
                        .put("certificates", buildArraySchema(SectionType.CERTIFICATE)),
                List.of("name", "gender", "markdownContent")
        );
    }

    /**
     * 构建基本信息 Schema
     */
    private Map<String, Object> buildBasicInfoSchema() {
        return SchemaGenerator.fromClass(SectionType.BASIC_INFO.getSchemaClass());
    }

    /**
     * 根据 SectionType 构建数组类型的 Schema
     *
     * @param sectionType 模块类型
     * @return 数组类型 Schema
     */
    private Map<String, Object> buildArraySchema(SectionType sectionType) {
        Class<?> schemaClass = sectionType.getSchemaClass();
        if (schemaClass == null) {
            return JsonSchemaBuilder.arraySchema(JsonSchemaBuilder.stringSchema(sectionType.getDescription()));
        }
        Map<String, Object> itemSchema = SchemaGenerator.fromClass(schemaClass);
        return JsonSchemaBuilder.arraySchema(itemSchema);
    }

    /**
     * 根据 SectionType 构建单个模块的 Schema
     *
     * @param sectionType 模块类型
     * @return 模块 Schema
     */
    public Map<String, Object> buildSchema(SectionType sectionType) {
        Class<?> schemaClass = sectionType.getSchemaClass();
        if (schemaClass == null) {
            return JsonSchemaBuilder.objectSchema(JsonSchemaBuilder.props());
        }
        return SchemaGenerator.fromClass(schemaClass);
    }
}
