package com.landit.resume.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.landit.resume.graph.ResumeOptimizeGraphConstants.*;

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

    // 存储工作流状态和节点输出（生产环境应使用 Redis 或数据库）
    private final Map<String, Map<String, Object>> stateStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> nodeOutputStore = new ConcurrentHashMap<>();

    /**
     * 执行简历优化工作流（快速模式）
     *
     * @param resumeId       简历ID
     * @param resumeContent  简历内容
     * @param targetPosition 目标岗位
     * @param threadId       会话线程ID
     * @return 工作流执行结果
     */
    public Map<String, Object> executeQuickOptimize(String resumeId, String resumeContent,
                                                     String targetPosition, String threadId) {
        log.info("执行简历优化工作流（快速模式）: resumeId={}", resumeId);

        Map<String, Object> initialState = new HashMap<>();
        initialState.put(STATE_RESUME_ID, resumeId);
        initialState.put(STATE_RESUME_CONTENT, resumeContent);
        initialState.put(STATE_TARGET_POSITION, targetPosition);
        initialState.put(STATE_DIAGNOSIS_MODE, MODE_QUICK);
        initialState.put(STATE_MESSAGES, new ArrayList<String>());

        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        // 同步执行工作流
        Flux<NodeOutput> stream = resumeOptimizeGraph.stream(initialState, config);

        // 收集最终结果
        Map<String, Object> finalResult = new HashMap<>();
        stream.doOnNext(output -> {
            log.info("节点输出: {}", output.node());
            finalResult.put(STATE_LAST_NODE, output.node());
        }).blockLast();

        // 获取最终状态
        StateSnapshot finalState = resumeOptimizeGraph.getState(config);
        if (finalState != null && finalState.state() != null) {
            finalResult.putAll(finalState.state().data());
            // 保存状态和节点输出
            stateStore.put(threadId, new HashMap<>(finalState.state().data()));
            extractNodeOutput(threadId, finalState.state().data());
        }

        return finalResult;
    }

    /**
     * 执行简历优化工作流（精准模式）
     *
     * @param resumeId       简历ID
     * @param resumeContent  简历内容
     * @param targetPosition 目标岗位
     * @param searchResults  搜索结果
     * @param threadId       会话线程ID
     * @return 工作流执行结果
     */
    public Map<String, Object> executePreciseOptimize(String resumeId, String resumeContent,
                                                       String targetPosition, String searchResults,
                                                       String threadId) {
        log.info("执行简历优化工作流（精准模式）: resumeId={}", resumeId);

        Map<String, Object> initialState = new HashMap<>();
        initialState.put(STATE_RESUME_ID, resumeId);
        initialState.put(STATE_RESUME_CONTENT, resumeContent);
        initialState.put(STATE_TARGET_POSITION, targetPosition);
        initialState.put(STATE_DIAGNOSIS_MODE, MODE_PRECISE);
        initialState.put(STATE_SEARCH_RESULTS, searchResults);
        initialState.put(STATE_MESSAGES, new ArrayList<String>());

        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        Flux<NodeOutput> stream = resumeOptimizeGraph.stream(initialState, config);

        Map<String, Object> finalResult = new HashMap<>();
        stream.doOnNext(output -> {
            log.info("节点输出: {}", output.node());
            finalResult.put(STATE_LAST_NODE, output.node());
        }).blockLast();

        StateSnapshot finalState = resumeOptimizeGraph.getState(config);
        if (finalState != null && finalState.state() != null) {
            finalResult.putAll(finalState.state().data());
            stateStore.put(threadId, new HashMap<>(finalState.state().data()));
            extractNodeOutput(threadId, finalState.state().data());
        }

        return finalResult;
    }

    /**
     * 流式执行工作流
     *
     * @param initialState 初始状态
     * @param threadId     会话线程ID
     * @return 流式输出
     */
    public Flux<NodeOutput> streamOptimize(Map<String, Object> initialState, String threadId) {
        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        return resumeOptimizeGraph.stream(initialState, config)
                .doOnNext(output -> {
                    // 更新状态存储
                    StateSnapshot state = resumeOptimizeGraph.getState(config);
                    if (state != null && state.state() != null) {
                        Map<String, Object> stateData = state.state().data();
                        stateStore.put(threadId, new HashMap<>(stateData));
                        extractNodeOutput(threadId, stateData);
                    }
                });
    }

    /**
     * 获取工作流当前状态
     *
     * @param threadId 会话线程ID
     * @return 当前状态
     */
    public Map<String, Object> getState(String threadId) {
        // 优先从本地存储获取
        Map<String, Object> cachedState = stateStore.get(threadId);
        if (cachedState != null) {
            return cachedState;
        }

        // 尝试从 Graph 获取
        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        StateSnapshot state = resumeOptimizeGraph.getState(config);
        if (state != null && state.state() != null) {
            return state.state().data();
        }
        return new HashMap<>();
    }

    /**
     * 获取最新节点输出（用于 SSE）
     *
     * @param threadId 会话线程ID
     * @return 节点输出数据
     */
    public Map<String, Object> getNodeOutput(String threadId) {
        return nodeOutputStore.getOrDefault(threadId, new HashMap<>());
    }

    /**
     * 更新工作流状态（用于人工审核后恢复）
     *
     * @param threadId  会话线程ID
     * @param updates   状态更新
     * @return 更新后的配置
     */
    public RunnableConfig updateState(String threadId, Map<String, Object> updates) {
        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        try {
            RunnableConfig updatedConfig = resumeOptimizeGraph.updateState(config, updates, null);

            // 更新本地存储
            Map<String, Object> currentState = stateStore.getOrDefault(threadId, new HashMap<>());
            currentState.putAll(updates);
            stateStore.put(threadId, currentState);

            return updatedConfig;
        } catch (Exception e) {
            log.error("更新工作流状态失败: threadId={}", threadId, e);
            throw new RuntimeException("更新工作流状态失败", e);
        }
    }

    /**
     * 恢复并继续执行工作流（用于人工审核后）
     *
     * @param threadId 会话线程ID
     * @return 继续执行的结果
     */
    public Map<String, Object> resumeOptimize(String threadId) {
        log.info("恢复工作流执行: threadId={}", threadId);

        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        // 使用 null 作为输入，继续使用之前的状态
        Flux<NodeOutput> stream = resumeOptimizeGraph.stream(null, config);

        Map<String, Object> finalResult = new HashMap<>();
        stream.doOnNext(output -> {
            log.info("节点输出: {}", output.node());
            finalResult.put(STATE_LAST_NODE, output.node());
        }).blockLast();

        StateSnapshot finalState = resumeOptimizeGraph.getState(config);
        if (finalState != null && finalState.state() != null) {
            finalResult.putAll(finalState.state().data());
            stateStore.put(threadId, new HashMap<>(finalState.state().data()));
            extractNodeOutput(threadId, finalState.state().data());
        }

        return finalResult;
    }

    /**
     * 提交人工审核结果并继续执行
     *
     * @param threadId      会话线程ID
     * @param approved      是否通过
     * @param modifications 修改内容（可选）
     * @return 继续执行的结果
     */
    public Map<String, Object> submitReview(String threadId, boolean approved, Map<String, Object> modifications) {
        log.info("提交人工审核结果: threadId={}, approved={}", threadId, approved);

        Map<String, Object> updates = new HashMap<>();
        updates.put(STATE_APPROVED, approved);
        if (modifications != null) {
            updates.putAll(modifications);
        }

        // 更新状态
        updateState(threadId, updates);

        // 继续执行
        return resumeOptimize(threadId);
    }

    /**
     * 从状态数据中提取节点输出
     */
    @SuppressWarnings("unchecked")
    private void extractNodeOutput(String threadId, Map<String, Object> stateData) {
        if (stateData.containsKey(STATE_NODE_OUTPUT)) {
            Map<String, Object> nodeOutput = (Map<String, Object>) stateData.get(STATE_NODE_OUTPUT);
            if (nodeOutput != null) {
                nodeOutputStore.put(threadId, new HashMap<>(nodeOutput));
            }
        }
    }

}
