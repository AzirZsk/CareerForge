package com.careerforge.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.careerforge.interview.dto.interviewcenter.*;
import com.careerforge.interview.entity.InterviewReviewNote;
import com.careerforge.interview.mapper.InterviewReviewNoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 面试复盘笔记服务
 * 仅处理用户手动记录的复盘笔记
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewReviewNoteService extends ServiceImpl<InterviewReviewNoteMapper, InterviewReviewNote> {

    /**
     * 根据面试ID获取复盘笔记
     */
    public InterviewReviewNote getByInterviewId(String interviewId) {
        LambdaQueryWrapper<InterviewReviewNote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewReviewNote::getInterviewId, interviewId);
        return this.getOne(wrapper);
    }

    /**
     * 保存复盘笔记
     */
    @Transactional(rollbackFor = Exception.class)
    public ReviewNoteVO saveNote(String interviewId, SaveReviewNoteRequest request) {
        InterviewReviewNote note = getByInterviewId(interviewId);
        if (note == null) {
            note = new InterviewReviewNote();
            note.setInterviewId(interviewId);
        }
        note.setOverallFeeling(request.getOverallFeeling());
        note.setHighPoints(request.getHighPoints());
        note.setWeakPoints(request.getWeakPoints());
        note.setLessonsLearned(request.getLessonsLearned());
        this.saveOrUpdate(note);
        log.info("保存复盘笔记成功: interviewId={}", interviewId);
        return convertToVO(note);
    }

    /**
     * 根据面试ID删除复盘笔记
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByInterviewId(String interviewId) {
        LambdaQueryWrapper<InterviewReviewNote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewReviewNote::getInterviewId, interviewId);
        this.remove(wrapper);
        log.info("删除复盘笔记: interviewId={}", interviewId);
    }

    private ReviewNoteVO convertToVO(InterviewReviewNote note) {
        ReviewNoteVO vo = new ReviewNoteVO();
        BeanUtils.copyProperties(note, vo);
        return vo;
    }

}
