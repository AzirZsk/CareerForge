package com.landit.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.common.enums.ResumeStatus;
import com.landit.common.enums.ResumeType;
import com.landit.resume.convertor.ResumeConvertor;
import com.landit.resume.dto.CreateResumeRequest;
import com.landit.resume.dto.DeriveResumeRequest;
import com.landit.resume.dto.ResumeParseResult;
import com.landit.resume.dto.ResumeStructuredData;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.dto.ResumeSuggestionVO;
import com.landit.resume.dto.ResumeVersionVO;
import com.landit.resume.dto.UpdateResumeRequest;
import com.landit.resume.entity.Resume;
import com.landit.resume.entity.ResumeSection;
import com.landit.resume.entity.ResumeVersion;
import com.landit.resume.mapper.ResumeMapper;
import com.landit.resume.mapper.ResumeSectionMapper;
import com.landit.resume.mapper.ResumeVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 简历服务实现类
 * 仅负责简历表的 CRUD 操作，聚合操作由 ResumeHandler 处理
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService extends ServiceImpl<ResumeMapper, Resume> {

    private final ResumeVersionMapper resumeVersionMapper;
    private final ResumeSectionMapper resumeSectionMapper;
    private final ResumeConvertor resumeConvertor;

    /**
     * 获取简历列表
     */
    public List<Resume> getResumes(ResumeStatus status) {
        // TODO: 实现查询逻辑
        return null;
    }

    /**
     * 获取简历详情
     */
    public ResumeDetailVO getResumeDetail(Long id) {
        // TODO: 实现查询逻辑
        return null;
    }

    /**
     * 创建空白简历
     */
    public Resume createResume(CreateResumeRequest request) {
        // TODO: 实现创建逻辑
        return null;
    }

    /**
     * 更新简历
     */
    public ResumeDetailVO updateResume(Long id, UpdateResumeRequest request) {
        // TODO: 实现更新逻辑
        return null;
    }

    /**
     * 删除简历
     */
    public void deleteResume(Long id) {
        // TODO: 实现删除逻辑
    }

    /**
     * 设置主简历
     */
    public void setPrimaryResume(Long id) {
        // TODO: 实现设置逻辑
    }

    /**
     * 获取优化建议
     */
    public List<ResumeSuggestionVO> getResumeSuggestions(Long id) {
        // TODO: 实现查询逻辑
        return null;
    }

    /**
     * 获取简历版本历史
     */
    public List<ResumeVersionVO> getVersionHistory(Long resumeId) {
        LambdaQueryWrapper<ResumeVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeVersion::getResumeId, resumeId)
                .orderByDesc(ResumeVersion::getVersion);
        List<ResumeVersion> versions = resumeVersionMapper.selectList(wrapper);
        return resumeConvertor.toVersionVOList(versions);
    }

    /**
     * 获取指定版本详情
     */
    public ResumeDetailVO getVersionDetail(Long resumeId, Integer version) {
        // TODO: 实现查询逻辑
        return null;
    }

    /**
     * 获取版本实体
     *
     * @param resumeId 简历ID
     * @param version  版本号
     * @return 版本实体
     */
    public ResumeVersion getVersionEntity(Long resumeId, Integer version) {
        LambdaQueryWrapper<ResumeVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeVersion::getResumeId, resumeId)
                .eq(ResumeVersion::getVersion, version);
        return resumeVersionMapper.selectOne(wrapper);
    }

    /**
     * 获取简历内容模块列表
     *
     * @param resumeId 简历ID
     * @return 内容模块列表
     */
    public List<ResumeSection> getResumeSections(Long resumeId) {
        LambdaQueryWrapper<ResumeSection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeSection::getResumeId, resumeId)
                .isNull(ResumeSection::getResumeVersionId);
        return resumeSectionMapper.selectList(wrapper);
    }

    /**
     * 创建派生简历记录
     *
     * @param sourceResume 源简历
     * @param request      派生请求
     * @return 派生后的简历
     */
    public Resume createDerivedResume(Resume sourceResume, DeriveResumeRequest request) {
        Resume derivedResume = new Resume();
        derivedResume.setUserId(sourceResume.getUserId());
        derivedResume.setName(request.getResumeName() != null
                ? request.getResumeName()
                : sourceResume.getName() + "-" + request.getTargetPosition());
        derivedResume.setTargetPosition(request.getTargetPosition());
        derivedResume.setResumeType(ResumeType.DERIVED);
        derivedResume.setSourceResumeId(sourceResume.getId());
        derivedResume.setVersion(1);
        derivedResume.setStatus(ResumeStatus.DRAFT);
        derivedResume.setScore(0);
        derivedResume.setCompleteness(0);
        derivedResume.setIsPrimary(false);
        save(derivedResume);
        return derivedResume;
    }

    /**
     * 从解析数据创建简历（用户初始化时调用）
     *
     * @param userId        用户ID
     * @param parsedResume  解析后的简历数据
     * @return 创建的简历实体
     */
    public Resume createResumeFromParsedData(Long userId, ResumeParseResult parsedResume) {
        ResumeStructuredData data = parsedResume.getStructuredData();
        String targetPosition = data != null && data.getBasicInfo() != null
                ? Objects.toString(data.getBasicInfo().getTargetPosition(), "")
                : "";

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setName(parsedResume.getName() + "的简历");
        resume.setTargetPosition(targetPosition);
        resume.setResumeType(ResumeType.PRIMARY);
        resume.setVersion(1);
        resume.setStatus(ResumeStatus.DRAFT);
        resume.setIsPrimary(true);
        resume.setCompleteness(calculateCompleteness(data));
        save(resume);
        log.info("简历主记录创建完成: resumeId={}", resume.getId());

        createResumeSections(resume.getId(), data, parsedResume.getRawText());

        return resume;
    }

    /**
     * 计算简历完整度（0-100）
     */
    private Integer calculateCompleteness(ResumeStructuredData data) {
        if (data == null) {
            return 10;
        }
        int score = 0;

        // 基本信息（最多40分）
        ResumeStructuredData.BasicInfo basic = data.getBasicInfo();
        if (basic != null) {
            if (isNotEmpty(basic.getName())) score += 10;
            if (isNotEmpty(basic.getPhone())) score += 10;
            if (isNotEmpty(basic.getEmail())) score += 10;
            if (isNotEmpty(basic.getSummary())) score += 10;
        }

        // 教育经历（最多20分）
        if (isNotEmpty(data.getEducation())) score += 20;

        // 工作经历（最多20分）
        if (isNotEmpty(data.getWork())) score += 20;

        // 项目经验（最多10分）
        if (isNotEmpty(data.getProjects())) score += 10;

        // 技能（最多10分）
        if (isNotEmpty(data.getSkills())) score += 10;

        return Math.min(score, 100);
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    private boolean isNotEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }

    // ==================== 简历模块创建方法 ====================

    /**
     * 创建简历各模块记录
     */
    private void createResumeSections(Long resumeId, ResumeStructuredData data, String rawText) {
        if (data == null) {
            createSection(resumeId, "RAW_TEXT", "简历内容", Map.of("content", Objects.toString(rawText, "")));
            return;
        }

        createBasicInfoSection(resumeId, data.getBasicInfo());
        createEducationSections(resumeId, data.getEducation());
        createWorkSections(resumeId, data.getWork());
        createProjectSections(resumeId, data.getProjects());
        createSkillsSection(resumeId, data.getSkills());
        createCertificateSections(resumeId, data.getCertificates());

        log.info("简历模块创建完成: resumeId={}", resumeId);
    }

    /**
     * 创建基本信息模块
     */
    private void createBasicInfoSection(Long resumeId, ResumeStructuredData.BasicInfo info) {
        if (info == null) {
            return;
        }
        Map<String, Object> content = new HashMap<>();
        content.put("name", Objects.toString(info.getName(), ""));
        content.put("gender", info.getGender() != null ? info.getGender().getDescription() : "");
        content.put("phone", Objects.toString(info.getPhone(), ""));
        content.put("email", Objects.toString(info.getEmail(), ""));
        content.put("targetPosition", Objects.toString(info.getTargetPosition(), ""));
        content.put("summary", Objects.toString(info.getSummary(), ""));

        createSection(resumeId, "BASIC_INFO", "基本信息", content);
    }

    /**
     * 创建教育经历模块
     */
    private void createEducationSections(Long resumeId, List<ResumeStructuredData.EducationExperience> educations) {
        if (educations == null || educations.isEmpty()) {
            return;
        }
        for (ResumeStructuredData.EducationExperience edu : educations) {
            Map<String, Object> content = new HashMap<>();
            content.put("school", Objects.toString(edu.getSchool(), ""));
            content.put("degree", Objects.toString(edu.getDegree(), ""));
            content.put("major", Objects.toString(edu.getMajor(), ""));
            content.put("period", Objects.toString(edu.getPeriod(), ""));

            createSection(resumeId, "EDUCATION", Objects.toString(edu.getSchool(), "教育经历"), content);
        }
    }

    /**
     * 创建工作经历模块
     */
    private void createWorkSections(Long resumeId, List<ResumeStructuredData.WorkExperience> works) {
        if (works == null || works.isEmpty()) {
            return;
        }
        for (ResumeStructuredData.WorkExperience work : works) {
            Map<String, Object> content = new HashMap<>();
            content.put("company", Objects.toString(work.getCompany(), ""));
            content.put("position", Objects.toString(work.getPosition(), ""));
            content.put("period", Objects.toString(work.getPeriod(), ""));
            content.put("description", Objects.toString(work.getDescription(), ""));

            createSection(resumeId, "WORK", Objects.toString(work.getCompany(), "工作经历"), content);
        }
    }

    /**
     * 创建项目经验模块
     */
    private void createProjectSections(Long resumeId, List<ResumeStructuredData.ProjectExperience> projects) {
        if (projects == null || projects.isEmpty()) {
            return;
        }
        for (ResumeStructuredData.ProjectExperience project : projects) {
            Map<String, Object> content = new HashMap<>();
            content.put("name", Objects.toString(project.getName(), ""));
            content.put("role", Objects.toString(project.getRole(), ""));
            content.put("period", Objects.toString(project.getPeriod(), ""));
            content.put("description", Objects.toString(project.getDescription(), ""));
            content.put("achievements", project.getAchievements() != null ? project.getAchievements() : List.of());

            createSection(resumeId, "PROJECT", Objects.toString(project.getName(), "项目经验"), content);
        }
    }

    /**
     * 创建技能模块
     */
    private void createSkillsSection(Long resumeId, List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return;
        }
        createSection(resumeId, "SKILLS", "专业技能", Map.of("skills", skills));
    }

    /**
     * 创建证书模块
     */
    private void createCertificateSections(Long resumeId, List<ResumeStructuredData.Certificate> certificates) {
        if (certificates == null || certificates.isEmpty()) {
            return;
        }
        for (ResumeStructuredData.Certificate cert : certificates) {
            Map<String, Object> content = new HashMap<>();
            content.put("name", Objects.toString(cert.getName(), ""));
            content.put("date", Objects.toString(cert.getDate(), ""));

            createSection(resumeId, "CERTIFICATE", Objects.toString(cert.getName(), "证书/荣誉"), content);
        }
    }

    /**
     * 通用方法：创建并保存一个简历模块
     */
    private void createSection(Long resumeId, String type, String title, Map<String, Object> content) {
        ResumeSection section = new ResumeSection();
        section.setResumeId(resumeId);
        section.setResumeVersionId(null);
        section.setType(type);
        section.setTitle(title);
        section.setContent(content);
        resumeSectionMapper.insert(section);
    }

}
