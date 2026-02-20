package com.landit.resume.entity;

import com.landit.common.entity.BaseEntity;
import com.landit.common.enums.SuggestionType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 简历优化建议实体类
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ResumeSuggestion extends BaseEntity {

    private Long resumeId;

    private SuggestionType type;

    private String category;

    private String title;

    private String description;

    private String impact;

    private String position;

}
