// =====================================================
// 复盘分析工作流 Composable
// @author Azir
// =====================================================

import { reactive } from 'vue'
import type {
  ReviewAnalysisState,
  ReviewStage,
  GraphProgressEvent,
  TranscriptAnalysisResult,
  InterviewAnalysisResult,
  AdviceItem
} from '@/types/interview-center'
import { streamReviewAnalysis } from '@/api/interview-center'

// 单例状态
let stateInstance: ReviewAnalysisState | null = null

/** 内部节点 ID 类型（包含工作流控制节点） */
type InternalNodeId = ReviewStage | '__START__' | '__END__' | 'start' | 'end'

/**
 * 节点顺序映射：当前节点完成时，下一个要开始计时的节点
 * __START__ 完成 → analyze_transcript 开始
 * analyze_transcript 完成 → analyze_interview 开始
 * analyze_interview 完成 → generate_advice 开始
 * generate_advice 完成 → 无下一个节点（等待 __END__）
 */
const NEXT_STAGE_MAP: Record<string, ReviewStage | null> = {
  '__START__': 'analyze_transcript',
  'analyze_transcript': 'analyze_interview',
  'analyze_interview': 'generate_advice',
  'generate_advice': null,
  '__END__': null,
  'start': null,
  'end': null
}

// 初始状态工厂
function createInitialState(): ReviewAnalysisState {
  return {
    isConnecting: false,
    isRunning: false,
    isCompleted: false,
    hasError: false,
    currentStage: '',
    progress: 0,
    message: '',
    adviceList: [],
    errorMessage: null,
    stageHistory: []
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
 * 使用 fetch + ReadableStream 流式获取 AI 复盘分析结果
 */
export function useReviewAnalysis() {
  if (!stateInstance) {
    stateInstance = reactive<ReviewAnalysisState>(createInitialState())
  }

  /**
   * 开始某个节点的计时
   */
  function startStageTimer(stage: ReviewStage, now: number): void {
    const existingIndex = stateInstance!.stageHistory.findIndex(h => h.stage === stage)
    if (existingIndex >= 0) {
      stateInstance!.stageHistory[existingIndex].startTime = now
      stateInstance!.stageHistory[existingIndex].endTime = undefined
      stateInstance!.stageHistory[existingIndex].completed = false
    } else {
      stateInstance!.stageHistory.push({
        stage,
        message: '',
        timestamp: now,
        startTime: now,
        completed: false,
        endTime: undefined,
        expanded: false,
        data: null
      })
    }
  }

  /**
   * 结束当前运行中节点的计时
   */
  function endRunningStage(now: number): void {
    const runningStage = stateInstance!.stageHistory.find(h => h.startTime && !h.endTime)
    if (runningStage) {
      runningStage.endTime = now
      runningStage.completed = true
    }
  }

  /**
   * 更新节点数据（不改变计时状态）
   */
  function updateStageData(
    stage: ReviewStage,
    data: TranscriptAnalysisResult | InterviewAnalysisResult | AdviceItem[] | null,
    message?: string
  ): void {
    const item = stateInstance!.stageHistory.find(h => h.stage === stage)
    if (item) {
      if (data !== undefined && data !== null) item.data = data
      if (message) item.message = message
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
      stateInstance!.message = event.message || '开始分析面试表现...'
      devLog('[SSE] 工作流开始')
      return
    }

    if (event.event === 'progress') {
      const nodeId = event.nodeId as InternalNodeId | null
      const data = event.data
      const now = Date.now()
      const isCached = event.cached === true

      if (!nodeId) {
        stateInstance!.progress = event.progress || stateInstance!.progress
        stateInstance!.message = event.message || stateInstance!.message
        return
      }

      // 处理 __START__: 开始 analyze_transcript 计时
      if (nodeId === '__START__') {
        startStageTimer('analyze_transcript', now)
        stateInstance!.currentStage = 'analyze_transcript'
        stateInstance!.progress = event.progress || stateInstance!.progress
        stateInstance!.message = event.message || stateInstance!.message
        devLog('[SSE] __START__ 完成，开始 analyze_transcript 计时')
        return
      }

      // 处理 __END__: 结束最后一个运行中的节点
      if (nodeId === '__END__') {
        endRunningStage(now)
        stateInstance!.currentStage = 'end'
        devLog('[SSE] __END__ 收到，结束所有节点')
        return
      }

      // 处理业务节点完成事件：结束当前节点，开始下一个节点
      if (nodeId in NEXT_STAGE_MAP) {
        const nextStage = NEXT_STAGE_MAP[nodeId]

        // 结束当前运行中的节点
        endRunningStage(now)

        // 根据阶段类型解析数据
        let parsedData: TranscriptAnalysisResult | InterviewAnalysisResult | AdviceItem[] | null = null
        if (data) {
          if (nodeId === 'analyze_transcript') {
            parsedData = data as unknown as TranscriptAnalysisResult
          } else if (nodeId === 'analyze_interview') {
            parsedData = data as unknown as InterviewAnalysisResult
          } else if (nodeId === 'generate_advice') {
            const adviceData = data as unknown
            if (Array.isArray(adviceData)) {
              parsedData = adviceData as AdviceItem[]
            }
            // 额外写入 adviceList 供 AIAnalysisCard 使用
            stateInstance!.adviceList = parsedData || []
          }
        }

        // 更新已完成节点的数据
        updateStageData(nodeId as ReviewStage, parsedData, event.message)

        // 标记 cached 节点
        if (isCached) {
          const item = stateInstance!.stageHistory.find(h => h.stage === nodeId)
          if (item) {
            item.cached = true
            item.completed = true
            item.endTime = now
          }
        }

        // 开始下一个节点的计时（如果有）
        if (nextStage) {
          startStageTimer(nextStage, now)
          stateInstance!.currentStage = nextStage
          devLog(`[SSE] ${nodeId} 完成，开始 ${nextStage} 计时`)
        }
      }

      stateInstance!.progress = event.progress || stateInstance!.progress
      stateInstance!.message = event.message || stateInstance!.message
      return
    }

    if (event.event === 'complete') {
      const now = Date.now()
      stateInstance!.isRunning = false
      stateInstance!.isCompleted = true
      stateInstance!.progress = 100
      stateInstance!.message = event.message || '复盘分析完成'

      // 标记所有阶段完成，使用前端本地时间
      stateInstance!.stageHistory.forEach(h => {
        h.completed = true
        if (!h.endTime) {
          h.endTime = now
        }
      })

      // 兜底：如果 complete 事件带有 adviceList 数据
      if (event.data && !stateInstance!.adviceList.length) {
        const adviceData = event.data as unknown
        if (Array.isArray(adviceData)) {
          stateInstance!.adviceList = adviceData as AdviceItem[]
        }
      }

      devLog('[SSE] 工作流完成')
      return
    }

    if (event.event === 'error') {
      stateInstance!.hasError = true
      stateInstance!.errorMessage = event.errorMessage || '分析失败'
      stateInstance!.isRunning = false
      devLog('[SSE] 工作流出错:', event.errorMessage)
    }
  }

  /**
   * 开始复盘分析
   */
  async function startAnalysis(interviewId: string, sessionTranscript: string) {
    resetState()
    stateInstance!.isConnecting = true
    stateInstance!.message = '正在连接...'

    try {
      await streamReviewAnalysis(
        interviewId,
        sessionTranscript,
        (event: GraphProgressEvent) => {
          handleEvent(event)
        },
        (error: Error) => {
          stateInstance!.isConnecting = false
          stateInstance!.isRunning = false
          stateInstance!.hasError = true
          stateInstance!.errorMessage = error.message
          stateInstance!.message = `分析失败: ${error.message}`
        },
        () => {
          // 流结束回调（兜底）
          stateInstance!.isRunning = false
          stateInstance!.isCompleted = true
          stateInstance!.progress = 100
          stateInstance!.message = '分析完成'
        }
      )
    } catch (error) {
      stateInstance!.isConnecting = false
      stateInstance!.isRunning = false
      stateInstance!.hasError = true
      stateInstance!.errorMessage = error instanceof Error ? error.message : '分析失败'
      stateInstance!.message = `分析失败: ${stateInstance!.errorMessage}`
    }
  }

  /**
   * 切换阶段展开状态
   */
  function toggleStageExpand(stage: ReviewStage) {
    const historyItem = stateInstance!.stageHistory.find(h => h.stage === stage)
    if (historyItem) {
      historyItem.expanded = !historyItem.expanded
    }
  }

  /**
   * 重置状态
   */
  function resetState() {
    Object.assign(stateInstance!, createInitialState())
  }

  return {
    state: stateInstance,
    startAnalysis,
    resetState,
    toggleStageExpand
  }
}
