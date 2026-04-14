package com.careerforge.task.controller;

import com.careerforge.common.util.SecurityUtils;
import com.careerforge.common.response.ApiResponse;
import com.careerforge.task.dto.TaskVO;
import com.careerforge.task.entity.AsyncTask;
import com.careerforge.task.enums.TaskStatus;
import com.careerforge.task.service.AsyncTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 异步任务控制器
 *
 * @author Azir
 */
@Slf4j
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Tag(name = "异步任务", description = "异步任务管理接口")
public class TaskController {

    private final AsyncTaskService asyncTaskService;

    /**
     * 获取任务列表
     */
    @Operation(summary = "获取任务列表")
    @GetMapping
    public ApiResponse<Map<String, Object>> listTasks(
            @Parameter(description = "任务类型") @RequestParam(required = false) String type,
            @Parameter(description = "任务状态") @RequestParam(required = false) String status) {
        String userId = SecurityUtils.getCurrentUserId();
        TaskStatus taskStatus = status != null ? TaskStatus.fromCode(status) : null;
        List<AsyncTask> tasks = asyncTaskService.listByUser(userId, taskStatus);
        List<TaskVO> voList = asyncTaskService.toVOList(tasks);
        Map<String, Object> result = new HashMap<>();
        result.put("list", voList);
        result.put("total", voList.size());
        return ApiResponse.success(result);
    }

    /**
     * 获取任务详情
     */
    @Operation(summary = "获取任务详情")
    @GetMapping("/{taskId}")
    public ApiResponse<TaskVO> getTask(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        AsyncTask task = asyncTaskService.getById(taskId);
        if (task == null) {
            return ApiResponse.error(404, "任务不存在");
        }
        return ApiResponse.success(asyncTaskService.toVO(task));
    }

    /**
     * 删除任务
     */
    @Operation(summary = "删除任务")
    @DeleteMapping("/{taskId}")
    public ApiResponse<Void> deleteTask(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        AsyncTask task = asyncTaskService.getById(taskId);
        if (task == null) {
            return ApiResponse.error(404, "任务不存在");
        }
        asyncTaskService.removeById(taskId);
        log.info("[TaskController] 删除任务: taskId={}", taskId);
        return ApiResponse.success(null);
    }

    /**
     * 清理已完成的任务
     */
    @Operation(summary = "清理已完成的任务")
    @DeleteMapping("/completed")
    public ApiResponse<Void> clearCompletedTasks() {
        String userId = SecurityUtils.getCurrentUserId();
        int count = asyncTaskService.clearCompletedTasks(userId);
        log.info("[TaskController] 清理已完成任务: userId={}, count={}", userId, count);
        return ApiResponse.success(null);
    }

}
