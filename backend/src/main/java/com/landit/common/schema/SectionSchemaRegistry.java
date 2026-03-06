package com.landit.common.schema;

import com.landit.common.enums.SectionType;
import com.landit.common.util.JsonSchemaBuilder;
import com.landit.common.util.SchemaGenerator;
import com.landit.resume.dto.ResumeStructuredData;
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
     * 直接使用 SchemaGenerator 从 ResumeStructuredData 类生成 Schema
     *
     * @return 完整的简历 JSON Schema
     */
    public Map<String, Object> buildResumeSchema() {
        return SchemaGenerator.fromClass(
                ResumeStructuredData.class
        );
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
