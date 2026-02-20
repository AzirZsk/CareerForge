package com.landit.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户存在检查响应DTO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExistsResponse {

    private Boolean exists;

    public static UserExistsResponse of(Boolean exists) {
        return UserExistsResponse.builder().exists(exists).build();
    }

}
