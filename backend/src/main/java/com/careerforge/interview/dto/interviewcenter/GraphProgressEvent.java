package com.careerforge.interview.dto.interviewcenter;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 面试工作流进度事件（SSE 返回格式）
 * 用于面试准备工作流和复盘分析工作流的 SSE 事件
 *
 * @author Azir
 */
@Data
@NoArgsConstructor
public class GraphProgressEvent {

    /**
     * 事件类型：start / progress / complete / error
     */
    private String event;

    /**
     * 当前节点ID
     */
    private String nodeId;

    /**
     * 进度百分比 0-100
     */
    private Integer progress;

    /**
     * 进度消息
     */
    private String message;

    /**
     * 线程ID（用于追踪工作流）
     */
    private String threadId;

    /**
     * 节点特定数据
     */
    private Object data;

    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * 是否使用缓存（节点跳过执行）
     */
    private Boolean cached = false;

    /**
     * 全参数构造函数
     */
    public GraphProgressEvent(String event, String nodeId, Integer progress, String message,
                              String threadId, Object data, long timestamp) {
        this.event = event;
        this.nodeId = nodeId;
        this.progress = progress;
        this.message = message;
        this.threadId = threadId;
        this.data = data;
        this.timestamp = timestamp;
        this.cached = false;
    }

    /**
     * 创建开始事件（面试准备工作流）
     *
     * @param interviewId 面试ID
     * @param threadId    线程ID
     * @return 开始事件
     */
    public static GraphProgressEvent startPreparation(String interviewId, String threadId) {
        Map<String, Object> data = new HashMap<>();
        data.put("interviewId", interviewId);

        return new GraphProgressEvent("start", "start", 5,
                "开始生成准备事项",
                threadId, data, System.currentTimeMillis());
    }

    /**
     * 创建开始事件（复盘分析工作流）
     *
     * @param interviewId 面试ID
     * @param threadId    线程ID
     * @return 开始事件
     */
    public static GraphProgressEvent startReviewAnalysis(String interviewId, String threadId) {
        Map<String, Object> data = new HashMap<>();
        data.put("interviewId", interviewId);

        return new GraphProgressEvent("start", "start", 5,
                "开始分析面试表现...",
                threadId, data, System.currentTimeMillis());
    }

    /**
     * 从节点输出创建进度事件
     *
     * @param nodeId     节点ID
     * @param threadId   线程ID
     * @param nodeOutput 节点输出数据
     * @return 进度事件
     */
    @SuppressWarnings("unchecked")
    public static GraphProgressEvent fromNodeOutput(String nodeId, String threadId, Map<String, Object> nodeOutput) {
        if (nodeOutput == null) {
            return new GraphProgressEvent("progress", nodeId, 0,
                    "处理中...", threadId, null, System.currentTimeMillis());
        }

        Integer progress = (Integer) nodeOutput.getOrDefault("progress", 0);
        String message = (String) nodeOutput.getOrDefault("message", "处理中...");
        Object data = nodeOutput.get("data");
        Boolean cached = (Boolean) nodeOutput.get("cached");

        GraphProgressEvent event = new GraphProgressEvent("progress", nodeId, progress, message, threadId, data, System.currentTimeMillis());
        if (cached != null && cached) {
            event.setCached(true);
        }
        return event;
    }

    /**
     * 创建完成事件（面试准备工作流）
     *
     * @param threadId         线程ID
     * @param preparationItems 准备事项列表
     * @return 完成事件
     */
    public static GraphProgressEvent completePreparation(String threadId, Object preparationItems) {
        Map<String, Object> data = new HashMap<>();
        data.put("preparationItems", preparationItems);

        return new GraphProgressEvent("complete", "end", 100,
                "准备事项生成完成", threadId, data, System.currentTimeMillis());
    }

    /**
     * 创建完成事件（复盘分析工作流）
     *
     * @param threadId   线程ID
     * @param adviceList 改进建议列表
     * @return 完成事件
     */
    public static GraphProgressEvent completeReviewAnalysis(String threadId, Object adviceList) {
        Map<String, Object> data = new HashMap<>();
        data.put("adviceList", adviceList);

        return new GraphProgressEvent("complete", "end", 100,
                "复盘分析完成", threadId, data, System.currentTimeMillis());
    }

    /**
     * 创建错误事件
     *
     * @param errorMsg 错误消息
     * @param threadId  线程ID
     * @return 错误事件
     */
    public static GraphProgressEvent error(String errorMsg, String threadId) {
        return new GraphProgressEvent("error", null, null, errorMsg, threadId, null, System.currentTimeMillis());
    }
}
