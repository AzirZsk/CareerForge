// =====================================================
// LandIt 简历风格改写 Composable
// @author Azir
// =====================================================

import { reactive, computed, onUnmounted } from 'vue'
import type {
  RewriteState,
  RewriteProgressEvent,
  RewriteStage,
  ResumeSection
} from '@/types/resume-rewrite'
import { REWRITE_STAGE_CONFIG } from '@/types/resume-rewrite'
import { applyOptimizeChanges, createRewriteResumeStream } from '@/api/resume'
import type { SectionDataItem } from '@/api/resume'

/** 内部节点 ID 类型（包含工作流控制节点） */
type InternalNodeId = RewriteStage | '__START__' | '__END__'

export function useResumeRewrite() {
  // 状态
  const state = reactive<RewriteState>({
    isConnecting: false,
    isRewriting: false,
    isCompleted: false,
    hasError: false,
    threadId: null,
    resumeId: null,
    tempKey: null,
    currentStage: 'start',
    progress: 0,
    message: '',
    errorMessage: null,
    stageHistory: [],
    referenceFileName: null
  })

  // EventSource 实例
  let eventSource: EventSource | null = null

  // 计算属性
  const isRewriting = computed(() => state.isRewriting)
  const progress = computed(() => state.progress)
  const currentMessage = computed(() => state.message)

  // 使用统一的阶段配置
  const stageConfig = REWRITE_STAGE_CONFIG

  /**
   * 开始风格改写
   * @param resumeId 简历ID
   * @param tempKey 参考简历缓存key
   */
  function startRewrite(resumeId: string, tempKey: string) {
    // 重置状态
    resetState()

    state.resumeId = resumeId
    state.tempKey = tempKey
    state.isConnecting = true
    state.isRewriting = true

    // 使用 API 模块创建 EventSource
    eventSource = createRewriteResumeStream(resumeId, tempKey)

    console.log('[Rewrite-SSE] 连接已创建')

    // 连接成功
    eventSource.onopen = () => {
      state.isConnecting = false
      console.log('[Rewrite-SSE] 连接成功')
    }

    // 接收消息
    eventSource.onmessage = (event) => {
      try {
        const data: RewriteProgressEvent = JSON.parse(event.data)
        handleEvent(data)
      } catch (e) {
        console.error('[Rewrite-SSE] 解析消息失败', e)
      }
    }

    // 错误处理
    eventSource.onerror = (error) => {
      console.error('[Rewrite-SSE] 连接错误', error)
      state.hasError = true
      state.errorMessage = '连接失败，请稍后重试'
      state.isRewriting = false
      state.isConnecting = false
    }
  }

  /**
   * 处理 SSE 事件
   */
  function handleEvent(event: RewriteProgressEvent) {
    console.log('[Rewrite-SSE] 事件:', event)

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
  function handleStartEvent(_event: RewriteProgressEvent) {
    state.isRewriting = true
    state.progress = 0
  }

  /**
   * 节点顺序映射
   * __START__ 完成 → analyze_style 开始计时
   * analyze_style 完成 → generate_style_diff 开始
   * generate_style_diff 完成 → rewrite_section 开始
   * rewrite_section 完成 → 无下一个节点（等待 __END__）
   */
  const NEXT_STAGE_MAP: Record<InternalNodeId, RewriteStage | null> = {
    '__START__': 'analyze_style',
    'analyze_style': 'generate_style_diff',
    'generate_style_diff': 'rewrite_section',
    'rewrite_section': null,
    '__END__': null,
    'start': null,
    'end': null
  }

  /**
   * 开始某个节点的计时
   */
  function startStageTimer(stage: RewriteStage, now: number) {
    const existingIndex = state.stageHistory.findIndex(h => h.stage === stage)
    if (existingIndex >= 0) {
      state.stageHistory[existingIndex].startTime = now
      state.stageHistory[existingIndex].endTime = undefined
    } else {
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
   * 更新节点数据
   */
  function updateStageData(stage: RewriteStage, data: any, message?: string) {
    const item = state.stageHistory.find(h => h.stage === stage)
    if (item) {
      if (data) item.data = data
      if (message) item.message = message
    }
  }

  /**
   * 处理进度事件
   */
  function handleProgressEvent(event: RewriteProgressEvent) {
    const nodeId = event.nodeId as InternalNodeId | null
    const data = event.data
    const now = Date.now()

    if (!nodeId) return

    // 处理 __START__: 开始 analyze_style 计时
    if (nodeId === '__START__') {
      startStageTimer('analyze_style', now)
      state.currentStage = 'analyze_style'
      return
    }

    // 处理 __END__: 结束 rewrite_section 计时
    if (nodeId === '__END__') {
      endRunningStage(now)
      state.currentStage = 'end'
      return
    }

    // 处理节点完成事件
    if (nodeId in NEXT_STAGE_MAP) {
      const nextStage = NEXT_STAGE_MAP[nodeId]

      // 结束当前运行中的节点
      endRunningStage(now)
      // 更新已完成节点的数据
      updateStageData(nodeId as RewriteStage, data, event.message)

      // 开始下一个节点的计时
      if (nextStage) {
        startStageTimer(nextStage, now)
        state.currentStage = nextStage
      }
    }
  }

  /**
   * 处理完成事件
   */
  function handleCompleteEvent(_event: RewriteProgressEvent) {
    state.isRewriting = false
    state.isCompleted = true
    state.progress = 100
    state.currentStage = 'end'
    state.message = '风格改写完成'

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
  function handleErrorEvent(event: RewriteProgressEvent) {
    state.hasError = true
    state.errorMessage = event.message || '风格改写失败'
    state.isRewriting = false

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
  function toggleStageExpanded(stage: RewriteStage) {
    const item = state.stageHistory.find(h => h.stage === stage)
    if (item) {
      item.expanded = !item.expanded
    }
  }

  /**
   * 取消改写
   */
  function cancelRewrite() {
    closeConnection()
    state.isRewriting = false
    state.message = '已取消'
  }

  /**
   * 重试改写
   */
  function retryRewrite() {
    if (!state.resumeId || !state.tempKey) {
      console.error('[Rewrite-SSE] 无法重试：缺少 resumeId 或 tempKey')
      return
    }

    // 重新开始改写
    startRewrite(state.resumeId, state.tempKey)
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
    state.isRewriting = false
    state.isCompleted = false
    state.hasError = false
    state.threadId = null
    state.resumeId = null
    state.tempKey = null
    state.currentStage = 'start'
    state.progress = 0
    state.message = ''
    state.errorMessage = null
    state.stageHistory = []
    state.referenceFileName = null
  }

  /**
   * 获取改写后的区块数据
   */
  function getRewrittenData(editedAfterSection?: ResumeSection[]): { beforeSection: SectionDataItem[]; afterSection: SectionDataItem[] } | null {
    const rewriteStage = state.stageHistory.find(h => h.stage === 'rewrite_section')
    if (!rewriteStage?.data) {
      return null
    }

    const { beforeSection, afterSection } = rewriteStage.data
    if (!beforeSection || !afterSection) {
      return null
    }

    // 优先使用传入的编辑后数据
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
   * 应用改写变更
   */
  async function applyChanges(editedAfterSection?: ResumeSection[]): Promise<boolean> {
    const rewrittenData = getRewrittenData(editedAfterSection)
    if (!rewrittenData || !state.resumeId) {
      console.error('[Rewrite] 无法应用变更：缺少改写数据或简历ID')
      return false
    }

    try {
      await applyOptimizeChanges(state.resumeId, rewrittenData)
      console.log('[Rewrite] 应用变更成功')
      return true
    } catch (error) {
      const message = error instanceof Error ? error.message : '应用变更失败'
      state.errorMessage = message
      console.error('[Rewrite] 应用变更失败:', message)
      return false
    }
  }

  // 组件卸载时关闭连接
  onUnmounted(() => {
    closeConnection()
  })

  return {
    state,
    isRewriting,
    progress,
    currentMessage,
    stageConfig,
    startRewrite,
    cancelRewrite,
    retryRewrite,
    resetState,
    toggleStageExpanded,
    getRewrittenData,
    applyChanges
  }
}
