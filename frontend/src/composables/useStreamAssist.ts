/**
 * SSE 流式求助 Composable
 * 处理语音面试中的快捷求助功能
 *
 * @author Azir
 */

import { ref, computed, onUnmounted } from 'vue'
import { useStreamingAudio } from './useStreamingAudio'
import type { AssistRequest, TextEventData, AudioEventData, DoneEventData, ErrorEventData } from '@/types/interview-voice'
import { authFetch } from '@/utils/request'

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

  /** AbortController 用于中断请求 */
  let abortController: AbortController | null = null

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
    await audioPlayer.initAudioContext(16000)

    // 创建 AbortController
    abortController = new AbortController()

    try {
      // 构建 SSE URL
      const params = new URLSearchParams({
        type: request.type,
        question: request.question || '',
        candidateDraft: request.candidateDraft || ''
      })
      const url = `/careerforge/interviews/sessions/${sessionId}/assist/stream?${params}`

      // 使用 authFetch 发起请求
      const response = await authFetch(url, {}, 60000) // 1分钟超时

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      // 获取 ReadableStream
      const reader = response.body?.getReader()
      if (!reader) {
        throw new Error('无法读取响应流')
      }

      const decoder = new TextDecoder()
      let buffer = ''

      // 读取流
      while (isRequesting.value) {
        const { done, value } = await reader.read()

        if (done) {
          console.log('[useStreamAssist] 流结束')
          break
        }

        // 解码并处理数据
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.trim() && line.startsWith('data:')) {
            try {
              const json = line.slice(5).trim()
              if (!json) continue

              const event = JSON.parse(json)

              // 根据事件类型分发
              if (event.type === 'text') {
                handleTextEventData(event.data)
              } else if (event.type === 'audio') {
                await handleAudioEventData(event.data)
              } else if (event.type === 'done') {
                handleDoneEventData(event.data)
              } else if (event.type === 'error') {
                handleErrorEventData(event.data)
              }
            } catch (e) {
              console.error('[useStreamAssist] 解析事件失败:', e, line)
            }
          }
        }
      }

      reader.releaseLock()
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
   * 注入外部 AudioContext（共享模式，由 useInterviewVoice 调用）
   */
  function setExternalAudioContext(ctx: AudioContext): void {
    audioPlayer.setExternalAudioContext(ctx)
  }

  /**
   * 清理资源
   */
  function cleanup(): void {
    if (abortController) {
      abortController.abort()
      abortController = null
    }
    isRequesting.value = false
  }

  // ============================================================================
  // 事件处理
  // ============================================================================

  /**
   * 处理文本事件数据
   */
  function handleTextEventData(data: TextEventData): void {
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
  }

  /**
   * 处理音频事件数据
   */
  async function handleAudioEventData(data: AudioEventData): Promise<void> {
    try {
      await audioPlayer.playAudioChunk(data.audio, data.format)
    } catch (e) {
      console.error('[useStreamAssist] 处理音频事件失败:', e)
    }
  }

  /**
   * 处理完成事件数据
   */
  function handleDoneEventData(data: DoneEventData): void {
    try {
      assistRemaining.value = data.assistRemaining
      totalDurationMs.value = data.totalDurationMs
    } catch (e) {
      console.error('[useStreamAssist] 解析完成事件失败:', e)
    } finally {
      cleanup()
    }
  }

  /**
   * 处理错误事件数据
   */
  function handleErrorEventData(data: ErrorEventData): void {
    error.value = data.message
    console.error('[useStreamAssist] 服务端错误:', data.code, data.message)
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
    setExternalAudioContext,
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
