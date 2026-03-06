package com.landit.common.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON Schema 构建工具类
 * 用于构建 DashScope 大模型的 JSON Schema 配置
 *
 * @author Azir
 */
public final class JsonSchemaBuilder {

    private JsonSchemaBuilder() {
    }

    /**
     * 创建属性构建器
     */
    public static SchemaPropsBuilder props() {
        return new SchemaPropsBuilder();
    }

    /**
     * 创建字符串类型 Schema
     */
    public static Map<String, Object> stringSchema(String description) {
        return Map.of("type", "string", "description", description);
    }

    /**
     * 创建整数类型 Schema
     */
    public static Map<String, Object> integerSchema(String description) {
        return Map.of("type", "integer", "description", description);
    }

    /**
     * 创建枚举类型 Schema
     */
    public static Map<String, Object> enumSchema(String description, List<String> values) {
        return Map.of("type", "string", "description", description, "enum", values);
    }

    /**
     * 创建数组类型 Schema
     */
    public static Map<String, Object> arraySchema(Map<String, Object> items) {
        return Map.of("type", "array", "items", items);
    }

    /**
     * 创建对象类型 Schema（无必填字段）
     */
    public static Map<String, Object> objectSchema(SchemaPropsBuilder properties) {
        return Map.of("type", "object", "properties", properties.build());
    }

    /**
     * 创建对象类型 Schema（含必填字段）
     */
    public static Map<String, Object> objectSchema(SchemaPropsBuilder properties, List<String> required) {
        return Map.of("type", "object", "properties", properties.build(), "required", required);
    }

    /**
     * 创建对象类型 Schema（含描述，无必填字段）
     */
    public static Map<String, Object> objectSchema(SchemaPropsBuilder properties, String description) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("description", description);
        schema.put("properties", properties.build());
        return Map.copyOf(schema);
    }

    /**
     * 创建对象类型 Schema（含描述和必填字段）
     */
    public static Map<String, Object> objectSchema(SchemaPropsBuilder properties, List<String> required, String description) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("description", description);
        schema.put("properties", properties.build());
        schema.put("required", required);
        return Map.copyOf(schema);
    }


    /**
     * 创建通用对象类型 Schema（仅含描述，允许任意属性）
     * 用于 Map<String, Object> 等动态结构
     */
    public static Map<String, Object> objectSchema(String description) {
        return Map.of("type", "object", "description", description);
    }

    /**
     * Schema 属性构建器
     */
    public static class SchemaPropsBuilder {
        private final Map<String, Object> props = new LinkedHashMap<>();

        public SchemaPropsBuilder put(String key, Map<String, Object> value) {
            props.put(key, value);
            return this;
        }

        public Map<String, Object> build() {
            return props;
        }
    }
}
