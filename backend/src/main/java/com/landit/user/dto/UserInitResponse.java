package com.landit.user.dto;

import com.landit.common.enums.Gender;
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

    private Gender gender;

}
