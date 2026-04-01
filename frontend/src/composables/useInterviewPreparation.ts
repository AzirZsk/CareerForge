// =====================================================
// 面试准备工作流 Composable
// @author Azir
// =====================================================

import { reactive } from 'vue'
import type {
  PreparationState,
  GraphProgressEvent,
  PreparationItem
} from '@/types/interview-center'

// 单例状态
let stateInstance: PreparationState | null = null

// 初始状态工厂
function createInitialState(): PreparationState {
  return {
    isConnecting: false,
    isRunning: false,
    isCompleted: false,
    hasError: false,
    currentStage: 'start',
    progress: 0,
    message: '',
    preparationItems: [],
    errorMessage: null
  }
}

// 开发环境日志
function devLog(...args: unknown[]): void {
  if (import.meta.env.DEV) {
    console.log('[useInterviewPreparation]', ...args)
  }
}

/**
 * 面试准备工作流 Composable
 * 使用 SSE 流式获取 AI 生成的面试准备事项
 */
export function useInterviewPreparation() {
  if (!stateInstance) {
    stateInstance = reactive<PreparationState>(createInitialState())
  }

  let eventSource: EventSource | null = null

  /**
   * 开始执行面试准备工作流
   */
  function startPreparation(interviewId: string): void {
    resetState()
    stateInstance!.isConnecting = true
    stateInstance!.message = '正在连接...'

    const url = `/landit/interview-center/${interviewId}/preparation/stream`
    eventSource = new EventSource(url)

    eventSource.onopen = () => {
      stateInstance!.isConnecting = false
      stateInstance!.isRunning = true
      devLog('[SSE] 连接已建立')
    }

    eventSource.onmessage = (event) => {
      try {
        const data: GraphProgressEvent = JSON.parse(event.data)
        handleEvent(data)
      } catch (e) {
        devLog('[SSE] 解析事件失败:', e)
      }
    }

    eventSource.onerror = () => {
      devLog('[SSE] 连接错误')
      stateInstance!.hasError = true
      stateInstance!.errorMessage = '连接失败，请稍后重试'
      closeConnection()
    }
  }

  /**
   * 处理 SSE 事件
   */
  function handleEvent(event: GraphProgressEvent): void {
    devLog('[SSE] 收到事件:', event)
    stateInstance!.isConnecting = false
    stateInstance!.isRunning = true

    if (event.event === 'start') {
      stateInstance!.currentStage = 'start'
      stateInstance!.progress = 5
      stateInstance!.message = event.message || '开始生成准备事项...'
      return
    }

    if (event.event === 'progress') {
      if (event.nodeId) {
        stateInstance!.currentStage = event.nodeId as PreparationState['currentStage']
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
      stateInstance!.message = event.message || '准备事项生成完成'
      if (event.data && typeof event.data === 'object') {
        const data = event.data as { preparationItems?: PreparationItem[] }
        if (data.preparationItems) {
          stateInstance!.preparationItems = data.preparationItems
        }
      }
      closeConnection()
      return
    }

    if (event.event === 'error') {
      stateInstance!.hasError = true
      stateInstance!.errorMessage = event.errorMessage || '生成失败'
      stateInstance!.isRunning = false
      closeConnection()
    }
  }

  /**
   * 关闭 SSE 连接
   */
  function closeConnection(): void {
    if (eventSource) {
      eventSource.close()
      eventSource = null
      devLog('[SSE] 连接已关闭')
    }
  }

  /**
   * 重置状态
   */
  function resetState(): void {
    closeConnection()
    Object.assign(stateInstance!, createInitialState())
  }

  return {
    state: stateInstance,
    startPreparation,
    resetState,
    closeConnection
  }
}
