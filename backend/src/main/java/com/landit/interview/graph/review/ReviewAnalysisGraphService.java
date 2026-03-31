package com.landit.interview.graph.review;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 复盘AI分析工作流服务
 * 提供复盘AI分析工作流的执行和管理
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewAnalysisGraphService {

    private final CompiledGraph reviewAnalysisGraph;

    /**
     * 流式执行工作流
     *
     * @param initialState 初始状态
     * @param threadId     会话线程ID
     * @return 流式输出
     */
    public Flux<NodeOutput> streamAnalysis(Map<String, Object> initialState, String threadId) {
        log.info("[复盘分析-Graph] 创建Graph流: threadId={}", threadId);
        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        return reviewAnalysisGraph.stream(initialState, config)
                .doOnSubscribe(s -> log.info("[复盘分析-Graph] Graph流被订阅: threadId={}", threadId))
                .doOnNext(output -> log.info("[复盘分析-Graph] Graph节点输出: node={}, thread={}",
                        output.node(), Thread.currentThread().getName()));
    }

}
