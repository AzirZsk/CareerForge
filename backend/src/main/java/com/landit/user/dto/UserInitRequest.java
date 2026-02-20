package com.landit.user.dto;

import com.landit.common.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户初始化请求DTO
 *
 * @author Azir
 */
@Data
public class UserInitRequest {

    @NotBlank(message = "姓名不能为空")
    private String name;

    private Gender gender;

}
