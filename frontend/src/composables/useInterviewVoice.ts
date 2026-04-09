/**
 * 语音面试主逻辑 Composable
 * 整合 WebSocket 语音对话、SSE 求助、录音功能
 *
 * @author Azir
 */

import { ref, computed, onUnmounted } from 'vue'
import { useStreamingAudio } from './useStreamingAudio'
import { useAudioRecorder } from './useAudioRecorder'
import type {
  SessionState,
  VoiceSettings,
  WSMessage,
  AudioData,
  StateData,
  ErrorData,
  TranscriptData,
  ReadyData,
  ConversationMessage
} from '@/types/interview-voice'

// 默认设置
const DEFAULT_SETTINGS: VoiceSettings = {
  mode: 'full_voice',
  sampleRate: 16000,
  interviewerVoice: 'longxiaochun_v2',
  assistantVoice: 'zhimiao_emo_v2',
  speechRate: 1.0,
  vadEnabled: true,
  vadSilenceMs: 1500
}

/**
 * 语音面试主逻辑
 * @param sessionId 面试会话ID
 */
export function useInterviewVoice(sessionId: string) {
  // ============================================================================
  // 状态
  // ============================================================================

  /** 语音设置 */
  const settings = ref<VoiceSettings>({ ...DEFAULT_SETTINGS })

  /** 会话状态 */
  const sessionState = ref<SessionState>('idle')

  /** 当前问题序号 */
  const currentQuestion = ref(0)

  /** 问题总数 */
  const totalQuestions = ref(10)

  /** 已求助次数 */
  const assistCount = ref(0)

  /** 求助上限 */
  const assistLimit = ref(5)

  /** 已用时间（秒) */
  const elapsedTime = ref(0)

  /** 对话消息列表 */
  const messages = ref<ConversationMessage[]>([])

  /** 错误信息 */
  const error = ref<string | null>(null)

  /** 计时器 */
  let elapsedTimer: number | null = null

  /** WebSocket 连接 */
  let ws: WebSocket | null = null

  /** 重连计数器 */
  const reconnectAttempts = ref(0)

  /** 最大重连次数 */
  const MAX_RECONNECT_ATTEMPTS = 5

  /** 重连延迟（毫秒） */
  const RECONNECT_DELAY = 3000

  /** 心跳定时器 */
  let heartbeatInterval: number | null = null

  /** 心跳间隔（毫秒） */
  const heartbeatIntervalMs = 30000

  /** 音频播放器 */
  const audioPlayer = useStreamingAudio()

  /** 音频录制器 */
  const recorder = useAudioRecorder()

  // ============================================================================
  // 计算属性
  // ============================================================================

  /** 是否面试进行中 */
  const isInterviewing = computed(() => sessionState.value === 'interviewing')

  /** 是否准备中（预生成进行中） */
  const isPreparing = computed(() => sessionState.value === 'preparing')

  /** 是否准备就绪（预生成完成，等待用户确认开始） */
  const isReady = computed(() => sessionState.value === 'ready')

  /** 是否冻结状态 */
  const isFrozen = computed(() => sessionState.value === 'frozen')

  /** 是否已完成 */
  const isCompleted = computed(() => sessionState.value === 'completed')

  /** 剩余求助次数 */
  const assistRemaining = computed(() => assistLimit.value - assistCount.value)

  /** 进度百分比 */
  const progress = computed(() =>
    totalQuestions.value > 0 ? (currentQuestion.value / totalQuestions.value) * 100 : 0
  )

  // ============================================================================
  // 初始化
  // ============================================================================

  /**
   * 初始化语音面试
   */
  async function init(): Promise<void> {
    try {
      // 初始化音频播放器
      await audioPlayer.initAudioContext(settings.value.sampleRate)

      // 初始化录音器
      await recorder.init({
        vadEnabled: settings.value.vadEnabled,
        vadSilenceMs: settings.value.vadSilenceMs,
        onAudioData: handleAudioData
      })

      connectWebSocket()
      startTimer()

      error.value = null
    } catch (e) {
      console.error('[useInterviewVoice] 初始化失败:', e)
      error.value = e instanceof Error ? e.message : '初始化失败'
    }
  }

  // ============================================================================
  // WebSocket 连接
  // ============================================================================

  /**
   * 建立 WebSocket 连接
   */
  function connectWebSocket(): void {
    // 开发环境直接连接后端，绕过 Vite 代理（Vite WS 代理有问题）
    const isDev = import.meta.env.DEV
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = isDev ? 'localhost:8080' : location.host
    const wsUrl = `${protocol}//${host}/landit/ws/interview/voice/${sessionId}`

    ws = new WebSocket(wsUrl)
    ws.onopen = handleWsOpen
    ws.onmessage = handleWsMessage
    ws.onerror = handleWsError
    ws.onclose = handleWsClose
  }

  /**
   * WebSocket 连接成功
   */
  function handleWsOpen(): void {
    reconnectAttempts.value = 0
    startHeartbeat()
    // 不再连接后自动发 start，等用户点击"开始面试"按钮
  }

  /**
   * 启动心跳检测
   */
  function startHeartbeat(): void {
    stopHeartbeat()
    heartbeatInterval = window.setInterval(() => {
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: 'ping' }))
      }
    }, heartbeatIntervalMs)
  }

  /**
   * 停止心跳检测
   */
  function stopHeartbeat(): void {
    if (heartbeatInterval) {
      clearInterval(heartbeatInterval)
      heartbeatInterval = null
    }
  }

  /**
   * WebSocket 消息处理
   */
  function handleWsMessage(event: MessageEvent): void {
    try {
      const msg: WSMessage = JSON.parse(event.data)

      switch (msg.type) {
        case 'transcript':
          handleTranscriptMessage(msg.data as TranscriptData)
          break
        case 'audio':
          handleAudioMessage(msg.data as AudioData)
          break
        case 'state':
          handleStateMessage(msg.data as StateData)
          break
        case 'error':
          handleErrorMessage(msg.data as ErrorData)
          break
        case 'ready':
          handleReadyMessage(msg.data as ReadyData)
          break
      }
    } catch (e) {
      console.error('[useInterviewVoice] 解析消息失败:', e)
    }
  }

  /**
   * WebSocket 错误
   */
  function handleWsError(event: Event): void {
    console.error('[useInterviewVoice] WebSocket 错误:', event)
    error.value = '连接错误'
  }

  /**
   * WebSocket 关闭
   */
  function handleWsClose(event: CloseEvent): void {
    stopHeartbeat()

    // 非正常关闭且未超过最大重连次数时尝试重连
    if (event.code !== 1000 && reconnectAttempts.value < MAX_RECONNECT_ATTEMPTS) {
      reconnectAttempts.value++
      setTimeout(() => {
        connectWebSocket()
      }, RECONNECT_DELAY)
    } else if (reconnectAttempts.value >= MAX_RECONNECT_ATTEMPTS) {
      error.value = '连接已断开，请刷新页面重试'
    }
  }

  // ============================================================================
  // 消息处理
  // ============================================================================

  /**
   * 处理转录消息
   */
  function handleTranscriptMessage(data: TranscriptData): void {
    // 如果是最终结果，添加到消息列表
    if (data.isFinal) {
      const message: ConversationMessage = {
        id: `msg_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`,
        role: data.role === 'interviewer' ? 'interviewer' : 'candidate',
        content: data.text,
        timestamp: Date.now()
      }
      messages.value.push(message)
    }
    // 更新实时转录状态（非最终结果）
    partialTranscript.value = data.isFinal ? null : data
  }

  /** 实时转录（非最终结果） */
  const partialTranscript = ref<TranscriptData | null>(null)

  /**
   * 处理音频消息
   */
  async function handleAudioMessage(data: AudioData): Promise<void> {
    await audioPlayer.playAudioChunk(data.audio, data.format as 'pcm' | 'wav')
  }

  /**
   * 处理状态消息
   */
  function handleStateMessage(data: StateData): void {
    // 防御性处理：将 "connected" 视为 "ready"（兼容旧版本后端）
    if ((data.state as string) === 'connected') {
      sessionState.value = 'ready'
      return
    }

    sessionState.value = data.state
    currentQuestion.value = data.currentQuestion
    totalQuestions.value = data.totalQuestions
    assistCount.value = assistLimit.value - data.assistRemaining
    elapsedTime.value = data.elapsedTime

    // 状态变为 interviewing 时立即开始录音
    // 录音和音频播放是并行的，用户可以随时打断面试官
    if (data.state === 'interviewing' && !recorder.isRecording.value) {
      recorder.startRecording()
    }
  }

  /**
   * 处理准备就绪消息（预生成完成）
   */
  function handleReadyMessage(data: ReadyData): void {
    console.log('[useInterviewVoice] 预生成完成:', data.message)
    sessionState.value = 'ready'
  }

  /**
   * 开始面试（用户确认）
   */
  function startInterview(): void {
    if (sessionState.value !== 'ready') {
      console.warn('[useInterviewVoice] 当前状态不允许开始面试:', sessionState.value)
      return
    }

    // 发送开始控制消息
    sendControlMessage('start')
  }

  /**
   * 处理错误消息
   */
  function handleErrorMessage(data: { code: string; message: string }): void {
    error.value = data.message
    console.error('[useInterviewVoice] 服务端错误:', data.code, data.message)
  }

  // ============================================================================
  // 发送消息
  // ============================================================================

  /**
   * 发送控制消息
   */
  function sendControlMessage(action: 'start' | 'stop' | 'end'): void {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({
        type: 'control',
        data: { action }
      }))
    }
  }

  /**
   * 发送音频数据
   */
  function sendAudioData(audioData: Int16Array): void {
    if (ws && ws.readyState === WebSocket.OPEN) {
      const base64Audio = arrayBufferToBase64(audioData.buffer as ArrayBuffer)
      ws.send(JSON.stringify({
        type: 'audio',
        data: {
          audio: base64Audio,
          format: 'pcm',
          sampleRate: settings.value.sampleRate
        }
      }))
    }
  }

  /**
   * 处理录音数据
   */
  function handleAudioData(data: Int16Array): void {
    sendAudioData(data)
  }

  // ============================================================================
  // 录音控制
  // ============================================================================

  /**
   * 开始录音
   */
  async function startRecording(): Promise<void> {
    if (!recorder.isRecording.value && isInterviewing.value) {
      recorder.startRecording()
    }
  }

  /**
   * 停止录音
   */
  function stopRecording(): void {
    recorder.stopRecording()
  }

  // ============================================================================
  // 会话控制
  // ============================================================================

  /**
   * 冻结面试(进入求助模式)
   */
  function freeze(): void {
    if (isInterviewing.value) {
      sessionState.value = 'frozen'
      stopRecording()
      stopTimer()
    }
  }

  /**
   * 恢复面试
   */
  function resumeInterview(): void {
    if (isFrozen.value) {
      sessionState.value = 'interviewing'
      startTimer()
    }
  }

  /**
   * 结束面试
   */
  function endInterview(): void {
    sendControlMessage('end')
    sessionState.value = 'completed'
    stopRecording()
    stopTimer()
  }

  // ============================================================================
  // 辅助方法
  // ============================================================================

  /**
   * 启动计时器
   */
  function startTimer(): void {
    if (!elapsedTimer) {
      elapsedTimer = window.setInterval(() => {
        elapsedTime.value++
      }, 1000)
    }
  }

  /**
   * 停止计时器
   */
  function stopTimer(): void {
    if (elapsedTimer) {
      clearInterval(elapsedTimer)
      elapsedTimer = null
    }
  }

  /**
   * ArrayBuffer 转 Base64
   */
  function arrayBufferToBase64(buffer: ArrayBuffer): string {
    const bytes = new Uint8Array(buffer)
    let binary = ''
    for (let i = 0; i < bytes.byteLength; i++) {
      binary += String.fromCharCode(bytes[i])
    }
    return btoa(binary)
  }

  /**
   * 清理资源
   */
  function dispose(): void {
    stopTimer()
    stopHeartbeat()
    stopRecording()

    if (ws) {
      ws.close(1000, 'User closed')
      ws = null
    }

    recorder.dispose()
    audioPlayer.dispose()
  }

  onUnmounted(dispose)

  // ============================================================================
  // 返回
  // ============================================================================

  return {
    // 状态
    settings,
    sessionState,
    currentQuestion,
    totalQuestions,
    assistCount,
    assistLimit,
    elapsedTime,
    messages,
    error,
    partialTranscript,
    reconnectAttempts,

    // 计算属性
    isInterviewing,
    isPreparing,
    isReady,
    isFrozen,
    isCompleted,
    assistRemaining,
    progress,
    // 录音状态
    isRecording: recorder.isRecording,
    recordingTime: recorder.recordingTime,
    audioLevel: recorder.audioLevel,
    // 播放状态
    isPlaying: audioPlayer.isPlaying,
    playbackState: audioPlayer.playbackState,
    // 方法
    init,
    startInterview,
    startRecording,
    stopRecording,
    freeze,
    resumeInterview,
    endInterview,
    dispose,
    // 音频控制
    pause: audioPlayer.pause,
    resumeAudio: audioPlayer.resume,
    setVolume: audioPlayer.setVolume,
    toggleMute: audioPlayer.toggleMute
  }
}
