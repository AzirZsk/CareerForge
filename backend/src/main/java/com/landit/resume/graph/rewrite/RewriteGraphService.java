package com.landit.resume.graph.rewrite;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 简历风格改写工作流服务
 * 提供简历风格改写工作流的执行和管理
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RewriteGraphService {

    private final CompiledGraph resumeRewriteGraph;

    /**
     * 流式执行风格改写工作流
     *
     * @param initialState 初始状态
     * @param threadId     会话线程ID
     * @return 流式输出
     */
    public Flux<NodeOutput> streamRewrite(Map<String, Object> initialState, String threadId) {
        log.info("[风格改写-Graph] 创建Graph流: threadId={}", threadId);
        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        return resumeRewriteGraph.stream(initialState, config)
                .doOnSubscribe(s -> log.info("[风格改写-Graph] Graph流被订阅: threadId={}", threadId))
                .doOnNext(output -> log.info("[风格改写-Graph] Graph节点输出: node={}, thread={}",
                        output.node(), Thread.currentThread().getName()));
    }
}
