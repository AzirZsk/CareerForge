package com.landit.jobposition.dto;

import lombok.Data;

/**
 * 更新职位请求 DTO
 *
 * @author Azir
 */
@Data
public class UpdateJobPositionRequest {

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 职位名称
     */
    private String title;

    /**
     * JD原文
     */
    private String jdContent;

}
