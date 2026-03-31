package com.landit.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.common.exception.BusinessException;
import com.landit.interview.dto.interviewcenter.*;
import com.landit.interview.entity.InterviewPreparation;
import com.landit.interview.mapper.InterviewPreparationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 面试准备事项服务
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewPreparationService extends ServiceImpl<InterviewPreparationMapper, InterviewPreparation> {

    /**
     * 获取面试的所有准备事项
     */
    public List<InterviewPreparation> getByInterviewId(String interviewId) {
        LambdaQueryWrapper<InterviewPreparation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewPreparation::getInterviewId, interviewId)
                .orderByAsc(InterviewPreparation::getSortOrder);
        return this.list(wrapper);
    }

    /**
     * 添加准备事项
     */
    @Transactional(rollbackFor = Exception.class)
    public PreparationVO addPreparation(String interviewId, AddPreparationRequest request) {
        InterviewPreparation preparation = new InterviewPreparation();
        preparation.setInterviewId(interviewId);
        preparation.setItemType("todo");
        preparation.setTitle(request.getTitle());
        preparation.setContent(request.getContent());
        preparation.setCompleted(false);
        preparation.setSource("manual");
        preparation.setSortOrder(getNextSortOrder(interviewId));
        this.save(preparation);
        log.info("添加准备事项成功: interviewId={}, preparationId={}", interviewId, preparation.getId());
        return convertToVO(preparation);
    }

    /**
     * 更新准备事项
     */
    @Transactional(rollbackFor = Exception.class)
    public PreparationVO updatePreparation(String interviewId, String preparationId, UpdatePreparationRequest request) {
        InterviewPreparation preparation = this.getById(preparationId);
        if (preparation == null || !preparation.getInterviewId().equals(interviewId)) {
            throw new BusinessException("准备事项不存在或不属于该面试");
        }
        if (request.getTitle() != null) {
            preparation.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            preparation.setContent(request.getContent());
        }
        if (request.getCompleted() != null) {
            preparation.setCompleted(request.getCompleted());
        }
        if (request.getSortOrder() != null) {
            preparation.setSortOrder(request.getSortOrder());
        }
        this.updateById(preparation);
        log.info("更新准备事项成功: preparationId={}", preparationId);
        return convertToVO(preparation);
    }

    /**
     * 切换准备事项完成状态
     */
    @Transactional(rollbackFor = Exception.class)
    public PreparationVO toggleComplete(String interviewId, String preparationId) {
        InterviewPreparation preparation = this.getById(preparationId);
        if (preparation == null || !preparation.getInterviewId().equals(interviewId)) {
            throw new BusinessException("准备事项不存在或不属于该面试");
        }
        preparation.setCompleted(!preparation.getCompleted());
        this.updateById(preparation);
        log.info("切换准备事项完成状态: preparationId={}, completed={}", preparationId, preparation.getCompleted());
        return convertToVO(preparation);
    }

    /**
     * 删除准备事项
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePreparation(String interviewId, String preparationId) {
        InterviewPreparation preparation = this.getById(preparationId);
        if (preparation == null || !preparation.getInterviewId().equals(interviewId)) {
            throw new BusinessException("准备事项不存在或不属于该面试");
        }
        this.removeById(preparationId);
        log.info("删除准备事项成功: preparationId={}", preparationId);
    }

    /**
     * 根据面试ID删除所有准备事项
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByInterviewId(String interviewId) {
        LambdaQueryWrapper<InterviewPreparation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewPreparation::getInterviewId, interviewId);
        this.remove(wrapper);
        log.info("删除面试所有准备事项: interviewId={}", interviewId);
    }

    private int getNextSortOrder(String interviewId) {
        List<InterviewPreparation> preparations = getByInterviewId(interviewId);
        return preparations.size() + 1;
    }

    private PreparationVO convertToVO(InterviewPreparation preparation) {
        PreparationVO vo = new PreparationVO();
        BeanUtils.copyProperties(preparation, vo);
        return vo;
    }

}
