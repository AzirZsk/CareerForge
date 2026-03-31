package com.landit.interview.handler;

import com.landit.interview.dto.interviewcenter.*;
import com.landit.interview.service.InterviewReviewNoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * 获取手动复盘笔记
     */
    public ReviewNoteVO getManualNote(String interviewId) {
        var note = reviewNoteService.getManualNoteByInterviewId(interviewId);
        if (note == null) {
            return null;
        }
        var vo = new ReviewNoteVO();
        org.springframework.beans.BeanUtils.copyProperties(note, vo);
        return vo;
    }

    /**
     * 保存手动复盘笔记
     */
    @Transactional(rollbackFor = Exception.class)
    public ReviewNoteVO saveManualNote(String interviewId, SaveReviewNoteRequest request) {
        log.info("保存手动复盘笔记: interviewId={}", interviewId);
        return reviewNoteService.saveManualNote(interviewId, request);
    }

}
