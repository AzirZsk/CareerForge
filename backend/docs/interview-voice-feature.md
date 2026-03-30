# 虚拟面试语音交互功能 - 技术方案

> 版本：1.4.0
> 创建日期：2026-03-12
> 更新日期：2026-03-25
> 状态：设计阶段

---

## 一、功能概述

### 1.1 核心需求

实现一个三角色虚拟面试系统，支持实时语音交互：

| 角色 | 扮演者 | 职责 |
|------|--------|------|
| **面试官** | AI | 提问、追问、评估回答质量、给出反馈 |
| **候选人** | 用户 | 回答问题、可向助手求助 |
| **AI助手** | AI | 提供实时辅导（思路提示、知识点补充、回答润色） |

### 1.2 关键特性

- **实时语音对话**：候选人与面试官进行实时语音交互（类似打电话）
- **流式语音合成**：AI 回复边生成边播放，无需等待整段语音生成完成
- **快捷求助**：通过按钮点击触发求助，面试对话冻结
- **录音回放**：面试结束后可回放整场录音

### 1.3 流式语音特性

```
传统方式（等待完整生成）：
  用户说话 → AI思考 → 生成完整文本 → 合成完整语音 → 播放
                                    ↑ 等待时间较长

流式方式（边生成边播放）：
  用户说话 → AI思考 → [生成片段1] → [合成片段1] → [播放片段1]
                         ↓              ↓            ↓
                    [生成片段2] → [合成片段2] → [播放片段2]
                         ↓              ↓            ↓
                    [生成片段3] → [合成片段3] → [播放片段3]
                    ↑ 首字节延迟大幅降低，用户体验更流畅
```

### 1.3 交互模式

| 模式 | 面试官 | 候选人 | AI助手 | 适用场景 |
|------|--------|--------|--------|----------|
| **纯文本** | 文字 | 文字 | 文字 | 快速练习、公共场合 |
| **半语音** | 语音 | 文字 | 语音 | 候选人打字但听题 |
| **全语音** | 语音 | 语音 | 语音 | 真实模拟面试 |
| **混合模式** | 语音 | 语音/文字 | 语音 | 灵活切换 |

---

## 二、系统架构

### 2.1 架构图

采用 **ASR + LLM + TTS 分开调用**的架构，实现更精细的控制和更好的灵活性：

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              前端 (Vue 3)                                │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐  │
│  │  面试对话界面    │  │  AI助手对话面板  │  │  录音回放界面            │  │
│  └────────┬────────┘  └────────┬────────┘  └────────────┬────────────┘  │
│           │                    │                        │               │
│           └────────────────────┼────────────────────────┘               │
│                                ▼                                        │
│                    ┌─────────────────────┐                              │
│                    │   VoiceController   │                              │
│                    │  (音频采集/播放管理) │                              │
│                    └──────────┬──────────┘                              │
└───────────────────────────────┼─────────────────────────────────────────┘
                                │ WebSocket / SSE
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            后端 (Spring Boot)                            │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                     InterviewVoiceGateway                        │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐   │    │
│  │  │ 会话管理     │  │ 角色路由     │  │ 录音存储             │   │    │
│  │  │ (冻结/恢复)  │  │ (面试官/助手)│  │ (片段合并)           │   │    │
│  │  └──────────────┘  └──────────────┘  └──────────────────────┘   │    │
│  └─────────────────────────────┬───────────────────────────────────┘    │
│                                │                                        │
│           ┌────────────────────┼────────────────────┐                   │
│           ▼                    ▼                    ▼                   │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────────────┐     │
│  │  面试官 Agent   │  │  AI助手 Agent  │  │  录音服务              │     │
│  │  (提问/追问)    │  │  (辅导/提示)   │  │  (存储/回放)           │     │
│  └───────┬────────┘  └───────┬────────┘  └────────────────────────┘     │
│          │                   │                                           │
└──────────┼───────────────────┼───────────────────────────────────────────┘
           │                   │
           └─────────┬─────────┘
                     │
    ┌────────────────┼────────────────┐
    │                │                │
    ▼                ▼                ▼
┌──────────┐   ┌─────────┐    ┌──────────┐
│ASRService│   │   LLM   │    │TTSService│    ◀── 语音服务接口层（抽象）
│  (接口)   │   │  大模型  │    │  (接口)   │
└────┬─────┘   └────┬────┘    └────┬─────┘
     │              │              │
     └──────────────┼──────────────┘
                    │
    ┌───────────────┼───────────────┐
    │               │               │
    ▼               ▼               ▼
┌──────────┐  ┌──────────┐  ┌──────────┐
│ Aliyun   │  │ OpenAI   │  │  Azure   │         ◀── 实现层（可扩展）
│ Impl     │  │ (预留)    │  │  (预留)  │
└────┬─────┘  └────┬─────┘  └────┬─────┘
     │             │             │
     └─────────────┼─────────────┘
                   │ WebSocket / HTTP
                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       阿里云百炼服务（默认实现）                           │
│                                                                         │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐       │
│  │   Paraformer     │  │   通义千问        │  │   CosyVoice      │       │
│  │  实时语音识别     │  │  (Qwen)          │  │   流式语音合成   │       │
│  │  WebSocket API   │  │  流式文本生成     │  │   WebSocket API  │       │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘       │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 技术选型

| 组件 | 服务 | API 类型 | 说明 |
|------|------|----------|------|
| **ASR** | Paraformer | WebSocket | 实时语音识别，支持流式输入输出 |
| **LLM** | 通义千问 (Qwen) | HTTP/SSE | 流式文本生成，可复用现有 OpenAI 配置 |
| **TTS** | CosyVoice | WebSocket | 流式语音合成，支持实时输出 |

### 2.3 核心流程

#### 实时语音对话流程（ASR + LLM + TTS）

```
候选人说话        后端处理              阿里云服务              前端展示
    │               │                     │                    │
    │  ── 音频流 ──▶│                     │                    │
    │               │  ── 音频流 ────────▶│                    │
    │               │                     │  (Paraformer ASR)  │
    │               │  ◀─ 文字(实时) ─────│                    │
    │               │                     │                    │
    │               │  ── 显示候选人说话 ─────────────────────▶│
    │               │                     │                    │
    │               │  ═══ LLM 推理 ═══   │                    │
    │               │                     │                    │
    │               │  ── 提示词+上下文 ─▶│                    │
    │               │                     │  (通义千问)        │
    │               │  ◀─ 文本片段1 ─────│                    │
    │               │  ── SSE推送 ───────────────────────────▶│ 显示文字
    │               │                     │                    │
    │               │  ═══ TTS 合成 ═══   │                    │
    │               │  ── 文本片段1 ────▶│                    │
    │               │                     │  (CosyVoice TTS)   │
    │               │  ◀─ 音频片段1 ─────│                    │
    │               │  ── SSE推送 ───────────────────────────▶│ 🎵 播放
    │               │                     │                    │
    │               │  ◀─ 文本片段2 ─────│                    │
    │               │  ── SSE推送 ───────────────────────────▶│ 显示文字
    │               │  ── 文本片段2 ────▶│                    │
    │               │  ◀─ 音频片段2 ─────│                    │
    │               │  ── SSE推送 ───────────────────────────▶│ 🎵 播放
    │               │                     │                    │
    │  用户几乎无感知延迟，边生成边播放    │                    │
    │                                                             │
```

#### 流式求助助手流程

```
点击求助按钮        后端处理              阿里云服务           前端展示
    │                │                     │                   │
    │  ── SSE请求 ─▶│                     │                   │
    │                │                     │                   │
    │                │  ═══ LLM 流式生成 ═══                   │
    │                │  ── 提示词+上下文 ─▶│                   │
    │                │                     │ (通义千问)        │
    │                │  ◀─ 文本片段1 ─────│                   │
    │                │  ── SSE事件 ─────────────────────────▶ │ 显示文字
    │                │                     │                   │
    │                │  ═══ TTS 流式合成 ═══                   │
    │                │  ── 文本片段1 ────▶│                   │
    │                │                     │ (CosyVoice)       │
    │                │  ◀─ 音频片段1 ─────│                   │
    │                │  ── SSE事件 ─────────────────────────▶ │ 🎵 播放
    │                │                     │                   │
    │                │  ◀─ 文本片段2 ─────│                   │
    │                │  ── SSE事件 ─────────────────────────▶ │ 显示文字
    │                │  ── 文本片段2 ────▶│                   │
    │                │  ◀─ 音频片段2 ─────│                   │
    │                │  ── SSE事件 ─────────────────────────▶ │ 🎵 播放
    │                │                     │                   │
    │                │  ═══ 生成完成 ═══   │                   │
    │                │  ── SSE完成事件 ─────────────────────▶ │ 面板就绪
    │                                                             │
```

### 2.4 数据流向图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           语音对话数据流                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   候选人语音                                                            │
│       │                                                                 │
│       ▼                                                                 │
│   ┌─────────┐      WebSocket       ┌─────────────┐                     │
│   │  前端   │ ──────────────────▶ │   后端      │                     │
│   │ 音频采集│    (PCM 16kHz)      │  Gateway    │                     │
│   └─────────┘                     └──────┬──────┘                     │
│                                          │                              │
│                        ┌─────────────────┼─────────────────┐           │
│                        │                 │                 │           │
│                        ▼                 │                 ▼           │
│                  ┌──────────┐            │           ┌──────────┐      │
│                  │  Paraformer          │           │ CosyVoice│      │
│                  │   (ASR)  │            │           │   (TTS)  │      │
│                  │ WebSocket│            │           │ WebSocket│      │
│                  └────┬─────┘            │           └────┬─────┘      │
│                       │                  │                │            │
│                       ▼                  │                │            │
│                  ┌──────────┐            │                │            │
│                  │  文字流   │            │                │            │
│                  │ "Vue的..."│            │                │            │
│                  └────┬─────┘            │                │            │
│                       │                  │                │            │
│                       ▼                  ▼                ▼            │
│                  ┌──────────────────────────────────────────┐          │
│                  │              LLM (通义千问)               │          │
│                  │                                          │          │
│                  │  输入: 候选人回答 + 面试上下文 + 提示词    │          │
│                  │  输出: AI 回复文本（流式）                │          │
│                  └──────────────────┬───────────────────────┘          │
│                                     │                                   │
│                                     ▼                                   │
│                              ┌─────────────┐                           │
│                              │  文本片段   │                           │
│                              │ "很好..."   │                           │
│                              └──────┬──────┘                           │
│                                     │                                   │
│                                     ▼                                   │
│                              ┌─────────────┐                           │
│                              │ CosyVoice   │                           │
│                              │ 流式TTS     │                           │
│                              └──────┬──────┘                           │
│                                     │                                   │
│                                     ▼                                   │
│                              ┌─────────────┐                           │
│                              │  音频片段   │                           │
│                              │  (PCM/WAV)  │                           │
│                              └──────┬──────┘                           │
│                                     │                                   │
│                                     ▼                                   │
│   ┌─────────┐      SSE/WS       ┌─────────────┐                       │
│   │  前端   │ ◀──────────────── │   后端      │                       │
│   │ 音频播放│   (Base64 Audio)  │  Gateway    │                       │
│   └─────────┘                   └─────────────┘                       │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 求助助手流程（冻结机制）

```
┌──────────────────────────────────────────────────────────────────┐
│  状态: INTERVIEWING (面试进行中)                                  │
│                                                                  │
│  候选人点击 [求助] 按钮                                           │
│      │                                                           │
│      ▼                                                           │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  状态切换: INTERVIEWING → FROZEN                            │ │
│  │                                                             │ │
│  │  1. 保存面试对话上下文                                       │ │
│  │  2. 暂停面试计时                                             │ │
│  │  3. 记录求助时间点（用于录音回放）                            │ │
│  │  4. 展示求助面板                                             │ │
│  └─────────────────────────────────────────────────────────────┘ │
│      │                                                           │
│      ▼                                                           │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  状态: FROZEN (冻结中)                                       │ │
│  │                                                             │ │
│  │  候选人选择快捷求助 或 自由提问                              │ │
│  │  AI助手 生成回复（文字 + 语音）                              │ │
│  │  (上下文: 当前问题 + 候选人简历 + 已输入内容)                 │ │
│  └─────────────────────────────────────────────────────────────┘ │
│      │                                                           │
│      │  候选人点击 [返回面试]                                    │
│      ▼                                                           │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  状态切换: FROZEN → INTERVIEWING                            │ │
│  │                                                             │ │
│  │  1. 恢复面试计时                                             │ │
│  │  2. 保存助手对话记录                                         │ │
│  │  3. 关闭求助面板                                             │ │
│  └─────────────────────────────────────────────────────────────┘ │
│      │                                                           │
│      ▼                                                           │
│  状态: INTERVIEWING (继续面试)                                   │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 三、语音服务接口抽象

> **设计目标**：ASR 和 TTS 抽象为接口，支持阿里云、OpenAI、Azure 等多种实现，通过配置切换。

### 3.1 架构分层

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           业务层 (VoiceDialogService)                    │
│                      通过 VoiceServiceFactory 获取服务实例               │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          接口层 (ASRService / TTSService)                │
│                                                                         │
│   ASRService                            TTSService                       │
│   ├── getProvider()                     ├── getProvider()                │
│   ├── streamRecognize()                 ├── streamSynthesize()           │
│   └── recognize()                       ├── streamSynthesizeBySentence() │
│                                          └── synthesize()                │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    │               │               │
                    ▼               ▼               ▼
┌─────────────────────┐ ┌─────────────────────┐ ┌─────────────────────┐
│   AliyunASRService  │ │  OpenAIASRService   │ │  AzureASRService    │
│   AliyunTTSService  │ │  OpenAITTSService   │ │  AzureTTSService    │
│   (Phase 1 已实现)   │ │   (Phase 2 预留)    │ │   (Phase 2 预留)    │
└─────────────────────┘ └─────────────────────┘ └─────────────────────┘
```

### 3.2 ASR 接口定义

```java
/**
 * ASR（语音识别）服务接口
 * 支持流式识别和同步识别
 */
public interface ASRService {

    /**
     * 获取服务提供商标识
     * @return "aliyun", "openai", "azure" 等
     */
    String getProvider();

    /**
     * 流式语音识别（实时场景）
     * @param audioStream 音频流（PCM 格式）
     * @param config 识别配置
     * @return 识别结果流
     */
    Flux<ASRResult> streamRecognize(Flux<byte[]> audioStream, ASRConfig config);

    /**
     * 同步语音识别（短音频）
     * @param audioData 音频数据
     * @param config 识别配置
     * @return 识别结果
     */
    ASRResult recognize(byte[] audioData, ASRConfig config);
}
```

### 3.3 TTS 接口定义

```java
/**
 * TTS（语音合成）服务接口
 * 支持流式合成和同步合成
 */
public interface TTSService {

    /**
     * 获取服务提供商标识
     * @return "aliyun", "openai", "azure" 等
     */
    String getProvider();

    /**
     * 流式语音合成（单句）
     * @param text 要合成的文本
     * @param config 合成配置
     * @return 音频片段流
     */
    Flux<byte[]> streamSynthesize(String text, TTSConfig config);

    /**
     * 分句流式合成（长文本，边生成边播放）
     * @param textStream 文本流（LLM 输出）
     * @param config 合成配置
     * @return 合成块流（包含文本和音频）
     */
    Flux<TTSChunk> streamSynthesizeBySentence(Flux<String> textStream, TTSConfig config);

    /**
     * 同步语音合成
     * @param text 要合成的文本
     * @param config 合成配置
     * @return 完整音频数据
     */
    byte[] synthesize(String text, TTSConfig config);
}
```

### 3.4 配置类与结果类

#### ASRConfig

```java
/**
 * ASR 配置
 */
@Data
@Builder
public class ASRConfig {
    private String format;           // 音频格式：pcm, wav, mp3
    private Integer sampleRate;      // 采样率：16000, 24000
    private Boolean enablePunctuation;   // 是否启用标点
    private Boolean enableITN;       // 是否启用逆文本正则化（数字转阿拉伯）
    private String language;         // 语言：zh, en
    private Boolean enableVAD;       // 是否启用语音活动检测
}
```

#### ASRResult

```java
/**
 * ASR 识别结果
 */
@Data
@Builder
public class ASRResult {
    private String text;             // 识别文本
    private Boolean isFinal;         // 是否最终结果（false=中间结果）
    private Double confidence;       // 置信度 0-1
    private Long startTime;          // 开始时间（毫秒）
    private Long endTime;            // 结束时间（毫秒）
}
```

#### TTSConfig

```java
/**
 * TTS 配置
 */
@Data
@Builder
public class TTSConfig {
    private String voice;            // 音色ID
    private String format;           // 输出格式：pcm, wav, mp3
    private Integer sampleRate;      // 采样率：16000, 24000
    private Double speechRate;       // 语速：0.5-2.0
    private Double volume;           // 音量：0-1
    private Double pitch;            // 音调：-1 到 1
}
```

#### TTSChunk

```java
/**
 * TTS 合成块（用于流式合成）
 */
@Data
@AllArgsConstructor
public class TTSChunk {
    private String text;             // 对应的文本片段
    private byte[] audio;            // 音频数据
    private Boolean isFinal;         // 是否最后一个块
}
```

### 3.5 VoiceServiceFactory

```java
/**
 * 语音服务工厂
 * 根据配置获取对应的 ASR/TTS 服务实例
 */
@Component
@RequiredArgsConstructor
public class VoiceServiceFactory {

    private final VoiceProperties voiceProperties;
    private final Map<String, ASRService> asrServices;
    private final Map<String, TTSService> ttsServices;

    /**
     * 获取当前配置的 ASR 服务
     */
    public ASRService getASRService() {
        String provider = voiceProperties.getProvider();
        return getASRService(provider);
    }

    /**
     * 获取指定提供商的 ASR 服务
     */
    public ASRService getASRService(String provider) {
        ASRService service = asrServices.get(provider);
        if (service == null) {
            throw new BusinessException("不支持的 ASR 服务提供商: " + provider);
        }
        return service;
    }

    /**
     * 获取当前配置的 TTS 服务
     */
    public TTSService getTTSService() {
        String provider = voiceProperties.getProvider();
        return getTTSService(provider);
    }

    /**
     * 获取指定提供商的 TTS 服务
     */
    public TTSService getTTSService(String provider) {
        TTSService service = ttsServices.get(provider);
        if (service == null) {
            throw new BusinessException("不支持的 TTS 服务提供商: " + provider);
        }
        return service;
    }
}
```

### 3.6 配置属性

```yaml
# application.yml
landit:
  voice:
    provider: aliyun  # aliyun, openai, azure（启动时确定，不支持运行时切换）

    aliyun:
      api-key: ${ALIYUN_API_KEY:}
      asr:
        model: paraformer-realtime-v2
        format: pcm
        sample-rate: 16000
        enable-punctuation: true
        enable-itn: true
      tts:
        model: cosyvoice-v2
        format: wav
        sample-rate: 16000
        interviewer-voice: longxiaochun_v2
        assistant-voice: zhimiao_emo_v2

    openai:
      api-key: ${OPENAI_API_KEY:}
      asr:
        model: whisper-1
      tts:
        model: tts-1
        default-voice: alloy
```

### 3.7 条件装配

使用 Spring Boot 条件注解实现自动切换：

```java
@Configuration
@ConditionalOnProperty(name = "landit.voice.provider", havingValue = "aliyun")
public class AliyunVoiceAutoConfiguration {

    @Bean
    public ASRService aliyunASRService(VoiceProperties properties) {
        return new AliyunASRService(properties.getAliyun());
    }

    @Bean
    public TTSService aliyunTTSService(VoiceProperties properties) {
        return new AliyunTTSService(properties.getAliyun());
    }
}

@Configuration
@ConditionalOnProperty(name = "landit.voice.provider", havingValue = "openai")
public class OpenAIVoiceAutoConfiguration {

    @Bean
    public ASRService openaiASRService(VoiceProperties properties) {
        return new OpenAIASRService(properties.getOpenai());
    }

    @Bean
    public TTSService openaiTTSService(VoiceProperties properties) {
        return new OpenAITTSService(properties.getOpenai());
    }
}
```

### 3.8 使用示例

```java
@Service
@RequiredArgsConstructor
public class VoiceDialogService {

    private final VoiceServiceFactory voiceServiceFactory;

    /**
     * 完整的语音对话流程
     */
    public Flux<VoiceDialogEvent> streamVoiceDialog(
        Flux<byte[]> audioInput,
        InterviewContext context
    ) {
        // 获取配置的服务实例
        ASRService asrService = voiceServiceFactory.getASRService();
        TTSService ttsService = voiceServiceFactory.getTTSService();

        // 构建配置
        ASRConfig asrConfig = ASRConfig.builder()
            .format("pcm")
            .sampleRate(16000)
            .enablePunctuation(true)
            .build();

        TTSConfig ttsConfig = TTSConfig.builder()
            .voice(context.isInterviewer() ? "longxiaochun_v2" : "zhimiao_emo_v2")
            .format("wav")
            .sampleRate(16000)
            .build();

        // ASR 识别
        Flux<ASRResult> asrStream = asrService.streamRecognize(audioInput, asrConfig);

        // ... LLM 处理 ...

        // TTS 合成
        Flux<TTSChunk> ttsStream = ttsService.streamSynthesizeBySentence(llmStream, ttsConfig);

        return buildEventStream(asrStream, ttsStream);
    }
}
```

---

## 四、求助助手功能设计

### 4.1 快捷求助按钮

| 按钮 | 功能 | 助手行为 |
|------|------|----------|
| **🎯 给我思路** | 获取答题框架 | 给出结构化的思考方向，不直接给答案 |
| **📖 解释概念** | 理解关键概念 | 解释问题中涉及的技术概念 |
| **✍️ 帮我润色** | 优化已输入内容 | 检查候选人已输入的草稿，给出优化建议 |
| **💬 自由提问** | 自由对话 | 展开文本输入框，自由提问 |

### 4.2 UI 布局

#### 面试进行中

```
┌──────────────────────────────────────────────────────────────────┐
│  ⏱️ 08:32    进度 3/10    🔊 语音模式    ❌ 结束面试               │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  面试官 🔊                                                  │  │
│  │  "请介绍一下Vue的响应式原理，以及Vue2和Vue3的区别？"         │  │
│  │  [━━━━━━━━━━●●●━━━━━━━━] 0:32/0:45                         │  │
│  │  [🔁 重听] [📝 显示文字]                                    │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ───────────────── 对话历史 ───────────────────────              │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  🎤 你的回答：                                              │  │
│  │  "Vue的响应式...嗯..."                                      │  │
│  │  ●●●●●● 正在聆听                                            │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
├──────────────────────────────────────────────────────────────────┤
│  💡 快捷求助 (剩余 4/5 次)                                        │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐            │
│  │🎯 给我思路│ │📖 解释概念│ │✍️ 帮我润色│ │💬 自由提问│            │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘            │
└──────────────────────────────────────────────────────────────────┘
```

#### 求助面板展开后

```
┌──────────────────────────────────────────────────────────────────┐
│  🧊 面试已暂停                              求助次数 2/5  [返回面试] │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  当前问题：请介绍一下Vue的响应式原理？                             │
│  ─────────────────────────────────────────────────────────────   │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  🤖 AI助手 🔊                                              │  │
│  │  ────────────────────────────────────────────────────────   │  │
│  │  "这个问题可以从三个角度来组织回答：                          │  │
│  │                                                             │  │
│  │  1. 数据劫持                                                │  │
│  │   - Vue2 使用 Object.defineProperty                         │  │
│  │   - Vue3 改用 Proxy，解决了数组监听问题                      │  │
│  │                                                             │  │
│  │  2. 依赖收集                                                │  │
│  │   - Dep 收集依赖，Watcher 触发更新                          │  │
│  │                                                             │  │
│  │  3. 派发更新                                                │  │
│  │   - 数据变化时通知所有 Watcher 执行更新"                     │  │
│  │                                                             │  │
│  │  [━━━━━━━━━━●●●━━━━━━━━] 🔊 播放中  [⏸ 暂停] [🔇 静音]     │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ───────────────── 继续求助？ ─────────────────────              │
│                                                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐            │
│  │🎯 继续要思路│ │📖 深入讲解│ │✍️ 模拟回答│ │  返回面试  │            │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘            │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  💬 或者直接输入问题：                    [🎤 语音] [发送]   │  │
│  │  ┌──────────────────────────────────────────────────────┐  │  │
│  │  │ 依赖收集具体是怎么实现的？                             │  │  │
│  │  └──────────────────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### 4.3 求助模式配置

| 模式 | 助手限制 | 适用场景 |
|------|----------|----------|
| **练习模式** | 无限制求助 | 学习阶段，熟悉面试流程 |
| **模拟模式** | 每场限3-5次 | 真实模拟，测试自身水平 |
| **实战模式** | 禁用助手 | 真实面试前的最终测试 |

---

## 五、数据库设计

### 5.1 表结构变更

#### 扩展 t_interview_session

```sql
-- 新增字段
ALTER TABLE t_interview_session ADD COLUMN voice_mode VARCHAR(20) DEFAULT 'text';
-- voice_mode: text(纯文本), half-voice(半语音), full-voice(全语音)

ALTER TABLE t_interview_session ADD COLUMN assist_count INT DEFAULT 0;
ALTER TABLE t_interview_session ADD COLUMN assist_limit INT DEFAULT 5;

ALTER TABLE t_interview_session ADD COLUMN status VARCHAR(20) DEFAULT 'in_progress';
-- status: in_progress(进行中), frozen(冻结), completed(已完成)
```

#### 新增 t_assistant_conversation（助手对话记录表）

```sql
CREATE TABLE t_assistant_conversation (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    freeze_index INT NOT NULL,           -- 冻结时的问题序号
    assist_type VARCHAR(30) NOT NULL,    -- 求助类型：GIVE_HINTS, EXPLAIN_CONCEPT, POLISH_ANSWER, FREE_QUESTION
    user_question TEXT,                  -- 用户问题（自由提问时）
    assistant_response TEXT NOT NULL,    -- 助手回复
    audio_url VARCHAR(500),              -- 音频存储路径
    duration_ms INT,                     -- 音频时长
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES t_interview_session(id)
);
```

#### 新增 t_interview_recording（面试录音片段表）

```sql
CREATE TABLE t_interview_recording (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    segment_index INT NOT NULL,          -- 片段序号
    role VARCHAR(20) NOT NULL,           -- interviewer / candidate / assistant
    content TEXT,                        -- 对应的文字内容
    audio_path VARCHAR(500) NOT NULL,    -- 音频文件路径
    duration_ms INT NOT NULL,            -- 时长(毫秒)
    start_time DATETIME NOT NULL,        -- 开始时间
    end_time DATETIME NOT NULL,          -- 结束时间
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES t_interview_session(id)
);
```

#### 新增 t_recording_index（录音索引表）

```sql
CREATE TABLE t_recording_index (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    total_duration_ms INT DEFAULT 0,     -- 总时长
    merged_audio_path VARCHAR(500),      -- 合并后的完整音频
    transcript TEXT,                     -- 完整文字记录(JSON)
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES t_interview_session(id)
);
```

---

## 六、后端接口设计

### 6.1 REST API

#### 求助助手（SSE 流式）

```java
/**
 * 请求助手帮助（流式响应）
 * GET /landit/interviews/sessions/{sessionId}/assist/stream
 * Accept: text/event-stream
 */
public interface AssistRequest {
    AssistType type;              // 求助类型
    String question;              // 自由提问时的内容（可选）
    String candidateDraft;        // 候选人已输入的草稿（润色时使用）
}

// SSE 事件格式
public interface AssistSSEEvent {
    String type;                  // text / audio / done / error
    Object data;
}

// 文本事件
public interface TextEventData {
    String content;               // 文本片段
    boolean isDelta;              // true=增量，false=完整
}

// 音频事件
public interface AudioEventData {
    String audio;                 // Base64 编码的音频片段
    String format;                // pcm / wav
    int sampleRate;               // 采样率
}

// 完成事件
public interface DoneEventData {
    int assistRemaining;          // 剩余求助次数
    int totalDurationMs;          // 总时长
}

// 求助类型枚举
public enum AssistType {
    GIVE_HINTS,        // 给我思路
    EXPLAIN_CONCEPT,   // 解释概念
    POLISH_ANSWER,     // 帮我润色
    FREE_QUESTION      // 自由提问
}
```

**SSE 响应示例：**

```
event: text
data: {"content": "这个问题可以从", "isDelta": true}

event: text
data: {"content": "三个角度来分析：", "isDelta": true}

event: audio
data: {"audio": "UklGRiQAAABXQVZFZm10IBAAAAABAAEA...", "format": "pcm", "sampleRate": 16000}

event: text
data: {"content": "1. 数据劫持", "isDelta": true}

event: audio
data: {"audio": "UklGRiQAAABXQVZFZm10IBAAAAABAAEA...", "format": "pcm", "sampleRate": 16000}

event: done
data: {"assistRemaining": 4, "totalDurationMs": 5200}
```

#### 录音回放

```java
/**
 * 获取录音回放信息
 * GET /landit/interviews/sessions/{sessionId}/recording
 */
public interface RecordingInfo {
    String sessionId;
    int totalDurationMs;
    String mergedAudioUrl;
    List<RecordingSegment> segments;
    List<TranscriptEntry> transcript;
}

/**
 * 下载完整录音
 * GET /landit/interviews/sessions/{sessionId}/recording/audio
 */

/**
 * 获取完整文字记录
 * GET /landit/interviews/sessions/{sessionId}/recording/transcript
 */
```

### 6.2 WebSocket 端点（实时语音）

```java
/**
 * 实时语音面试 WebSocket
 * GET /landit/ws/interview/voice/{sessionId}
 */

// 请求消息格式
public interface VoiceRequest {
    String type;                  // audio / control
    Object data;
}

// audio 类型
public interface AudioData {
    String audio;                 // Base64 音频数据
    String format;                // pcm/wav/mp3
    int sampleRate;               // 采样率
}

// control 类型
public interface ControlData {
    String action;                // start / stop / end
}

// 响应消息格式
public interface VoiceResponse {
    String type;                  // transcript / audio / state / error
    Object data;
}

// transcript 类型
public interface TranscriptData {
    String text;
    boolean isFinal;
    String role;                  // interviewer / candidate
}

// audio 类型
public interface AudioData {
    String audio;                 // Base64
    String format;
}

// state 类型
public interface StateData {
    String state;                 // interviewing / frozen / completed
    int currentQuestion;
    int totalQuestions;
    int assistRemaining;
    int elapsedTime;              // 已用时间(秒)
}
```

---

## 七、前端类型定义

```typescript
// types/interview-voice.ts

/** 语音模式 */
export type VoiceMode = 'text' | 'half-voice' | 'full-voice'

/** 会话状态 */
export type SessionState = 'interviewing' | 'frozen' | 'completed'

/** 求助类型 */
export type AssistType = 'give_hints' | 'explain_concept' | 'polish_answer' | 'free_question'

/** 语音设置 */
export interface VoiceSettings {
  mode: VoiceMode
  inputFormat: 'pcm' | 'wav'
  sampleRate: 16000 | 24000
  interviewerVoice: string      // 面试官音色ID
  assistantVoice: string        // 助手音色ID
  speechRate: number            // 语速 0.5-2.0
  vadEnabled: boolean           // 语音活动检测
  vadSilenceMs: number          // 静音检测阈值
}

/** 求助请求 */
export interface AssistRequest {
  type: AssistType
  question?: string
  candidateDraft?: string
}

/** SSE 事件类型 */
export type SSEEventType = 'text' | 'audio' | 'done' | 'error'

/** SSE 事件基础接口 */
export interface AssistSSEEvent {
  type: SSEEventType
  data: TextEventData | AudioEventData | DoneEventData | ErrorEventData
}

/** 文本事件数据 */
export interface TextEventData {
  content: string
  isDelta: boolean              // true=增量文本，false=完整文本
}

/** 音频事件数据 */
export interface AudioEventData {
  audio: string                 // Base64 编码
  format: 'pcm' | 'wav'
  sampleRate: number
}

/** 完成事件数据 */
export interface DoneEventData {
  assistRemaining: number
  totalDurationMs: number
}

/** 错误事件数据 */
export interface ErrorEventData {
  code: string
  message: string
}

/** 面试会话状态 */
export interface InterviewSessionState {
  sessionId: string
  status: SessionState
  currentQuestion: number
  totalQuestions: number
  assistCount: number
  assistLimit: number
  elapsedTime: number
}

/** WebSocket 消息 */
export interface WSMessage {
  type: 'transcript' | 'audio' | 'state' | 'error'
  data: TranscriptData | AudioData | StateData | ErrorData
}

export interface TranscriptData {
  text: string
  isFinal: boolean
  role: 'interviewer' | 'candidate'
}

export interface AudioData {
  audio: string           // Base64
  format: string
}

export interface StateData {
  state: SessionState
  currentQuestion: number
  totalQuestions: number
  assistRemaining: number
  elapsedTime: number
}

/** 录音片段 */
export interface RecordingSegment {
  id: string
  segmentIndex: number
  role: 'interviewer' | 'candidate' | 'assistant'
  content: string
  audioUrl: string
  durationMs: number
  startTime: string
}

/** 录音回放信息 */
export interface RecordingInfo {
  sessionId: string
  totalDurationMs: number
  mergedAudioUrl: string
  segments: RecordingSegment[]
  transcript: TranscriptEntry[]
}

export interface TranscriptEntry {
  role: string
  content: string
  timestamp: number
}
```

---

## 八、阿里云百炼集成

### 8.1 依赖配置

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter</artifactId>
</dependency>
```

### 8.2 配置文件

```yaml
# application.yml
spring:
  ai:
    # LLM 配置（复用现有 OpenAI 协议配置，或切换为阿里云）
    openai:
      api-key: ${OPENAI_API_KEY:}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: ${AI_MODEL:gpt-4o}
          temperature: 0.7

    # 阿里云语音服务配置
    alibaba:
      api-key: ${ALIBABA_API_KEY}
      voice:
        # ASR 配置
        asr:
          model: paraformer-realtime-v2    # 实时语音识别模型
          format: pcm
          sample-rate: 16000
          enable-punctuation: true
          enable-inverse-text-normalization: true

        # TTS 配置
        tts:
          model: cosyvoice-v2              # 流式语音合成模型
          format: wav
          sample-rate: 16000

        # 音色配置
        voices:
          interviewer: longxiaochun_v2     # 面试官音色
          assistant: zhimiao_emo_v2        # 助手音色（更亲切）
```

### 8.3 ASR 服务（Paraformer）

#### 实时语音识别客户端

```java
@Service
@RequiredArgsConstructor
public class ParaformerASRClient {

    @Value("${spring.ai.alibaba.api-key}")
    private String apiKey;

    /**
     * 建立实时语音识别连接
     * 通过 WebSocket 接收音频流，实时返回识别文字
     */
    public Flux<ASRResult> streamRecognize(Flux<byte[]> audioStream, ASRConfig config) {
        return Flux.create(emitter -> {
            // 构建 WebSocket URL
            String wsUrl = buildASRWebSocketUrl(config);

            WebSocketClient client = new StandardWebSocketClient();
            AtomicReference<WebSocketSession> sessionRef = new AtomicReference<>();

            client.doHandshake(new WebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession session) {
                    sessionRef.set(session);
                    emitter.onDispose(() -> {
                        try {
                            session.close();
                        } catch (Exception ignored) {}
                    });
                }

                @Override
                public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
                    JsonNode response = parseResponse(message);

                    ASRResult result = ASRResult.builder()
                        .text(response.path("text").asText())
                        .isFinal(response.path("is_final").asBoolean(false))
                        .confidence(response.path("confidence").asDouble(0.0))
                        .build();

                    emitter.next(result);

                    if (result.isFinal()) {
                        emitter.complete();
                    }
                }
            }, wsUrl);

            // 发送音频流
            audioStream.subscribe(audioData -> {
                WebSocketSession session = sessionRef.get();
                if (session != null && session.isOpen()) {
                    session.sendMessage(new BinaryMessage(audioData));
                }
            });
        });
    }

    private String buildASRWebSocketUrl(ASRConfig config) {
        return UriComponentsBuilder.fromHttpUrl("wss://dashscope.aliyuncs.com/api-ws/v1/inference")
            .path("/asr/paraformer-realtime-v2")
            .queryParam("format", config.getFormat())
            .queryParam("sample_rate", config.getSampleRate())
            .queryParam("enable_punctuation_prediction", config.isEnablePunctuation())
            .queryParam("enable_inverse_text_normalization", config.isEnableITN())
            .build()
            .toUriString();
    }
}

@Data
@Builder
public class ASRResult {
    private String text;          // 识别文字
    private boolean isFinal;      // 是否最终结果
    private double confidence;    // 置信度
}
```

### 8.4 TTS 服务（CosyVoice）

#### 流式语音合成客户端

```java
@Service
@RequiredArgsConstructor
public class CosyVoiceTTSClient {

    @Value("${spring.ai.alibaba.api-key}")
    private String apiKey;

    /**
     * 流式语音合成
     * 输入文本，流式输出音频片段
     */
    public Flux<byte[]> streamSynthesize(String text, TTSConfig config) {
        return Flux.create(emitter -> {
            String wsUrl = buildTTSWebSocketUrl(config);

            WebSocketClient client = new StandardWebSocketClient();

            client.doHandshake(new WebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession session) {
                    // 发送合成请求
                    JsonObject request = new JsonObject();
                    request.addProperty("text", text);
                    request.addProperty("voice", config.getVoice());
                    request.addProperty("format", config.getFormat());
                    request.addProperty("sample_rate", config.getSampleRate());

                    try {
                        session.sendMessage(new TextMessage(request.toString()));
                    } catch (Exception e) {
                        emitter.error(e);
                    }
                }

                @Override
                public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
                    JsonNode response = parseResponse(message);

                    if (response.has("audio")) {
                        // Base64 解码音频
                        String audioBase64 = response.get("audio").asText();
                        byte[] audioData = Base64.getDecoder().decode(audioBase64);
                        emitter.next(audioData);
                    }

                    if (response.path("done").asBoolean(false)) {
                        emitter.complete();
                    }
                }
            }, wsUrl);
        });
    }

    /**
     * 分句流式合成（用于长文本）
     * 将文本按句子分割，逐句合成，实现边生成边播放
     */
    public Flux<TTSChunk> streamSynthesizeBySentence(Flux<String> textStream, TTSConfig config) {
        StringBuilder sentenceBuffer = new StringBuilder();

        return textStream.flatMap(delta -> {
            sentenceBuffer.append(delta);

            // 检测句子结束
            if (isSentenceEnd(delta)) {
                String sentence = sentenceBuffer.toString();
                sentenceBuffer.setLength(0);

                // 流式合成这个句子
                return streamSynthesize(sentence, config)
                    .map(audio -> new TTSChunk(sentence, audio));
            }

            return Flux.empty();
        });
    }

    private boolean isSentenceEnd(String text) {
        return text.matches(".*[。！？.!?]$");
    }

    private String buildTTSWebSocketUrl(TTSConfig config) {
        return UriComponentsBuilder.fromHttpUrl("wss://dashscope.aliyuncs.com/api-ws/v1/inference")
            .path("/tts/cosyvoice-v2")
            .build()
            .toUriString();
    }
}

@Data
@AllArgsConstructor
public class TTSChunk {
    private String text;    // 对应的文本
    private byte[] audio;   // 音频数据
}
```

### 8.5 LLM 服务（通义千问）

可以使用现有的 OpenAI 协议配置，或切换为阿里云通义千问：

```java
@Service
@RequiredArgsConstructor
public class StreamLLMService {

    private final ChatClient.Builder chatClientBuilder;

    /**
     * 流式生成回复
     */
    public Flux<String> streamGenerate(String systemPrompt, String userPrompt) {
        return chatClientBuilder.build()
            .prompt()
            .system(systemPrompt)
            .user(userPrompt)
            .stream()
            .content();
    }
}
```

### 8.6 组合服务（ASR + LLM + TTS）

```java
@Service
@RequiredArgsConstructor
public class VoiceDialogService {

    private final ParaformerASRClient asrClient;
    private final StreamLLMService llmService;
    private final CosyVoiceTTSClient ttsClient;

    /**
     * 完整的语音对话流程
     * 1. ASR: 音频 -> 文字
     * 2. LLM: 文字 -> 回复文字
     * 3. TTS: 回复文字 -> 音频
     */
    public Flux<VoiceDialogEvent> streamVoiceDialog(
        Flux<byte[]> audioInput,
        String systemPrompt,
        InterviewContext context
    ) {
        // 文本缓冲区（用于记录完整的候选人回答）
        StringBuilder candidateText = new StringBuilder();

        // Step 1: ASR 识别
        Flux<ASRResult> asrStream = asrClient.streamRecognize(audioInput, defaultASRConfig());

        // Step 2 & 3: 当 ASR 完成后，调用 LLM + TTS
        return asrStream
            // 收集候选人说的所有话
            .doOnNext(asr -> candidateText.append(asr.getText()))
            // 转发 ASR 中间结果
            .map(asr -> VoiceDialogEvent.transcript(asr.getText(), asr.isFinal()))
            // ASR 完成后，开始 LLM + TTS
            .concatWith(
                asrStream
                    .filter(ASRResult::isFinal)
                    .take(1)
                    .flatMapMany(finalAsr -> {
                        // 构建 LLM 输入
                        String userPrompt = buildUserPrompt(candidateText.toString(), context);

                        // LLM 流式生成
                        Flux<String> llmStream = llmService.streamGenerate(systemPrompt, userPrompt);

                        // 并行处理：返回文本 + TTS 合成
                        return processLLMStream(llmStream);
                    })
            );
    }

    /**
     * 处理 LLM 流式输出
     * 同时返回文本事件和音频事件
     */
    private Flux<VoiceDialogEvent> processLLMStream(Flux<String> llmStream) {
        StringBuilder textBuffer = new StringBuilder();

        return llmStream.flatMap(delta -> {
            textBuffer.append(delta);

            // 检测句子结束，触发 TTS
            if (isSentenceEnd(delta)) {
                String sentence = textBuffer.toString();
                textBuffer.setLength(0);

                // 返回文本事件
                Mono<VoiceDialogEvent> textEvent = Mono.just(
                    VoiceDialogEvent.text(sentence)
                );

                // TTS 合成
                Flux<VoiceDialogEvent> audioEvents = ttsClient.streamSynthesize(sentence, defaultTTSConfig())
                    .map(audio -> VoiceDialogEvent.audio(audio));

                return Flux.concat(textEvent, audioEvents);
            }

            // 非句子结束，只返回增量文本
            return Flux.just(VoiceDialogEvent.textDelta(delta));
        });
    }

    private boolean isSentenceEnd(String text) {
        return text.matches(".*[。！？.!?]$");
    }
}

// 事件类型
@Data
@AllArgsConstructor
public class VoiceDialogEvent {
    private EventType type;
    private Object data;

    public enum EventType {
        TRANSCRIPT,    // ASR 识别结果
        TEXT_DELTA,    // LLM 增量文本
        TEXT,          // 完整句子文本
        AUDIO          // 音频片段
    }

    public static VoiceDialogEvent transcript(String text, boolean isFinal) {
        return new VoiceDialogEvent(EventType.TRANSCRIPT, Map.of("text", text, "isFinal", isFinal));
    }

    public static VoiceDialogEvent textDelta(String delta) {
        return new VoiceDialogEvent(EventType.TEXT_DELTA, Map.of("content", delta));
    }

    public static VoiceDialogEvent text(String text) {
        return new VoiceDialogEvent(EventType.TEXT, Map.of("content", text));
    }

    public static VoiceDialogEvent audio(byte[] audio) {
        return new VoiceDialogEvent(EventType.AUDIO, Map.of("audio", Base64.getEncoder().encodeToString(audio)));
    }
}
```

### 8.7 API 参考

| API | 说明 | 文档链接 |
|-----|------|----------|
| Paraformer | 实时语音识别（WebSocket） | https://help.aliyun.com/zh/model-studio/websocket-for-paraformer-real-time-service |
| CosyVoice | 流式语音合成 | https://help.aliyun.com/zh/model-studio/cosyvoice-clone-design-api |
| Qwen-TTS | 实时语音合成 | https://help.aliyun.com/zh/model-studio/interactive-process-of-qwen-tts-realtime-synthesis |
| 通义千问 | 大语言模型 | https://help.aliyun.com/zh/model-studio/developer-reference/api-details |

---

## 九、前端流式播放实现

### 9.1 SSE 事件处理

```typescript
// composables/useStreamAssist.ts

export function useStreamAssist(sessionId: string) {
  const textContent = ref('')
  const isStreaming = ref(false)
  const assistRemaining = ref(5)

  // 音频播放器
  let audioContext: AudioContext
  let audioQueue: AudioBuffer[] = []
  let isPlaying = false

  /**
   * 请求流式求助
   */
  async function requestAssist(request: AssistRequest) {
    isStreaming.value = true
    textContent.value = ''

    // 初始化音频上下文
    audioContext = new AudioContext({ sampleRate: 16000 })
    audioQueue = []
    isPlaying = false

    // 构建 SSE URL
    const params = new URLSearchParams({
      type: request.type,
      question: request.question || '',
      candidateDraft: request.candidateDraft || ''
    })
    const url = `/landit/interviews/sessions/${sessionId}/assist/stream?${params}`

    const eventSource = new EventSource(url)

    eventSource.addEventListener('text', (event) => {
      const data: TextEventData = JSON.parse(event.data)
      if (data.isDelta) {
        textContent.value += data.content
      } else {
        textContent.value = data.content
      }
    })

    eventSource.addEventListener('audio', async (event) => {
      const data: AudioEventData = JSON.parse(event.data)
      await playAudioChunk(data.audio, data.format, data.sampleRate)
    })

    eventSource.addEventListener('done', (event) => {
      const data: DoneEventData = JSON.parse(event.data)
      assistRemaining.value = data.assistRemaining
      isStreaming.value = false
      eventSource.close()
    })

    eventSource.addEventListener('error', (event) => {
      console.error('SSE Error:', event)
      isStreaming.value = false
      eventSource.close()
    })
  }

  /**
   * 播放音频片段（流式）
   */
  async function playAudioChunk(base64Audio: string, format: string, sampleRate: number) {
    try {
      // Base64 解码为 ArrayBuffer
      const binaryString = atob(base64Audio)
      const bytes = new Uint8Array(binaryString.length)
      for (let i = 0; i < binaryString.length; i++) {
        bytes[i] = binaryString.charCodeAt(i)
      }

      // 解码音频数据
      const audioBuffer = await audioContext.decodeAudioData(bytes.buffer)

      // 加入播放队列
      audioQueue.push(audioBuffer)

      // 如果没有在播放，开始播放
      if (!isPlaying) {
        playNextChunk()
      }
    } catch (error) {
      console.error('Audio decode error:', error)
    }
  }

  /**
   * 播放下一个音频片段
   */
  function playNextChunk() {
    if (audioQueue.length === 0) {
      isPlaying = false
      return
    }

    isPlaying = true
    const buffer = audioQueue.shift()!
    const source = audioContext.createBufferSource()
    source.buffer = buffer
    source.connect(audioContext.destination)

    source.onended = () => {
      playNextChunk()  // 自动播放下一个片段
    }

    source.start()
  }

  /**
   * 停止播放
   */
  function stopStreaming() {
    audioQueue = []
    isPlaying = false
    audioContext?.close()
  }

  return {
    textContent,
    isStreaming,
    assistRemaining,
    requestAssist,
    stopStreaming
  }
}
```

### 9.2 实时语音录制与发送

```typescript
// composables/useAudioRecorder.ts

export function useAudioRecorder(sessionId: string) {
  const isRecording = ref(false)
  const transcript = ref('')

  let mediaStream: MediaStream
  let audioContext: AudioContext
  let processor: ScriptProcessorNode
  let ws: WebSocket

  /**
   * 开始录音并发送到 WebSocket
   */
  async function startRecording() {
    // 获取麦克风权限
    mediaStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        sampleRate: 16000,
        channelCount: 1,
        echoCancellation: true,
        noiseSuppression: true
      }
    })

    // 初始化音频上下文
    audioContext = new AudioContext({ sampleRate: 16000 })
    const source = audioContext.createMediaStreamSource(mediaStream)

    // 创建音频处理器
    processor = audioContext.createScriptProcessor(4096, 1, 1)
    source.connect(processor)
    processor.connect(audioContext.destination)

    // 建立 WebSocket 连接
    ws = new WebSocket(`/landit/ws/interview/voice/${sessionId}`)

    ws.onopen = () => {
      // 发送开始控制消息
      ws.send(JSON.stringify({ type: 'control', data: { action: 'start' } }))
    }

    ws.onmessage = (event) => {
      const msg: WSMessage = JSON.parse(event.data)

      if (msg.type === 'transcript') {
        const data = msg.data as TranscriptData
        if (data.isFinal) {
          transcript.value = data.text
        } else {
          // 实时显示识别中的文字
          transcript.value = data.text + '...'
        }
      }

      if (msg.type === 'audio') {
        // 播放 AI 返回的音频
        playAudioChunk(msg.data as AudioData)
      }
    }

    // 音频数据回调
    processor.onaudioprocess = (event) => {
      if (ws.readyState === WebSocket.OPEN) {
        const inputData = event.inputBuffer.getChannelData(0)
        // 转换为 16bit PCM
        const pcmData = float32ToPcm(inputData)
        // 发送到后端
        ws.send(JSON.stringify({
          type: 'audio',
          data: {
            audio: arrayBufferToBase64(pcmData),
            format: 'pcm',
            sampleRate: 16000
          }
        }))
      }
    }

    isRecording.value = true
  }

  /**
   * 停止录音
   */
  function stopRecording() {
    processor?.disconnect()
    mediaStream?.getTracks().forEach(t => t.stop())
    audioContext?.close()

    if (ws?.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'control', data: { action: 'stop' } }))
    }

    isRecording.value = false
  }

  return {
    isRecording,
    transcript,
    startRecording,
    stopRecording
  }
}

// 工具函数
function float32ToPcm(float32Array: Float32Array): ArrayBuffer {
  const buffer = new ArrayBuffer(float32Array.length * 2)
  const view = new DataView(buffer)
  for (let i = 0; i < float32Array.length; i++) {
    const s = Math.max(-1, Math.min(1, float32Array[i]))
    view.setInt16(i * 2, s < 0 ? s * 0x8000 : s * 0x7FFF, true)
  }
  return buffer
}

function arrayBufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer)
  let binary = ''
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i])
  }
  return btoa(binary)
}
```

---

## 十、录音回放功能

### 10.1 录音存储策略

```
backend/data/recordings/
├── {sessionId}/
│   ├── segments/
│   │   ├── 001_interviewer.wav
│   │   ├── 002_candidate.wav
│   │   ├── 003_assistant.wav    # 求助时的对话
│   │   └── ...
│   ├── merged.mp3               # 合并后的完整录音
│   └── metadata.json            # 元数据（时间戳、文字记录）
```

### 10.2 录音合并流程

```java
@Service
public class RecordingMergeService {

    /**
     * 面试结束后合并录音
     */
    public RecordingInfo mergeRecordings(String sessionId) {
        List<RecordingSegment> segments = recordingMapper.findBySessionId(sessionId);

        // 1. 合并音频文件
        Path mergedFile = mergeAudioFiles(segments);

        // 2. 生成文字记录
        List<TranscriptEntry> transcript = buildTranscript(segments);

        // 3. 计算总时长
        int totalDuration = segments.stream()
            .mapToInt(RecordingSegment::getDurationMs)
            .sum();

        // 4. 保存索引
        RecordingIndex index = new RecordingIndex();
        index.setSessionId(sessionId);
        index.setTotalDurationMs(totalDuration);
        index.setMergedAudioPath(mergedFile.toString());
        index.setTranscript(JsonUtils.toJson(transcript));
        recordingIndexMapper.insert(index);

        return buildRecordingInfo(index, segments);
    }
}
```

---

## 十一、项目结构变更

### 11.1 后端

```
backend/src/main/java/com/landit/
├── interview/
│   ├── voice/                          # 新增：语音模块
│   │   ├── controller/
│   │   │   ├── InterviewVoiceController.java      # WebSocket 语音对话
│   │   │   └── AssistantController.java           # SSE 流式求助
│   │   ├── gateway/
│   │   │   └── InterviewVoiceGateway.java         # 语音会话网关
│   │   ├── service/
│   │   │   ├── AliyunStreamVoiceService.java      # 阿里云流式语音服务
│   │   │   ├── StreamAssistService.java           # 流式求助服务
│   │   │   └── RecordingService.java              # 录音存储服务
│   │   ├── handler/
│   │   │   ├── InterviewerAgentHandler.java       # 面试官 Agent
│   │   │   └── AssistantAgentHandler.java         # AI助手 Agent
│   │   ├── dto/
│   │   │   ├── VoiceRequest.java
│   │   │   ├── VoiceResponse.java
│   │   │   ├── AssistRequest.java
│   │   │   ├── AssistSSEEvent.java                # SSE 事件 DTO
│   │   │   └── VoiceChunk.java                    # 流式音频块
│   │   ├── entity/
│   │   │   ├── AssistantConversation.java
│   │   │   ├── InterviewRecording.java
│   │   │   └── RecordingIndex.java
│   │   ├── enums/
│   │   │   ├── VoiceMode.java
│   │   │   ├── SessionState.java
│   │   │   ├── AssistType.java
│   │   │   └── ChunkType.java                     # 流式块类型
│   │   └── mapper/
│   │       ├── AssistantConversationMapper.java
│   │       ├── InterviewRecordingMapper.java
│   │       └── RecordingIndexMapper.java
│   └── ...existing files
```

### 11.2 前端

```
frontend/src/
├── views/
│   └── interview/
│       ├── InterviewSession.vue        # 修改：支持语音模式
│       └── InterviewRecording.vue      # 新增：录音回放页面
├── components/
│   └── interview/
│       ├── VoiceControls.vue           # 新增：语音控制组件
│       ├── TranscriptDisplay.vue       # 新增：文字记录显示
│       ├── AssistantPanel.vue          # 新增：助手对话面板
│       ├── QuickAssistButtons.vue      # 新增：快捷求助按钮
│       ├── StreamingAudioPlayer.vue    # 新增：流式音频播放器
│       └── RecordingPlayer.vue         # 新增：录音播放器
├── composables/
│   ├── useInterviewVoice.ts            # 新增：语音面试逻辑
│   ├── useStreamAssist.ts              # 新增：SSE 流式求助
│   ├── useAudioRecorder.ts             # 新增：音频录制
│   └── useStreamingAudio.ts            # 新增：流式音频播放
├── api/
│   └── interview-voice.ts              # 新增：语音相关API
└── types/
    └── interview-voice.ts              # 新增：语音类型定义
```

---

## 十二、开发排期

| 阶段 | 内容 | 预估工作量 |
|------|------|-----------|
| **Phase 1** | 基础流式语音能力 | 4-5 天 |
| | - 阿里云流式语音服务集成 | |
| | - WebSocket 连接管理 | |
| | - 前端音频采集 | |
| | - **流式音频播放（关键）** | |
| **Phase 2** | 多角色对话 + 快捷求助 | 3-4 天 |
| | - 面试官 Agent（流式语音） | |
| | - AI助手 Agent（流式语音） | |
| | - SSE 流式求助接口 | |
| | - 冻结/恢复机制 | |
| | - 快捷求助按钮 | |
| **Phase 3** | 录音回放 | 2 天 |
| | - 录音存储 | |
| | - 录音合并 | |
| | - 回放界面 | |
| **Phase 4** | 优化完善 | 2-3 天 |
| | - 语音模式切换 | |
| | - 弱网处理 | |
| | - 首字节延迟优化 | |
| | - UI 优化 | |

---

## 十三、待确认事项

- [x] 语音服务提供商：阿里云百炼
- [x] 实时语音对话：需要
- [x] **流式语音合成：需要（边生成边播放）**
- [x] 录音回放：需要
- [x] 语音波形可视化：不需要
- [x] 求助触发方式：按钮点击（快捷功能）

---

## 变更记录

| 日期 | 版本 | 变更内容 |
|------|------|----------|
| 2026-03-25 | 1.4.0 | 新增「语音服务接口抽象」章节，支持多服务商切换（阿里云/OpenAI/Azure），更新架构图体现接口层 |
| 2026-03-12 | 1.3.0 | 改为 ASR + LLM + TTS 分开调用架构，删除端到端方案，更新为 Paraformer + 通义千问 + CosyVoice |
| 2026-03-12 | 1.2.0 | 改为 ASR + LLM + TTS 分开调用架构，删除端到端方案，新增完整的数据流向图 |
| 2026-03-12 | 1.1.0 | 新增流式语音合成方案、SSE 接口设计、前端流式播放实现 |
| 2026-03-12 | 1.0.0 | 初始版本：完整技术方案 |
