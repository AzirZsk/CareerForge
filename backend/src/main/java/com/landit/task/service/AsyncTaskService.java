package com.landit.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.task.dto.TaskVO;
import com.landit.task.entity.AsyncTask;
import com.landit.task.enums.TaskStatus;
import com.landit.task.enums.TaskType;
import com.landit.task.mapper.AsyncTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 异步任务服务
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTaskService extends ServiceImpl<AsyncTaskMapper, AsyncTask> {

    /**
     * 创建任务
     */
    @Transactional(rollbackFor = Exception.class)
    public AsyncTask createTask(String userId, TaskType taskType, String businessId) {
        AsyncTask task = new AsyncTask();
        task.setUserId(userId);
        task.setTaskType(taskType.getCode());
        task.setBusinessId(businessId);
        task.setStatus(TaskStatus.PENDING.getCode());
        task.setProgress(0);
        task.setMessage("任务已创建");
        save(task);
        log.info("[AsyncTask] 创建任务: taskId={}, type={}, businessId={}",
                task.getId(), taskType.getCode(), businessId);
        return task;
    }

    /**
     * 更新任务状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String taskId, TaskStatus status, Integer progress, String message) {
        AsyncTask task = getById(taskId);
        if (task == null) {
            log.warn("[AsyncTask] 任务不存在: taskId={}", taskId);
            return;
        }
        task.setStatus(status.getCode());
        if (progress != null) {
            task.setProgress(progress);
        }
        if (message != null) {
            task.setMessage(message);
        }
        updateById(task);
        log.debug("[AsyncTask] 更新任务状态: taskId={}, status={}, progress={}%",
                taskId, status.getCode(), progress);
    }

    /**
     * 标记任务开始执行
     */
    @Transactional(rollbackFor = Exception.class)
    public void markRunning(String taskId) {
        updateStatus(taskId, TaskStatus.RUNNING, 0, "任务开始执行");
    }

    /**
     * 更新任务进度
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProgress(String taskId, Integer progress, String message) {
        AsyncTask task = getById(taskId);
        if (task == null) {
            return;
        }
        task.setProgress(progress);
        if (message != null) {
            task.setMessage(message);
        }
        updateById(task);
    }

    /**
     * 标记任务完成
     */
    @Transactional(rollbackFor = Exception.class)
    public void markCompleted(String taskId, String result) {
        AsyncTask task = getById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(TaskStatus.COMPLETED.getCode());
        task.setProgress(100);
        task.setMessage("任务完成");
        task.setResult(result);
        updateById(task);
        log.info("[AsyncTask] 任务完成: taskId={}", taskId);
    }

    /**
     * 标记任务失败
     */
    @Transactional(rollbackFor = Exception.class)
    public void markFailed(String taskId, String errorMessage) {
        AsyncTask task = getById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(TaskStatus.FAILED.getCode());
        task.setMessage("任务失败");
        task.setErrorMessage(errorMessage);
        updateById(task);
        log.error("[AsyncTask] 任务失败: taskId={}, error={}", taskId, errorMessage);
    }

    /**
     * 获取用户的任务列表
     */
    public List<AsyncTask> listByUser(String userId, TaskStatus status) {
        LambdaQueryWrapper<AsyncTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AsyncTask::getUserId, userId)
                .orderByDesc(AsyncTask::getCreatedAt);
        if (status != null) {
            wrapper.eq(AsyncTask::getStatus, status.getCode());
        }
        return list(wrapper);
    }

    /**
     * 获取用户的进行中任务
     */
    public List<AsyncTask> listActiveTasks(String userId) {
        LambdaQueryWrapper<AsyncTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AsyncTask::getUserId, userId)
                .in(AsyncTask::getStatus, TaskStatus.PENDING.getCode(), TaskStatus.RUNNING.getCode())
                .orderByDesc(AsyncTask::getCreatedAt);
        return list(wrapper);
    }

    /**
     * 获取业务关联的任务
     */
    public AsyncTask getByBusinessId(String businessId, TaskType taskType) {
        LambdaQueryWrapper<AsyncTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AsyncTask::getBusinessId, businessId)
                .eq(AsyncTask::getTaskType, taskType.getCode())
                .orderByDesc(AsyncTask::getCreatedAt)
                .last("LIMIT 1");
        return getOne(wrapper);
    }

    /**
     * 清理已完成的任务
     */
    @Transactional(rollbackFor = Exception.class)
    public int clearCompletedTasks(String userId) {
        LambdaQueryWrapper<AsyncTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AsyncTask::getUserId, userId)
                .in(AsyncTask::getStatus, TaskStatus.COMPLETED.getCode(), TaskStatus.FAILED.getCode());
        int count = baseMapper.delete(wrapper);
        log.info("[AsyncTask] 清理已完成任务: userId={}, count={}", userId, count);
        return count;
    }

    /**
     * 转换为 VO
     */
    public TaskVO toVO(AsyncTask task) {
        if (task == null) {
            return null;
        }
        TaskVO vo = new TaskVO();
        BeanUtils.copyProperties(task, vo);
        // 设置任务类型标签
        TaskType taskType = TaskType.fromCode(task.getTaskType());
        if (taskType != null) {
            vo.setTaskTypeLabel(taskType.getLabel());
        }
        // 设置状态标签
        TaskStatus status = TaskStatus.fromCode(task.getStatus());
        if (status != null) {
            vo.setStatusLabel(status.getLabel());
        }
        return vo;
    }

    /**
     * 批量转换为 VO
     */
    public List<TaskVO> toVOList(List<AsyncTask> tasks) {
        return tasks.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

}
