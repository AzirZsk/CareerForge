package com.careerforge.resume.handler;

import com.careerforge.common.enums.ResumeStatus;
import com.careerforge.common.enums.ResumeType;
import com.careerforge.common.exception.BusinessException;
import com.careerforge.common.service.AIService;
import com.careerforge.common.service.FileToImageService;
import com.careerforge.common.util.JsonParseHelper;
import com.careerforge.resume.convertor.ResumeConvertor;
import com.careerforge.resume.dto.AddSectionRequest;
import com.careerforge.resume.dto.ApplyOptimizeRequest;
import com.careerforge.resume.dto.DeriveResumeRequest;
import com.careerforge.resume.dto.UpdateResumeRequest;
import com.careerforge.resume.dto.UpdateSectionRequest;
import com.careerforge.resume.dto.DiagnoseResumeRequest;
import com.careerforge.resume.dto.DiagnoseResumeResponse;
import com.careerforge.resume.dto.MatchJobRequest;
import com.careerforge.resume.dto.MatchJobResponse;
import com.careerforge.resume.dto.OptimizeResumeRequest;
import com.careerforge.resume.dto.OptimizeResumeResponse;
import com.careerforge.resume.dto.CreateResumeRequest;
import com.careerforge.resume.dto.PrimaryResumeVO;
import com.careerforge.resume.dto.ResumeListVO;
import com.careerforge.resume.dto.ResumeDetailVO;
import com.careerforge.resume.entity.Resume;
import com.careerforge.resume.entity.ResumeSection;
import com.careerforge.resume.entity.ResumeSuggestion;
import com.careerforge.resume.graph.optimize.DiagnoseResumeNode;
import com.careerforge.resume.service.ResumeSectionService;
import com.careerforge.resume.service.ResumeService;
import com.careerforge.resume.service.ResumeSuggestionService;
import com.careerforge.resume.mapper.ResumeSectionMapper;
import com.alibaba.cloud.ai.graph.OverAllState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.careerforge.resume.graph.optimize.ResumeOptimizeGraphConstants.STATE_RESUME_CONTENT;

/**
 * 简历业务编排处理器
 * 负责处理涉及文件处理、AI调用、跨表操作等聚合操作
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeHandler {

    private final ResumeService resumeService;
    private final ResumeSectionService resumeSectionService;
    private final AIService aiService;
    private final FileToImageService fileToImageService;
    private final ResumeSectionMapper resumeSectionMapper;
    private final ResumeConvertor resumeConvertor;
    private final ResumeSuggestionService resumeSuggestionService;
    private final DiagnoseResumeNode diagnoseResumeNode;

    /**
     * 获取主简历
     *
     * @return 主简历VO，不存在则返回 null
     */
    public PrimaryResumeVO getPrimaryResume() {
        Resume resume = resumeService.getPrimaryResume();
        if (resume == null) {
            return null;
        }

        // 判断是否已完成分析（根据评分和完整度）
        boolean analyzed = resumeService.isResumeAnalyzed(resume);

        PrimaryResumeVO vo = new PrimaryResumeVO();
        vo.setId(resume.getId());
        vo.setName(resume.getName());
        vo.setTargetPosition(resume.getTargetPosition());
        vo.setStatus(resume.getStatus());
        vo.setScore(resume.getScore());
        vo.setCompleteness(resume.getCompleteness());
        vo.setAnalyzed(analyzed);
        vo.setCreatedAt(resume.getCreatedAt());
        vo.setUpdatedAt(resume.getUpdatedAt());

        return vo;
    }

    // ==================== 简历列表管理方法 ====================

    /**
     * 获取所有简历列表
     *
     * @param status 简历状态筛选（可选）
     * @return 简历列表
     */
    public List<ResumeListVO> getAllResumes(String status) {
        List<Resume> resumes = resumeService.getAllResumes(status);
        return resumes.stream()
                .map(this::toListVO)
                .collect(Collectors.toList());
    }

    /**
     * 创建空白简历
     *
     * @param request 创建请求
     * @return 创建后的简历详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumeDetailVO createBlankResume(CreateResumeRequest request) {
        Resume resume = resumeService.createBlankResume(
                request.getName(),
                request.getTargetPosition()
        );
        return resumeService.getResumeDetail(resume.getId());
    }

    /**
     * 删除简历
     *
     * @param resumeId 简历ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteResume(String resumeId) {
        resumeService.deleteResume(resumeId);
    }

    /**
     * 设置主简历
     *
     * @param resumeId 简历ID
     * @return 更新后的主简历信息
     */
    @Transactional(rollbackFor = Exception.class)
    public PrimaryResumeVO setPrimaryResume(String resumeId) {
        resumeService.setPrimaryResume(resumeId);
        return getPrimaryResume();
    }

    /**
     * 更新简历基本信息（名称和目标岗位）
     *
     * @param resumeId 简历ID
     * @param request  更新请求
     * @return 更新后的简历详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumeDetailVO updateResumeBasicInfo(String resumeId, UpdateResumeRequest request) {
        resumeService.updateResumeBasicInfo(resumeId, request.getName(), request.getTargetPosition());
        return resumeService.getResumeDetail(resumeId);
    }

    /**
     * 将 Resume 实体转换为 ResumeListVO
     */
    private ResumeListVO toListVO(Resume resume) {
        return ResumeListVO.builder()
                .id(resume.getId())
                .name(resume.getName())
                .targetPosition(resume.getTargetPosition())
                .status(resume.getStatus())
                .score(resume.getScore())
                .completeness(resume.getCompleteness())
                .isPrimary(resume.getIsPrimary())
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .build();
    }

    /**
     * 获取简历详情
     *
     * @param id 简历ID
     * @return 简历详情VO
     */
    public ResumeDetailVO getResumeDetail(String id) {
        return resumeService.getResumeDetail(id);
    }

    /**
     * 解析简历文件，将文件转换为图片列表
     * 返回的图片列表可用于视觉模型解析
     *
     * @param file 上传的简历文件
     * @return Base64编码的图片列表
     */
    public List<String> parseResumeToImages(MultipartFile file) {
        // 校验文件类型
        String filename = file.getOriginalFilename();
        if (!fileToImageService.isSupported(file.getContentType(), filename)) {
            throw new BusinessException("不支持的文件格式，请上传图片（jpg/png）、PDF 或 Word 文档");
        }

        // 将文件转换为图片
        List<String> imageDataUrls = fileToImageService.convertToImages(file);
        if (imageDataUrls.isEmpty()) {
            throw new BusinessException("文件转换失败，无法提取简历内容");
        }

        log.info("简历文件转换为 {} 张图片", imageDataUrls.size());
        return imageDataUrls;
    }

    /**
     * 上传简历文件
     * 涉及：文件解析 -> 创建简历记录 -> 创建内容模块
     *
     * @param file 上传的简历文件
     * @return 简历详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumeDetailVO uploadResume(MultipartFile file) {
        // TODO: 实现上传逻辑
        // 1. 解析文件为图片
        // 2. 调用 AI 提取结构化内容
        // 3. 创建简历记录
        // 4. 创建内容模块
        log.info("上传简历文件: {}", file.getOriginalFilename());
        return null;
    }

    /**
     * 应用优化建议
     * 涉及：多表操作（更新简历内容、更新建议状态）
     *
     * @param id           简历ID
     * @param suggestionId 建议ID
     * @return 更新后的简历详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumeDetailVO applyResumeSuggestion(String id, String suggestionId) {
        // TODO: 实现应用建议逻辑
        log.info("应用优化建议: resumeId={}, suggestionId={}", id, suggestionId);
        return null;
    }

    /**
     * AI优化简历
     * 涉及：AI调用 -> 创建版本快照 -> 更新简历内容
     *
     * @param id      简历ID
     * @param request 优化请求
     * @return 优化结果
     */
    @Transactional(rollbackFor = Exception.class)
    public OptimizeResumeResponse optimizeResume(String id, OptimizeResumeRequest request) {
        // TODO: 实现 AI 优化逻辑
        log.info("AI优化简历: resumeId={}", id);
        return null;
    }

    /**
     * 导出简历PDF
     * 涉及：读取简历内容 -> 生成PDF
     *
     * @param id 简历ID
     * @return PDF字节数组
     */
    public byte[] exportResume(String id) {
        // TODO: 实现 PDF 导出逻辑
        log.info("导出简历PDF: resumeId={}", id);
        return null;
    }

    /**
     * 基于主简历派生岗位定制简历
     * 涉及：验证源简历 -> 创建派生简历 -> 复制内容模块
     *
     * @param sourceResumeId 源简历ID
     * @param request        派生请求
     * @return 派生后的简历
     */
    @Transactional(rollbackFor = Exception.class)
    public Resume deriveResume(String sourceResumeId, DeriveResumeRequest request) {
        // 获取源简历
        Resume sourceResume = resumeService.getById(sourceResumeId);
        if (sourceResume == null) {
            throw BusinessException.notFound("源简历不存在");
        }

        // 只有已优化的简历才能定制
        if (!ResumeStatus.OPTIMIZED.getValue().equals(sourceResume.getStatus())) {
            throw new BusinessException("只能基于已优化的简历进行定制");
        }

        // 创建派生简历
        Resume derivedResume = resumeService.createDerivedResume(sourceResume, request);

        // 复制源简历的内容模块到派生简历
        copySectionsToDerivedResume(sourceResumeId, derivedResume.getId());

        log.info("派生简历创建成功: sourceResumeId={}, derivedResumeId={}", sourceResumeId, derivedResume.getId());
        return derivedResume;
    }

    /**
     * 复制内容模块到派生简历
     */
    private void copySectionsToDerivedResume(String sourceResumeId, String derivedResumeId) {
        List<ResumeSection> sourceSections = resumeService.getResumeSections(sourceResumeId);
        for (ResumeSection section : sourceSections) {
            ResumeSection newSection = new ResumeSection();
            newSection.setResumeId(derivedResumeId);
            newSection.setType(section.getType());
            newSection.setTitle(section.getTitle());
            newSection.setContent(section.getContent());
            newSection.setScore(section.getScore());
            resumeSectionMapper.insert(newSection);
        }
    }

    // ==================== 模块级操作 ====================

    /**
     * 更新简历模块
     * 涉及：更新模块内容 -> 重新计算简历评分
     *
     * @param resumeId  简历ID
     * @param sectionId 模块ID
     * @param request   更新请求
     * @return 更新后的简历详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumeDetailVO updateResumeSection(String resumeId, String sectionId, UpdateSectionRequest request) {
        // 更新模块内容
        resumeService.updateSection(sectionId, request.getContent());
        // 重新计算简历评分
        recalculateResumeScore(resumeId);
        // 返回更新后的详情
        return resumeService.getResumeDetail(resumeId);
    }

    /**
     * 新增简历模块
     * 涉及：创建模块 -> 重新计算完整度
     *
     * @param resumeId 简历ID
     * @param request  新增请求
     * @return 更新后的简历详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumeDetailVO addResumeSection(String resumeId, AddSectionRequest request) {
        // content 已经是前端序列化好的 JSON 字符串，直接使用
        resumeService.createSectionPublic(resumeId, request.getType(), request.getTitle(), request.getContent());
        // 重新计算简历完整度
        recalculateResumeCompleteness(resumeId);
        // 返回更新后的详情
        return resumeService.getResumeDetail(resumeId);
    }

    /**
     * 删除简历模块
     * 涉及：删除模块 -> 重新计算完整度
     *
     * @param resumeId  简历ID
     * @param sectionId 模块ID
     * @return 更新后的简历详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumeDetailVO deleteResumeSection(String resumeId, String sectionId) {
        // 删除模块
        resumeService.deleteSection(sectionId);
        // 重新计算简历完整度
        recalculateResumeCompleteness(resumeId);
        // 返回更新后的详情
        return resumeService.getResumeDetail(resumeId);
    }

    /**
     * 批量应用优化变更
     * 将优化后的区块内容完全替换简历的区块（完全替换策略）
     *
     * @param resumeId 简历ID
     * @param request  应用变更请求（包含优化前后的区块数据）
     * @return 更新后的简历详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumeDetailVO applyOptimizeChanges(String resumeId, ApplyOptimizeRequest request) {
        List<ApplyOptimizeRequest.SectionDataItem> afterSections = request.getAfterSection();

        log.info("批量应用优化变更（完全替换）: resumeId={}, 区块数量={}", resumeId, afterSections.size());

        // 1. 缓存旧 section 评分，删除后新 section 需要继承旧评分
        List<ResumeSection> oldSections = resumeService.getResumeSections(resumeId);
        Map<String, Integer> oldScoreMap = oldSections.stream()
                .filter(s -> s.getScore() != null && s.getScore() > 0)
                .collect(Collectors.toMap(ResumeSection::getId, ResumeSection::getScore, (a, b) -> b));

        // 2. 删除该简历的所有现有区块
        resumeSectionService.deleteAllByResumeId(resumeId);

        // 3. 根据传入数据创建新区块，继承旧评分
        for (ApplyOptimizeRequest.SectionDataItem section : afterSections) {
            Integer oldScore = oldScoreMap.getOrDefault(section.getId(), 0);
            resumeSectionService.createWithScore(
                resumeId,
                section.getType(),
                section.getTitle(),
                section.getContent(),
                oldScore
            );
        }

        // 4. 删除该简历的所有优化建议（内容已更新，旧建议不再适用）
        resumeSuggestionService.deleteByResumeId(resumeId);
        log.info("已清除简历的优化建议: resumeId={}", resumeId);

        // 5. 评分更新：用户未编辑时跳过AI评分，使用估算分数
        boolean shouldSkipScoring = Boolean.TRUE.equals(request.getSkipScoring());
        if (shouldSkipScoring && request.getEstimatedOverallScore() != null) {
            log.info("跳过AI评分，使用估算分数: resumeId={}, estimatedScore={}", resumeId, request.getEstimatedOverallScore());
            resumeService.updateOverallScore(resumeId, request.getEstimatedOverallScore());
        } else {
            triggerDiagnosis(resumeId);
        }

        // 6. 重新计算简历完整度
        recalculateResumeCompleteness(resumeId);

        // 7. 将简历状态更新为"已优化"（用户已应用优化变更）
        resumeService.updateStatus(resumeId, ResumeStatus.OPTIMIZED);

        // 8. 返回更新后的简历详情
        return resumeService.getResumeDetail(resumeId);
    }

    /**
     * 重新计算简历评分
     * 基于所有模块评分的平均值
     */
    private void recalculateResumeScore(String resumeId) {
        List<ResumeSection> sections = resumeService.getResumeSections(resumeId);
        int avgScore = 0;
        if (!sections.isEmpty()) {
            double sum = sections.stream()
                    .filter(s -> s.getScore() != null && s.getScore() > 0)
                    .mapToInt(ResumeSection::getScore)
                    .average()
                    .orElse(0);
            avgScore = (int) Math.round(sum);
        }
        Resume resume = resumeService.getById(resumeId);
        if (resume != null) {
            resume.setScore(avgScore);
            resumeService.updateById(resume);
        }
    }

    /**
     * 触发简历诊断，更新评分
     * 调用 DiagnoseResumeNode 对简历进行 AI 诊断，获取真实评分
     *
     * @param resumeId 简历ID
     */
    private void triggerDiagnosis(String resumeId) {
        try {
            // 获取简历详情
            ResumeDetailVO resumeDetail = resumeService.getResumeDetail(resumeId);
            if (resumeDetail == null) {
                log.warn("简历不存在，无法诊断: resumeId={}", resumeId);
                return;
            }

            // 构建 OverAllState
            Map<String, Object> initialState = new HashMap<>();
            initialState.put(STATE_RESUME_CONTENT, resumeDetail);

            OverAllState state = new OverAllState(initialState);

            // 调用诊断节点
            diagnoseResumeNode.apply(state);

            log.info("应用变更后诊断完成: resumeId={}", resumeId);
        } catch (Exception e) {
            // 诊断失败只记录日志，不影响主流程
            log.error("应用变更后诊断失败: resumeId={}", resumeId, e);
        }
    }

    /**
     * 删除单条优化建议
     *
     * @param resumeId     简历ID
     * @param suggestionId 建议ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSuggestion(String resumeId, String suggestionId) {
        // 验证建议存在
        ResumeSuggestion suggestion = resumeSuggestionService.getById(suggestionId);
        if (suggestion == null) {
            throw BusinessException.notFound("建议不存在");
        }
        // 验证建议属于该简历
        if (!resumeId.equals(suggestion.getResumeId())) {
            throw new BusinessException("建议不属于该简历");
        }
        // 删除建议
        resumeSuggestionService.deleteSuggestionById(suggestionId);
        log.info("已删除优化建议: suggestionId={}, resumeId={}", suggestionId, resumeId);
    }

    /**
     * 重新计算简历完整度
     * 基于模块数量和类型
     */
    private void recalculateResumeCompleteness(String resumeId) {
        List<ResumeSection> sections = resumeService.getResumeSections(resumeId);
        // 模块类型对应的完整度分数
        Map<String, Integer> typeScores = Map.of(
                "BASIC_INFO", 40,
                "EDUCATION", 20,
                "WORK", 20,
                "SKILLS", 10,
                "PROJECT", 10
        );
        // 统计已有的模块类型
        Set<String> existingTypes = sections.stream()
                .map(ResumeSection::getType)
                .collect(Collectors.toSet());
        // 计算完整度分数
        int completeness = existingTypes.stream()
                .mapToInt(type -> typeScores.getOrDefault(type, 0))
                .sum();
        Resume resume = resumeService.getById(resumeId);
        if (resume != null) {
            resume.setCompleteness(completeness);
            resumeService.updateById(resume);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 对象转JSON字符串
     */
    private String toJsonString(Object obj) {
        return JsonParseHelper.toJsonString(obj);
    }

}
