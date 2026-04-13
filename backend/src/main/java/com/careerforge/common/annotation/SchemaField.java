package com.careerforge.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Schema 字段注解
 * 用于标记 DTO 字段对应的 JSON Schema 属性
 *
 * @author Azir
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SchemaField {

    /**
     * 字段描述
     */
    String value();

    /**
     * 是否必填
     */
    boolean required() default false;

    /**
     * 枚举值（可选）
     * 仅当字段为枚举类型时使用，若为空则自动从枚举类提取
     */
    String[] enumValues() default {};
}
