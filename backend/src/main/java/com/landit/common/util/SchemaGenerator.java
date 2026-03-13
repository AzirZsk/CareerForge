package com.landit.common.util;

import com.landit.common.annotation.SchemaField;
import com.landit.common.enums.Gender;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Schema 生成器
 * 通过反射读取 @SchemaField 注解自动生成 JSON Schema
 *
 * @author Azir
 */
public final class SchemaGenerator {

    private SchemaGenerator() {
    }

    /**
     * 从类生成 JSON Schema
     *
     * @param clazz 带有 @SchemaField 注解的类
     * @return JSON Schema Map
     */
    public static Map<String, Object> fromClass(Class<?> clazz) {
        return fromClass(clazz, new ArrayList<>(), null);
    }

    /**
     * 从类生成 JSON Schema（含必填字段）
     *
     * @param clazz    带有 @SchemaField 注解的类
     * @param required 必填字段列表
     * @return JSON Schema Map
     */
    public static Map<String, Object> fromClass(Class<?> clazz, List<String> required) {
        return fromClass(clazz, required, null);
    }

    /**
     * 从类生成 JSON Schema（含描述）
     *
     * @param clazz       带有 @SchemaField 注解的类
     * @param description 对象级别描述
     * @return JSON Schema Map
     */
    public static Map<String, Object> fromClass(Class<?> clazz, String description) {
        return fromClass(clazz, new ArrayList<>(), description);
    }

    /**
     * 从类生成 JSON Schema（含描述和必填字段）
     *
     * @param clazz       带有 @SchemaField 注解的类
     * @param required    必填字段列表
     * @param description 对象级别描述
     * @return JSON Schema Map
     */
    public static Map<String, Object> fromClass(Class<?> clazz, List<String> required, String description) {
        JsonSchemaBuilder.SchemaPropsBuilder propsBuilder = JsonSchemaBuilder.props();
        List<String> requiredFields = new ArrayList<>(required);
        // 遍历所有字段
        for (Field field : clazz.getDeclaredFields()) {
            SchemaField annotation = field.getAnnotation(SchemaField.class);
            if (annotation == null) {
                continue;
            }
            String fieldName = field.getName();
            Map<String, Object> fieldSchema = generateFieldSchema(field, annotation);
            propsBuilder.put(fieldName, fieldSchema);
            if (annotation.required()) {
                requiredFields.add(fieldName);
            }
        }
        // 根据 description 和 required 决定使用哪个重载方法
        if (description != null && !description.isEmpty()) {
            if (requiredFields.isEmpty()) {
                return JsonSchemaBuilder.objectSchema(propsBuilder, description);
            }
            return JsonSchemaBuilder.objectSchema(propsBuilder, requiredFields, description);
        }
        if (requiredFields.isEmpty()) {
            return JsonSchemaBuilder.objectSchema(propsBuilder);
        }
        return JsonSchemaBuilder.objectSchema(propsBuilder, requiredFields);
    }

    /**
     * 生成单个字段的 Schema
     */
    private static Map<String, Object> generateFieldSchema(Field field, SchemaField annotation) {
        Class<?> fieldType = field.getType();
        String description = annotation.value();
        // 处理枚举类型
        if (isEnumType(fieldType)) {
            List<String> enumValues = resolveEnumValues(fieldType, annotation.enumValues());
            return JsonSchemaBuilder.enumSchema(description, enumValues);
        }
        // 处理字符串类型
        if (String.class.equals(fieldType)) {
            return JsonSchemaBuilder.stringSchema(description);
        }
        // 处理整数类型
        if (Integer.class.equals(fieldType) || int.class.equals(fieldType)) {
            return JsonSchemaBuilder.integerSchema(description);
        }
        // 处理 List 类型
        if (List.class.equals(fieldType)) {
            return generateListSchema(field, description);
        }
        // 处理 Map 类型（动态对象结构）
        if (Map.class.equals(fieldType)) {
            return JsonSchemaBuilder.objectSchema(description);
        }
        // 处理嵌套对象类型，传递 description
        if (isNestedObject(fieldType)) {
            return fromClass(fieldType, description);
        }
        // 默认返回字符串类型
        return JsonSchemaBuilder.stringSchema(description);
    }

    /**
     * 生成 List 类型的 Schema
     */
    private static Map<String, Object> generateListSchema(Field field, String description) {
        // 获取泛型参数类型
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        Class<?> itemType = (Class<?>) genericType.getActualTypeArguments()[0];
        // 处理 List<String> 类型
        if (String.class.equals(itemType)) {
            return JsonSchemaBuilder.arraySchema(JsonSchemaBuilder.stringSchema(), description);
        }
        // 处理 List<嵌套对象> 类型，传递 description 给数组本身
        if (isNestedObject(itemType)) {
            Map<String, Object> itemSchema = fromClass(itemType);
            return JsonSchemaBuilder.arraySchema(itemSchema, description);
        }
        // 默认返回字符串数组
        return JsonSchemaBuilder.arraySchema(JsonSchemaBuilder.stringSchema(), description);
    }

    /**
     * 判断是否为枚举类型
     */
    private static boolean isEnumType(Class<?> type) {
        return type.isEnum();
    }

    /**
     * 解析枚举值
     * 优先使用注解指定的值，否则从枚举类提取
     */
    private static List<String> resolveEnumValues(Class<?> enumType, String[] annotationValues) {
        // 如果注解中指定了枚举值，直接使用
        if (annotationValues.length > 0) {
            return Arrays.asList(annotationValues);
        }
        // 从 Gender 枚举类提取描述值
        if (enumType == Gender.class) {
            return Arrays.asList("男", "女");
        }
        // 默认提取枚举常量名称
        return Arrays.stream(enumType.getEnumConstants())
                .map(obj -> ((Enum<?>) obj).name())
                .toList();
    }

    /**
     * 判断是否为嵌套对象类型
     * 排除 Java 基础类型、包装类、String、List、Map 等
     */
    private static boolean isNestedObject(Class<?> type) {
        return !type.isPrimitive()
                && !type.getName().startsWith("java.lang")
                && !List.class.isAssignableFrom(type)
                && !Map.class.isAssignableFrom(type);
    }
}
