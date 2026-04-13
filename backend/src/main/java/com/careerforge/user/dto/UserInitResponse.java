package com.careerforge.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户初始化响应DTO
 * 用于返回解析后的用户信息
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInitResponse {

    private String id;

    private String name;

    private String gender;

}
