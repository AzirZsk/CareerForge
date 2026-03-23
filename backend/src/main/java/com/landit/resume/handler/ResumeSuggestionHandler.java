package com.landit.resume.handler;

import com.landit.resume.dto.ResumeSuggestionVO;
import com.landit.resume.dto.ResumeSuggestionsGroupVO;
import com.landit.resume.entity.ResumeSuggestion;
import com.landit.resume.service.ResumeSuggestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 简历优化建议业务处理器
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeSuggestionHandler {

    private final ResumeSuggestionService suggestionService;

    /**
     * 获取简历的所有优化建议
     *
     * @param resumeId 简历ID
     * @return 建议列表
     */
    public List<ResumeSuggestionVO> getSuggestions(String resumeId) {
        List<ResumeSuggestion> suggestions = suggestionService.getSuggestionsByResumeId(resumeId);
        return suggestions.stream()
                .map(this::toSuggestionVO)
                .collect(Collectors.toList());
    }

    /**
     * 删除单条建议
     *
     * @param suggestionId 建议ID
     */
    public void deleteSuggestion(String suggestionId) {
        boolean success = suggestionService.deleteSuggestionById(suggestionId);
        if (!success) {
            log.warn("删除建议失败或建议不存在: suggestionId={}", suggestionId);
        }
    }

    /**
     * 获取所有简历的优化建议（按简历分组）
     *
     * @return 分组建议列表
     */
    public List<ResumeSuggestionsGroupVO> getAllSuggestionsGrouped() {
        return suggestionService.getAllSuggestionsGrouped();
    }

    /**
     * 实体转VO
     */
    private ResumeSuggestionVO toSuggestionVO(ResumeSuggestion entity) {
        return ResumeSuggestionVO.builder()
                .id(String.valueOf(entity.getId()))
                .sectionId(entity.getSectionId())
                .type(entity.getType())
                .category(entity.getCategory())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .impact(entity.getImpact())
                .position(entity.getPosition())
                .build();
    }

}
