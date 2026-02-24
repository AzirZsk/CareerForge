package com.landit.common.schema;

import com.landit.common.enums.SectionType;
import com.landit.common.util.JsonSchemaBuilder;
import com.landit.common.util.SchemaGenerator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
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
     * 动态遍历 SectionType 枚举，自动包含所有区块类型
     *
     * @return 完整的简历 JSON Schema
     */
    public Map<String, Object> buildResumeSchema() {
        Map<String, Object> props = new LinkedHashMap<>();

        // 根级必填字段
        props.put("name", JsonSchemaBuilder.stringSchema("姓名"));
        props.put("gender", JsonSchemaBuilder.enumSchema("性别", List.of("男", "女", "未知")));
        props.put("markdownContent", JsonSchemaBuilder.stringSchema("markdown格式的简历完整内容"));

        // 动态遍历 SectionType 枚举构建各区块 Schema
        for (SectionType type : SectionType.values()) {
            String fieldName = type.getSchemaFieldName();
            if (fieldName == null) {
                continue; // 跳过不包含在 Schema 中的类型（如 RAW_TEXT）
            }

            if (type == SectionType.BASIC_INFO) {
                // 基本信息是单个对象
                props.put(fieldName, buildObjectSchema(type));
            } else if (type.getSchemaClass() != null) {
                // 有 schemaClass 的是对象数组
                props.put(fieldName, buildArraySchema(type));
            } else {
                // 无 schemaClass 的是简单类型数组（如 SKILLS 是字符串数组）
                props.put(fieldName, JsonSchemaBuilder.arraySchema(
                        JsonSchemaBuilder.stringSchema(type.getDescription())));
            }
        }

        return JsonSchemaBuilder.objectSchema(props, List.of("name", "gender", "markdownContent"));
    }

    /**
     * 构建单个对象的 Schema
     */
    private Map<String, Object> buildObjectSchema(SectionType sectionType) {
        Class<?> schemaClass = sectionType.getSchemaClass();
        if (schemaClass == null) {
            return JsonSchemaBuilder.objectSchema(JsonSchemaBuilder.props());
        }
        return SchemaGenerator.fromClass(schemaClass);
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
