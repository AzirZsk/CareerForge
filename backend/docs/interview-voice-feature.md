# 虚拟面试语音交互功能 - 技术方案

> 版本：1.1.0
> 创建日期：2026-03-12
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
                                │ WebSocket
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
                     │ WebSocket
                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       阿里云百炼语音服务                                  │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │            EndToEndRealTimeDialog API                            │    │
│  │   (实时语音识别 + LLM 推理 + 实时语音合成)                         │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                         │
│  备选方案（分开调用）：                                                    │
│  ┌──────────────────┐    ┌──────────────────┐                          │
│  │ Paraformer (ASR) │    │  CosyVoice (TTS) │                          │
│  │  实时语音识别     │    │   实时语音合成    │                          │
│  └──────────────────┘    └──────────────────┘                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 核心流程

#### 流式语音对话流程（关键）

```
候选人说话         阿里云百炼              后端处理              前端展示
    │                 │                     │                    │
    │  ── 音频流 ───▶ │                     │                    │
    │                 │  ── 文字(实时) ───▶ │                    │
    │                 │                     │  ── 显示候选人说话 ─▶│
    │                 │                     │                    │
    │                 │  ◀─ 提示词+上下文 ──│                    │
    │                 │                     │                    │
    │                 │                     │                    │
    │                 │  ═══ 流式响应开始 ═══                    │
    │                 │                     │                    │
    │                 │  ── 文本片段1 ────▶ │  ── SSE推送 ──────▶│ 显示文字
    │                 │  ── 音频片段1 ────▶ │  ── SSE推送 ──────▶│ 🎵 播放
    │                 │                     │                    │
    │                 │  ── 文本片段2 ────▶ │  ── SSE推送 ──────▶│ 显示文字
    │                 │  ── 音频片段2 ────▶ │  ── SSE推送 ──────▶│ 🎵 播放
    │                 │                     │                    │
    │                 │  ── 文本片段3 ────▶ │  ── SSE推送 ──────▶│ 显示文字
    │                 │  ── 音频片段3 ────▶ │  ── SSE推送 ──────▶│ 🎵 播放
    │                 │                     │                    │
    │                 │  ═══ 流式响应结束 ═══                    │
    │                 │                     │                    │
    │                                                              │
    │  用户几乎无感知延迟，边生成边播放                              │
    │                                                              │
```

#### 流式求助助手流程

```
点击求助按钮         后端处理              阿里云百炼            前端展示
    │                  │                     │                    │
    │  ── POST请求 ──▶ │                     │                    │
    │                  │  ── 建立SSE连接 ──▶ │                    │
    │                  │                     │                    │
    │                  │  ◀─ 提示词+上下文 ──│                    │
    │                  │                     │                    │
    │                  │                     │  ═══ 流式生成 ═══  │
    │                  │                     │                    │
    │                  │  ◀── 文本片段1 ──── │                    │
    │                  │  ── SSE事件 ───────────────────────────▶ │ 显示文字
    │                  │                     │                    │
    │                  │  ◀── 音频片段1 ──── │                    │
    │                  │  ── SSE事件 ───────────────────────────▶ │ 🎵 播放
    │                  │                     │                    │
    │                  │  ◀── 文本片段2 ──── │                    │
    │                  │  ── SSE事件 ───────────────────────────▶ │ 显示文字
    │                  │                     │                    │
    │                  │  ◀── 音频片段2 ──── │                    │
    │                  │  ── SSE事件 ───────────────────────────▶ │ 🎵 播放
    │                  │                     │                    │
    │                  │                     │  ═══ 生成完成 ═══  │
    │                  │  ── SSE完成事件 ───────────────────────▶ │ 面板就绪
    │                                                              │
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

## 三、求助助手功能设计

### 3.1 快捷求助按钮

| 按钮 | 功能 | 助手行为 |
|------|------|----------|
| **🎯 给我思路** | 获取答题框架 | 给出结构化的思考方向，不直接给答案 |
| **📖 解释概念** | 理解关键概念 | 解释问题中涉及的技术概念 |
| **✍️ 帮我润色** | 优化已输入内容 | 检查候选人已输入的草稿，给出优化建议 |
| **💬 自由提问** | 自由对话 | 展开文本输入框，自由提问 |

### 3.2 UI 布局

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

### 3.3 求助模式配置

| 模式 | 助手限制 | 适用场景 |
|------|----------|----------|
| **练习模式** | 无限制求助 | 学习阶段，熟悉面试流程 |
| **模拟模式** | 每场限3-5次 | 真实模拟，测试自身水平 |
| **实战模式** | 禁用助手 | 真实面试前的最终测试 |

---

## 四、数据库设计

### 4.1 表结构变更

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

## 五、后端接口设计

### 5.1 REST API

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

### 5.2 WebSocket 端点（实时语音）

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

## 六、前端类型定义

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

## 七、阿里云百炼集成

### 7.1 依赖配置

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter</artifactId>
</dependency>
```

### 7.2 配置文件

```yaml
# application.yml
spring:
  ai:
    alibaba:
      api-key: ${ALIBABA_API_KEY}
      voice:
        # 实时语音对话
        realtime-dialog:
          enabled: true
          asr-model: paraformer-realtime-v2    # 语音识别模型
          tts-model: cosyvoice-v2              # 语音合成模型
          input-format: pcm
          output-format: wav
          sample-rate: 16000
        # 音色配置
        voices:
          interviewer: longxiaochun_v2         # 面试官音色
          assistant: zhimiao_emo_v2            # 助手音色（更亲切）
```

### 7.3 流式语音合成实现

#### 方案一：使用 EndToEndRealTimeDialog（推荐）

阿里云百炼的 `EndToEndRealTimeDialog` API 天然支持流式语音合成：

```java
@Service
@RequiredArgsConstructor
public class AliyunStreamVoiceService {

    /**
     * 流式语音对话（边生成边播放）
     * 通过 WebSocket 连接阿里云，实现：
     * 1. 接收用户音频流
     * 2. 实时语音识别
     * 3. LLM 生成回复
     * 4. 流式语音合成
     * 5. 返回音频流
     */
    public Flux<VoiceChunk> streamVoiceDialog(
        String sessionId,
        Flux<byte[]> audioInput,
        AgentConfig config
    ) {
        // 建立 WebSocket 连接
        WebSocketClient client = new StandardWebSocketClient();

        return Flux.create(emitter -> {
            // 连接阿里云实时语音对话 API
            client.doHandshake(new WebSocketHandler() {
                @Override
                public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
                    JsonNode response = parseResponse(message);

                    // 解析流式响应
                    if (response.has("text")) {
                        // 文本片段 - 立即推送给前端显示
                        emitter.next(new VoiceChunk(
                            ChunkType.TEXT,
                            response.get("text").asText()
                        ));
                    }

                    if (response.has("audio")) {
                        // 音频片段 - 立即推送给前端播放
                        emitter.next(new VoiceChunk(
                            ChunkType.AUDIO,
                            response.get("audio").asText()  // Base64
                        ));
                    }

                    if (response.has("done") && response.get("done").asBoolean()) {
                        emitter.complete();
                    }
                }
            }, buildWsUrl(config));
        });
    }
}

@Data
@AllArgsConstructor
public class VoiceChunk {
    private ChunkType type;     // TEXT / AUDIO
    private String content;     // 文本或 Base64 音频
}

public enum ChunkType {
    TEXT, AUDIO
}
```

#### 方案二：分开调用 LLM + 流式 TTS

如果需要更精细控制，可以分开调用：

```java
@Service
@RequiredArgsConstructor
public class StreamAssistService {

    private final ChatClient chatClient;
    private final CosyVoiceTTSClient ttsClient;

    /**
     * 流式求助响应
     * 1. LLM 流式生成文本
     * 2. 文本分块送入 TTS 流式合成
     * 3. 音频流实时返回
     */
    public Flux<AssistSSEEvent> streamAssist(AssistRequest request, InterviewContext context) {
        // 构建提示词
        String systemPrompt = buildSystemPrompt(request.getType());
        String userPrompt = buildUserPrompt(request, context);

        // 文本缓冲区（用于分句合成）
        StringBuilder textBuffer = new StringBuilder();

        return chatClient.stream()
            .system(systemPrompt)
            .user(userPrompt)
            .stream()
            .flatMap(chunk -> {
                String delta = chunk.getContent();
                textBuffer.append(delta);

                // 检测到句子结束，触发语音合成
                if (isSentenceEnd(delta)) {
                    String sentence = textBuffer.toString();
                    textBuffer.setLength(0);

                    // 先返回文本事件
                    Mono<AssistSSEEvent> textEvent = Mono.just(
                        new AssistSSEEvent("text", new TextEventData(sentence, false))
                    );

                    // 流式合成语音
                    Flux<AssistSSEEvent> audioEvents = ttsClient.streamSynthesize(sentence)
                        .map(audio -> new AssistSSEEvent("audio", new AudioEventData(
                            audio,
                            "pcm",
                            16000
                        )));

                    return Flux.concat(textEvent, audioEvents);
                } else {
                    // 返回增量文本
                    return Flux.just(new AssistSSEEvent("text", new TextEventData(delta, true)));
                }
            })
            .concatWith(Mono.just(new AssistSSEEvent("done", new DoneEventData(0, 0))));
    }

    private boolean isSentenceEnd(String text) {
        return text.matches(".*[。！？.!?]$");
    }
}
```

### 7.4 API 参考

| API | 说明 | 文档链接 |
|-----|------|----------|
| EndToEndRealTimeDialog | 端到端实时语音对话（推荐） | https://help.aliyun.com/zh/model-studio/api-dianjin-2024-06-28-endtoendrealtimedialog |
| Paraformer | 实时语音识别 | https://help.aliyun.com/zh/model-studio/real-time-speech-recognition |
| CosyVoice | 流式语音合成 | https://help.aliyun.com/zh/model-studio/cosyvoice-clone-design-api |
| Qwen-TTS | 实时语音合成 | https://help.aliyun.com/zh/model-studio/interactive-process-of-qwen-tts-realtime-synthesis |

---

## 八、前端流式播放实现

### 8.1 SSE 事件处理

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

### 8.2 实时语音录制与发送

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

## 九、录音回放功能

### 8.1 录音存储策略

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

### 8.2 录音合并流程

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

## 十、项目结构变更

### 10.1 后端

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

### 10.2 前端

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

## 十一、开发排期

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

## 十二、待确认事项

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
| 2026-03-12 | 1.1.0 | 新增流式语音合成方案、SSE 接口设计、前端流式播放实现 |
| 2026-03-12 | 1.0.0 | 初始版本：完整技术方案 |
