package com.landit.interview.graph.preparation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.company.entity.Company;
import com.landit.company.service.CompanyService;
import com.landit.interview.entity.Interview;
import com.landit.interview.entity.InterviewReviewNote;
import com.landit.interview.entity.InterviewAIAnalysis;
import com.landit.interview.service.InterviewCenterService;
import com.landit.interview.service.InterviewReviewNoteService;
import com.landit.interview.service.InterviewAIAnalysisService;
import com.landit.interview.graph.review.dto.AdviceItem;
import com.landit.jobposition.entity.JobPosition;
import com.landit.jobposition.service.JobPositionService;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 面试准备工作流上下文构建器
 * 根据 interviewId 查询所有需要的上下文数据，避免 Handler 层预注入大量 initialState
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PreparationContextBuilder {

    private final InterviewCenterService interviewService;
    private final JobPositionService jobPositionService;
    private final CompanyService companyService;
    private final ResumeService resumeService;
    private final InterviewReviewNoteService reviewNoteService;
    private final InterviewAIAnalysisService aiAnalysisService;
    private final ObjectMapper objectMapper;

    /**
     * 根据面试ID构建准备工作流所需的全部上下文
     *
     * @param interviewId 面试ID
     * @return 上下文数据
     */
    public PreparationContext buildContext(String interviewId) {
        Interview interview = interviewService.getById(interviewId);
        JobPosition jobPosition = getLinkedJobPosition(interview);
        String companyName = resolveCompanyName(jobPosition);
        String positionTitle = resolvePositionTitle(jobPosition);
        String jdContent = resolveJdContent(interview, jobPosition);
        String resumeContent = resolveResumeContent(interview);
        String previousReviewNotes = resolvePreviousReviewNotes(interview);
        return new PreparationContext(companyName, positionTitle, jdContent, resumeContent, previousReviewNotes);
    }

    /**
     * 获取面试关联的 JobPosition
     */
    private JobPosition getLinkedJobPosition(Interview interview) {
        if (interview == null || isBlank(interview.getJobPositionId())) {
            return null;
        }
        try {
            return jobPositionService.getById(interview.getJobPositionId());
        } catch (Exception e) {
            log.warn("查询JobPosition失败: jobPositionId={}, error={}", interview.getJobPositionId(), e.getMessage());
            return null;
        }
    }

    /**
     * 解析公司名称
     */
    private String resolveCompanyName(JobPosition jobPosition) {
        if (jobPosition == null || isBlank(jobPosition.getCompanyId())) {
            return "";
        }
        try {
            Company company = companyService.getById(jobPosition.getCompanyId());
            return company != null && company.getName() != null ? company.getName() : "";
        } catch (Exception e) {
            log.warn("查询Company失败: companyId={}, error={}", jobPosition.getCompanyId(), e.getMessage());
            return "";
        }
    }

    /**
     * 解析职位名称
     */
    private String resolvePositionTitle(JobPosition jobPosition) {
        if (jobPosition == null || jobPosition.getTitle() == null) {
            return "";
        }
        return jobPosition.getTitle();
    }

    /**
     * 判断字符串是否为空
     */
    private boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    /**
     * 解析JD原文：优先取Interview上的，否则从关联的JobPosition取
     */
    private String resolveJdContent(Interview interview, JobPosition jobPosition) {
        if (interview != null && !isBlank(interview.getJdContent())) {
            return interview.getJdContent();
        }
        if (jobPosition != null && !isBlank(jobPosition.getJdContent())) {
            return jobPosition.getJdContent();
        }
        return "";
    }

    /**
     * 解析简历内容
     */
    private String resolveResumeContent(Interview interview) {
        if (interview == null || isBlank(interview.getResumeId())) {
            return "";
        }
        try {
            ResumeDetailVO resume = resumeService.getResumeDetail(interview.getResumeId());
            if (resume == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            if (!isBlank(resume.getTargetPosition())) {
                sb.append("【目标岗位】").append(resume.getTargetPosition()).append("\n");
            }
            if (resume.getSections() != null) {
                for (ResumeDetailVO.ResumeSectionVO section : resume.getSections()) {
                    sb.append("【").append(section.getTitle()).append("】\n");
                    String content = section.getContent();
                    if (content != null && !content.isBlank()) {
                        sb.append(content).append("\n");
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("构建简历上下文失败: resumeId={}, error={}", interview.getResumeId(), e.getMessage());
            return "";
        }
    }

    /**
     * 解析上一轮复盘笔记
     */
    private String resolvePreviousReviewNotes(Interview interview) {
        if (interview == null || isBlank(interview.getJobPositionId())) {
            return "";
        }
        try {
            List<Interview> interviews = interviewService.list(
                    new LambdaQueryWrapper<Interview>()
                            .eq(Interview::getJobPositionId, interview.getJobPositionId())
                            .ne(Interview::getId, Long.parseLong(interview.getId()))
                            .isNotNull(Interview::getDate)
                            .orderByDesc(Interview::getDate)
            );
            if (interviews.isEmpty()) {
                return "";
            }
            Interview previousInterview = interviews.get(0);
            InterviewReviewNote manualNote = reviewNoteService.getByInterviewId(String.valueOf(previousInterview.getId()));
            InterviewAIAnalysis aiAnalysis = aiAnalysisService.getByInterviewId(String.valueOf(previousInterview.getId()));
            List<Map<String, Object>> notesList = new ArrayList<>();
            if (manualNote != null) {
                notesList.add(buildManualNoteMap(manualNote));
            }
            if (aiAnalysis != null) {
                notesList.add(buildAIAnalysisMap(aiAnalysis));
            }
            if (notesList.isEmpty()) {
                return "";
            }
            return objectMapper.writeValueAsString(notesList);
        } catch (Exception e) {
            log.warn("解析上一轮复盘笔记失败: interviewId={}, error={}", interview.getId(), e.getMessage());
            return "";
        }
    }

    /**
     * 构建手动笔记Map
     */
    private Map<String, Object> buildManualNoteMap(InterviewReviewNote note) {
        Map<String, Object> noteMap = new HashMap<>();
        noteMap.put("type", "manual");
        noteMap.put("weakPoints", note.getWeakPoints());
        noteMap.put("lessonsLearned", note.getLessonsLearned());
        return noteMap;
    }

    /**
     * 构建 AI 分析Map
     */
    private Map<String, Object> buildAIAnalysisMap(InterviewAIAnalysis aiAnalysis) {
        Map<String, Object> noteMap = new HashMap<>();
        noteMap.put("type", "ai_analysis");
        List<AdviceItem> adviceList = aiAnalysisService.parseAdviceList(aiAnalysis);
        // 提取关键建议信息
        List<String> suggestions = adviceList.stream()
                .map(item -> item.getTitle() + ": " + item.getDescription())
                .toList();
        noteMap.put("suggestions", suggestions);
        return noteMap;
    }

    /**
     * 准备工作流上下文数据
     */
    public record PreparationContext(
            String companyName,
            String positionTitle,
            String jdContent,
            String resumeContent,
            String previousReviewNotes
    ) {}

}
