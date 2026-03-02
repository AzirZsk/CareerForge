package com.landit.resume.handler;

import com.landit.common.util.JsonParseHelper;
import com.landit.resume.dto.OptimizeGraphRequest;
import com.landit.resume.dto.OptimizeProgressEvent;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.graph.ResumeOptimizeGraphService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

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

    /**
     * 流式执行简历优化工作流
     *
     * @param id             简历ID
     * @param mode           优化模式（quick/precise）
     * @param targetPosition 目标岗位（可选）
     * @return SSE 事件流
     */
    public Flux<OptimizeProgressEvent> streamOptimize(String id, String mode, String targetPosition) {
        log.info("开始流式简历优化: resumeId={}, mode={}", id, mode);

        // 从数据库获取简历详情
        ResumeDetailVO resumeDetail = resumeHandler.getResumeDetail(id);
        if (resumeDetail == null) {
            return Flux.just(OptimizeProgressEvent.error("简历不存在", null));
        }

        // 获取目标岗位
        String position = resolveTargetPosition(targetPosition, resumeDetail);

        // 将简历内容转为JSON
        String resumeContent = toJsonString(resumeDetail);

        // 提前计算模块信息和完整性
        Map<String, Object> parseResult = extractResumeModules(resumeDetail);

        String threadId = UUID.randomUUID().toString();

        // 构建初始状态
        Map<String, Object> initialState = buildInitialState(id, resumeContent, position, mode);

        // 发送开始事件（包含模块数据）+ 工作流流 + 完成事件
        log.info("[SSE诊断] 创建Flux流: threadId={}", threadId);
        return Flux.concat(
                Flux.just(OptimizeProgressEvent.startWithModules(id, threadId, position, mode, parseResult))
                        .doOnSubscribe(s -> log.info("[SSE诊断] 开始事件被订阅"))
                        .doOnNext(e -> log.info("[SSE诊断] 开始事件发射: event={}, nodeId={}", e.getEvent(), e.getNodeId())),
                graphService.streamOptimize(initialState, threadId)
                        .map(output -> {
                            log.info("[SSE诊断] Graph节点完成: node={}", output.node());
                            Map<String, Object> nodeOutput = graphService.getNodeOutput(threadId);
                            return OptimizeProgressEvent.fromNodeOutput(output.node(), threadId, nodeOutput);
                        })
                        .doOnNext(e -> log.info("[SSE诊断] 节点事件发射: event={}, nodeId={}", e.getEvent(), e.getNodeId())),
                Flux.just(OptimizeProgressEvent.complete(threadId))
                        .doOnNext(e -> log.info("[SSE诊断] 完成事件发射: event={}, nodeId={}", e.getEvent(), e.getNodeId()))
        )
        .doOnSubscribe(s -> log.info("[SSE诊断] 整个Flux被订阅"))
        .doOnComplete(() -> log.info("[SSE诊断] Flux流完成"))
        .onErrorResume(e -> {
            log.error("工作流执行失败", e);
            return Flux.just(OptimizeProgressEvent.error(e.getMessage(), threadId));
        });
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

        // 设置响应头禁用缓冲
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);

        // 获取Flux流
        Flux<OptimizeProgressEvent> flux = streamOptimize(id, mode, targetPosition);

        // 创建超时时间为5分钟的SseEmitter
        SseEmitter emitter = new SseEmitter(300000L);

        // 订阅Flux并实时发送事件（使用JSON序列化）
        flux.subscribe(
                event -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(event, MediaType.APPLICATION_JSON));
                        log.info("[SSE] 发送事件: event={}, nodeId={}", event.getEvent(), event.getNodeId());
                    } catch (IOException e) {
                        log.error("[SSE] 发送失败", e);
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    log.error("[SSE] 流异常", error);
                    emitter.completeWithError(error);
                },
                () -> {
                    log.info("[SSE] 流完成");
                    emitter.complete();
                }
        );

        return emitter;
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

        // 从数据库获取简历
        ResumeDetailVO resumeDetail = resumeHandler.getResumeDetail(id);
        if (resumeDetail == null) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "简历不存在");
            errorResult.put("code", 404);
            return errorResult;
        }

        // 解析请求参数
        String mode = resolveMode(request);
        String position = resolveTargetPosition(request != null ? request.getTargetPosition() : null, resumeDetail);

        String resumeContent = toJsonString(resumeDetail);
        String threadId = UUID.randomUUID().toString();

        // 根据模式执行不同的优化流程
        Map<String, Object> result;
        if (MODE_PRECISE.equals(mode)) {
            result = graphService.executePreciseOptimize(
                    id, resumeContent, position,
                    request != null ? request.getSearchResults() : "",
                    threadId
            );
        } else {
            result = graphService.executeQuickOptimize(id, resumeContent, position, threadId);
        }

        result.put(STATE_THREAD_ID, threadId);
        return result;
    }

    /**
     * 提取简历模块信息和计算完整性
     *
     * @param resumeDetail 简历详情
     * @return 包含模块列表和完整性信息的Map
     */
    public Map<String, Object> extractResumeModules(ResumeDetailVO resumeDetail) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> modules = new ArrayList<>();

        List<ResumeDetailVO.ResumeSectionVO> sections = resumeDetail.getSections();
        if (sections != null) {
            for (ResumeDetailVO.ResumeSectionVO section : sections) {
                Map<String, Object> moduleInfo = new HashMap<>();
                moduleInfo.put("id", section.getId());
                moduleInfo.put("type", section.getType());
                moduleInfo.put("title", section.getTitle());
                moduleInfo.put("score", section.getScore());
                moduleInfo.put("hasContent", section.getContent() != null ||
                        (section.getItems() != null && !section.getItems().isEmpty()));
                modules.add(moduleInfo);
            }
        }

        int totalModules = modules.size();
        int completeModules = (int) modules.stream()
                .filter(m -> Boolean.TRUE.equals(m.get("hasContent")))
                .count();

        result.put("modules", modules);
        result.put("moduleCount", totalModules);
        result.put("completeness", totalModules > 0 ? (completeModules * 100 / totalModules) : 0);

        return result;
    }

    /**
     * 构建工作流初始状态
     */
    private Map<String, Object> buildInitialState(String resumeId, String resumeContent,
                                                   String position, String mode) {
        Map<String, Object> initialState = new HashMap<>();
        initialState.put(STATE_RESUME_ID, resumeId);
        initialState.put(STATE_RESUME_CONTENT, resumeContent);
        initialState.put(STATE_TARGET_POSITION, position);
        initialState.put(STATE_DIAGNOSIS_MODE, mode);
        initialState.put(STATE_MESSAGES, new ArrayList<String>());
        return initialState;
    }

    /**
     * 解析目标岗位
     */
    private String resolveTargetPosition(String targetPosition, ResumeDetailVO resumeDetail) {
        String position = targetPosition != null ? targetPosition : resumeDetail.getTargetPosition();
        if (position == null || position.isEmpty()) {
            position = DEFAULT_TARGET_POSITION;
        }
        return position;
    }

    /**
     * 解析优化模式
     */
    private String resolveMode(OptimizeGraphRequest request) {
        return request != null && request.getMode() != null ? request.getMode() : MODE_QUICK;
    }

    /**
     * 对象转JSON字符串
     */
    private String toJsonString(Object obj) {
        return JsonParseHelper.toJsonString(obj);
    }
}
