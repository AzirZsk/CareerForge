package com.landit.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.interview.dto.interviewcenter.*;
import com.landit.interview.entity.InterviewReviewNote;
import com.landit.interview.mapper.InterviewReviewNoteMapper;
import com.landit.interview.service.InterviewReviewNoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 面试复盘笔记服务实现类
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewReviewNoteServiceImpl extends ServiceImpl<InterviewReviewNoteMapper, InterviewReviewNote> implements InterviewReviewNoteService {

    @Override
    public InterviewReviewNote getManualNoteByInterviewId(String interviewId) {
        LambdaQueryWrapper<InterviewReviewNote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewReviewNote::getInterviewId, interviewId)
                .eq(InterviewReviewNote::getType, "manual");
        return this.getOne(wrapper);
    }

    @Override
    public InterviewReviewNote getAiAnalysisByInterviewId(String interviewId) {
        LambdaQueryWrapper<InterviewReviewNote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewReviewNote::getInterviewId, interviewId)
                .eq(InterviewReviewNote::getType, "ai_analysis");
        return this.getOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewNoteVO saveManualNote(String interviewId, SaveReviewNoteRequest request) {
        InterviewReviewNote note = getManualNoteByInterviewId(interviewId);
        if (note == null) {
            note = new InterviewReviewNote();
            note.setInterviewId(interviewId);
            note.setType("manual");
        }
        note.setOverallFeeling(request.getOverallFeeling());
        note.setHighPoints(request.getHighPoints());
        note.setWeakPoints(request.getWeakPoints());
        note.setLessonsLearned(request.getLessonsLearned());
        this.saveOrUpdate(note);
        log.info("保存手动复盘笔记成功: interviewId={}", interviewId);
        return convertToVO(note);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByInterviewId(String interviewId) {
        LambdaQueryWrapper<InterviewReviewNote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewReviewNote::getInterviewId, interviewId);
        this.remove(wrapper);
        log.info("删除面试所有复盘笔记: interviewId={}", interviewId);
    }

    private ReviewNoteVO convertToVO(InterviewReviewNote note) {
        ReviewNoteVO vo = new ReviewNoteVO();
        BeanUtils.copyProperties(note, vo);
        return vo;
    }

}
