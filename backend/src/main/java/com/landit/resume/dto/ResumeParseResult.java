package com.landit.resume.dto;

import com.landit.common.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简历解析结果DTO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeParseResult {

    private String name;

    private Gender gender;

    private String rawText;

}
