package com.landit.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.common.enums.ResumeStatus;
import com.landit.common.enums.ResumeType;
import com.landit.common.enums.SectionType;
import com.landit.common.exception.BusinessException;
import com.landit.resume.convertor.ResumeConvertor;
import com.landit.resume.dto.CreateResumeRequest;
import com.landit.resume.dto.DeriveResumeRequest;
import com.landit.resume.dto.DiagnoseResumeResponse;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    private static final String SINGLE_USER_ID = "1";

    private final ResumeVersionMapper resumeVersionMapper;
    private final ResumeSectionMapper resumeSectionMapper;
    private final ResumeConvertor resumeConvertor;

    /**
     * 获取主简历
     *
     * @return 主简历实体，不存在则返回 null
     */
    public Resume getPrimaryResume() {
        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Resume::getUserId, SINGLE_USER_ID)
                .eq(Resume::getIsPrimary, true);
        return getOne(wrapper);
    }

    /**
     * 判断简历是否已完成分析
     * 根据评分和完整度是否有效来判断
     *
     * @param resume 简历实体
     * @return true 已完成分析，false 未完成
     */
    public boolean isResumeAnalyzed(Resume resume) {
        return resume.getScore() != null && resume.getScore() > 0
                && resume.getCompleteness() != null && resume.getCompleteness() > 0;
    }

    /**
     * 获取简历列表
     */
    public List<Resume> getResumes(ResumeStatus status) {
        // TODO: 实现查询逻辑
        return null;
    }

    /**
     * 获取简历详情
     *
     * @param id 简历ID
     * @return 简历详情VO
     */
    public ResumeDetailVO getResumeDetail(String id) {
        // 查询简历主信息
        Resume resume = getById(id);
        if (resume == null) {
            throw BusinessException.notFound("简历不存在");
        }

        // 转换为VO（基础字段映射）
        ResumeDetailVO vo = resumeConvertor.toDetailVO(resume);

        // 查询简历模块列表（非版本快照）
        List<ResumeSection> sections = getResumeSections(id);
        // 聚合sections：多条类型合并为items数组
        vo.setSections(aggregateSections(sections));

        // 设置是否已完成分析
        vo.setAnalyzed(isResumeAnalyzed(resume));

        // 设置评分默认值
        setDefaultScoresIfNeeded(vo);

        return vo;
    }

    /**
     * 聚合sections数据
     * 对于可能有多条的类型（PROJECT、WORK、EDUCATION、CERTIFICATE）进行聚合
     * 单条类型（BASIC_INFO、SKILLS）保持原有结构
     *
     * @param sections 原始sections列表
     * @return 聚合后的VO列表
     */
    private List<ResumeDetailVO.ResumeSectionVO> aggregateSections(List<ResumeSection> sections) {
        // 使用LinkedHashMap保持插入顺序
        Map<String, List<ResumeSection>> groupedSections = new LinkedHashMap<>();
        // 按类型分组
        for (ResumeSection section : sections) {
            groupedSections.computeIfAbsent(section.getType(), k -> new ArrayList<>()).add(section);
        }
        // 按原始顺序构建VO
        return groupedSections.entrySet().stream()
                .map(entry -> buildSectionVO(entry.getKey(), entry.getValue()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 构建单个SectionVO
     * 聚合类型使用items，单条类型使用content
     */
    private ResumeDetailVO.ResumeSectionVO buildSectionVO(String typeCode, List<ResumeSection> sections) {
        ResumeSection first = sections.get(0);
        SectionType sectionType = SectionType.fromCode(typeCode);
        // 聚合类型：使用items数组
        if (sectionType != null && sectionType.isAggregate()) {
            List<ResumeDetailVO.ResumeSectionItemVO> items = sections.stream()
                    .map(section -> ResumeDetailVO.ResumeSectionItemVO.builder()
                            .id(section.getId())
                            .title(section.getTitle())
                            .content(section.getContent())
                            .score(section.getScore())
                            .build())
                    .collect(java.util.stream.Collectors.toList());
            return ResumeDetailVO.ResumeSectionVO.builder()
                    .id(first.getId())
                    .type(typeCode)
                    .title(sectionType.getDescription())
                    .score(first.getScore())
                    .items(items)
                    .suggestions(null)
                    .build();
        }
        // 单条类型：保持原有结构
        return ResumeDetailVO.ResumeSectionVO.builder()
                .id(first.getId())
                .type(typeCode)
                .title(first.getTitle())
                .content(first.getContent())
                .score(first.getScore())
                .items(null)
                .suggestions(null)
                .build();
    }

    /**
     * 设置评分默认值（如果数据库中为空）
     * 评分数据从数据库中读取，此方法确保空值被设置为默认值0
     */
    private void setDefaultScoresIfNeeded(ResumeDetailVO vo) {
        vo.setOverallScore(defaultZeroIfNull(vo.getOverallScore()));
        vo.setContentScore(defaultZeroIfNull(vo.getContentScore()));
        vo.setStructureScore(defaultZeroIfNull(vo.getStructureScore()));
        vo.setMatchingScore(defaultZeroIfNull(vo.getMatchingScore()));
        vo.setCompetitivenessScore(defaultZeroIfNull(vo.getCompetitivenessScore()));
    }

    /**
     * 如果值为null则返回0
     */
    private Integer defaultZeroIfNull(Integer value) {
        return value != null ? value : 0;
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
    public ResumeDetailVO updateResume(String id, UpdateResumeRequest request) {
        // TODO: 实现更新逻辑
        return null;
    }

    /**
     * 删除简历
     */
    public void deleteResume(String id) {
        // TODO: 实现删除逻辑
    }

    /**
     * 设置主简历
     */
    public void setPrimaryResume(String id) {
        // TODO: 实现设置逻辑
    }

    /**
     * 获取优化建议
     */
    public List<ResumeSuggestionVO> getResumeSuggestions(String id) {
        // TODO: 实现查询逻辑
        return null;
    }

    /**
     * 获取简历版本历史
     */
    public List<ResumeVersionVO> getVersionHistory(String resumeId) {
        LambdaQueryWrapper<ResumeVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeVersion::getResumeId, resumeId)
                .orderByDesc(ResumeVersion::getVersion);
        List<ResumeVersion> versions = resumeVersionMapper.selectList(wrapper);
        return resumeConvertor.toVersionVOList(versions);
    }

    /**
     * 获取指定版本详情
     */
    public ResumeDetailVO getVersionDetail(String resumeId, Integer version) {
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
    public ResumeVersion getVersionEntity(String resumeId, Integer version) {
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
    public List<ResumeSection> getResumeSections(String resumeId) {
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
    public Resume createResumeFromParsedData(String userId, ResumeParseResult parsedResume) {
        ResumeStructuredData data = parsedResume.getStructuredData();
        String targetPosition = data != null && data.getBasicInfo() != null
                ? Objects.toString(data.getBasicInfo().getTargetPosition(), "")
                : "";

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setName(parsedResume.getName() + "的简历");
        resume.setTargetPosition(targetPosition);
        resume.setMarkdownContent(parsedResume.getRawText());
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
    private void createResumeSections(String resumeId, ResumeStructuredData data, String rawText) {
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
    private void createBasicInfoSection(String resumeId, ResumeStructuredData.BasicInfo info) {
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
    private void createEducationSections(String resumeId, List<ResumeStructuredData.EducationExperience> educations) {
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
    private void createWorkSections(String resumeId, List<ResumeStructuredData.WorkExperience> works) {
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
    private void createProjectSections(String resumeId, List<ResumeStructuredData.ProjectExperience> projects) {
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
    private void createSkillsSection(String resumeId, List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return;
        }
        createSection(resumeId, "SKILLS", "专业技能", Map.of("skills", skills));
    }

    /**
     * 创建证书模块
     */
    private void createCertificateSections(String resumeId, List<ResumeStructuredData.Certificate> certificates) {
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
    private void createSection(String resumeId, String type, String title, Map<String, Object> content) {
        ResumeSection section = new ResumeSection();
        section.setResumeId(resumeId);
        section.setResumeVersionId(null);
        section.setType(type);
        section.setTitle(title);
        section.setContent(content);
        resumeSectionMapper.insert(section);
    }

    // ==================== 模块级 CRUD 操作 ====================

    /**
     * 更新单个模块内容
     *
     * @param sectionId 模块ID
     * @param content   新的内容
     */
    public void updateSection(String sectionId, Map<String, Object> content) {
        ResumeSection section = resumeSectionMapper.selectById(sectionId);
        if (section == null) {
            throw BusinessException.notFound("模块不存在");
        }
        section.setContent(content);
        resumeSectionMapper.updateById(section);
    }

    /**
     * 新增简历模块
     *
     * @param resumeId 简历ID
     * @param type     模块类型
     * @param title    模块标题
     * @param content  模块内容
     * @return 创建的模块实体
     */
    public ResumeSection createSectionPublic(String resumeId, String type, String title, Map<String, Object> content) {
        ResumeSection section = new ResumeSection();
        section.setResumeId(resumeId);
        section.setResumeVersionId(null);
        section.setType(type);
        section.setTitle(title);
        section.setContent(content);
        section.setScore(0);
        resumeSectionMapper.insert(section);
        return section;
    }

    /**
     * 删除简历模块
     *
     * @param sectionId 模块ID
     */
    public void deleteSection(String sectionId) {
        ResumeSection section = resumeSectionMapper.selectById(sectionId);
        if (section == null) {
            throw BusinessException.notFound("模块不存在");
        }
        resumeSectionMapper.deleteById(sectionId);
    }

    /**
     * 根据ID获取模块实体
     *
     * @param sectionId 模块ID
     * @return 模块实体
     */
    public ResumeSection getSectionById(String sectionId) {
        return resumeSectionMapper.selectById(sectionId);
    }

    /**
     * 更新简历诊断评分
     *
     * @param resumeId        简历ID
     * @param overallScore    综合评分
     * @param dimensionScores 维度评分
     */
    public void updateDiagnosisScores(String resumeId, Integer overallScore,
                                       DiagnoseResumeResponse.DimensionScores dimensionScores) {
        Resume resume = getById(resumeId);
        if (resume == null) {
            log.warn("简历不存在，无法更新诊断评分: resumeId={}", resumeId);
            return;
        }

        resume.setOverallScore(overallScore);
        if (dimensionScores != null) {
            resume.setContentScore(dimensionScores.getContent());
            resume.setStructureScore(dimensionScores.getStructure());
            resume.setMatchingScore(dimensionScores.getMatching());
            resume.setCompetitivenessScore(dimensionScores.getCompetitiveness());
        }

        // 同时更新 score 字段（兼容旧逻辑）
        resume.setScore(overallScore);

        updateById(resume);
        log.info("简历诊断评分已更新: resumeId={}, overallScore={}, content={}, structure={}, matching={}, competitiveness={}",
                resumeId, overallScore,
                dimensionScores != null ? dimensionScores.getContent() : 0,
                dimensionScores != null ? dimensionScores.getStructure() : 0,
                dimensionScores != null ? dimensionScores.getMatching() : 0,
                dimensionScores != null ? dimensionScores.getCompetitiveness() : 0);
    }

    /**
     * 批量更新简历模块评分
     *
     * @param resumeId      简历ID
     * @param sectionScores 模块评分 Map<sectionId, score>
     */
    public void updateSectionScores(String resumeId, Map<String, Integer> sectionScores) {
        if (sectionScores == null || sectionScores.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Integer> entry : sectionScores.entrySet()) {
            ResumeSection section = resumeSectionMapper.selectById(entry.getKey());
            if (section != null && section.getResumeId().equals(resumeId)) {
                section.setScore(entry.getValue());
                resumeSectionMapper.updateById(section);
            }
        }
        log.info("模块评分已更新: resumeId={}, count={}", resumeId, sectionScores.size());
    }
}
