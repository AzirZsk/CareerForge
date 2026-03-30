package com.landit.resume.handler;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.landit.common.service.AIService;
import com.landit.common.util.JsonParseHelper;
import com.landit.resume.dto.OptimizeProgressEvent;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.dto.ResumeStructuredData;
import com.landit.resume.graph.rewrite.RewriteGraphService;
import com.landit.resume.util.GraphSseHelper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.landit.resume.graph.rewrite.RewriteGraphConstants.*;

/**
 * 简历风格改写工作流业务处理器
 * 负责处理参考简历缓存和SSE推送
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeRewriteGraphHandler {

    private final RewriteGraphService graphService;
    private final ResumeHandler resumeHandler;
    private final AIService aiService;

    /**
     * 参考简历缓存（tempKey -> CacheEntry）
     */
    private final ConcurrentHashMap<String, CacheEntry> referenceCache = new ConcurrentHashMap<>();

    /**
     * 解析上传的参考简历文件并缓存
     *
     * @param file 上传的参考简历文件
     * @return 包含 tempKey 和摘要信息的 Map
     */
    public Map<String, Object> parseAndCacheReference(MultipartFile file) {
        log.info("开始解析参考简历文件: {}", file.getOriginalFilename());
        // 复用现有的AI解析能力
        ResumeStructuredData parsedData = aiService.parseResumeFromFile(file);
        // 序列化为JSON字符串
        String referenceJson = JsonParseHelper.toJsonString(parsedData);
        // 生成临时缓存key
        String tempKey = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        // 缓存（30分钟过期）
        referenceCache.put(tempKey, new CacheEntry(referenceJson, System.currentTimeMillis() + 30 * 60 * 1000L));
        // 清理过期缓存
        cleanupExpiredCache();
        log.info("参考简历解析并缓存完成: tempKey={}", tempKey);
        Map<String, Object> result = new HashMap<>();
        result.put("tempKey", tempKey);
        result.put("fileName", file.getOriginalFilename());
        return result;
    }

    /**
     * 流式执行风格改写工作流（使用SseEmitter）
     *
     * @param id       简历ID
     * @param tempKey   参考简历缓存key
     * @param response  HTTP响应（用于设置响应头）
     * @return SSE事件发射器
     */
    public SseEmitter streamRewriteWithSse(String id, String tempKey, HttpServletResponse response) {
        log.info("开始SSE流式风格改写: resumeId={}, tempKey={}", id, tempKey);

        GraphSseHelper.configureSseResponse(response);
        SseEmitter emitter = GraphSseHelper.createEmitter();
        String threadId = UUID.randomUUID().toString();

        // 从缓存获取参考简历
        CacheEntry cacheEntry = referenceCache.get(tempKey);
        if (cacheEntry == null) {
            log.error("参考简历缓存不存在或已过期: tempKey={}", tempKey);
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(OptimizeProgressEvent.error("参考简历缓存不存在或已过期，请重新上传", threadId)));
                emitter.complete();
            } catch (Exception e) {
                log.error("发送错误事件失败", e);
            }
            return emitter;
        }
        // 检查缓存是否过期
        if (cacheEntry.isExpired()) {
            referenceCache.remove(tempKey);
            log.error("参考简历缓存已过期: tempKey={}", tempKey);
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(OptimizeProgressEvent.error("参考简历缓存已过期，请重新上传", threadId)));
                emitter.complete();
            } catch (Exception e) {
                log.error("发送错误事件失败", e);
            }
            return emitter;
        }

        streamRewriteWithThreadId(id, cacheEntry.getContent(), threadId).subscribe(
                event -> GraphSseHelper.sendEvent(emitter, "[SSE-风格改写]", event),
                error -> {
                    log.error("[SSE-风格改写] 流异常", error);
                    emitter.completeWithError(error);
                },
                () -> {
                    log.info("[SSE-风格改写] 流完成");
                    emitter.complete();
                }
        );

        return emitter;
    }

    /**
     * 流式执行风格改写工作流（带指定threadId）
     */
    private Flux<OptimizeProgressEvent> streamRewriteWithThreadId(String id, String referenceContent, String threadId) {
        log.info("开始流式风格改写: resumeId={}, threadId={}", id, threadId);

        ResumeDetailVO resumeDetail = resumeHandler.getResumeDetail(id);
        if (resumeDetail == null) {
            return Flux.just(OptimizeProgressEvent.error("简历不存在", null));
        }

        Map<String, Object> initialState = buildInitialState(resumeDetail, referenceContent);

        return Flux.concat(
                Flux.just(OptimizeProgressEvent.start(id, threadId, null, "rewrite")),
                graphService.streamRewrite(initialState, threadId)
                        .filter(output -> !GraphSseHelper.isInterruptionOutput(output))
                        .map(output -> {
                            Map<String, Object> data = output.state().data();
                            @SuppressWarnings("unchecked")
                            Map<String, Object> nodeOutput = (Map<String, Object>) data.get(STATE_NODE_OUTPUT);
                            return OptimizeProgressEvent.fromNodeOutput(output.node(), threadId, nodeOutput);
                        }),
                Flux.just(OptimizeProgressEvent.complete(threadId))
        )
        .onErrorResume(e -> {
            log.error("风格改写工作流执行失败", e);
            return Flux.just(OptimizeProgressEvent.error(e.getMessage(), threadId));
        })
        .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 构建工作流初始状态
     */
    private Map<String, Object> buildInitialState(ResumeDetailVO resumeDetail, String referenceContent) {
        Map<String, Object> state = new HashMap<>();
        state.put(STATE_RESUME_CONTENT, resumeDetail);
        state.put(STATE_REFERENCE_CONTENT, referenceContent);
        state.put(STATE_MESSAGES, new ArrayList<String>());
        return state;
    }

    /**
     * 清理过期缓存
     */
    private void cleanupExpiredCache() {
        long now = System.currentTimeMillis();
        referenceCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * 缓存条目
     */
    private static class CacheEntry {
        private final String content;
        private final long expireTime;

        public CacheEntry(String content, long expireTime) {
            this.content = content;
            this.expireTime = expireTime;
        }

        public String getContent() {
            return content;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }
}
