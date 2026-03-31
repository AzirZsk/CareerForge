package com.landit.interview.dto.interviewcenter;

import lombok.Data;

/**
 * 更新准备事项请求 DTO
 *
 * @author Azir
 */
@Data
public class UpdatePreparationRequest {

    private String title;

    private String content;

    private Boolean completed;

    private Integer sortOrder;

}
