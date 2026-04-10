package com.landit.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 用户登录请求 DTO
 *
 * @author Azir
 */
@Data
public class LoginRequest {

    /**
     * 账号（邮箱或手机号）
     */
    @NotBlank(message = "账号不能为空")
    private String account;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
