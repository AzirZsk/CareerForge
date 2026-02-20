package com.landit.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.common.enums.ChangeType;
import com.landit.common.enums.ResumeStatus;
import com.landit.common.enums.ResumeType;
import com.landit.resume.convertor.ResumeConvertor;
import com.landit.resume.dto.CreateResumeRequest;
import com.landit.resume.dto.DeriveResumeRequest;
import com.landit.resume.dto.OptimizeResumeRequest;
import com.landit.resume.dto.OptimizeResumeResponse;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.dto.ResumeSuggestionVO;
import com.landit.resume.dto.ResumeParseResult;
import com.landit.resume.dto.ResumeVersionVO;
import com.landit.resume.dto.UpdateResumeRequest;
import com.landit.resume.entity.Resume;
import com.landit.resume.entity.ResumeSection;
import com.landit.resume.entity.ResumeVersion;
import com.landit.resume.mapper.ResumeMapper;
import com.landit.resume.mapper.ResumeSectionMapper;
import com.landit.resume.mapper.ResumeVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 简历服务实现类
 *
 * @author Azir
 */
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
        return null;
    }

    /**
     * 解析简历文件，提取姓名和性别
     * 当前返回空结果，后续接入 AI 实现
     */
    public ResumeParseResult parseResume(MultipartFile file) {
        return ResumeParseResult.builder()
                .name(null)
                .gender(null)
                .rawText(null)
                .build();
    }

    /**
     * 获取简历详情
     */
    public ResumeDetailVO getResumeDetail(Long id) {
        return null;
    }

    /**
     * 上传简历文件
     */
    public ResumeDetailVO uploadResume(MultipartFile file) {
        return null;
    }

    /**
     * 创建空白简历
     */
    public Resume createResume(CreateResumeRequest request) {
        return null;
    }

    /**
     * 更新简历
     */
    public ResumeDetailVO updateResume(Long id, UpdateResumeRequest request) {
        return null;
    }

    /**
     * 删除简历
     */
    public void deleteResume(Long id) {
    }

    /**
     * 设置主简历
     */
    public void setPrimaryResume(Long id) {
    }

    /**
     * 获取优化建议
     */
    public List<ResumeSuggestionVO> getResumeSuggestions(Long id) {
        return null;
    }

    /**
     * 应用优化建议
     */
    public ResumeDetailVO applyResumeSuggestion(Long id, Long suggestionId) {
        return null;
    }

    /**
     * AI优化简历
     */
    public OptimizeResumeResponse optimizeResume(Long id, OptimizeResumeRequest request) {
        return null;
    }

    /**
     * 导出简历PDF
     */
    public byte[] exportResume(Long id) {
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
        return null;
    }

    /**
     * 回滚到指定版本
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumeDetailVO rollbackToVersion(Long resumeId, Integer targetVersion) {
        // 获取当前简历
        Resume resume = getById(resumeId);
        if (resume == null) {
            throw new RuntimeException("简历不存在");
        }
        // 获取目标版本
        LambdaQueryWrapper<ResumeVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeVersion::getResumeId, resumeId)
                .eq(ResumeVersion::getVersion, targetVersion);
        ResumeVersion targetVersionEntity = resumeVersionMapper.selectOne(wrapper);
        if (targetVersionEntity == null) {
            throw new RuntimeException("目标版本不存在");
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
        updateById(resume);
        return getResumeDetail(resumeId);
    }

    /**
     * 基于主简历派生岗位定制简历
     */
    @Transactional(rollbackFor = Exception.class)
    public Resume deriveResume(Long sourceResumeId, DeriveResumeRequest request) {
        // 获取源简历
        Resume sourceResume = getById(sourceResumeId);
        if (sourceResume == null) {
            throw new RuntimeException("源简历不存在");
        }
        // 只有主简历才能派生
        if (sourceResume.getResumeType() != ResumeType.PRIMARY) {
            throw new RuntimeException("只能基于主简历派生");
        }
        // 创建派生简历
        Resume derivedResume = new Resume();
        derivedResume.setUserId(sourceResume.getUserId());
        derivedResume.setName(request.getResumeName() != null
                ? request.getResumeName()
                : sourceResume.getName() + "-" + request.getTargetPosition());
        derivedResume.setTargetPosition(request.getTargetPosition());
        derivedResume.setResumeType(ResumeType.DERIVED);
        derivedResume.setSourceResumeId(sourceResumeId);
        derivedResume.setVersion(1);
        derivedResume.setStatus(ResumeStatus.DRAFT);
        derivedResume.setScore(0);
        derivedResume.setCompleteness(0);
        derivedResume.setIsPrimary(false);
        save(derivedResume);
        // 复制源简历的内容模块到派生简历
        LambdaQueryWrapper<ResumeSection> sectionWrapper = new LambdaQueryWrapper<>();
        sectionWrapper.eq(ResumeSection::getResumeId, sourceResumeId)
                .isNull(ResumeSection::getResumeVersionId);
        List<ResumeSection> sourceSections = resumeSectionMapper.selectList(sectionWrapper);
        for (ResumeSection section : sourceSections) {
            ResumeSection newSection = new ResumeSection();
            newSection.setResumeId(derivedResume.getId());
            newSection.setType(section.getType());
            newSection.setTitle(section.getTitle());
            newSection.setContent(section.getContent());
            newSection.setScore(section.getScore());
            resumeSectionMapper.insert(newSection);
        }
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

}
