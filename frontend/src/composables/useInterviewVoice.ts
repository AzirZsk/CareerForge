/**
 * 语音面试主逻辑 Composable
 * 整合 WebSocket 语音对话、SSE 求助、录音功能
 *
 * @author Azir
 */

import { ref, computed, onUnmounted } from 'vue'
import { useStreamingAudio } from './useStreamingAudio'
import { useAudioRecorder } from './useAudioRecorder'
import { usePageGuard } from './usePageGuard'
import type {
  SessionState,
  VoiceMode,
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
  speechRate: 1.0
}

/**
 * 语音面试主逻辑
 * @param sessionId 面试会话ID
 */
export function useInterviewVoice(sessionId: string) {
  const { registerGuard, unregisterGuard } = usePageGuard()

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

  /** 冻结期间发送静音帧的定时器 */
  let silenceFrameTimer: number | null = null

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

  /** 共享 AudioContext（由本 composable 创建和持有） */
  let sharedAudioContext: AudioContext | null = null

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
  async function init(voiceMode?: VoiceMode): Promise<void> {
    try {
      // 如果调用方传入了 voiceMode，覆盖默认设置
      if (voiceMode) {
        settings.value.mode = voiceMode
      }
      // 创建共享 AudioContext
      sharedAudioContext = new AudioContext({ sampleRate: settings.value.sampleRate })
      if (sharedAudioContext.state === 'suspended') {
        await sharedAudioContext.resume()
      }

      // 初始化音频播放器（注入共享 context）
      await audioPlayer.initAudioContext(settings.value.sampleRate, sharedAudioContext)

      // 注册播放时序回调，用于文字-音频同步
      audioPlayer.setPlaybackCallbacks({
        onChunkStart: handleChunkStart,
        onPlaybackEnd: handlePlaybackEnd
      })

      // 初始化录音器（注入共享 context）
      await recorder.init({
        onAudioData: handleAudioData,
        audioContext: sharedAudioContext
      })

      connectWebSocket()
      startTimer()

      error.value = null
    } catch (e) {
      console.error('[useInterviewVoice] 初始化失败:', e)
      error.value = e instanceof Error ? e.message : '初始化失败'
    }
  }

  /**
   * 获取共享 AudioContext（供外部 composable 使用）
   */
  function getAudioContext(): AudioContext | null {
    return sharedAudioContext
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
    // 获取 token（WebSocket 不支持自定义 Header，只能通过 URL 参数传递）
    const token = localStorage.getItem('token')
    const wsUrl = `${protocol}//${host}/careerforge/ws/interview/voice/${sessionId}${token ? `?token=${token}` : ''}`

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
    if (data.role === 'interviewer') {
      // half_voice 模式：收到 transcript 就关闭等待（文字立即显示）
      // full_voice 模式：保持等待状态，直到文字真正开始显示（revealTick 触发时关闭）
      if (!isFullVoice()) {
        isWaitingForAI.value = false
      }
      // 面试官 transcript：累积文字（后端按句分块发送）
      if (!data.isFinal) {
        if (data.text) {
          interviewerAccumulatedText += data.text
        }
        if (isFullVoice()) {
          // full_voice 模式：始终缓冲，等音频播放时再释放
          textGateBuffer = interviewerAccumulatedText
        } else {
          // half_voice 模式：立即显示
          partialTranscript.value = { ...data, text: interviewerAccumulatedText }
        }
      } else {
        // 面试官回复结束（可能是空 final 信号或带文字的 final）
        if (data.text) {
          interviewerAccumulatedText += data.text
        }
        if (isFullVoice() && revealedChars < interviewerAccumulatedText.length) {
          // full_voice 且文字还没释放完：标记流结束，等门控追上来再提交
          textGateBuffer = interviewerAccumulatedText
          isTextStreamDone = true
          // 启动安全回退定时器：如果 TTS 失败没有音频到达，3 秒后强制显示文字
          startTextFallbackTimer()
        } else {
          // half_voice 或文字已全部释放：立即提交
          commitInterviewerMessage()
        }
      }
    } else {
      // 候选人 transcript：保持原逻辑
      if (data.isFinal) {
        const message: ConversationMessage = {
          id: `msg_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`,
          role: data.role === 'interviewer' ? 'interviewer' : 'candidate',
          content: data.text,
          timestamp: Date.now()
        }
        messages.value.push(message)
        // 候选人说完话，开始等待AI回复
        isWaitingForAI.value = true
      }
      // 更新实时转录状态（非最终结果）
      partialTranscript.value = data.isFinal ? null : data
    }
  }

  /** 实时转录（非最终结果） */
  const partialTranscript = ref<TranscriptData | null>(null)

  /** 等待AI面试官回复 */
  const isWaitingForAI = ref(false)

  /** 面试官文字累积（后端按句分块发送，前端需要拼接完整回复） */
  let interviewerAccumulatedText = ''

  // ==================== 音频门控文字同步 ====================

  /** 门控模式下的完整文字缓冲（已到达但未全部显示） */
  let textGateBuffer = ''

  /** 已释放的字符数 */
  let revealedChars = 0

  /** 累积音频总时长（秒） */
  let accumulatedAudioDuration = 0

  /** 文字流是否已结束（isFinal 已到达） */
  let isTextStreamDone = false

  /** 逐字释放定时器 */
  let revealTimer: number | null = null

  /** 小数累积器（平滑释放速率） */
  let fractionalChars = 0

  /** 定时器间隔（毫秒） */
  const REVEAL_INTERVAL_MS = 50

  /** 中文最低语速估算（字/秒），用于防止初期释放速率过高 */
  const MIN_SPEECH_RATE = 5

  /** TTS 安全回退定时器：如果 TTS 失败没有音频到达，强制显示文字 */
  let textFallbackTimer: number | null = null

  /** TTS 安全回退超时时间（毫秒） */
  const TEXT_FALLBACK_TIMEOUT_MS = 3000

  /**
   * 启动安全回退定时器
   * 如果 TTS 失败没有音频到达，超时后强制显示所有已缓冲文字
   */
  function startTextFallbackTimer(): void {
    clearTextFallbackTimer()
    textFallbackTimer = window.setTimeout(() => {
      console.warn('[useInterviewVoice] TTS 回退定时器触发，强制显示文字')
      isWaitingForAI.value = false
      if (textGateBuffer.length > 0 && revealedChars < textGateBuffer.length) {
        revealedChars = textGateBuffer.length
        partialTranscript.value = {
          text: textGateBuffer,
          isFinal: false,
          role: 'interviewer'
        }
      }
      if (interviewerAccumulatedText.trim()) {
        commitInterviewerMessage()
      }
      textFallbackTimer = null
    }, TEXT_FALLBACK_TIMEOUT_MS)
  }

  /**
   * 清除安全回退定时器
   */
  function clearTextFallbackTimer(): void {
    if (textFallbackTimer) {
      clearTimeout(textFallbackTimer)
      textFallbackTimer = null
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
    // 防御性处理：将 "connected" 视为 "ready"（兼容旧版本后端）
    if ((data.state as string) === 'connected') {
      sessionState.value = 'ready'
      registerGuard('voice-interview')
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
    registerGuard('voice-interview')
  }

  /**
   * 开始面试（用户确认）
   */
  function startInterview(): void {
    if (sessionState.value !== 'ready') {
      console.warn('[useInterviewVoice] 当前状态不允许开始面试:', sessionState.value)
      return
    }

    // 面试开始后立即显示"AI思考中"动画，等待面试官第一个问题
    isWaitingForAI.value = true
    sendControlMessage('start')
  }

  /**
   * 处理错误消息
   */
  function handleErrorMessage(data: { code: string; message: string }): void {
    error.value = data.message
    // 错误发生时关闭三点动画，防止永远 loading
    isWaitingForAI.value = false
    clearTextFallbackTimer()
    console.error('[useInterviewVoice] 服务端错误:', data.code, data.message)
  }

  // ============================================================================
  // 音频门控文字同步
  // ============================================================================

  /** 是否为全语音模式（full_voice 下才启用门控） */
  function isFullVoice(): boolean {
    return settings.value.mode === 'full_voice'
  }

  /**
   * 音频 chunk 开始播放回调
   * 累加音频时长，启动逐字释放定时器
   */
  function handleChunkStart(chunkDuration: number): void {
    if (!isFullVoice()) return
    accumulatedAudioDuration += chunkDuration
    // 音频正常到达，重置回退定时器（TTS 没有失败，不需要回退）
    if (textFallbackTimer && isTextStreamDone) {
      clearTextFallbackTimer()
      startTextFallbackTimer()
    }
    // 第一个音频 chunk 到达且缓冲有文字时，启动释放定时器
    if (!revealTimer && textGateBuffer.length > 0) {
      revealTimer = window.setInterval(revealTick, REVEAL_INTERVAL_MS)
    }
  }

  /**
   * 定时器回调：按音频播放速率逐字释放文字
   * 速率 = 缓冲文字总长 / 累积音频总时长（chars/sec）
   */
  function revealTick(): void {
    if (textGateBuffer.length === 0 || accumulatedAudioDuration <= 0) return
    // 有效时长 = max(已播放音频时长, 文字长度/最低语速)，防止初期速率爆炸
    const minEstimatedDuration = textGateBuffer.length / MIN_SPEECH_RATE
    const effectiveDuration = Math.max(accumulatedAudioDuration, minEstimatedDuration)
    const charsPerSecond = textGateBuffer.length / effectiveDuration
    const charsPerTick = charsPerSecond * (REVEAL_INTERVAL_MS / 1000)
    fractionalChars += charsPerTick
    const toReveal = Math.floor(fractionalChars)
    if (toReveal <= 0) return
    fractionalChars -= toReveal
    const prevChars = revealedChars
    revealedChars = Math.min(revealedChars + toReveal, textGateBuffer.length)
    if (revealedChars === prevChars) return
    partialTranscript.value = {
      text: textGateBuffer.substring(0, revealedChars),
      isFinal: false,
      role: 'interviewer'
    }
    // 文字开始出现时关闭三点动画，消除割裂感（三点消失和文字出现在同一帧）
    if (isWaitingForAI.value) {
      isWaitingForAI.value = false
      clearTextFallbackTimer()
    }
    // 文字流已结束且全部释放完 → 提交消息
    if (isTextStreamDone && revealedChars >= textGateBuffer.length) {
      commitInterviewerMessage()
    }
  }

  /**
   * 音频播放结束回调
   * 立刻刷出所有剩余文字
   */
  function handlePlaybackEnd(): void {
    if (!isFullVoice()) return
    // 安全兜底：确保三点动画关闭
    if (isWaitingForAI.value) {
      isWaitingForAI.value = false
    }
    clearTextFallbackTimer()
    if (textGateBuffer.length > revealedChars) {
      revealedChars = textGateBuffer.length
      partialTranscript.value = {
        text: textGateBuffer,
        isFinal: false,
        role: 'interviewer'
      }
    }
    // 文字流已结束 → 提交消息
    if (isTextStreamDone) {
      commitInterviewerMessage()
    }
    stopRevealTimer()
  }

  /**
   * 停止逐字释放定时器
   */
  function stopRevealTimer(): void {
    if (revealTimer) {
      clearInterval(revealTimer)
      revealTimer = null
    }
    fractionalChars = 0
  }

  /**
   * 提交面试官消息到消息列表，重置门控状态
   */
  function commitInterviewerMessage(): void {
    if (interviewerAccumulatedText.trim()) {
      const message: ConversationMessage = {
        id: `msg_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`,
        role: 'interviewer',
        content: interviewerAccumulatedText,
        timestamp: Date.now()
      }
      messages.value.push(message)
    }
    interviewerAccumulatedText = ''
    partialTranscript.value = null
    resetTextGate()
  }

  /**
   * 重置所有门控状态
   */
  function resetTextGate(): void {
    textGateBuffer = ''
    revealedChars = 0
    accumulatedAudioDuration = 0
    isTextStreamDone = false
    stopRevealTimer()
    clearTextFallbackTimer()
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
   * AI 说话期间发送静音帧，避免回声/噪音污染 ASR 识别
   */
  function handleAudioData(data: Int16Array): void {
    if (audioPlayer.isPlaying.value) {
      sendAudioData(new Int16Array(data.length))
    } else {
      sendAudioData(data)
    }
  }

  /**
   * 启动静音帧发送定时器
   * 冻结期间每 256ms 发送一帧静音 PCM，保持 ASR 连接活跃
   */
  function startSilenceFrameSender(): void {
    stopSilenceFrameSender()
    const silenceFrame = new Int16Array(4096)
    silenceFrameTimer = window.setInterval(() => {
      sendAudioData(silenceFrame)
    }, 256)
  }

  /**
   * 停止静音帧发送定时器
   */
  function stopSilenceFrameSender(): void {
    if (silenceFrameTimer) {
      clearInterval(silenceFrameTimer)
      silenceFrameTimer = null
    }
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
      startSilenceFrameSender()
    }
  }

  /**
   * 恢复面试
   */
  function resumeInterview(): void {
    if (isFrozen.value) {
      sessionState.value = 'interviewing'
      startTimer()
      stopSilenceFrameSender()
      startRecording()
    }
  }

  /**
   * 结束面试
   */
  function endInterview(): void {
    sendControlMessage('end')
    sessionState.value = 'completed'
    unregisterGuard('voice-interview')
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
    stopRevealTimer()
    clearTextFallbackTimer()
    stopSilenceFrameSender()
    unregisterGuard('voice-interview')

    if (ws) {
      ws.close(1000, 'User closed')
      ws = null
    }

    recorder.dispose()
    audioPlayer.dispose()

    // 关闭共享 AudioContext（子 composable 已断开节点，不会报错）
    if (sharedAudioContext) {
      sharedAudioContext.close()
      sharedAudioContext = null
    }
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
    isWaitingForAI,

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
    // 共享 AudioContext
    getAudioContext,
    // 音频控制
    pause: audioPlayer.pause,
    resumeAudio: audioPlayer.resume,
    setVolume: audioPlayer.setVolume,
    toggleMute: audioPlayer.toggleMute
  }
}
