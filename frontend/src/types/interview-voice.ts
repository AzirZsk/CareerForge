/**
 * 语音面试相关类型定义
 *
 * @author Azir
 */

// ============================================================================
// 语音模式与状态
// ============================================================================

/** 语音模式 */
export type VoiceMode = 'half_voice' | 'full_voice'

/** 会话状态 */
export type SessionState = 'idle' | 'preparing' | 'ready' | 'interviewing' | 'frozen' | 'completed'

/** 求助类型 */
export type AssistType = 'give_hints' | 'explain_concept' | 'polish_answer' | 'free_question'

/** 对话角色 */
export type ConversationRole = 'interviewer' | 'candidate' | 'assistant'

// ============================================================================
// 语音设置
// ============================================================================

/** 语音设置 */
export interface VoiceSettings {
  /** 语音模式 */
  mode: VoiceMode
  /** 采样率 */
  sampleRate: 16000 | 24000
  /** 面试官音色ID */
  interviewerVoice: string
  /** 助手音色ID */
  assistantVoice: string
  /** 语速 0.5-2.0 */
  speechRate: number
}

/** 默认语音设置 */
export const defaultVoiceSettings: VoiceSettings = {
  mode: 'full_voice',
  sampleRate: 16000,
  interviewerVoice: 'longxiaochun_v2',
  assistantVoice: 'zhimiao_emo_v2',
  speechRate: 1.0
}

// ============================================================================
// TTS（语音合成）相关
// ============================================================================

/** TTS 配置 */
export interface TTSConfig {
  /** 音色ID */
  voice?: string
  /** 输出格式 */
  format: 'pcm' | 'wav' | 'mp3'
  /** 采样率 */
  sampleRate: number
  /** 语速 0.5-2.0 */
  speechRate?: number
  /** 音量 0-1 */
  volume?: number
  /** 音调 -1 到 1 */
  pitch?: number
}

/** TTS 合成块 */
export interface TTSChunk {
  /** 对应的文本片段 */
  text: string
  /** 音频数据（Base64） */
  audio: string
  /** 是否最后一个块 */
  isFinal: boolean
}

// ============================================================================
// SSE 事件
// ============================================================================

/** SSE 事件类型 */
export type SSEEventType = 'text' | 'structured' | 'done' | 'error'

/** SSE 事件基础接口 */
export interface AssistSSEEvent {
  type: SSEEventType
  data: TextEventData | StructuredEventData | DoneEventData | ErrorEventData
}

/** 文本事件数据 */
export interface TextEventData {
  /** 文本内容 */
  content: string
  /** 是否增量文本 */
  isDelta: boolean
}

/** 结构化事件数据 */
export interface StructuredEventData {
  /** 求助类型 code */
  assistType: AssistType
  /** 结构化内容（按类型不同，实际类型由 assistType 决定） */
  content: Record<string, any>
}

/** 完成事件数据 */
export interface DoneEventData {
  /** 剩余求助次数 */
  assistRemaining: number
  /** 总时长（毫秒） */
  totalDurationMs: number
}

/** 错误事件数据 */
export interface ErrorEventData {
  /** 错误码 */
  code: string
  /** 错误信息 */
  message: string
}

// ============================================================================
// 求助结构化数据
// ============================================================================

/** 提示思路数据 */
export interface HintsData {
  /** 思路总结 */
  summary: string
  /** 思考步骤 */
  thinkingSteps: string[]
  /** 关键知识点 */
  keyPoints: string[]
  /** 建议的回答结构 */
  answerStructure: string
}

/** 解释概念数据 */
export interface ExplainData {
  /** 概念定义 */
  definition: string
  /** 类比理解 */
  analogy: string
  /** 应用场景 */
  applications: string[]
  /** 代码示例（可选） */
  codeExample?: string
  /** 总结 */
  summary: string
}

/** 润色答案数据 */
export interface PolishData {
  /** 润色后的回答 */
  polishedAnswer: string
  /** 改进要点 */
  improvements: Array<{ point: string; before: string; after: string }>
  /** 面试建议 */
  tips: string[]
}

/** 自由提问数据 */
export interface FreeQuestionData {
  /** AI 回答 */
  answer: string
  /** 建议 */
  suggestions: string[]
  /** 相关话题 */
  relatedTopics?: string[]
}

/** 降级纯文本数据 */
export interface FallbackData {
  /** 降级文本 */
  fallbackText: string
}

// ============================================================================
// WebSocket 消息
// ============================================================================

/** 转录数据（与后端 VoiceResponse.TranscriptData 对应） */
export interface TranscriptData {
  /** 转录文本 */
  text: string
  /** 是否最终结果 */
  isFinal: boolean
  /** 角色：interviewer, candidate */
  role: string
  /** 置信度 */
  confidence?: number
}

/** WebSocket 消息 */
export interface WSMessage {
  type: 'transcript' | 'audio' | 'state' | 'error' | 'ready'
  data: TranscriptData | AudioData | StateData | ErrorData | ReadyData
}

/** 音频数据 */
export interface AudioData {
  /** Base64 音频 */
  audio: string
  /** 格式 */
  format: string
  /** 采样率 */
  sampleRate?: number
}

/** 状态数据 */
export interface StateData {
  /** 会话状态 */
  state: SessionState
  /** 当前问题序号 */
  currentQuestion: number
  /** 问题总数 */
  totalQuestions: number
  /** 剩余求助次数 */
  assistRemaining: number
  /** 已用时间（秒） */
  elapsedTime: number
}

/** 错误数据 */
export interface ErrorData {
  /** 错误码 */
  code: string
  /** 错误信息 */
  message: string
}

/** 准备就绪数据（预生成完成通知） */
export interface ReadyData {
  /** 就绪状态 */
  state: 'ready'
  /** 提示消息 */
  message: string
}

// ============================================================================
// 面试会话
// ============================================================================

/** 求助请求 */
export interface AssistRequest {
  /** 求助类型 */
  type: AssistType
  /** 用户问题（自由提问时） */
  question?: string
  /** 候选人草稿（润色时使用） */
  candidateDraft?: string
}

/** 面试会话状态 */
export interface InterviewSessionState {
  /** 会话ID */
  sessionId: string
  /** 状态 */
  status: SessionState
  /** 当前问题序号 */
  currentQuestion: number
  /** 问题总数 */
  totalQuestions: number
  /** 已求助次数 */
  assistCount: number
  /** 求助上限 */
  assistLimit: number
  /** 已用时间（秒） */
  elapsedTime: number
}

/** 对话消息 */
export interface ConversationMessage {
  /** ID */
  id: string
  /** 角色 */
  role: ConversationRole
  /** 内容 */
  content: string
  /** 时间戳 */
  timestamp: number
  /** 音频URL（可选） */
  audioUrl?: string
  /** 是否正在播放 */
  isPlaying?: boolean
}

// ============================================================================
// 录音相关
// ============================================================================

/** 录音片段 */
export interface RecordingSegment {
  /** 片段序号 */
  index: number
  /** 角色 */
  role: ConversationRole
  /** 文字内容 */
  content: string
  /** 时长（毫秒） */
  durationMs: number
  /** 开始时间 */
  startTime: string
  /** 结束时间 */
  endTime: string
  /** 音频URL */
  audioUrl: string
}

/** 文字记录条目 */
export interface TranscriptEntry {
  /** 角色 */
  role: ConversationRole
  /** 内容 */
  content: string
  /** 时间戳（毫秒） */
  timestamp: number
  /** 片段序号 */
  segmentIndex: number
}

/** 录音回放信息 */
export interface RecordingInfo {
  /** 会话ID */
  sessionId: string
  /** 总时长（毫秒） */
  totalDurationMs: number
  /** 合并音频URL */
  mergedAudioUrl: string
  /** 片段列表 */
  segments: RecordingSegment[]
  /** 文字记录列表 */
  transcript: TranscriptEntry[]
}

// ============================================================================
// 音频播放状态
// ============================================================================

/** 播放状态 */
export type PlaybackState = 'idle' | 'playing' | 'paused' | 'loading'

/** 音频播放器状态 */
export interface AudioPlayerState {
  /** 播放状态 */
  playbackState: PlaybackState
  /** 当前播放时间（秒） */
  currentTime: number
  /** 总时长（秒） */
  duration: number
  /** 音量 0-1 */
  volume: number
  /** 是否静音 */
  muted: boolean
}
