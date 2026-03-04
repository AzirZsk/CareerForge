package com.landit.resume.handler;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.landit.common.util.JsonParseHelper;
import com.landit.resume.dto.DiagnoseResumeResponse;
import com.landit.resume.dto.OptimizeGraphRequest;
import com.landit.resume.dto.OptimizeProgressEvent;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.graph.ResumeOptimizeGraphService;
import com.landit.resume.service.ResumeService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.landit.resume.graph.ResumeOptimizeGraphConstants.*;

/**
 * 简历优化工作流业务处理器
 * 负责处理工作流的业务编排逻辑
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeOptimizeGraphHandler {

    private final ResumeOptimizeGraphService graphService;
    private final ResumeHandler resumeHandler;
    private final ResumeService resumeService;

    /**
     * 流式执行简历优化工作流
     *
     * @param id             简历ID
     * @param mode           优化模式（quick/precise）
     * @param targetPosition 目标岗位（可选）
     * @return SSE 事件流
     */
    public Flux<OptimizeProgressEvent> streamOptimize(String id, String mode, String targetPosition) {
        return streamOptimizeWithThreadId(id, mode, targetPosition, UUID.randomUUID().toString());
    }

    /**
     * 流式执行简历优化工作流（使用SseEmitter）
     * 将Flux转换为SseEmitter以确保在Tomcat容器上实时推送事件
     *
     * @param id             简历ID
     * @param mode           优化模式（quick/precise）
     * @param targetPosition 目标岗位（可选）
     * @param response       HTTP响应（用于设置响应头）
     * @return SSE事件发射器
     */
    public SseEmitter streamOptimizeWithSse(String id, String mode, String targetPosition,
                                              HttpServletResponse response) {
        log.info("开始SSE流式简历优化: resumeId={}, mode={}", id, mode);

        configureSseResponse(response);
        SseEmitter emitter = new SseEmitter(300000L);
        String threadId = UUID.randomUUID().toString();

        streamOptimizeWithThreadId(id, mode, targetPosition, threadId).subscribe(
                event -> sendSseEvent(emitter, event),
                error -> {
                    log.error("[SSE] 流异常", error);
                    emitter.completeWithError(error);
                },
                () -> {
                    log.info("[SSE] 流完成");
                    // 流完成后保存诊断评分
                    saveDiagnosisScores(id, threadId);
                    emitter.complete();
                }
        );

        return emitter;
    }

    /**
     * 流式执行简历优化工作流（带指定threadId）
     *
     * @param id             简历ID
     * @param mode           优化模式（quick/precise）
     * @param targetPosition 目标岗位（可选）
     * @param threadId       会话线程ID
     * @return SSE 事件流
     */
    private Flux<OptimizeProgressEvent> streamOptimizeWithThreadId(String id, String mode,
                                                                    String targetPosition, String threadId) {
        log.info("开始流式简历优化: resumeId={}, mode={}, threadId={}", id, mode, threadId);

        ResumeDetailVO resumeDetail = resumeHandler.getResumeDetail(id);
        if (resumeDetail == null) {
            return Flux.just(OptimizeProgressEvent.error("简历不存在", null));
        }

        String position = resolveTargetPosition(targetPosition, resumeDetail);
        Map<String, Object> parseResult = extractResumeModules(resumeDetail);
        Map<String, Object> initialState = buildInitialState(id, resumeDetail, position, mode);

        return Flux.concat(
                Flux.just(OptimizeProgressEvent.startWithModules(id, threadId, position, mode, parseResult)),
                graphService.streamOptimize(initialState, threadId)
                        .filter(output -> !isInterruptionOutput(output))
                        .map(output -> OptimizeProgressEvent.fromNodeOutput(
                                output.node(), threadId, graphService.getNodeOutput(threadId))),
                Flux.just(OptimizeProgressEvent.complete(threadId))
        )
        .onErrorResume(e -> {
            log.error("工作流执行失败", e);
            return Flux.just(OptimizeProgressEvent.error(e.getMessage(), threadId));
        })
        .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 配置SSE响应头
     */
    private void configureSseResponse(HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    /**
     * 发送SSE事件
     */
    private void sendSseEvent(SseEmitter emitter, OptimizeProgressEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(event));
            log.info("[SSE] 发送事件: event={}, nodeId={}", event.getEvent(), event.getNodeId());
        } catch (IOException e) {
            log.error("[SSE] 发送失败", e);
            emitter.completeWithError(e);
        }
    }

    /**
     * 同步执行简历优化工作流
     *
     * @param id      简历ID
     * @param request 优化请求
     * @return 优化结果
     */
    public Map<String, Object> executeOptimize(String id, OptimizeGraphRequest request) {
        log.info("执行简历优化: resumeId={}", id);

        ResumeDetailVO resumeDetail = resumeHandler.getResumeDetail(id);
        if (resumeDetail == null) {
            return errorResult("简历不存在", 404);
        }

        String mode = resolveMode(request);
        String position = resolveTargetPosition(
                request != null ? request.getTargetPosition() : null, resumeDetail);
        String threadId = UUID.randomUUID().toString();

        Map<String, Object> result = MODE_PRECISE.equals(mode)
                ? graphService.executePreciseOptimize(id, resumeDetail, position,
                        request != null ? request.getSearchResults() : "", threadId)
                : graphService.executeQuickOptimize(id, resumeDetail, position, threadId);

        result.put(STATE_THREAD_ID, threadId);
        return result;
    }

    /**
     * 创建错误结果
     */
    private Map<String, Object> errorResult(String message, int code) {
        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("error", message);
        errorResult.put("code", code);
        return errorResult;
    }

    /**
     * 提取简历模块信息和计算完整性
     *
     * @param resumeDetail 简历详情
     * @return 包含模块列表和完整性信息的Map
     */
    public Map<String, Object> extractResumeModules(ResumeDetailVO resumeDetail) {
        List<Map<String, Object>> modules = new ArrayList<>();
        List<ResumeDetailVO.ResumeSectionVO> sections = resumeDetail.getSections();

        if (sections != null) {
            for (ResumeDetailVO.ResumeSectionVO section : sections) {
                Map<String, Object> moduleInfo = new HashMap<>();
                moduleInfo.put("id", section.getId());
                moduleInfo.put("type", section.getType());
                moduleInfo.put("title", section.getTitle());
                moduleInfo.put("score", section.getScore());
                moduleInfo.put("hasContent", hasContent(section));
                modules.add(moduleInfo);
            }
        }

        int totalModules = modules.size();
        long completeModules = modules.stream()
                .filter(m -> Boolean.TRUE.equals(m.get("hasContent")))
                .count();

        Map<String, Object> result = new HashMap<>();
        result.put("modules", modules);
        result.put("moduleCount", totalModules);
        result.put("completeness", totalModules > 0 ? (int) (completeModules * 100 / totalModules) : 0);
        return result;
    }

    /**
     * 判断简历区块是否有内容
     */
    private boolean hasContent(ResumeDetailVO.ResumeSectionVO section) {
        return section.getContent() != null ||
                (section.getItems() != null && !section.getItems().isEmpty());
    }

    /**
     * 构建工作流初始状态
     */
    private Map<String, Object> buildInitialState(String resumeId, ResumeDetailVO resumeDetail,
                                                   String position, String mode) {
        Map<String, Object> state = new HashMap<>();
        state.put(STATE_RESUME_ID, resumeId);
        state.put(STATE_RESUME_CONTENT, resumeDetail);
        state.put(STATE_TARGET_POSITION, position);
        state.put(STATE_DIAGNOSIS_MODE, mode);
        state.put(STATE_MESSAGES, new ArrayList<String>());
        return state;
    }

    /**
     * 解析目标岗位
     */
    private String resolveTargetPosition(String targetPosition, ResumeDetailVO resumeDetail) {
        String position = firstNonEmpty(targetPosition, resumeDetail.getTargetPosition());
        return (position != null && !position.isEmpty()) ? position : DEFAULT_TARGET_POSITION;
    }

    /**
     * 获取第一个非空字符串
     */
    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    /**
     * 解析优化模式
     */
    private String resolveMode(OptimizeGraphRequest request) {
        return (request != null && request.getMode() != null) ? request.getMode() : MODE_QUICK;
    }

    /**
     * 判断节点输出是否是中断事件
     * 当Graph执行到配置了interruptBefore的节点时，会发出一个中断元数据输出
     * 这类输出不应该转换为进度事件发送给前端
     *
     * @param output 节点输出
     * @return true表示是中断事件，应该被过滤
     */
    private boolean isInterruptionOutput(NodeOutput output) {
        return output instanceof InterruptionMetadata;
    }

    /**
     * 保存诊断评分到数据库
     *
     * @param resumeId 简历ID
     * @param threadId 会话线程ID
     */
    @SuppressWarnings("unchecked")
    private void saveDiagnosisScores(String resumeId, String threadId) {
        try {
            // 从 graphService 获取最新的诊断状态
            Map<String, Object> state = graphService.getState(threadId);
            if (state == null || state.isEmpty()) {
                log.warn("[SSE] 无法获取工作流状态，跳过保存评分: resumeId={}, threadId={}", resumeId, threadId);
                return;
            }

            // 提取评分数据
            Integer overallScore = (Integer) state.get(STATE_OVERALL_SCORE);
            Object dimensionsObj = state.get(STATE_DIMENSIONS);

            if (overallScore == null) {
                log.warn("[SSE] 未找到综合评分，跳过保存: resumeId={}, threadId={}", resumeId, threadId);
                return;
            }

            // 转换维度评分
            DiagnoseResumeResponse.DimensionScores dimensionScores = null;
            if (dimensionsObj instanceof Map) {
                Map<String, Object> dimensionsMap = (Map<String, Object>) dimensionsObj;
                dimensionScores = DiagnoseResumeResponse.DimensionScores.builder()
                        .content((Integer) dimensionsMap.get("content"))
                        .structure((Integer) dimensionsMap.get("structure"))
                        .matching((Integer) dimensionsMap.get("matching"))
                        .competitiveness((Integer) dimensionsMap.get("competitiveness"))
                        .build();
            } else if (dimensionsObj instanceof DiagnoseResumeResponse.DimensionScores) {
                dimensionScores = (DiagnoseResumeResponse.DimensionScores) dimensionsObj;
            }

            // 保存评分
            resumeService.updateDiagnosisScores(resumeId, overallScore, dimensionScores);
            log.info("[SSE] 诊断评分保存成功: resumeId={}, threadId={}", resumeId, threadId);

        } catch (Exception e) {
            log.error("[SSE] 保存诊断评分失败: resumeId={}, threadId={}, error={}", resumeId, threadId, e.getMessage(), e);
        }
    }
}
