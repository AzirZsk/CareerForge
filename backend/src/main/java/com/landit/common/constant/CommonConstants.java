package com.landit.common.constant;

/**
 * 公共常量类
 * 统一管理全项目通用的常量定义
 *
 * @author Azir
 */
public final class CommonConstants {

    private CommonConstants() {
        // 私有构造函数，防止实例化
    }

    /**
     * 单用户模式下的用户ID
     * 当前系统设计为单用户模式，所有数据都属于这个固定用户
     */
    public static final String SINGLE_USER_ID = "1";

}
