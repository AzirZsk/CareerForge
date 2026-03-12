package com.landit.resume.graph.optimize;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * 简历优化工作流服务
 * 提供简历优化工作流的执行和管理
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeOptimizeGraphService {

    private final CompiledGraph resumeOptimizeGraph;

    /**
     * 流式执行工作流
     *
     * @param initialState 初始状态
     * @param threadId     会话线程ID
     * @return 流式输出
     */
    public Flux<NodeOutput> streamOptimize(Map<String, Object> initialState, String threadId) {
        log.info("[SSE诊断-Graph] 创建Graph流: threadId={}", threadId);
        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        return resumeOptimizeGraph.stream(initialState, config)
                .doOnSubscribe(s -> log.info("[SSE诊断-Graph] Graph流被订阅: threadId={}", threadId))
                .doOnNext(output -> log.info("[SSE诊断-Graph] Graph节点输出: node={}, thread={}",
                        output.node(), Thread.currentThread().getName()));
    }


}
