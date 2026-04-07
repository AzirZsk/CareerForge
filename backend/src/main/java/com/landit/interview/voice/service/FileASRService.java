package com.landit.interview.voice.service;

import com.landit.interview.voice.dto.FileASRResult;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

/**
 * 离线语音识别服务接口
 * 支持上传音频文件进行转录（非实时场景）
 *
 * <p>实现类：
 * <ul>
 *   <li>{@code AliyunFileASRService} - 阿里云 Paraformer 录音文件识别</li>
 * </ul>
 *
 * <p>使用场景：
 * <ul>
 *   <li>面试复盘上传录音文件转文字</li>
 * </ul>
 *
 * @author Azir
 */
public interface FileASRService {

    /**
     * 获取服务提供商标识
     *
     * @return "aliyun"
     */
    String getProvider();

    /**
     * 异步转录音频文件
     * 适用于复盘场景，上传录音文件后获取转录文本
     *
     * <p>返回 Flux 流式结果，支持 SSE 推送进度：
     * <ul>
     *   <li>processing - 处理中，包含进度和状态消息</li>
     *   <li>completed - 完成，包含转录文本</li>
     *   <li>failed - 失败，包含错误信息</li>
     * </ul>
     *
     * <p>使用示例：
     * <pre>{@code
     * Flux<FileASRResult> results = fileASRService.transcribe(file);
     * results.subscribe(result -> {
     *     if ("completed".equals(result.getStatus())) {
     *         System.out.println("转录文本: " + result.getText());
     *     }
     * });
     * }</pre>
     *
     * @param file 音频文件（支持 wav/mp3/m4a/aac/ogg/flac 等格式）
     * @return 转录结果流
     */
    Flux<FileASRResult> transcribe(MultipartFile file);

    /**
     * 检查服务是否可用
     * 用于健康检查或启动时验证配置
     *
     * @return true 表示服务可用
     */
    default boolean isAvailable() {
        return true;
    }
}
