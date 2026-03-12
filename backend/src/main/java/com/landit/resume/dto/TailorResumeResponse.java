package com.landit.resume.dto;

import com.landit.resume.dto.ResumeStructuredData.BasicInfo;
import com.landit.resume.dto.ResumeStructuredData.EducationExperience;
import com.landit.resume.dto.ResumeStructuredData.WorkExperience;
import com.landit.resume.dto.ResumeStructuredData.ProjectExperience;
import com.landit.resume.dto.ResumeStructuredData.Skill;
import com.landit.resume.dto.ResumeStructuredData.Certificate;
import com.landit.resume.dto.ResumeStructuredData.OpenSourceContribution;
import com.landit.resume.dto.ResumeStructuredData.CustomSection;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 定制简历生成结果 DTO
 *
 * @author Azir
 */
@Data
public class TailorResumeResponse {

    /**
     * 基本信息（定制后）
     */
    private BasicInfo basicInfo;

    /**
     * 教育经历（定制后）
     */
    private List<EducationExperience> education;

    /**
     * 工作经历（定制后，已强调相关性）
     */
    private List<WorkExperience> work;

    /**
     * 项目经历（定制后，已强调相关性）
     */
    private List<ProjectExperience> projects;

    /**
     * 技能（定制后，已调整顺序和描述）
     */
    private List<Skill> skills;

    /**
     * 证书（定制后）
     */
    private List<Certificate> certificates;

    /**
     * 开源贡献（定制后）
     */
    private List<OpenSourceContribution> openSource;

    /**
     * 自定义区块（定制后）
     */
    private List<CustomSection> customSections;

    /**
     * 定制说明（描述做了哪些调整）
     */
    private List<String> tailorNotes;

    /**
     * 模块评分（各区块与JD的相关性评分）
     */
    private Map<String, Integer> sectionRelevanceScores;

}
