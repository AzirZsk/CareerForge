// =====================================================
// LandIt 职位适配简历 Composable
// @author Azir
// =====================================================

import { reactive, computed, onUnmounted } from 'vue'
import type {
  TailorState,
  TailorProgressEvent,
  TailorStage,
  JobRequirements,
  MatchAnalysis,
  TailorResumeResponse
} from '@/types/resume-tailor'
import { TAILOR_STAGE_CONFIG } from '@/types/resume-tailor'

const API_BASE = '/landit'

export function useResumeTailor() {
  // 状态
  const state = reactive<TailorState>({
    isConnecting: false,
    isTailoring: false,
    isCompleted: false,
    hasError: false,
    threadId: null,
    resumeId: null,
    targetPosition: '',
    jobDescription: '',
    currentStage: 'start',
    progress: 0,
    message: '',
    errorMessage: null,
    jobRequirements: null,
    matchAnalysis: null,
    tailoredResume: null,
    stageHistory: []
  })

  // EventSource 实例
  let eventSource: EventSource | null = null

  // 计算属性
  const isTailoring = computed(() => state.isTailoring)
  const progress = computed(() => state.progress)
  const currentMessage = computed(() => state.message)

  // 使用统一的阶段配置
  const stageConfig = TAILOR_STAGE_CONFIG

  /**
   * 开始定制
   */
  function startTailor(resumeId: string, targetPosition: string, jobDescription: string) {
    // 重置状态
    resetState()

    state.resumeId = resumeId
    state.targetPosition = targetPosition
    state.jobDescription = jobDescription
    state.isConnecting = true
    state.isTailoring = true

    // 构建 URL
    const params = new URLSearchParams()
    params.append('targetPosition', targetPosition)
    params.append('jobDescription', jobDescription)

    const url = `${API_BASE}/resumes/${resumeId}/tailor/stream?${params.toString()}`

    console.log('[职位适配-SSE] 连接URL:', url)

    // 创建 EventSource
    eventSource = new EventSource(url)

    // 连接成功
    eventSource.onopen = () => {
      state.isConnecting = false
      console.log('[职位适配-SSE] 连接成功')
    }

    // 接收消息
    eventSource.onmessage = (event) => {
      try {
        const data: TailorProgressEvent = JSON.parse(event.data)
        handleEvent(data)
      } catch (e) {
        console.error('[职位适配-SSE] 解析消息失败', e)
      }
    }

    // 错误处理
    eventSource.onerror = (error) => {
      console.error('[职位适配-SSE] 连接错误', error)
      state.hasError = true
      state.errorMessage = '连接失败，请稍后重试'
      state.isTailoring = false
      state.isConnecting = false
    }
  }

  /**
   * 处理 SSE 事件
   */
  function handleEvent(event: TailorProgressEvent) {
    console.log('[职位适配-SSE] 事件:', event)

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
  function handleStartEvent(event: TailorProgressEvent) {
    state.isTailoring = true
    state.progress = 0
    if (event.data?.targetPosition) {
      state.targetPosition = event.data.targetPosition
    }
  }

  /**
   * 处理进度事件
   */
  function handleProgressEvent(event: TailorProgressEvent) {
    const nodeId = event.nodeId
    const data = event.data

    if (!nodeId || nodeId === 'start' || nodeId === 'end') return

    // 根据节点类型保存数据
    if (nodeId === 'analyze_jd' && data) {
      state.jobRequirements = data as JobRequirements
    } else if (nodeId === 'match_resume' && data) {
      state.matchAnalysis = data as MatchAnalysis
    } else if (nodeId === 'generate_tailored' && data) {
      state.tailoredResume = data as TailorResumeResponse
    }

    // 更新阶段历史
    updateStageHistory(nodeId, event.message, event.timestamp, true, data)
  }

  /**
   * 处理完成事件
   */
  function handleCompleteEvent(_event: TailorProgressEvent) {
    state.isTailoring = false
    state.isCompleted = true
    state.progress = 100
    state.currentStage = 'end'
    state.message = '定制完成'

    // 关闭连接
    closeConnection()
  }

  /**
   * 处理错误事件
   */
  function handleErrorEvent(event: TailorProgressEvent) {
    state.hasError = true
    state.errorMessage = event.message || '定制失败'
    state.isTailoring = false

    // 关闭连接
    closeConnection()
  }

  /**
   * 更新阶段历史
   */
  function updateStageHistory(
    stage: TailorStage,
    message: string,
    timestamp: number,
    completed: boolean,
    data?: any
  ) {
    const existingIndex = state.stageHistory.findIndex(h => h.stage === stage)

    if (existingIndex >= 0) {
      // 更新现有记录
      state.stageHistory[existingIndex].message = message
      state.stageHistory[existingIndex].timestamp = timestamp
      state.stageHistory[existingIndex].completed = completed
      if (data) {
        state.stageHistory[existingIndex].data = data
      }
    } else {
      // 添加新记录
      state.stageHistory.push({
        stage,
        message,
        timestamp,
        completed,
        data,
        expanded: false
      })
    }
  }

  /**
   * 切换阶段展开状态
   */
  function toggleStageExpanded(stage: TailorStage) {
    const item = state.stageHistory.find(h => h.stage === stage)
    if (item) {
      item.expanded = !item.expanded
    }
  }

  /**
   * 取消定制
   */
  function cancelTailor() {
    closeConnection()
    state.isTailoring = false
    state.message = '已取消'
  }

  /**
   * 重试定制
   */
  function retryTailor() {
    if (!state.resumeId) {
      console.error('[职位适配-SSE] 无法重试：缺少 resumeId')
      return
    }

    // 保存当前信息
    const resumeId = state.resumeId
    const targetPosition = state.targetPosition
    const jobDescription = state.jobDescription

    // 重新开始定制
    startTailor(resumeId, targetPosition, jobDescription)
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
    state.isTailoring = false
    state.isCompleted = false
    state.hasError = false
    state.threadId = null
    state.resumeId = null
    state.targetPosition = ''
    state.jobDescription = ''
    state.currentStage = 'start'
    state.progress = 0
    state.message = ''
    state.errorMessage = null
    state.jobRequirements = null
    state.matchAnalysis = null
    state.tailoredResume = null
    state.stageHistory = []
  }

  // 组件卸载时关闭连接
  onUnmounted(() => {
    closeConnection()
  })

  return {
    state,
    isTailoring,
    progress,
    currentMessage,
    stageConfig,
    startTailor,
    cancelTailor,
    retryTailor,
    resetState,
    toggleStageExpanded
  }
}
