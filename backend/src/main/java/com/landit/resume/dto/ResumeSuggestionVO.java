package com.landit.resume.dto;

import com.landit.common.enums.SuggestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简历优化建议VO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeSuggestionVO {

    private String id;

    private SuggestionType type;

    private String category;

    private String title;

    private String description;

    private String impact;

    private String position;

}
