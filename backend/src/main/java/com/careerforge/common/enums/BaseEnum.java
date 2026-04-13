package com.careerforge.common.enums;

/**
 * 枚举基类接口
 * 提供通用的枚举方法
 *
 * @author Azir
 */
public interface BaseEnum {

    /**
     * 获取枚举编码值
     */
    String getCode();

    /**
     * 获取枚举描述
     */
    String getDescription();

    /**
     * 根据编码获取枚举实例
     */
    static <T extends Enum<T> & BaseEnum> T fromCode(Class<T> enumClass, String code) {
        for (T enumValue : enumClass.getEnumConstants()) {
            if (enumValue.getCode().equals(code)) {
                return enumValue;
            }
        }
        return null;
    }

}
