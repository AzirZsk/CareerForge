package com.landit.interview.handler;

import com.landit.interview.dto.interviewcenter.*;
import com.landit.interview.entity.InterviewPreparation;
import com.landit.interview.service.InterviewPreparationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 面试准备事项业务编排处理器
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewPreparationHandler {

    private final InterviewPreparationService preparationService;

    /**
     * 获取准备清单
     */
    public List<PreparationVO> getPreparations(String interviewId) {
        List<InterviewPreparation> preparations = preparationService.getByInterviewId(interviewId);
        return preparations.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 添加准备事项
     */
    @Transactional(rollbackFor = Exception.class)
    public PreparationVO addPreparation(String interviewId, AddPreparationRequest request) {
        log.info("添加准备事项: interviewId={}, title={}", interviewId, request.getTitle());
        return preparationService.addPreparation(interviewId, request);
    }

    /**
     * 批量添加准备事项
     */
    @Transactional(rollbackFor = Exception.class)
    public List<PreparationVO> batchAddPreparations(String interviewId, BatchAddPreparationRequest request) {
        log.info("批量添加准备事项: interviewId={}, count={}", interviewId, request.getItems().size());
        return preparationService.batchAddPreparations(interviewId, request);
    }

    /**
     * 更新准备事项
     */
    @Transactional(rollbackFor = Exception.class)
    public PreparationVO updatePreparation(String interviewId, String preparationId, UpdatePreparationRequest request) {
        log.info("更新准备事项: preparationId={}", preparationId);
        return preparationService.updatePreparation(interviewId, preparationId, request);
    }

    /**
     * 切换完成状态
     */
    @Transactional(rollbackFor = Exception.class)
    public PreparationVO toggleComplete(String interviewId, String preparationId) {
        log.info("切换准备事项完成状态: preparationId={}", preparationId);
        return preparationService.toggleComplete(interviewId, preparationId);
    }

    /**
     * 删除准备事项
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePreparation(String interviewId, String preparationId) {
        log.info("删除准备事项: preparationId={}", preparationId);
        preparationService.deletePreparation(interviewId, preparationId);
    }

    /**
     * 实体转 VO
     */
    private PreparationVO convertToVO(InterviewPreparation preparation) {
        PreparationVO vo = new PreparationVO();
        BeanUtils.copyProperties(preparation, vo);
        return vo;
    }

}
