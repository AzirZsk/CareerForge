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
import { API_BASE } from '@/api/config'

/** 内部节点 ID 类型（包含工作流控制节点） */
type InternalNodeId = TailorStage | '__START__' | '__END__'

/**
 * 节点顺序映射：当前节点完成时，下一个要开始计时的节点
 * __START__ 完成 → analyze_jd 开始
 * analyze_jd 完成 → match_resume 开始
 * match_resume 完成 → generate_tailored 开始
 * generate_tailored 完成 → 无下一个节点（等待 __END__）
 */
const NEXT_STAGE_MAP: Record<string, TailorStage | null> = {
  '__START__': 'analyze_jd',
  'analyze_jd': 'match_resume',
  'match_resume': 'generate_tailored',
  'generate_tailored': null
}

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

    // 获取 token（EventSource 不支持自定义 Header，只能通过 URL 参数传递）
    const token = localStorage.getItem('token')
    if (token) {
      params.append('token', token)
    }

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
   * 开始某个节点的计时
   */
  function startStageTimer(stage: TailorStage, now: number) {
    const existingIndex = state.stageHistory.findIndex(h => h.stage === stage)
    if (existingIndex >= 0) {
      // 已存在，只设置开始时间
      state.stageHistory[existingIndex].startTime = now
      state.stageHistory[existingIndex].endTime = undefined
    } else {
      // 不存在，创建新记录
      state.stageHistory.push({
        stage,
        message: '',
        timestamp: now,
        completed: false,
        expanded: false,
        startTime: now,
        endTime: undefined
      })
    }
  }

  /**
   * 结束当前运行中节点的计时
   */
  function endRunningStage(now: number) {
    const runningStage = state.stageHistory.find(h => h.startTime && !h.endTime)
    if (runningStage) {
      runningStage.endTime = now
      runningStage.completed = true
    }
  }

  /**
   * 更新节点数据（不改变计时状态）
   */
  function updateStageData(stage: TailorStage, data: any, message?: string) {
    const item = state.stageHistory.find(h => h.stage === stage)
    if (item) {
      if (data) item.data = data
      if (message) item.message = message
    }
  }

  /**
   * 处理进度事件
   *
   * SSE 事件时序：
   * 1. nodeId="__START__" → analyze_jd 开始计时
   * 2. nodeId="analyze_jd" → analyze_jd 结束，match_resume 开始
   * 3. nodeId="match_resume" → match_resume 结束，generate_tailored 开始
   * 4. nodeId="__END__" → generate_tailored 结束
   */
  function handleProgressEvent(event: TailorProgressEvent) {
    const nodeId = event.nodeId as InternalNodeId | null
    const data = event.data
    const now = Date.now()

    if (!nodeId) return

    // 处理 __START__: 开始 analyze_jd 计时
    if (nodeId === '__START__') {
      startStageTimer('analyze_jd', now)
      state.currentStage = 'analyze_jd'
      return
    }

    // 处理 __END__: 结束 generate_tailored 计时
    if (nodeId === '__END__') {
      endRunningStage(now)
      state.currentStage = 'end'
      return
    }

    // 跳过 start 和 end 节点
    if (nodeId === 'start' || nodeId === 'end') return

    // 处理节点完成事件：结束当前节点，开始下一个节点
    if (nodeId in NEXT_STAGE_MAP) {
      const nextStage = NEXT_STAGE_MAP[nodeId]

      // 结束当前运行中的节点
      endRunningStage(now)
      // 更新已完成节点的数据
      updateStageData(nodeId as TailorStage, data, event.message)

      // 根据节点类型保存数据
      if (nodeId === 'analyze_jd' && data) {
        state.jobRequirements = data as JobRequirements
      } else if (nodeId === 'match_resume' && data) {
        state.matchAnalysis = data as MatchAnalysis
      } else if (nodeId === 'generate_tailored' && data) {
        state.tailoredResume = data as TailorResumeResponse
      }

      // 开始下一个节点的计时（如果有）
      if (nextStage) {
        startStageTimer(nextStage, now)
        state.currentStage = nextStage
      }
    }
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
  function handleErrorEvent(event: TailorProgressEvent) {
    state.hasError = true
    state.errorMessage = event.message || '定制失败'
    state.isTailoring = false

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
