package com.landit.task.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.interview.entity.Interview;
import com.landit.interview.service.InterviewService;
import com.landit.interview.voice.dto.FileASRResult;
import com.landit.interview.voice.service.FileASRService;
import com.landit.task.dto.AudioTranscribeResult;
import com.landit.task.entity.AsyncTask;
import com.landit.task.enums.TaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.scheduler.Schedulers;

/**
 * 音频转录任务服务
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AudioTranscribeTaskService {

    private final AsyncTaskService asyncTaskService;
    private final FileASRService fileASRService;
    private final ObjectMapper objectMapper;
    private final InterviewService interviewService;

    /**
     * 创建音频转录任务
     *
     * @param userId      用户ID
     * @param interviewId 面试ID
     * @param file        音频文件
     * @return 任务实体
     */
    public AsyncTask createTranscribeTask(String userId, String interviewId, MultipartFile file) {
        // 创建任务记录
        AsyncTask task = asyncTaskService.createTask(userId, TaskType.AUDIO_TRANSCRIBE, interviewId);
        String taskId = task.getId();
        log.info("[AudioTranscribeTask] 创建转录任务: taskId={}, interviewId={}, filename={}",
                taskId, interviewId, file.getOriginalFilename());
        // 异步执行转录
        executeTranscribeAsync(taskId, file);
        return task;
    }

    /**
     * 异步执行转录
     */
    private void executeTranscribeAsync(String taskId, MultipartFile file) {
        fileASRService.transcribe(file)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        result -> handleTranscribeProgress(taskId, result),
                        error -> handleTranscribeError(taskId, error),
                        () -> log.info("[AudioTranscribeTask] 转录流结束: taskId={}", taskId)
                );
    }

    /**
     * 处理转录进度更新
     */
    private void handleTranscribeProgress(String taskId, FileASRResult result) {
        log.debug("[AudioTranscribeTask] 转录进度: taskId={}, status={}, progress={}%",
                taskId, result.getStatus(), result.getProgress());
        switch (result.getStatus()) {
            case "processing":
                asyncTaskService.updateProgress(taskId, result.getProgress(), result.getMessage());
                break;
            case "completed":
                handleTranscribeCompleted(taskId, result);
                break;
            case "failed":
                asyncTaskService.markFailed(taskId, result.getErrorMessage());
                break;
            default:
                log.warn("[AudioTranscribeTask] 未知状态: taskId={}, status={}",
                        taskId, result.getStatus());
        }
    }

    /**
     * 处理转录完成
     */
    private void handleTranscribeCompleted(String taskId, FileASRResult result) {
        try {
            // 构建结果 JSON
            AudioTranscribeResult transcribeResult = AudioTranscribeResult.builder()
                    .transcriptText(result.getText())
                    .build();
            String resultJson = objectMapper.writeValueAsString(transcribeResult);
            // 标记任务完成
            asyncTaskService.markCompleted(taskId, resultJson);
            log.info("[AudioTranscribeTask] 转录完成: taskId={}, textLength={}",
                    taskId, result.getText().length());
            // 自动保存到 Interview.transcript
            AsyncTask task = asyncTaskService.getById(taskId);
            if (task == null) {
                log.warn("[AudioTranscribeTask] 任务不存在，无法保存到面试记录: taskId={}", taskId);
                return;
            }
            Interview interview = interviewService.getById(task.getBusinessId());
            if (interview == null) {
                log.warn("[AudioTranscribeTask] 关联面试不存在: interviewId={}", task.getBusinessId());
                return;
            }
            // 更新面试记录的 transcript
            interview.setTranscript(result.getText());
            interviewService.updateById(interview);
            log.info("[AudioTranscribeTask] 转录结果已自动保存到面试记录: taskId={}, interviewId={}, textLength={}",
                    taskId, task.getBusinessId(), result.getText().length());
        } catch (JsonProcessingException e) {
            log.error("[AudioTranscribeTask] 序列化结果失败: taskId={}", taskId, e);
            asyncTaskService.markFailed(taskId, "序列化结果失败");
        }
    }

    /**
     * 处理转录错误
     */
    private void handleTranscribeError(String taskId, Throwable error) {
        log.error("[AudioTranscribeTask] 转录错误: taskId={}", taskId, error);
        asyncTaskService.markFailed(taskId, error.getMessage());
    }

}
