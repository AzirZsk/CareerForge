package com.landit.interview.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.landit.interview.dto.interviewcenter.*;
import com.landit.interview.entity.InterviewReviewNote;

/**
 * 面试复盘笔记服务接口
 *
 * @author Azir
 */
public interface InterviewReviewNoteService extends IService<InterviewReviewNote> {

    /**
     * 获取面试的手动复盘笔记
     */
    InterviewReviewNote getManualNoteByInterviewId(String interviewId);

    /**
     * 获取面试的AI分析复盘笔记
     */
    InterviewReviewNote getAiAnalysisByInterviewId(String interviewId);

    /**
     * 保存手动复盘笔记
     */
    ReviewNoteVO saveManualNote(String interviewId, SaveReviewNoteRequest request);

    /**
     * 根据面试ID删除所有复盘笔记
     */
    void deleteByInterviewId(String interviewId);

}
