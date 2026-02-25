package com.landit.resume.handler;

import com.landit.common.enums.ChangeType;
import com.landit.common.enums.ResumeType;
import com.landit.common.exception.BusinessException;
import com.landit.common.service.AIService;
import com.landit.common.service.FileToImageService;
import com.landit.common.service.SearchService;
import com.landit.common.util.JsonParseHelper;
import com.landit.resume.convertor.ResumeConvertor;
import com.landit.resume.dto.DeriveResumeRequest;
import com.landit.resume.dto.DiagnoseResumeRequest;
import com.landit.resume.dto.DiagnoseResumeResponse;
import com.landit.resume.dto.MatchJobRequest;
import com.landit.resume.dto.MatchJobResponse;
import com.landit.resume.dto.OptimizeResumeRequest;
import com.landit.resume.dto.OptimizeResumeResponse;
import com.landit.resume.dto.OptimizeSectionRequest;
import com.landit.resume.dto.OptimizeSectionResponse;
import com.landit.resume.dto.PrimaryResumeVO;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.entity.Resume;
import com.landit.resume.entity.ResumeSection;
import com.landit.resume.entity.ResumeVersion;
import com.landit.resume.service.ResumeService;
import com.landit.resume.mapper.ResumeSectionMapper;
import com.landit.resume.mapper.ResumeVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final AIService aiService;
    private final FileToImageService fileToImageService;
    private final SearchService searchService;
    private final ResumeVersionMapper resumeVersionMapper;
    private final ResumeSectionMapper resumeSectionMapper;
    private final ResumeConvertor resumeConvertor;

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
     * 回滚到指定版本
     * 涉及：创建当前版本快照 -> 恢复目标版本数据
     *
     * @param resumeId     简历ID
     * @param targetVersion 目标版本号
     * @return 回滚后的简历详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumeDetailVO rollbackToVersion(String resumeId, Integer targetVersion) {
        // 获取当前简历
        Resume resume = resumeService.getById(resumeId);
        if (resume == null) {
            throw BusinessException.notFound("简历不存在");
        }

        // 获取目标版本
        ResumeVersion targetVersionEntity = resumeService.getVersionEntity(resumeId, targetVersion);
        if (targetVersionEntity == null) {
            throw BusinessException.notFound("目标版本不存在");
        }

        // 创建当前版本快照
        createVersionSnapshot(resume, "回滚到版本 " + targetVersion, ChangeType.ROLLBACK);

        // 恢复目标版本数据到主表
        resume.setName(targetVersionEntity.getName());
        resume.setTargetPosition(targetVersionEntity.getTargetPosition());
        resume.setStatus(targetVersionEntity.getStatus());
        resume.setScore(targetVersionEntity.getScore());
        resume.setCompleteness(targetVersionEntity.getCompleteness());
        resume.setVersion(resume.getVersion() + 1);
        resumeService.updateById(resume);

        log.info("简历版本回滚成功: resumeId={}, targetVersion={}", resumeId, targetVersion);
        return resumeService.getResumeDetail(resumeId);
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

        // 只有主简历才能派生
        if (sourceResume.getResumeType() != ResumeType.PRIMARY) {
            throw new BusinessException("只能基于主简历派生");
        }

        // 创建派生简历
        Resume derivedResume = resumeService.createDerivedResume(sourceResume, request);

        // 复制源简历的内容模块到派生简历
        copySectionsToDerivedResume(sourceResumeId, derivedResume.getId());

        log.info("派生简历创建成功: sourceResumeId={}, derivedResumeId={}", sourceResumeId, derivedResume.getId());
        return derivedResume;
    }

    /**
     * 创建版本快照
     */
    private void createVersionSnapshot(Resume resume, String changeSummary, ChangeType changeType) {
        ResumeVersion version = resumeConvertor.toResumeVersion(resume);
        version.setChangeSummary(changeSummary);
        version.setChangeType(changeType);
        resumeVersionMapper.insert(version);
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
     * @param content   新的模块内容
     * @return 更新后的简历详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumeDetailVO updateResumeSection(String resumeId, String sectionId, Map<String, Object> content) {
        // 更新模块内容
        resumeService.updateSection(sectionId, content);
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
     * @param type     模块类型
     * @param title    模块标题
     * @param content  模块内容
     * @return 更新后的简历详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumeDetailVO addResumeSection(String resumeId, String type, String title, Map<String, Object> content) {
        // 创建模块
        resumeService.createSectionPublic(resumeId, type, title, content);
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

    // ==================== 简历优化相关方法 ====================

    /**
     * 简历诊断
     *
     * @param id      简历ID
     * @param request 诊断请求
     * @return 诊断结果
     */
    public DiagnoseResumeResponse diagnoseResume(String id, DiagnoseResumeRequest request) {
        // 获取简历详情
        ResumeDetailVO resumeDetail = resumeService.getResumeDetail(id);
        if (resumeDetail == null) {
            throw new BusinessException("简历不存在");
        }

        // 将简历内容转为JSON字符串
        String resumeContent = toJsonString(resumeDetail);

        // 根据模式选择诊断方式
        if (request.isPreciseMode()) {
            // 精准模式：先搜索，再诊断
            String searchQuery = request.getTargetPosition() + " 技能要求 岗位职责 2025";
            String searchResults = searchService.search(searchQuery);
            return aiService.diagnoseResumePrecise(resumeContent, request.getTargetPosition(), searchResults);
        } else {
            // 快速模式：直接诊断
            return aiService.diagnoseResume(resumeContent, request.getTargetPosition());
        }
    }

    /**
     * 优化简历模块
     *
     * @param id      简历ID
     * @param request 优化请求
     * @return 优化结果
     */
    @Transactional(rollbackFor = Exception.class)
    public OptimizeSectionResponse optimizeSection(String id, OptimizeSectionRequest request) {
        // 获取简历详情
        ResumeDetailVO resumeDetail = resumeService.getResumeDetail(id);
        if (resumeDetail == null) {
            throw new BusinessException("简历不存在");
        }

        // 调用AI优化
        OptimizeSectionResponse response = aiService.optimizeSection(
                request.getSectionType(),
                request.getOriginalContent(),
                request.getTargetPosition()
        );

        log.info("模块优化完成: resumeId={}, sectionType={}, confidence={}",
                id, request.getSectionType(), response.getConfidence());

        return response;
    }

    /**
     * 岗位JD匹配分析
     *
     * @param id      简历ID
     * @param request 匹配请求
     * @return 匹配结果
     */
    public MatchJobResponse matchJob(String id, MatchJobRequest request) {
        // 获取简历详情
        ResumeDetailVO resumeDetail = resumeService.getResumeDetail(id);
        if (resumeDetail == null) {
            throw new BusinessException("简历不存在");
        }

        // 将简历内容转为JSON字符串
        String resumeContent = toJsonString(resumeDetail);

        // 调用AI匹配分析
        return aiService.matchJobDescription(resumeContent, request.getJobDescription());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 对象转JSON字符串
     */
    private String toJsonString(Object obj) {
        return JsonParseHelper.toJsonString(obj);
    }

}
