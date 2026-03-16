package com.landit.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.resume.entity.ResumeSection;
import com.landit.resume.mapper.ResumeSectionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 简历区块服务
 * 负责简历区块的 CRUD 操作
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeSectionService extends ServiceImpl<ResumeSectionMapper, ResumeSection> {

    /**
     * 删除简历的所有区块（非版本快照）
     *
     * @param resumeId 简历ID
     */
    public void deleteAllByResumeId(String resumeId) {
        LambdaQueryWrapper<ResumeSection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeSection::getResumeId, resumeId)
               .isNull(ResumeSection::getResumeVersionId);
        remove(wrapper);
        log.info("已删除简历的所有区块: resumeId={}", resumeId);
    }

    /**
     * 创建简历区块
     *
     * @param resumeId 简历ID
     * @param type     区块类型
     * @param title    区块标题
     * @param content  区块内容（JSON字符串）
     * @return 创建的区块实体
     */
    public ResumeSection create(String resumeId, String type, String title, String content) {
        ResumeSection section = new ResumeSection();
        section.setResumeId(resumeId);
        section.setResumeVersionId(null);
        section.setType(type);
        section.setTitle(title);
        section.setContent(content);
        section.setScore(0);
        save(section);
        return section;
    }
}
