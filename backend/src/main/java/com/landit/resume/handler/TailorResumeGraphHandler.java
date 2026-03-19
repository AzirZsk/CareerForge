package com.landit.resume.handler;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.landit.common.enums.ResumeStatus;
import com.landit.common.enums.SectionType;
import com.landit.common.exception.BusinessException;
import com.landit.resume.dto.DeriveResumeRequest;
import com.landit.resume.dto.OptimizeProgressEvent;
import com.landit.resume.util.GraphSseHelper;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.dto.SaveTailoredResumeRequest;
import com.landit.resume.entity.Resume;
import com.landit.resume.graph.tailor.TailorResumeGraphService;
import com.landit.resume.service.ResumeSectionService;
import com.landit.resume.service.ResumeService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.landit.resume.graph.tailor.TailorResumeGraphConstants.*;

/**
 * 职位适配工作流业务处理器
 * 负责处理职位适配工作流的业务编排逻辑
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TailorResumeGraphHandler {

    private final TailorResumeGraphService graphService;
    private final ResumeHandler resumeHandler;
    private final ResumeService resumeService;
    private final ResumeSectionService resumeSectionService;

    /**
     * 流式执行职位适配工作流（使用SseEmitter）
     * 将Flux转换为SseEmitter以确保在Tomcat容器上实时推送事件
     *
     * @param id              简历ID
     * @param targetPosition  目标职位
     * @param jobDescription  职位描述
     * @param response        HTTP响应（用于设置响应头）
     * @return SSE事件发射器
     */
    public SseEmitter streamTailorWithSse(String id, String targetPosition, String jobDescription,
                                           HttpServletResponse response) {
        log.info("开始SSE流式职位适配: resumeId={}, targetPosition={}", id, targetPosition);

        configureSseResponse(response);
        SseEmitter emitter = new SseEmitter(GraphSseHelper.SSE_TIMEOUT);
        String threadId = UUID.randomUUID().toString();

        streamTailorWithThreadId(id, targetPosition, jobDescription, threadId).subscribe(
                event -> sendSseEvent(emitter, event),
                error -> {
                    log.error("[SSE-职位适配] 流异常", error);
                    emitter.completeWithError(error);
                },
                () -> {
                    log.info("[SSE-职位适配] 流完成");
                    emitter.complete();
                }
        );

        return emitter;
    }

    /**
     * 流式执行职位适配工作流（带指定threadId）
     *
     * @param id              简历ID
     * @param targetPosition  目标职位
     * @param jobDescription  职位描述
     * @param threadId        会话线程ID
     * @return SSE 事件流
     */
    private Flux<OptimizeProgressEvent> streamTailorWithThreadId(String id, String targetPosition,
                                                                   String jobDescription, String threadId) {
        log.info("开始流式职位适配: resumeId={}, targetPosition={}, threadId={}", id, targetPosition, threadId);

        ResumeDetailVO resumeDetail = resumeHandler.getResumeDetail(id);
        if (resumeDetail == null) {
            return Flux.just(OptimizeProgressEvent.error("简历不存在", null));
        }

        Map<String, Object> initialState = buildInitialState(resumeDetail, targetPosition, jobDescription);

        return Flux.concat(
                Flux.just(OptimizeProgressEvent.start(id, threadId, targetPosition, "tailor")),
                graphService.streamTailor(initialState, threadId)
                        .filter(output -> !isInterruptionOutput(output))
                        .map(output -> {
                            Map<String, Object> data = output.state().data();
                            @SuppressWarnings("unchecked")
                            Map<String, Object> nodeOutput = (Map<String, Object>) data.get(STATE_NODE_OUTPUT);
                            return OptimizeProgressEvent.fromNodeOutput(output.node(), threadId, nodeOutput);
                        }),
                Flux.just(OptimizeProgressEvent.complete(threadId))
        )
        .onErrorResume(e -> {
            log.error("职位适配工作流执行失败", e);
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
            log.info("[SSE-职位适配] 发送事件: event={}, nodeId={}", event.getEvent(), event.getNodeId());
        } catch (IOException e) {
            log.error("[SSE-职位适配] 发送失败", e);
            emitter.completeWithError(e);
        }
    }

    /**
     * 构建工作流初始状态
     */
    private Map<String, Object> buildInitialState(ResumeDetailVO resumeDetail, String targetPosition, String jobDescription) {
        Map<String, Object> state = new HashMap<>();
        state.put(STATE_RESUME_CONTENT, resumeDetail);
        state.put(STATE_TARGET_POSITION, targetPosition);
        state.put(STATE_JOB_DESCRIPTION, jobDescription);
        state.put(STATE_MESSAGES, new ArrayList<String>());
        return state;
    }

    /**
     * 判断节点输出是否是中断事件
     */
    private boolean isInterruptionOutput(NodeOutput output) {
        return output instanceof InterruptionMetadata;
    }

    /**
     * 保存定制简历
     * 创建派生简历并应用定制内容
     *
     * @param sourceResumeId 源简历ID
     * @param request        保存请求
     * @return 新创建的简历详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumeDetailVO saveTailoredResume(String sourceResumeId, SaveTailoredResumeRequest request) {
        log.info("保存定制简历: sourceResumeId={}, targetPosition={}", sourceResumeId, request.getTargetPosition());

        // 获取源简历并验证
        Resume sourceResume = resumeService.getById(sourceResumeId);
        if (sourceResume == null) {
            throw BusinessException.notFound("源简历不存在");
        }
        if (!ResumeStatus.OPTIMIZED.getValue().equals(sourceResume.getStatus())) {
            throw new BusinessException("只能基于已优化的简历进行定制");
        }

        // 创建派生简历
        DeriveResumeRequest deriveRequest = new DeriveResumeRequest();
        deriveRequest.setTargetPosition(request.getTargetPosition());
        deriveRequest.setResumeName(request.getResumeName());
        deriveRequest.setJobDescription(request.getJobDescription());
        Resume derivedResume = resumeService.createDerivedResume(sourceResume, deriveRequest);
        log.info("派生简历创建完成: derivedResumeId={}", derivedResume.getId());

        // 删除自动复制的区块（createDerivedResume不会复制区块，这里不需要删除）
        // 直接创建定制内容的区块（带评分）
        for (SaveTailoredResumeRequest.SectionDataItem item : request.getAfterSection()) {
            String type = item.getType() != null ? item.getType() : SectionType.CUSTOM.getCode();
            String title = item.getTitle() != null ? item.getTitle() : "";
            // 获取区块相关性评分
            Integer sectionScore = 0;
            if (request.getSectionRelevanceScores() != null && type != null) {
                // 【修复】将 code 转换为 schemaFieldName 进行匹配
                // 前端传的是驼峰命名(basicInfo)，后端 code 是大写(BASIC_INFO)
                SectionType sectionType = SectionType.fromCode(type);
                String mapKey = sectionType != null ? sectionType.getSchemaFieldName() : type;
                sectionScore = request.getSectionRelevanceScores().get(mapKey);
            }
            // 如果 item 中有 score，优先使用
            if (item.getScore() != null) {
                sectionScore = item.getScore();
            }
            resumeSectionService.createWithScore(derivedResume.getId(), type, title, item.getContent(), sectionScore);
        }
        log.info("定制区块创建完成: count={}", request.getAfterSection().size());

        // 更新四大维度评分
        if (request.getDimensionScores() != null) {
            SaveTailoredResumeRequest.DimensionScores ds = request.getDimensionScores();
            derivedResume.setContentScore(ds.getContent() != null ? ds.getContent() : 0);
            derivedResume.setStructureScore(ds.getStructure() != null ? ds.getStructure() : 0);
            derivedResume.setMatchingScore(ds.getMatching() != null ? ds.getMatching() : 0);
            derivedResume.setCompetitivenessScore(ds.getCompetitiveness() != null ? ds.getCompetitiveness() : 0);
            // 计算综合评分（四大维度的平均值）
            int avgScore = (derivedResume.getContentScore() + derivedResume.getStructureScore()
                    + derivedResume.getMatchingScore() + derivedResume.getCompetitivenessScore()) / 4;
            derivedResume.setOverallScore(avgScore);
            derivedResume.setScore(avgScore);
            resumeService.updateById(derivedResume);
            log.info("四大维度评分已更新: content={}, structure={}, matching={}, competitiveness={}",
                    ds.getContent(), ds.getStructure(), ds.getMatching(), ds.getCompetitiveness());
        }

        // 更新状态为已优化（定制简历已经过 AI 优化）
        resumeService.updateStatus(derivedResume.getId(), ResumeStatus.OPTIMIZED);
        log.info("定制简历状态已更新为 OPTIMIZED: resumeId={}", derivedResume.getId());

        // 返回新简历详情
        return resumeService.getResumeDetail(derivedResume.getId());
    }

}
