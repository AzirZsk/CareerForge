package com.careerforge.interview.graph.review;

import com.careerforge.company.entity.Company;
import com.careerforge.company.service.CompanyService;
import com.careerforge.interview.entity.Interview;
import com.careerforge.interview.service.InterviewCenterService;
import com.careerforge.jobposition.entity.JobPosition;
import com.careerforge.jobposition.service.JobPositionService;
import com.careerforge.resume.dto.ResumeDetailVO;
import com.careerforge.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 复盘分析工作流上下文构建器
 * 根据 interviewId 查询所有需要的上下文数据，避免 Handler 层预注入大量 initialState
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewContextBuilder {

    private final InterviewCenterService interviewService;
    private final JobPositionService jobPositionService;
    private final CompanyService companyService;
    private final ResumeService resumeService;

    /**
     * 根据面试ID构建复盘分析所需的全部上下文
     *
     * @param interviewId 面试ID
     * @return 上下文数据
     */
    public ReviewContext buildContext(String interviewId) {
        Interview interview = interviewService.getById(interviewId);
        // 查询关联的 JobPosition 和 Company
        JobPosition jobPosition = getLinkedJobPosition(interview);
        String companyName = resolveCompanyName(jobPosition);
        String positionTitle = resolvePositionTitle(jobPosition);
        String jdContent = resolveJdContent(interview, jobPosition);
        String jdAnalysis = resolveJdAnalysis(jobPosition);
        String resumeContent = buildResumeContext(interview != null ? interview.getResumeId() : null);
        String transcript = interview != null ? interview.getTranscript() : null;
        log.info("复盘上下文构建完成: 面试={}, 公司={}, 职位={}, JD={}, 简历={}, 对话记录={}",
                interviewId,
                companyName.isEmpty() ? "无" : "有",
                positionTitle.isEmpty() ? "无" : "有",
                jdContent.isEmpty() ? "无" : "有",
                resumeContent.isEmpty() ? "无" : "有",
                (transcript != null && !transcript.isBlank()) ? "有" : "无");
        return new ReviewContext(companyName, positionTitle, jdContent, jdAnalysis, resumeContent, transcript);
    }

    /**
     * 获取面试关联的 JobPosition
     */
    private JobPosition getLinkedJobPosition(Interview interview) {
        if (interview == null || interview.getJobPositionId() == null || interview.getJobPositionId().isBlank()) {
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
        if (jobPosition == null || jobPosition.getCompanyId() == null || jobPosition.getCompanyId().isBlank()) {
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
     * 解析JD原文：优先取Interview上的，否则从关联的JobPosition取
     */
    private String resolveJdContent(Interview interview, JobPosition jobPosition) {
        if (interview != null && interview.getJdContent() != null && !interview.getJdContent().isBlank()) {
            return interview.getJdContent();
        }
        if (jobPosition != null && jobPosition.getJdContent() != null) {
            return jobPosition.getJdContent();
        }
        return "";
    }

    /**
     * 解析JD分析结果：从JobPosition取
     */
    private String resolveJdAnalysis(JobPosition jobPosition) {
        if (jobPosition != null && jobPosition.getJdAnalysis() != null
                && !jobPosition.getJdAnalysis().isBlank()
                && !jobPosition.getJdAnalysis().equals("{}")) {
            return jobPosition.getJdAnalysis();
        }
        return "";
    }

    /**
     * 构建简历上下文文本
     */
    private String buildResumeContext(String resumeId) {
        try {
            if (resumeId == null || resumeId.isBlank()) {
                return "";
            }
            ResumeDetailVO resume = resumeService.getResumeDetail(resumeId);
            if (resume == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            // 目标岗位
            if (resume.getTargetPosition() != null && !resume.getTargetPosition().isBlank()) {
                sb.append("【目标岗位】").append(resume.getTargetPosition()).append("\n");
            }
            // 提取所有区块内容
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
            log.warn("构建简历上下文失败: resumeId={}, error={}", resumeId, e.getMessage());
            return "";
        }
    }

    /**
     * 复盘分析上下文数据
     */
    public record ReviewContext(
            String companyName,
            String positionTitle,
            String jdContent,
            String jdAnalysis,
            String resumeContent,
            String transcript
    ) {}

}
