// =====================================================
// 复盘分析工作流 Composable
// @author Azir
// =====================================================

import { reactive } from 'vue'
import type {
  ReviewAnalysisState,
  GraphProgressEvent,
  AdviceItem
} from '@/types/interview-center'

// 单例状态
let stateInstance: ReviewAnalysisState | null = null

// 初始状态工厂
function createInitialState(): ReviewAnalysisState {
  return {
    isConnecting: false,
    isRunning: false,
    isCompleted: false,
    hasError: false,
    currentStage: 'start',
    progress: 0,
    message: '',
    adviceList: [],
    errorMessage: null
  }
}

// 开发环境日志
function devLog(...args: unknown[]): void {
  if (import.meta.env.DEV) {
    console.log('[useReviewAnalysis]', ...args)
  }
}

/**
 * 复盘分析工作流 Composable
 * 使用 fetch + ReadableStream 处理 POST SSE 请求
 */
export function useReviewAnalysis() {
  if (!stateInstance) {
    stateInstance = reactive<ReviewAnalysisState>(createInitialState())
  }

  let abortController: AbortController | null = null

  /**
   * 开始执行复盘分析工作流
   */
  async function startAnalysis(interviewId: string, transcript: string): Promise<void> {
    resetState()
    stateInstance!.isConnecting = true
    stateInstance!.message = '正在连接...'

    abortController = new AbortController()

    try {
      const response = await fetch(`/landit/interview-center/${interviewId}/review-analysis/stream`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ transcript }),
        signal: abortController.signal
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      stateInstance!.isConnecting = false
      stateInstance!.isRunning = true

      const reader = response.body?.getReader()
      if (!reader) {
        throw new Error('无法获取响应流')
      }

      await processStream(reader)

      if (!stateInstance!.isCompleted && !stateInstance!.hasError) {
        stateInstance!.isRunning = false
        stateInstance!.isCompleted = true
        stateInstance!.currentStage = 'complete'
        stateInstance!.progress = 100
        stateInstance!.message = '复盘分析完成'
      }

    } catch (error) {
      if ((error as Error).name === 'AbortError') {
        devLog('[SSE] 请求已取消')
        return
      }
      devLog('[SSE] 请求错误:', error)
      stateInstance!.hasError = true
      stateInstance!.errorMessage = (error as Error).message || '连接失败，请稍后重试'
      stateInstance!.isRunning = false
    }
  }

  /**
   * 处理 SSE 响应流
   */
  async function processStream(reader: ReadableStreamDefaultReader<Uint8Array>): Promise<void> {
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const json = line.slice(5).trim()
          if (json) {
            try {
              const event: GraphProgressEvent = JSON.parse(json)
              handleEvent(event)
            } catch (e) {
              devLog('[SSE] 解析事件失败:', e, json)
            }
          }
        }
      }
    }
  }

  /**
   * 处理 SSE 事件
   */
  function handleEvent(event: GraphProgressEvent): void {
    devLog('[SSE] 收到事件:', event)

    if (event.event === 'start') {
      stateInstance!.currentStage = 'start'
      stateInstance!.progress = 5
      stateInstance!.message = event.message || '开始分析面试表现...'
      return
    }

    if (event.event === 'progress') {
      if (event.nodeId) {
        stateInstance!.currentStage = event.nodeId as ReviewAnalysisState['currentStage']
      }
      stateInstance!.progress = event.progress || stateInstance!.progress
      stateInstance!.message = event.message || stateInstance!.message
      return
    }

    if (event.event === 'complete') {
      stateInstance!.isRunning = false
      stateInstance!.isCompleted = true
      stateInstance!.currentStage = 'complete'
      stateInstance!.progress = 100
      stateInstance!.message = event.message || '复盘分析完成'
      if (event.data && typeof event.data === 'object') {
        const data = event.data as { adviceList?: AdviceItem[] }
        if (data.adviceList) {
          stateInstance!.adviceList = data.adviceList
        }
      }
      return
    }

    if (event.event === 'error') {
      stateInstance!.hasError = true
      stateInstance!.errorMessage = event.errorMessage || '分析失败'
      stateInstance!.isRunning = false
    }
  }

  /**
   * 取消请求
   */
  function cancelAnalysis(): void {
    if (abortController) {
      abortController.abort()
      abortController = null
      devLog('[SSE] 请求已取消')
    }
  }

  /**
   * 重置状态
   */
  function resetState(): void {
    cancelAnalysis()
    Object.assign(stateInstance!, createInitialState())
  }

  return {
    state: stateInstance,
    startAnalysis,
    cancelAnalysis,
    resetState
  }
}
