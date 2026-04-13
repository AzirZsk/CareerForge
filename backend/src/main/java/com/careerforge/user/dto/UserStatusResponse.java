package com.careerforge.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户状态响应DTO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusResponse {

    private Boolean exists;

    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {

        private String id;

        private String name;

        private String gender;

        private String avatar;

    }

    public static UserStatusResponse notExists() {
        return UserStatusResponse.builder().exists(false).build();
    }

    public static UserStatusResponse exists(String id, String name, String gender, String avatar) {
        return UserStatusResponse.builder()
                .exists(true)
                .user(UserInfo.builder()
                        .id(id)
                        .name(name)
                        .gender(gender)
                        .avatar(avatar)
                        .build())
                .build();
    }

}
