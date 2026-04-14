// =====================================================
// CareerForge 简历优化 Composable
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

/** 内部节点 ID 类型（包含工作流控制节点） */
type InternalNodeId = OptimizeStage | '__START__' | '__END__'
import { STAGE_CONFIG } from '@/types/resume-optimize'
import { applyOptimizeChanges } from '@/api/resume'
import type { SectionDataItem } from '@/api/resume'
import { API_BASE } from '@/api/config'
import { authFetch } from '@/utils/request'
import { usePageGuard } from './usePageGuard'

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

  // AbortController 用于中断请求
  let abortController: AbortController | null = null

  const { registerGuard, unregisterGuard } = usePageGuard()

  // 计算属性
  const isOptimizing = computed(() => state.isOptimizing)
  const progress = computed(() => state.progress)
  const currentMessage = computed(() => state.message)

  // 使用统一的阶段配置
  const stageConfig = STAGE_CONFIG

  /**
   * 开始优化
   */
  async function startOptimize(resumeId: string, options?: {
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
    registerGuard('optimize')

    // 创建 AbortController
    abortController = new AbortController()

    // 构建 URL
    const params = new URLSearchParams()
    params.append('mode', state.mode)
    if (state.targetPosition) {
      params.append('targetPosition', state.targetPosition)
    }

    const url = `${API_BASE}/resumes/${resumeId}/optimize/stream?${params.toString()}`

    console.log('[SSE] 连接URL:', url)

    try {
      // 使用 authFetch 发起请求
      const response = await authFetch(url, {}, 120000) // 2分钟超时

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      state.isConnecting = false
      console.log('[SSE] 连接成功')

      // 获取 ReadableStream
      const reader = response.body?.getReader()
      if (!reader) {
        throw new Error('无法读取响应流')
      }

      const decoder = new TextDecoder()
      let buffer = ''

      // 读取流
      while (state.isOptimizing && !state.isCompleted && !state.hasError) {
        const { done, value } = await reader.read()

        if (done) {
          console.log('[SSE] 流结束')
          break
        }

        // 解码并处理数据
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          const trimmed = line.trim()
          if (!trimmed) continue
          // 兼容标准 SSE 格式（event:\ndata:{...}）和 NDJSON 格式（纯 JSON）
          if (trimmed.startsWith('event:')) continue
          try {
            const jsonStr = trimmed.startsWith('data:') ? trimmed.slice(5).trim() : trimmed
            if (jsonStr) {
              const data: OptimizeProgressEvent = JSON.parse(jsonStr)
              handleEvent(data)
            }
          } catch (e) {
            console.error('[SSE] 解析消息失败', e, trimmed)
          }
        }
      }

      reader.releaseLock()
    } catch (error) {
      console.error('[SSE] 请求失败', error)
      state.hasError = true
      state.errorMessage = error instanceof Error ? error.message : '连接失败，请稍后重试'
      state.isOptimizing = false
      unregisterGuard('optimize')
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
   * 节点顺序映射：当前节点完成时，下一个要开始计时的节点
   * __START__ 完成 → diagnose_quick 开始
   * diagnose_quick 完成 → generate_suggestions 开始
   * generate_suggestions 完成 → optimize_section 开始
   * optimize_section 完成 → 无下一个节点（等待 __END__）
   */
  const NEXT_STAGE_MAP: Record<InternalNodeId, OptimizeStage | null> = {
    '__START__': 'diagnose_quick',
    'diagnose_quick': 'generate_suggestions',
    'generate_suggestions': 'optimize_section',
    'optimize_section': null,  // 最后一个节点，完成后没有下一个
    '__END__': null,
    'start': null,
    'diagnose_precise': null,
    'human_review': null,
    'end': null
  }

  /**
   * 开始某个节点的计时
   */
  function startStageTimer(stage: OptimizeStage, now: number) {
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
  function updateStageData(stage: OptimizeStage, data: any, message?: string) {
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
   * 1. nodeId="__START__" → diagnose_quick 开始计时
   * 2. nodeId="diagnose_quick" → diagnose_quick 结束，generate_suggestions 开始
   * 3. nodeId="generate_suggestions" → generate_suggestions 结束，optimize_section 开始
   * 4. nodeId="__END__" → optimize_section 结束
   */
  function handleProgressEvent(event: OptimizeProgressEvent) {
    const nodeId = event.nodeId as InternalNodeId | null
    const data = event.data
    const now = Date.now()

    if (!nodeId) return

    // 处理 __START__: 开始 diagnose_quick 计时
    if (nodeId === '__START__') {
      startStageTimer('diagnose_quick', now)
      state.currentStage = 'diagnose_quick'
      return
    }

    // 处理 __END__: 结束 optimize_section 计时
    if (nodeId === '__END__') {
      endRunningStage(now)
      state.currentStage = 'end'
      return
    }

    // 处理节点完成事件：结束当前节点，开始下一个节点
    if (nodeId in NEXT_STAGE_MAP) {
      const nextStage = NEXT_STAGE_MAP[nodeId]

      // 结束当前运行中的节点
      endRunningStage(now)
      // 更新已完成节点的数据
      updateStageData(nodeId as OptimizeStage, data, event.message)

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
  function handleCompleteEvent(_event: OptimizeProgressEvent) {
    state.isOptimizing = false
    unregisterGuard('optimize')
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
    unregisterGuard('optimize')

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
    unregisterGuard('optimize')
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
    if (abortController) {
      abortController.abort()
      abortController = null
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
    unregisterGuard('optimize')
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
   * @param editedAfterSection 编辑后的 afterSection 数据（优先使用）
   */
  function getOptimizedData(editedAfterSection?: ResumeSection[]): { beforeSection: SectionDataItem[]; afterSection: SectionDataItem[] } | null {
    const optimizeStage = state.stageHistory.find(h => h.stage === 'optimize_section')
    if (!optimizeStage?.data) {
      return null
    }

    const { beforeSection, afterSection } = optimizeStage.data
    if (!beforeSection || !afterSection) {
      return null
    }

    // 优先使用传入的编辑后数据，否则使用原始数据
    const finalAfterSection = editedAfterSection || afterSection

    // 转换为 API 需要的格式
    return {
      beforeSection: beforeSection.map((s: ResumeSection) => ({
        id: s.id,
        type: s.type,
        title: s.title,
        content: s.content || ''
      })),
      afterSection: finalAfterSection.map((s: ResumeSection) => ({
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
   * @param editedAfterSection 编辑后的 afterSection 数据（优先使用）
   */
  async function applyChanges(editedAfterSection?: ResumeSection[]): Promise<boolean> {
    const optimizedData = getOptimizedData(editedAfterSection)
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
