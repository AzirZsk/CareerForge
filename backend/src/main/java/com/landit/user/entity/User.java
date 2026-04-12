package com.landit.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体类
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_user", autoResultMap = true)
public class User extends BaseEntity {

    private String name;

    private String gender;

    private String avatar;

    // 认证相关字段

    /**
     * 邮箱（登录账号）
     */
    private String email;

    /**
     * 手机号（可选登录方式）
     */
    private String phone;

    /**
     * 密码（BCrypt 加密）
     */
    private String password;

    /**
     * 账号状态（ACTIVE-正常 FROZEN-冻结 DELETED-已删除）
     */
    private String status;

    /**
     * 最后登录时间
     */
    private java.time.LocalDateTime lastLoginAt;

    /**
     * 注册IP
     */
    private String registerIp;

}
