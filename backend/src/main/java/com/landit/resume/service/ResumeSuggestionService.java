package com.landit.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.resume.dto.DiagnoseResumeResponse;
import com.landit.resume.entity.ResumeSuggestion;
import com.landit.resume.mapper.ResumeSuggestionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 简历优化建议服务
 * 继承 ServiceImpl 以使用 MyBatis-Plus 提供的批量操作方法
 *
 * @author Azir
 */
@Slf4j
@Service
public class ResumeSuggestionService extends ServiceImpl<ResumeSuggestionMapper, ResumeSuggestion> {

    /**
     * 保存简历优化建议（先删除旧建议，再批量插入新建议）
     *
     * @param resumeId           简历ID
     * @param suggestions        建议列表
     * @param shortIdToRealIdMap 简短标识符到真实ID的映射
     */
    @Transactional
    public void saveSuggestions(String resumeId,
                                List<DiagnoseResumeResponse.Suggestion> suggestions,
                                Map<String, String> shortIdToRealIdMap) {
        // 1. 删除该简历的旧建议
        remove(new LambdaQueryWrapper<ResumeSuggestion>()
                .eq(ResumeSuggestion::getResumeId, resumeId));

        // 2. 转换为实体
        List<ResumeSuggestion> entities = suggestions.stream()
                .map(s -> toEntity(resumeId, s, shortIdToRealIdMap))
                .collect(Collectors.toList());

        // 3. 批量插入（使用 MyBatis-Plus 的 saveBatch 方法）
        if (!entities.isEmpty()) {
            saveBatch(entities);
            log.info("已保存 {} 条建议到数据库, resumeId={}", entities.size(), resumeId);
        }
    }

    private ResumeSuggestion toEntity(String resumeId,
                                      DiagnoseResumeResponse.Suggestion suggestion,
                                      Map<String, String> shortIdToRealIdMap) {
        ResumeSuggestion entity = new ResumeSuggestion();
        entity.setResumeId(resumeId);

        // 将简短标识符映射为真实的 sectionId
        String realSectionId = shortIdToRealIdMap.get(suggestion.getSectionId());
        entity.setSectionId(realSectionId);

        entity.setType(suggestion.getType());
        entity.setImpact(mapImpact(suggestion.getImpact()));

        entity.setCategory(suggestion.getCategory());
        entity.setTitle(suggestion.getTitle());
        entity.setDescription(suggestion.getDirection());
        entity.setPosition(suggestion.getPosition());
        return entity;
    }

    /**
     * 映射影响程度
     */
    private String mapImpact(String impact) {
        if (impact == null) {
            return "低";
        }
        return switch (impact.toLowerCase()) {
            case "high" -> "高";
            case "medium" -> "中";
            default -> "低";
        };
    }

    /**
     * 根据简历ID查询所有建议
     *
     * @param resumeId 简历ID
     * @return 建议列表
     */
    public List<ResumeSuggestion> getSuggestionsByResumeId(String resumeId) {
        return list(new LambdaQueryWrapper<ResumeSuggestion>()
                .eq(ResumeSuggestion::getResumeId, resumeId));
    }

    /**
     * 按模块ID分组查询建议
     *
     * @param resumeId 简历ID
     * @return 模块ID -> 建议列表的映射
     */
    public Map<String, List<ResumeSuggestion>> getSuggestionsGroupedBySection(String resumeId) {
        List<ResumeSuggestion> suggestions = getSuggestionsByResumeId(resumeId);
        return suggestions.stream()
                .filter(s -> s.getSectionId() != null)
                .collect(Collectors.groupingBy(ResumeSuggestion::getSectionId));
    }

    /**
     * 删除指定简历的所有建议
     *
     * @param resumeId 简历ID
     */
    public void deleteByResumeId(String resumeId) {
        remove(new LambdaQueryWrapper<ResumeSuggestion>()
                .eq(ResumeSuggestion::getResumeId, resumeId));
        log.info("已删除简历的所有建议: resumeId={}", resumeId);
    }

    /**
     * 根据建议ID删除单条建议
     *
     * @param suggestionId 建议ID
     * @return 是否删除成功
     */
    public boolean deleteSuggestionById(String suggestionId) {
        boolean result = removeById(suggestionId);
        if (result) {
            log.info("已删除优化建议: suggestionId={}", suggestionId);
        }
        return result;
    }
}
