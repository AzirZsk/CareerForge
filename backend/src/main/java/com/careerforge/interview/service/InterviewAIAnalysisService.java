package com.careerforge.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.careerforge.interview.entity.InterviewAIAnalysis;
import com.careerforge.interview.graph.review.dto.AdviceItem;
import com.careerforge.interview.graph.review.dto.InterviewAnalysisResult;
import com.careerforge.interview.graph.review.dto.TranscriptAnalysisResult;
import com.careerforge.interview.mapper.InterviewAIAnalysisMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * AI 面试分析服务
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewAIAnalysisService extends ServiceImpl<InterviewAIAnalysisMapper, InterviewAIAnalysis> {

    private final ObjectMapper objectMapper;

    /**
     * 根据面试ID获取 AI 分析结果
     */
    public InterviewAIAnalysis getByInterviewId(String interviewId) {
        LambdaQueryWrapper<InterviewAIAnalysis> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewAIAnalysis::getInterviewId, interviewId);
        return this.getOne(wrapper);
    }

    /**
     * 保存 AI 分析结果（覆盖旧结果）
     */
    @Transactional(rollbackFor = Exception.class)
    public InterviewAIAnalysis saveAnalysis(String interviewId, List<AdviceItem> adviceList, String transcriptAnalysisJson, String interviewAnalysisJson) {
        // 先删除旧记录
        deleteByInterviewId(interviewId);

        // 创建新记录
        InterviewAIAnalysis analysis = new InterviewAIAnalysis();
        analysis.setInterviewId(interviewId);
        try {
            analysis.setAdviceList(objectMapper.writeValueAsString(adviceList));
        } catch (JsonProcessingException e) {
            log.error("序列化 AI 分析结果失败", e);
            analysis.setAdviceList("[]");
        }
        analysis.setTranscriptAnalysis(transcriptAnalysisJson);
        analysis.setInterviewAnalysis(interviewAnalysisJson);

        this.save(analysis);
        log.info("保存 AI 分析结果成功: interviewId={}", interviewId);
        return analysis;
    }

    /**
     * 解析 AI 分析结果
     */
    public List<AdviceItem> parseAdviceList(InterviewAIAnalysis analysis) {
        if (analysis == null || analysis.getAdviceList() == null || analysis.getAdviceList().isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(analysis.getAdviceList(), new TypeReference<List<AdviceItem>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析 AI 分析结果失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 删除指定面试的 AI 分析结果
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByInterviewId(String interviewId) {
        LambdaQueryWrapper<InterviewAIAnalysis> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewAIAnalysis::getInterviewId, interviewId);
        this.remove(wrapper);
        log.info("删除 AI 分析结果: interviewId={}", interviewId);
    }

    /**
     * 解析对话分析结果
     */
    public TranscriptAnalysisResult parseTranscriptAnalysis(InterviewAIAnalysis analysis) {
        if (analysis == null || analysis.getTranscriptAnalysis() == null || analysis.getTranscriptAnalysis().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(analysis.getTranscriptAnalysis(), TranscriptAnalysisResult.class);
        } catch (JsonProcessingException e) {
            log.error("解析对话分析结果失败", e);
            return null;
        }
    }

    /**
     * 解析面试分析结果
     */
    public InterviewAnalysisResult parseInterviewAnalysis(InterviewAIAnalysis analysis) {
        if (analysis == null || analysis.getInterviewAnalysis() == null || analysis.getInterviewAnalysis().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(analysis.getInterviewAnalysis(), InterviewAnalysisResult.class);
        } catch (JsonProcessingException e) {
            log.error("解析面试分析结果失败", e);
            return null;
        }
    }

}
