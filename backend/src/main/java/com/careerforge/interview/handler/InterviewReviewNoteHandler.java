package com.careerforge.interview.handler;

import com.careerforge.interview.dto.interviewcenter.*;
import com.careerforge.interview.service.InterviewReviewNoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 面试复盘笔记业务编排处理器
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewReviewNoteHandler {

    private final InterviewReviewNoteService reviewNoteService;

    /**
     * 获取复盘笔记
     */
    public ReviewNoteVO getNote(String interviewId) {
        var note = reviewNoteService.getByInterviewId(interviewId);
        if (note == null) {
            return null;
        }
        var vo = new ReviewNoteVO();
        BeanUtils.copyProperties(note, vo);
        return vo;
    }

    /**
     * 保存复盘笔记
     */
    @Transactional(rollbackFor = Exception.class)
    public ReviewNoteVO saveNote(String interviewId, SaveReviewNoteRequest request) {
        log.info("保存复盘笔记: interviewId={}", interviewId);
        return reviewNoteService.saveNote(interviewId, request);
    }

}
