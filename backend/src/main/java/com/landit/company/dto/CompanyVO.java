package com.landit.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 公司 VO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyVO {

    /**
     * 公司ID
     */
    private String id;

    /**
     * 公司名称
     */
    private String name;

    /**
     * 公司调研结果（JSON格式）
     */
    private String research;

    /**
     * 调研更新时间
     */
    private LocalDateTime researchUpdatedAt;

}
