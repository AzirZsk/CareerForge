package com.landit.resume.util;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * Graph 工作流 SSE 辅助工具类
 * 提供公共的 SSE 响应配置和事件发送方法
 *
 * @author Azir
 */
@Slf4j
public final class GraphSseHelper {

    /**
     * SSE 连接超时时间（10分钟）
     */
    public static final long SSE_TIMEOUT = 600000L;

    private GraphSseHelper() {
        // 工具类禁止实例化
    }

    /**
     * 配置 SSE 响应头
     *
     * @param response HTTP 响应对象
     */
    public static void configureSseResponse(HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    /**
     * 创建带超时的 SSE Emitter
     *
     * @return SseEmitter 实例
     */
    public static SseEmitter createEmitter() {
        return new SseEmitter(SSE_TIMEOUT);
    }

    /**
     * 发送 SSE 事件
     *
     * @param emitter SSE 发射器
     * @param logPrefix 日志前缀（用于区分不同工作流）
     * @param event 事件数据
     */
    public static void sendEvent(SseEmitter emitter, String logPrefix, Object event) {
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(event));
            if (event != null) {
                String eventType = event.getClass().getSimpleName();
                log.info("[{}] 发送事件: type={}", logPrefix, eventType);
            }
        } catch (IOException e) {
            log.error("[{}] 发送失败", logPrefix, e);
            emitter.completeWithError(e);
        }
    }

    /**
     * 判断节点输出是否是中断事件
     * 当 Graph 执行到配置了 interruptBefore 的节点时，会发出一个中断元数据输出
     * 这类输出不应该转换为进度事件发送给前端
     *
     * @param output 节点输出
     * @return true 表示是中断事件，应该被过滤
     */
    public static boolean isInterruptionOutput(NodeOutput output) {
        return output instanceof InterruptionMetadata;
    }

    /**
     * 安全关闭 SSE 连接
     *
     * @param emitter SSE 发射器
     * @param logPrefix 日志前缀
     */
    public static void safeComplete(SseEmitter emitter, String logPrefix) {
        try {
            emitter.complete();
            log.info("[{}] 流完成", logPrefix);
        } catch (Exception e) {
            log.warn("[{}] 关闭连接时出错", logPrefix, e);
        }
    }

    /**
     * 安全关闭 SSE 连接（带错误）
     *
     * @param emitter SSE 发射器
     * @param logPrefix 日志前缀
     * @param error 错误信息
     */
    public static void safeCompleteWithError(SseEmitter emitter, String logPrefix, Throwable error) {
        try {
            emitter.completeWithError(error);
            log.error("[{}] 流异常", logPrefix, error);
        } catch (Exception e) {
            log.warn("[{}] 关闭连接时出错", logPrefix, e);
        }
    }
}
