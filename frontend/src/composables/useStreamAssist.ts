/**
 * SSE 流式求助 Composable
 * 处理语音面试中的快捷求助功能
 *
 * @author Azir
 */

import { ref, computed, onUnmounted } from 'vue'
import { useStreamingAudio } from './useStreamingAudio'
import type { AssistType, AssistRequest, AssistSSEEvent, TextEventData, AudioEventData, DoneEventData, ErrorEventData } from '@/types/interview-voice'

/**
 * SSE 流式求助
 * @param sessionId 面试会话ID
 */
export function useStreamAssist(sessionId: string) {
  // ============================================================================
  // 状态
  // ============================================================================

  /** 流式音频播放器 */
  const audioPlayer = useStreamingAudio()

  /** 是否正在请求 */
  const isRequesting = ref(false)

  /** 文本内容 */
  const textContent = ref('')

  /** 增量文本（用于实时显示） */
  const deltaText = ref('')

  /** 剩余求助次数 */
  const assistRemaining = ref(5)

  /** 总时长（毫秒） */
  const totalDurationMs = ref(0)

  /** 错误信息 */
  const error = ref<string | null>(null)

  /** EventSource 实例 */
  let eventSource: EventSource | null = null

  // ============================================================================
  // 计算属性
  // ============================================================================

  /** 是否有剩余次数 */
  const hasRemaining = computed(() => assistRemaining.value > 0)

  // ============================================================================
  // 核心方法
  // ============================================================================

  /**
   * 请求流式求助
   * @param request 求助请求
   */
  async function requestAssist(request: AssistRequest): Promise<void> {
    if (isRequesting.value) {
      console.warn('[useStreamAssist] 已有请求进行中')
      return
    }

    if (!hasRemaining.value) {
      error.value = '求助次数已用完'
      return
    }

    // 重置状态
    isRequesting.value = true
    textContent.value = ''
    deltaText.value = ''
    error.value = null

    // 初始化音频播放器
    audioPlayer.initAudioContext(16000)

    try {
      // 构建 SSE URL
      const params = new URLSearchParams({
        type: request.type,
        question: request.question || '',
        candidateDraft: request.candidateDraft || ''
      })
      const url = `/landit/interviews/sessions/${sessionId}/assist/stream?${params}`

      // 创建 EventSource
      eventSource = new EventSource(url)

      // 文本事件
      eventSource.addEventListener('text', handleTextEvent)

      // 音频事件
      eventSource.addEventListener('audio', handleAudioEvent)

      // 完成事件
      eventSource.addEventListener('done', handleDoneEvent)

      // 错误事件
      eventSource.addEventListener('error', handleErrorEvent)

      // 连接错误
      eventSource.onerror = (event) => {
        console.error('[useStreamAssist] SSE 连接错误:', event)
        error.value = '连接失败，请重试'
        cleanup()
      }
    } catch (e) {
      console.error('[useStreamAssist] 请求失败:', e)
      error.value = e instanceof Error ? e.message : '请求失败'
      cleanup()
    }
  }

  /**
   * 停止流式请求
   */
  function stopAssist(): void {
    cleanup()
    audioPlayer.stop()
  }

  /**
   * 清理资源
   */
  function cleanup(): void {
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    isRequesting.value = false
  }

  // ============================================================================
  // 事件处理
  // ============================================================================

  /**
   * 处理文本事件
   */
  function handleTextEvent(event: MessageEvent): void {
    try {
      const data: TextEventData = JSON.parse(event.data)

      if (data.isDelta) {
        // 增量文本，追加显示
        deltaText.value = data.content
        textContent.value += data.content
      } else {
        // 完整文本，替换显示
        textContent.value = data.content
      }

      // 同步到音频播放器的文本显示
      audioPlayer.appendText(data.content, !data.isDelta)
    } catch (e) {
      console.error('[useStreamAssist] 解析文本事件失败:', e)
    }
  }

  /**
   * 处理音频事件
   */
  async function handleAudioEvent(event: MessageEvent): Promise<void> {
    try {
      const data: AudioEventData = JSON.parse(event.data)
      await audioPlayer.playAudioChunk(data.audio, data.format)
    } catch (e) {
      console.error('[useStreamAssist] 处理音频事件失败:', e)
    }
  }

  /**
   * 处理完成事件
   */
  function handleDoneEvent(event: MessageEvent): void {
    try {
      const data: DoneEventData = JSON.parse(event.data)
      assistRemaining.value = data.assistRemaining
      totalDurationMs.value = data.totalDurationMs
    } catch (e) {
      console.error('[useStreamAssist] 解析完成事件失败:', e)
    } finally {
      cleanup()
    }
  }

  /**
   * 处理错误事件
   */
  function handleErrorEvent(event: MessageEvent): void {
    try {
      const data: ErrorEventData = JSON.parse(event.data)
      error.value = data.message
      console.error('[useStreamAssist] 服务端错误:', data.code, data.message)
    } catch (e) {
      error.value = '未知错误'
    }
    cleanup()
  }

  // ============================================================================
  // 快捷方法
  // ============================================================================

  /**
   * 给我思路
   */
  function giveHints(): Promise<void> {
    return requestAssist({ type: 'give_hints' })
  }

  /**
   * 解释概念
   */
  function explainConcept(): Promise<void> {
    return requestAssist({ type: 'explain_concept' })
  }

  /**
   * 帮我润色
   */
  function polishAnswer(candidateDraft: string): Promise<void> {
    return requestAssist({ type: 'polish_answer', candidateDraft })
  }

  /**
   * 自由提问
   */
  function freeQuestion(question: string): Promise<void> {
    return requestAssist({ type: 'free_question', question })
  }

  // 组件卸载时清理
  onUnmounted(() => {
    cleanup()
    audioPlayer.dispose()
  })

  // ============================================================================
  // 返回
  // ============================================================================

  return {
    // 状态
    isRequesting,
    textContent,
    deltaText,
    assistRemaining,
    totalDurationMs,
    error,
    hasRemaining,

    // 音频播放器状态
    isPlaying: audioPlayer.isPlaying,
    playbackState: audioPlayer.playbackState,

    // 方法
    requestAssist,
    stopAssist,
    giveHints,
    explainConcept,
    polishAnswer,
    freeQuestion,

    // 音频控制
    pause: audioPlayer.pause,
    resume: audioPlayer.resume,
    setVolume: audioPlayer.setVolume,
    toggleMute: audioPlayer.toggleMute
  }
}
