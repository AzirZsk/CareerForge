/**
 * SSE 流式求助 Composable
 * 处理语音面试中的快捷求助功能（结构化 JSON 响应）
 *
 * @author Azir
 */

import { ref, computed, onUnmounted } from 'vue'
import type { AssistRequest, TextEventData, StructuredEventData, DoneEventData, ErrorEventData, AssistType } from '@/types/interview-voice'
import { authFetch } from '@/utils/request'

/**
 * SSE 流式求助
 * @param sessionId 面试会话ID
 */
export function useStreamAssist(sessionId: string) {
  // ============================================================================
  // 状态
  // ============================================================================

  /** 是否正在请求 */
  const isRequesting = ref(false)

  /** 剩余求助次数 */
  const assistRemaining = ref(5)

  /** 总时长（毫秒） */
  const totalDurationMs = ref(0)

  /** 错误信息 */
  const error = ref<string | null>(null)

  /** 当前求助类型 */
  const currentAssistType = ref<AssistType | null>(null)

  /** 结构化数据（AI 完成后设置） */
  const structuredData = ref<StructuredEventData | null>(null)

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
    console.log('[useStreamAssist] 开始请求, type:', request.type)

    if (isRequesting.value) {
      console.warn('[useStreamAssist] 已有请求进行中，跳过')
      return
    }

    if (!hasRemaining.value) {
      console.warn('[useStreamAssist] 求助次数已用完')
      error.value = '求助次数已用完'
      return
    }

    // 重置状态
    isRequesting.value = true
    error.value = null
    structuredData.value = null
    currentAssistType.value = request.type

    // 创建 AbortController
    abortController = new AbortController()

    try {
      // 构建 SSE URL
      const params = new URLSearchParams({
        type: request.type,
        question: request.question || ''
      })
      const url = `/careerforge/interviews/sessions/${sessionId}/assist/stream?${params}`
      console.log('[useStreamAssist] 请求URL:', url)

      const requestStartTime = Date.now()
      const response = await authFetch(url, { signal: abortController.signal }, 60000)

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
      let eventCount = 0

      // 读取流
      while (isRequesting.value) {
        const { done, value } = await reader.read()

        if (done) {
          console.log('[useStreamAssist] 流结束, 事件数:', eventCount, '耗时:', Date.now() - requestStartTime, 'ms')
          break
        }

        // 解码并处理数据
        const chunk = decoder.decode(value, { stream: true })
        buffer += chunk

        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.trim() && line.startsWith('data:')) {
            try {
              const json = line.slice(5).trim()
              if (!json) continue

              const event = JSON.parse(json)
              eventCount++

              // 根据事件类型分发
              if (event.type === 'text') {
                handleTextEventData(event.data)
              } else if (event.type === 'structured') {
                console.log('[useStreamAssist] 收到 structured 事件, assistType:', event.data?.assistType)
                handleStructuredEventData(event.data)
              } else if (event.type === 'done') {
                console.log('[useStreamAssist] 收到 done 事件, remaining:', event.data?.assistRemaining)
                handleDoneEventData(event.data)
              } else if (event.type === 'error') {
                console.error('[useStreamAssist] 收到 error 事件:', event.data?.code, event.data?.message)
                handleErrorEventData(event.data)
              }
            } catch (e) {
              console.error('[useStreamAssist] 解析事件失败:', line, e)
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
   * 处理文本事件数据（结构化模式下忽略）
   */
  function handleTextEventData(_data: TextEventData): void {
    console.debug('[useStreamAssist] 忽略 text 事件（结构化模式）')
  }

  /**
   * 处理结构化事件数据
   */
  function handleStructuredEventData(data: StructuredEventData): void {
    try {
      structuredData.value = data
      if (data.assistType) {
        currentAssistType.value = data.assistType
      }
    } catch (e) {
      console.error('[useStreamAssist] 处理结构化数据失败:', e)
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
   * 自由提问
   */
  function freeQuestion(question: string): Promise<void> {
    return requestAssist({ type: 'free_question', question })
  }

  // 组件卸载时清理
  onUnmounted(() => {
    cleanup()
  })

  // ============================================================================
  // 返回
  // ============================================================================

  return {
    // 状态
    isRequesting,
    assistRemaining,
    totalDurationMs,
    error,
    hasRemaining,
    currentAssistType,
    structuredData,

    // 方法
    requestAssist,
    stopAssist,
    giveHints,
    explainConcept,
    freeQuestion
  }
}
