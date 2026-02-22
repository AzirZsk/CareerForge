package com.landit.review.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.common.response.PageResponse;
import com.landit.review.dto.InterviewReviewVO;
import com.landit.review.dto.ReviewListItemVO;
import com.landit.review.entity.InterviewReview;
import com.landit.review.mapper.InterviewReviewMapper;
import org.springframework.stereotype.Service;

/**
 * 复盘服务实现类
 * 仅负责复盘表的 CRUD 操作，聚合操作由 ReviewHandler 处理
 *
 * @author Azir
 */
@Service
public class ReviewService extends ServiceImpl<InterviewReviewMapper, InterviewReview> {

    /**
     * 获取复盘列表
     */
    public PageResponse<ReviewListItemVO> getReviews(Integer page, Integer size) {
        // TODO: 实现查询逻辑
        return null;
    }

    /**
     * 获取复盘详情
     */
    public InterviewReviewVO getReviewDetail(String id) {
        // TODO: 实现查询逻辑
        return null;
    }

}
