package com.careerforge.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 优化进度事件（SSE 返回格式）
 *
 * 包含丰富的优化数据：
 * - 基本信息：节点、进度、消息
 * - 节点特定数据：
 *   - start: 模块列表、完整性（来自 Controller 层计算）
 *   - diagnose_*: 评分、维度、问题
 *   - generate_suggestions: 建议列表
 *   - optimize_section: 变更详情
 *   - save_version: 版本信息
 *
 * @author Azir
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptimizeProgressEvent {
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
     * 创建开始事件（包含模块信息，合并原 parse_resume 阶段数据）
     *
     * @param resumeId    简历ID
     * @param threadId    线程ID
     * @param position    目标岗位
     * @param mode        诊断模式
     * @param parseResult 解析结果（包含模块信息）
     * @return 开始事件
     */
    public static OptimizeProgressEvent startWithModules(String resumeId, String threadId,
                                                          String position, String mode,
                                                          Map<String, Object> parseResult) {
        Map<String, Object> data = new HashMap<>();
        data.put("resumeId", resumeId);
        data.put("targetPosition", position);
        data.put("mode", mode);
        data.put("modules", parseResult.get("modules"));
        data.put("moduleCount", parseResult.get("moduleCount"));
        data.put("completeness", parseResult.get("completeness"));

        return new OptimizeProgressEvent("start", "start", 5,
                "简历解析完成，开始诊断 - " + position + " (" + mode + "模式)",
                threadId,
                data,
                System.currentTimeMillis());
    }

    /**
     * 创建开始事件（简单版本，用于职位适配等场景）
     *
     * @param resumeId       简历ID
     * @param threadId       线程ID
     * @param targetPosition 目标岗位
     * @param mode           模式
     * @return 开始事件
     */
    public static OptimizeProgressEvent start(String resumeId, String threadId,
                                               String targetPosition, String mode) {
        Map<String, Object> data = new HashMap<>();
        data.put("resumeId", resumeId);
        data.put("targetPosition", targetPosition);
        data.put("mode", mode);

        return new OptimizeProgressEvent("start", "start", 0,
                "开始处理 - " + targetPosition,
                threadId,
                data,
                System.currentTimeMillis());
    }

    /**
     * 从节点输出创建进度事件
     *
     * @param nodeId     节点ID
     * @param threadId   线程ID
     * @param nodeOutput 节点输出数据
     * @return 进度事件
     */
    public static OptimizeProgressEvent fromNodeOutput(String nodeId, String threadId, Map<String, Object> nodeOutput) {
        if (nodeOutput == null) {
            return new OptimizeProgressEvent("progress", nodeId, 0,
                    "处理中...", threadId, null, System.currentTimeMillis());
        }

        Integer progress = (Integer) nodeOutput.getOrDefault("progress", 0);
        String message = (String) nodeOutput.getOrDefault("message", "处理中...");
        Object data = nodeOutput.get("data");

        return new OptimizeProgressEvent("progress", nodeId, progress, message, threadId, data, System.currentTimeMillis());
    }

    /**
     * 创建完成事件
     *
     * @param threadId 线程ID
     * @return 完成事件
     */
    public static OptimizeProgressEvent complete(String threadId) {
        return new OptimizeProgressEvent("complete", "end", 100,
                "优化完成", threadId, null, System.currentTimeMillis());
    }

    /**
     * 创建错误事件
     *
     * @param errorMsg 错误消息
     * @param threadId 线程ID
     * @return 错误事件
     */
    public static OptimizeProgressEvent error(String errorMsg, String threadId) {
        return new OptimizeProgressEvent("error", null, null, errorMsg, threadId, null, System.currentTimeMillis());
    }
}
