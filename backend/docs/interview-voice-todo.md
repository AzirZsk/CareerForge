# 语音面试功能 - 待完成开发任务

> 文档版本：1.0.0
> 创建日期：2026-03-25
> 状态：Phase 1 已完成，Phase 2-4 待开发

---

## 一、已完成内容（Phase 1）

### 1.1 后端基础架构 ✅

| 文件 | 路径 | 状态 |
|------|------|------|
| VoiceProperties.java | `common/config/` | ✅ 完成 |
| ASRConfig.java | `interview/voice/dto/` | ✅ 完成 |
| ASRResult.java | `interview/voice/dto/` | ✅ 完成 |
| TTSConfig.java | `interview/voice/dto/` | ✅ 完成 |
| TTSChunk.java | `interview/voice/dto/` | ✅ 完成 |
| ASRService.java | `interview/voice/service/` | ✅ 接口定义完成 |
| TTSService.java | `interview/voice/service/` | ✅ 接口定义完成 |
| VoiceServiceFactory.java | `interview/voice/service/` | ✅ 完成 |
| AliyunVoiceBaseService.java | `interview/voice/service/impl/` | ✅ 完成（基类） |
| AliyunASRService.java | `interview/voice/service/impl/` | ⚠️ 存根实现 |
| AliyunTTSService.java | `interview/voice/service/impl/` | ⚠️ 存根实现 |
| AliyunVoiceAutoConfiguration.java | `interview/voice/config/` | ✅ 完成 |
| schema.sql | `resources/` | ✅ 已添加语音相关表 |

### 1.2 前端基础架构 ✅

| 文件 | 路径 | 状态 |
|------|------|------|
| interview-voice.ts | `types/` | ✅ 完成 |
| useStreamingAudio.ts | `composables/` | ✅ 完成 |
| useAudioRecorder.ts | `composables/` | ✅ 完成 |
| useStreamAssist.ts | `composables/` | ✅ 完成 |
| useInterviewVoice.ts | `composables/` | ✅ 完成 |

### 1.3 配置更新 ✅

| 文件 | 变更内容 |
|------|----------|
| application.yml | 添加 `landit.voice` 配置节 |
| pom.xml | 添加 WebSocket、WebFlux、Jakarta WebSocket API 依赖 |

---

## 二、待完成内容（Phase 2-4）

### 2.1 Phase 2: 多角色对话 + 快捷求助（预估 3-4 天）

#### 2.1.1 后端任务

**核心控制器**

| 序号 | 文件 | 路径 | 说明 |
|------|------|------|------|
| 1 | InterviewVoiceController.java | `interview/voice/controller/` | WebSocket 语音对话入口 |
| 2 | AssistantController.java | `interview/voice/controller/` | SSE 流式求助接口 |

**业务处理器**

| 序号 | 文件 | 路径 | 说明 |
|------|------|------|------|
| 3 | InterviewVoiceGateway.java | `interview/voice/gateway/` | 语音会话网关（核心） |
| 4 | InterviewerAgentHandler.java | `interview/voice/handler/` | 面试官 Agent 处理器 |
| 5 | AssistantAgentHandler.java | `interview/voice/handler/` | AI 助手 Agent 处理器 |

**服务层**

| 序号 | 文件 | 路径 | 说明 |
|------|------|------|------|
| 6 | StreamAssistService.java | `interview/voice/service/` | SSE 流式求助服务 |
| 7 | VoiceDialogService.java | `interview/voice/service/` | 语音对话编排服务 |

**完善 ASR/TTS 实现**

| 序号 | 文件 | 说明 |
|------|------|------|
| 8 | AliyunASRService.java | 完善 WebSocket 实时语音识别（当前是存根） |
| 9 | AliyunTTSService.java | 完善 WebSocket 流式语音合成（当前是存根） |

**详细实现要点：**

```java
// InterviewVoiceGateway.java 核心职责
public class InterviewVoiceGateway {
    // 1. 会话状态管理
    - 冻结面试（进入求助模式）
    - 恢复面试（退出求助模式）
    - 结束面试

    // 2. 角色路由
    - 面试官消息 → InterviewerAgentHandler
    - 助手消息 → AssistantAgentHandler

    // 3. 录音存储
    - 接收音频片段
    - 存储到 t_interview_recording
    - 更新 t_recording_index
}

// AssistantController.java SSE 接口
@GetMapping("/sessions/{sessionId}/assist/stream")
public Flux<ServerSentEvent<AssistSSEEvent>> streamAssist(
    @PathVariable String sessionId,
    @RequestParam AssistType type,
    @RequestParam(required = false) String question
) {
    // 1. 检查求助次数
    // 2. 冻结会话
    // 3. 调用 LLM 流式生成
    // 4. 调用 TTS 流式合成
    // 5. 返回 SSE 事件流
}
```

#### 2.1.2 前端任务

**组件开发**

| 序号 | 文件 | 路径 | 说明 |
|------|------|------|------|
| 1 | VoiceControls.vue | `components/interview/voice/` | 语音控制面板 |
| 2 | TranscriptDisplay.vue | `components/interview/voice/` | 实时转录显示 |
| 3 | AssistantPanel.vue | `components/interview/voice/` | 助手对话面板 |
| 4 | QuickAssistButtons.vue | `components/interview/voice/` | 快捷求助按钮组 |

**页面修改**

| 序号 | 文件 | 路径 | 说明 |
|------|------|------|------|
| 5 | InterviewSession.vue | `views/` | 集成语音面试功能 |

---

### 2.2 Phase 3: 录音回放（预估 2 天）

#### 2.2.1 后端任务

| 序号 | 文件 | 路径 | 说明 |
|------|------|------|------|
| 1 | RecordingService.java | `interview/voice/service/` | 录音存储服务 |
| 2 | RecordingMergeService.java | `interview/voice/service/` | 录音合并服务 |
| 3 | RecordingController.java | `interview/voice/controller/` | 录音回放接口 |

**详细实现要点：**

```java
// RecordingService.java
public interface RecordingService {
    // 保存录音片段
    void saveSegment(String sessionId, RecordingSegment segment);

    // 获取录音片段列表
    List<RecordingSegment> getSegments(String sessionId);

    // 获取录音回放信息
    RecordingInfo getRecordingInfo(String sessionId);
}

// RecordingMergeService.java
public interface RecordingMergeService {
    // 合并所有片段为完整音频
    Path mergeRecordings(String sessionId);

    // 生成文字记录
    List<TranscriptEntry> generateTranscript(String sessionId);
}

// API 接口
GET  /interviews/sessions/{sessionId}/recording          // 获取录音信息
GET  /interviews/sessions/{sessionId}/recording/audio   // 下载完整音频
GET  /interviews/sessions/{sessionId}/recording/transcript  // 获取文字记录
```

#### 2.2.2 前端任务

| 序号 | 文件 | 路径 | 说明 |
|------|------|------|------|
| 1 | InterviewRecording.vue | `views/` | 录音回放页面 |
| 2 | RecordingPlayer.vue | `components/interview/voice/` | 录音播放器组件 |
| 3 | TranscriptViewer.vue | `components/interview/voice/` | 文字记录查看器 |

---

### 2.3 Phase 4: 优化完善（预估 2-3 天）

#### 2.3.1 功能优化

| 序号 | 任务 | 说明 |
|------|------|------|
| 1 | 语音模式切换 | 支持纯文本、半语音、全语音模式切换 |
| 2 | 弱网处理 | WebSocket 断线重连、音频缓冲 |
| 3 | 首字节延迟优化 | LLM + TTS 流水线并行处理 |
| 4 | VAD 参数调优 | 静音检测阈值、静音超时配置 |

#### 2.3.2 UI/UX 优化

| 序号 | 任务 | 说明 |
|------|------|------|
| 1 | 音频波形可视化 | 实时显示音频电平 |
| 2 | 状态指示器 | 清晰展示当前状态（录音中、识别中、合成中） |
| 3 | 错误提示优化 | 友好的错误提示和恢复引导 |
| 4 | 响应式布局 | 适配移动端 |

#### 2.3.3 性能优化

| 序号 | 任务 | 说明 |
|------|------|------|
| 1 | 音频队列优化 | 避免播放卡顿 |
| 2 | 内存管理 | 及时释放音频缓冲区 |
| 3 | 连接池管理 | WebSocket 连接复用 |

---

## 三、数据库表结构（已创建）

### 3.1 扩展字段

```sql
-- t_interview_session 新增字段
voice_mode VARCHAR(20) DEFAULT 'text';    -- text, half_voice, full_voice
assist_count INT DEFAULT 0;
assist_limit INT DEFAULT 5;
freeze_at DATETIME;                       -- 冻结时间点
```

### 3.2 新增表

```sql
-- t_assistant_conversation（助手对话记录）
CREATE TABLE t_assistant_conversation (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    freeze_index INT NOT NULL,
    assist_type VARCHAR(30) NOT NULL,
    user_question TEXT,
    assistant_response TEXT NOT NULL,
    audio_url VARCHAR(500),
    duration_ms INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_interview_recording（面试录音片段）
CREATE TABLE t_interview_recording (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    segment_index INT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT,
    audio_path VARCHAR(500) NOT NULL,
    duration_ms INT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- t_recording_index（录音索引）
CREATE TABLE t_recording_index (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    total_duration_ms INT DEFAULT 0,
    merged_audio_path VARCHAR(500),
    transcript TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 四、配置说明

### 4.1 application.yml 配置

```yaml
landit:
  voice:
    provider: aliyun
    aliyun:
      api-key: ${ALIYUN_API_KEY:}
      asr:
        model: paraformer-realtime-v2
        format: pcm
        sample-rate: 16000
        enable-punctuation: true
        enable-itn: true
        language: zh
        enable-vad: true
      tts:
        model: cosyvoice-v2
        format: wav
        sample-rate: 16000
      voices:
        interviewer: longxiaochun_v2
        assistant: zhimiao_emo_v2
```

### 4.2 环境变量

```bash
# 阿里云 API Key（必需）
export ALIYUN_API_KEY=your_api_key_here

# 现有 OpenAI 配置（复用）
export OPENAI_API_KEY=your_openai_key
export OPENAI_BASE_URL=https://api.openai.com
```

---

## 五、关键依赖

### 5.1 已添加依赖（pom.xml）

```xml
<!-- WebSocket 客户端 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>

<!-- Jakarta WebSocket API -->
<dependency>
    <groupId>jakarta.websocket</groupId>
    <artifactId>jakarta.websocket-api</artifactId>
    <version>2.1.1</version>
</dependency>

<!-- WebFlux（响应式流） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

---

## 六、测试验证清单

### 6.1 Phase 1 验证

- [ ] 后端编译通过：`mvn clean compile`
- [ ] 前端编译通过：`npm run build`
- [ ] 配置加载正确：检查 `/actuator/configprops`

### 6.2 Phase 2 验证

- [ ] WebSocket 连接建立成功
- [ ] ASR 实时识别正常
- [ ] TTS 流式合成正常
- [ ] SSE 求助接口正常
- [ ] 冻结/恢复机制正常

### 6.3 Phase 3 验证

- [ ] 录音片段正确保存
- [ ] 录音合并正确
- [ ] 文字记录完整
- [ ] 回放功能正常

### 6.4 Phase 4 验证

- [ ] 弱网重连正常
- [ ] 首字节延迟 < 1s
- [ ] 内存无泄漏
- [ ] 移动端适配正常

---

## 七、参考文档

| 文档 | 链接 |
|------|------|
| 阿里云 Paraformer ASR | https://help.aliyun.com/zh/model-studio/websocket-for-paraformer-real-time-service |
| 阿里云 CosyVoice TTS | https://help.aliyun.com/zh/model-studio/cosyvoice-clone-design-api |
| 语音面试技术方案 | `backend/docs/interview-voice-feature.md` |

---

## 八、注意事项

1. **API Key 安全**：生产环境使用环境变量或密钥管理服务
2. **WebSocket 稳定性**：实现心跳检测和断线重连
3. **音频格式**：统一使用 16kHz PCM/WAV
4. **错误处理**：提供友好的降级方案（如语音失败降级到文字模式）
5. **性能监控**：记录首字节延迟、识别准确率等指标

---

**文档维护者**：老王
**最后更新**：2026-03-25
