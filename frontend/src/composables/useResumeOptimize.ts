// =====================================================
// LandIt 简历优化 Composable
// @author Azir
// =====================================================

import { reactive, computed, onUnmounted } from 'vue'
import type {
  OptimizeState,
  OptimizeProgressEvent,
  OptimizeMode,
  OptimizeStage,
  ResumeSection
} from '@/types/resume-optimize'
import { STAGE_CONFIG } from '@/types/resume-optimize'
import { applyOptimizeChanges } from '@/api/resume'
import type { SectionDataItem } from '@/api/resume'

const API_BASE = '/landit'

export function useResumeOptimize() {
  // 状态
  const state = reactive<OptimizeState>({
    isConnecting: false,
    isOptimizing: false,
    isCompleted: false,
    hasError: false,
    isApplying: false,
    applyError: null,
    threadId: null,
    resumeId: null,
    targetPosition: '',
    mode: 'quick',
    currentStage: 'start',
    progress: 0,
    message: '',
    errorMessage: null,
    stageHistory: []
  })

  // EventSource 实例
  let eventSource: EventSource | null = null

  // 计算属性
  const isOptimizing = computed(() => state.isOptimizing)
  const progress = computed(() => state.progress)
  const currentMessage = computed(() => state.message)

  // 使用统一的阶段配置
  const stageConfig = STAGE_CONFIG

  /**
   * 开始优化
   */
  function startOptimize(resumeId: string, options?: {
    mode?: OptimizeMode
    targetPosition?: string
  }) {
    // 重置状态
    resetState()

    state.resumeId = resumeId
    state.mode = options?.mode || 'quick'
    state.targetPosition = options?.targetPosition || ''
    state.isConnecting = true
    state.isOptimizing = true

    // 构建 URL
    const params = new URLSearchParams()
    params.append('mode', state.mode)
    if (state.targetPosition) {
      params.append('targetPosition', state.targetPosition)
    }

    const url = `${API_BASE}/resumes/${resumeId}/optimize/stream?${params.toString()}`

    console.log('[SSE] 连接URL:', url)

    // 创建 EventSource
    eventSource = new EventSource(url)

    // 连接成功
    eventSource.onopen = () => {
      state.isConnecting = false
      console.log('[SSE] 连接成功')
    }

    // 接收消息
    eventSource.onmessage = (event) => {
      try {
        const data: OptimizeProgressEvent = JSON.parse(event.data)
        handleEvent(data)
      } catch (e) {
        console.error('[SSE] 解析消息失败', e)
      }
    }

    // 错误处理
    eventSource.onerror = (error) => {
      console.error('[SSE] 连接错误', error)
      state.hasError = true
      state.errorMessage = '连接失败，请稍后重试'
      state.isOptimizing = false
      state.isConnecting = false
    }
  }

  /**
   * 处理 SSE 事件
   */
  function handleEvent(event: OptimizeProgressEvent) {
    console.log('[SSE] 事件:', event)

    // 更新通用状态
    if (event.threadId) state.threadId = event.threadId
    if (event.progress !== null) state.progress = event.progress
    if (event.message) state.message = event.message
    if (event.nodeId) state.currentStage = event.nodeId

    // 根据事件类型处理
    switch (event.event) {
      case 'start':
        handleStartEvent(event)
        break
      case 'progress':
        handleProgressEvent(event)
        break
      case 'complete':
        handleCompleteEvent(event)
        break
      case 'error':
        handleErrorEvent(event)
        break
    }
  }

  /**
   * 处理开始事件
   */
  function handleStartEvent(event: OptimizeProgressEvent) {
    state.isOptimizing = true
    state.progress = 0
    if (event.data?.targetPosition) {
      state.targetPosition = event.data.targetPosition
    }
  }

  /**
   * 处理进度事件
   * progress 事件表示节点正在运行，不是已完成
   * 节点完成时机：
   * 1. 下一个节点开始时（updateStageHistory 中处理）
   * 2. 整个工作流完成时（handleCompleteEvent 中处理）
   */
  function handleProgressEvent(event: OptimizeProgressEvent) {
    const nodeId = event.nodeId
    const data = event.data

    if (!nodeId || nodeId === 'start' || nodeId === 'end') return

    // 更新或添加阶段历史，completed: false 表示节点正在运行
    updateStageHistory(nodeId, event.message, event.timestamp, false, data)
  }

  /**
   * 处理完成事件
   */
  function handleCompleteEvent(_event: OptimizeProgressEvent) {
    state.isOptimizing = false
    state.isCompleted = true
    state.progress = 100
    state.currentStage = 'end'
    state.message = '优化完成'

    // 标记最后一个运行中节点的结束时间
    const now = Date.now()
    const runningStage = state.stageHistory.find(h => h.startTime && !h.endTime)
    if (runningStage) {
      runningStage.endTime = now
    }

    // 关闭连接
    closeConnection()
  }

  /**
   * 处理错误事件
   */
  function handleErrorEvent(event: OptimizeProgressEvent) {
    state.hasError = true
    state.errorMessage = event.message || '优化失败'
    state.isOptimizing = false

    // 标记当前运行中节点的结束时间
    const now = Date.now()
    const runningStage = state.stageHistory.find(h => h.startTime && !h.endTime)
    if (runningStage) {
      runningStage.endTime = now
    }

    // 关闭连接
    closeConnection()
  }

  /**
   * 更新阶段历史
   */
  function updateStageHistory(
    stage: OptimizeStage,
    message: string,
    timestamp: number,
    completed: boolean,
    data?: any
  ) {
    const now = Date.now()
    const existingIndex = state.stageHistory.findIndex(h => h.stage === stage)

    if (existingIndex >= 0) {
      // 更新现有记录
      state.stageHistory[existingIndex].message = message
      state.stageHistory[existingIndex].timestamp = timestamp
      state.stageHistory[existingIndex].completed = completed
      if (data) {
        state.stageHistory[existingIndex].data = data
      }
      // 节点完成时记录结束时间
      if (completed && !state.stageHistory[existingIndex].endTime) {
        state.stageHistory[existingIndex].endTime = now
      }
    } else {
      // 添加新记录，同时标记上一个运行中节点的结束时间
      const prevRunning = state.stageHistory.find(h => h.startTime && !h.endTime)
      if (prevRunning) {
        prevRunning.endTime = now
      }
      state.stageHistory.push({
        stage,
        message,
        timestamp,
        completed,
        data,
        expanded: false,
        startTime: now,
        endTime: completed ? now : undefined
      })
    }
  }

  /**
   * 切换阶段展开状态
   */
  function toggleStageExpanded(stage: OptimizeStage) {
    const item = state.stageHistory.find(h => h.stage === stage)
    if (item) {
      item.expanded = !item.expanded
    }
  }

  /**
   * 取消优化
   */
  function cancelOptimize() {
    closeConnection()
    state.isOptimizing = false
    state.message = '已取消'
  }

  /**
   * 重试优化
   */
  function retryOptimize() {
    if (!state.resumeId) {
      console.error('[SSE] 无法重试：缺少 resumeId')
      return
    }

    // 保存当前信息
    const resumeId = state.resumeId
    const mode = state.mode
    const targetPosition = state.targetPosition

    // 重新开始优化
    startOptimize(resumeId, { mode, targetPosition })
  }

  /**
   * 关闭连接
   */
  function closeConnection() {
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    state.isConnecting = false
  }

  /**
   * 重置状态
   */
  function resetState() {
    closeConnection()

    state.isConnecting = false
    state.isOptimizing = false
    state.isCompleted = false
    state.hasError = false
    state.isApplying = false
    state.applyError = null
    state.threadId = null
    state.resumeId = null
    state.targetPosition = ''
    state.currentStage = 'start'
    state.progress = 0
    state.message = ''
    state.errorMessage = null
    state.stageHistory = []
  }

  /**
   * 获取优化后的区块数据
   * 从 optimize_section 阶段的数据中提取 beforeSection 和 afterSection
   */
  function getOptimizedData(): { beforeSection: SectionDataItem[]; afterSection: SectionDataItem[] } | null {
    const optimizeStage = state.stageHistory.find(h => h.stage === 'optimize_section')
    if (!optimizeStage?.data) {
      return null
    }

    const { beforeSection, afterSection } = optimizeStage.data
    if (!beforeSection || !afterSection) {
      return null
    }

    // 转换为 API 需要的格式
    return {
      beforeSection: beforeSection.map((s: ResumeSection) => ({
        id: s.id,
        type: s.type,
        title: s.title,
        content: s.content || ''
      })),
      afterSection: afterSection.map((s: ResumeSection) => ({
        id: s.id,
        type: s.type,
        title: s.title,
        content: s.content || ''
      }))
    }
  }

  /**
   * 应用优化变更
   * 调用后端批量 API 更新所有区块
   */
  async function applyChanges(): Promise<boolean> {
    const optimizedData = getOptimizedData()
    if (!optimizedData || !state.resumeId) {
      console.error('[Optimize] 无法应用变更：缺少优化数据或简历ID')
      return false
    }

    state.isApplying = true
    state.applyError = null

    try {
      await applyOptimizeChanges(state.resumeId, optimizedData)
      console.log('[Optimize] 应用变更成功')
      return true
    } catch (error) {
      const message = error instanceof Error ? error.message : '应用变更失败'
      state.applyError = message
      console.error('[Optimize] 应用变更失败:', message)
      return false
    } finally {
      state.isApplying = false
    }
  }

  // 组件卸载时关闭连接
  onUnmounted(() => {
    closeConnection()
  })

  return {
    state,
    isOptimizing,
    progress,
    currentMessage,
    stageConfig,
    startOptimize,
    cancelOptimize,
    retryOptimize,
    resetState,
    toggleStageExpanded,
    getOptimizedData,
    applyChanges
  }
}
