package com.landit.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.common.enums.ResumeStatus;
import com.landit.common.enums.ResumeType;
import com.landit.common.enums.SectionType;
import com.landit.common.exception.BusinessException;
import com.landit.common.util.JsonParseHelper;
import com.landit.resume.convertor.ResumeConvertor;
import com.landit.resume.dto.CreateResumeRequest;
import com.landit.resume.dto.DeriveResumeRequest;
import com.landit.resume.dto.DiagnoseResumeResponse;
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

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
        // 每种类型一条记录，直接映射
        vo.setSections(sections.stream().map(this::buildSectionVO).collect(Collectors.toList()));

        // 设置是否已完成分析
        vo.setAnalyzed(isResumeAnalyzed(resume));

        // 设置评分默认值
        setDefaultScoresIfNeeded(vo);

        return vo;
    }

    /**
     * 构建单个SectionVO
     * 聚合类型从 content 解析数组并生成 items，单条类型直接返回 content
     *
     * @param section 简历模块实体
     * @return SectionVO
     */
    private ResumeDetailVO.ResumeSectionVO buildSectionVO(ResumeSection section) {
        SectionType sectionType = SectionType.fromCode(section.getType());

        // 聚合类型：从 content 解析数组并生成 items
        if (sectionType != null && sectionType.isAggregate()) {
            List<?> items = JsonParseHelper.parseToEntity(section.getContent(), new TypeReference<List<Object>>() {});
            List<ResumeDetailVO.ResumeSectionItemVO> itemVOs = new ArrayList<>();
            if (items != null) {
                for (int i = 0; i < items.size(); i++) {
                    itemVOs.add(ResumeDetailVO.ResumeSectionItemVO.builder()
                        .id(section.getId() + "_" + i)
                        .title(extractTitle(items.get(i), sectionType))
                        .content(toJsonString(items.get(i)))
                        .build());
                }
            }
            return ResumeDetailVO.ResumeSectionVO.builder()
                .id(section.getId())
                .type(section.getType())
                .title(sectionType.getDescription())
                .score(section.getScore())
                .items(itemVOs)
                .suggestions(null)
                .build();
        }
        // 单条类型：保持原有结构，content 直接返回 JSON 字符串
        return ResumeDetailVO.ResumeSectionVO.builder()
            .id(section.getId())
            .type(section.getType())
            .title(section.getTitle())
            .content(section.getContent())
            .score(section.getScore())
            .items(null)
            .suggestions(null)
            .build();
    }

    /**
     * 从聚合类型的单个元素中提取标题
     *
     * @param item 单个元素（Map 类型）
     * @param type 区块类型
     * @return 提取的标题
     */
    private String extractTitle(Object item, SectionType type) {
        if (item instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) item;
            switch (type) {
                case WORK:
                    return Objects.toString(map.get("company"), "工作经历");
                case PROJECT:
                    return Objects.toString(map.get("name"), "项目经验");
                case EDUCATION:
                    return Objects.toString(map.get("school"), "教育经历");
                case CERTIFICATE:
                    return Objects.toString(map.get("name"), "证书");
                case OPEN_SOURCE:
                    return Objects.toString(map.get("projectName"), "开源贡献");
                case CUSTOM:
                    return Objects.toString(map.get("title"), "自定义区块");
                case SKILLS:
                    return "技能";
                default:
                    return type.getDescription();
            }
        }
        return type.getDescription();
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
                .ne(ResumeSection::getType, SectionType.RAW_TEXT.getCode())
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
     * @param userId 用户ID
     * @param data   解析后的简历数据
     * @return 创建的简历实体
     */
    public Resume createResumeFromParsedData(String userId, ResumeStructuredData data) {
        Objects.requireNonNull(data);
        String targetPosition = data.getBasicInfo() != null
                ? Objects.toString(data.getBasicInfo().getTargetPosition(), "")
                : "";

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setName(data.getBasicInfo().getName() + "的简历");
        resume.setTargetPosition(targetPosition);
        resume.setMarkdownContent(data.getMarkdownContent());
        resume.setResumeType(ResumeType.PRIMARY);
        resume.setVersion(1);
        resume.setStatus(ResumeStatus.DRAFT);
        resume.setIsPrimary(true);
        resume.setCompleteness(calculateCompleteness(data));
        save(resume);
        log.info("简历主记录创建完成: resumeId={}", resume.getId());

        createResumeSections(resume.getId(), data);

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
    private void createResumeSections(String resumeId, ResumeStructuredData data) {
        createSection(resumeId, SectionType.RAW_TEXT, SectionType.RAW_TEXT.getDescription(), toJsonString(data));
        createBasicInfoSection(resumeId, data.getBasicInfo());
        createAggregateSection(resumeId, SectionType.EDUCATION, data.getEducation());
        createAggregateSection(resumeId, SectionType.WORK, data.getWork());
        createAggregateSection(resumeId, SectionType.PROJECT, data.getProjects());
        createAggregateSection(resumeId, SectionType.SKILLS, data.getSkills());
        createAggregateSection(resumeId, SectionType.CERTIFICATE, data.getCertificates());
        createAggregateSection(resumeId, SectionType.OPEN_SOURCE, data.getOpenSource());
        createAggregateSection(resumeId, SectionType.CUSTOM, data.getCustomSections());

        log.info("简历模块创建完成: resumeId={}", resumeId);
    }

    /**
     * 创建基本信息模块
     */
    private void createBasicInfoSection(String resumeId, ResumeStructuredData.BasicInfo info) {
        if (info == null) {
            return;
        }
        createSection(resumeId, SectionType.BASIC_INFO, SectionType.BASIC_INFO.getDescription(), toJsonString(info));
    }

    /**
     * 创建聚合类型区块（每种类型只保存一条记录，content 存储数组 JSON）
     *
     * @param resumeId 简历ID
     * @param type     区块类型
     * @param items    区块内容列表
     */
    private void createAggregateSection(String resumeId, SectionType type, List<?> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        createSection(resumeId, type, type.getDescription(), toJsonString(items));
    }

    /**
     * 通用方法：创建并保存一个简历模块
     */
    private void createSection(String resumeId, SectionType type, String title, String content) {
        ResumeSection section = new ResumeSection();
        section.setResumeId(resumeId);
        section.setResumeVersionId(null);
        section.setType(type.getCode());
        section.setTitle(title);
        section.setContent(content);
        resumeSectionMapper.insert(section);
    }

    /**
     * 将对象序列化为JSON字符串
     */
    private String toJsonString(Object obj) {
        return JsonParseHelper.toJsonString(obj);
    }

    // ==================== 模块级 CRUD 操作 ====================

    /**
     * 更新单个模块内容
     *
     * @param sectionId 模块ID
     * @param content   新的内容（JSON字符串）
     */
    public void updateSection(String sectionId, String content) {
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
     * @param content  模块内容（JSON字符串）
     * @return 创建的模块实体
     */
    public ResumeSection createSectionPublic(String resumeId, String type, String title, String content) {
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
