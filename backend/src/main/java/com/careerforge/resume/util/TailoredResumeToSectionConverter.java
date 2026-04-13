package com.careerforge.resume.util;

import com.careerforge.common.enums.SectionType;
import com.careerforge.common.util.JsonParseHelper;
import com.careerforge.resume.dto.ResumeDetailVO;
import com.careerforge.resume.dto.TailorResumeResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 将 TailorResumeResponse（结构化简历）转换为 ResumeSectionVO 列表（区块格式）
 * 供前端 ResumeComparison 组件使用
 *
 * @author Azir
 */
@Component
public class TailoredResumeToSectionConverter {

    /**
     * 将定制简历响应转换为区块列表
     *
     * @param response 定制简历响应
     * @param resumeId 简历ID
     * @return 区块列表
     */
    public List<ResumeDetailVO.ResumeSectionVO> convert(TailorResumeResponse response, String resumeId) {
        List<ResumeDetailVO.ResumeSectionVO> sections = new ArrayList<>();

        // 基本信息 -> BASIC_INFO
        if (response.getBasicInfo() != null) {
            sections.add(createSection(
                    resumeId,
                    SectionType.BASIC_INFO,
                    "基本信息",
                    JsonParseHelper.toJsonString(response.getBasicInfo())
            ));
        }

        // 工作经历 -> WORK
        if (response.getWork() != null && !response.getWork().isEmpty()) {
            sections.add(createSection(
                    resumeId,
                    SectionType.WORK,
                    "工作经历",
                    JsonParseHelper.toJsonString(response.getWork())
            ));
        }

        // 项目经历 -> PROJECT
        if (response.getProjects() != null && !response.getProjects().isEmpty()) {
            sections.add(createSection(
                    resumeId,
                    SectionType.PROJECT,
                    "项目经历",
                    JsonParseHelper.toJsonString(response.getProjects())
            ));
        }

        // 教育经历 -> EDUCATION
        if (response.getEducation() != null && !response.getEducation().isEmpty()) {
            sections.add(createSection(
                    resumeId,
                    SectionType.EDUCATION,
                    "教育经历",
                    JsonParseHelper.toJsonString(response.getEducation())
            ));
        }

        // 技能 -> SKILLS
        if (response.getSkills() != null && !response.getSkills().isEmpty()) {
            sections.add(createSection(
                    resumeId,
                    SectionType.SKILLS,
                    "专业技能",
                    JsonParseHelper.toJsonString(response.getSkills())
            ));
        }

        // 证书 -> CERTIFICATE
        if (response.getCertificates() != null && !response.getCertificates().isEmpty()) {
            sections.add(createSection(
                    resumeId,
                    SectionType.CERTIFICATE,
                    "证书荣誉",
                    JsonParseHelper.toJsonString(response.getCertificates())
            ));
        }

        // 开源贡献 -> OPEN_SOURCE
        if (response.getOpenSource() != null && !response.getOpenSource().isEmpty()) {
            sections.add(createSection(
                    resumeId,
                    SectionType.OPEN_SOURCE,
                    "开源贡献",
                    JsonParseHelper.toJsonString(response.getOpenSource())
            ));
        }

        // 自定义区块 -> CUSTOM
        if (response.getCustomSections() != null && !response.getCustomSections().isEmpty()) {
            sections.add(createSection(
                    resumeId,
                    SectionType.CUSTOM,
                    "自定义区块",
                    JsonParseHelper.toJsonString(response.getCustomSections())
            ));
        }

        return sections;
    }

    /**
     * 创建区块 VO
     */
    private ResumeDetailVO.ResumeSectionVO createSection(
            String resumeId,
            SectionType type,
            String title,
            String content) {
        ResumeDetailVO.ResumeSectionVO section = new ResumeDetailVO.ResumeSectionVO();
        section.setId(UUID.randomUUID().toString());
        section.setResumeId(resumeId);
        section.setType(type.getCode());
        section.setTitle(title);
        section.setContent(content);
        // 默认分数，前端会根据内容计算
        section.setScore(0);
        section.setSuggestions(null);
        return section;
    }
}
