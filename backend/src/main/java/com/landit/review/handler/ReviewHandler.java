package com.landit.review.handler;

import com.landit.review.dto.InterviewReviewVO;
import com.landit.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 复盘业务编排处理器
 * 负责处理涉及文件导出等聚合操作
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewHandler {

    private final ReviewService reviewService;

    /**
     * 导出复盘报告
     * 涉及：读取复盘数据 -> 生成 PDF 报告
     *
     * @param id 复盘ID
     * @return PDF 字节数组
     */
    @Transactional(readOnly = true)
    public byte[] exportReview(Long id) {
        // TODO: 实现导出复盘报告逻辑
        // 1. 获取复盘详情
        // 2. 生成 PDF 报告
        log.info("导出复盘报告: reviewId={}", id);
        return null;
    }

}
