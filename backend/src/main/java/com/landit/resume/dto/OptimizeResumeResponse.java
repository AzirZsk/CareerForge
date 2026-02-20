package com.landit.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI优化简历响应DTO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizeResumeResponse {

    private ResumeDetailVO optimizedResume;

    private List<ResumeSuggestionVO> suggestions;

}
