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
  TranscriptData,
  AudioData,
  StateData,
  ErrorData,
  ConversationMessage
} from '@/types/interview-voice'

// 默认设置
const DEFAULT_SETTINGS: VoiceSettings = {
  mode: 'full_voice',
  inputFormat: 'pcm',
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
  const sessionState = ref<SessionState>('interviewing')

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

  /** 当前识别文本 */
  const currentTranscript = ref('')

  /** 是否正在识别 */
  const isRecognizing = ref(false)

  /** 错误信息 */
  const error = ref<string | null>(null)

  /** 计时器 */
  let elapsedTimer: number | null = null

  /** WebSocket 连接 */
  let ws: WebSocket | null = null

  /** 音频播放器 */
  const audioPlayer = useStreamingAudio()

  /** 音频录制器 */
  const recorder = useAudioRecorder()

  // ============================================================================
  // 计算属性
  // ============================================================================

  /** 是否面试进行中 */
  const isInterviewing = computed(() => sessionState.value === 'interviewing')

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
      audioPlayer.initAudioContext(settings.value.sampleRate)

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
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${location.host}/landit/ws/interview/voice/${sessionId}`

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
    console.log('[useInterviewVoice] WebSocket 连接成功')
    sendControlMessage('start')
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
    console.log('[useInterviewVoice] WebSocket 关闭:', event.code, event.reason)
  }

  // ============================================================================
  // 消息处理
  // ============================================================================

  /**
   * 处理转录消息
   */
  function handleTranscriptMessage(data: TranscriptData): void {
    if (data.isFinal) {
      addMessage(data.role, data.text)
      currentTranscript.value = ''
      isRecognizing.value = false
    } else {
      currentTranscript.value = data.text + '...'
      isRecognizing.value = true
    }
  }

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
    sessionState.value = data.state
    currentQuestion.value = data.currentQuestion
    totalQuestions.value = data.totalQuestions
    assistCount.value = assistLimit.value - data.assistRemaining
    elapsedTime.value = data.elapsedTime
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
          format: settings.value.inputFormat,
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
   * 添加消息
   */
  function addMessage(role: 'interviewer' | 'candidate' | 'assistant', content: string): void {
    messages.value.push({
      id: `msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      role,
      content,
      timestamp: Date.now()
    })
  }

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
    stopRecording()

    if (ws) {
      ws.close()
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
    currentTranscript,
    isRecognizing,
    error,

    // 计算属性
    isInterviewing,
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
