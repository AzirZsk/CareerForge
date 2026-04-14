// =====================================================
// 面试准备工作流 Composable
// @author Azir
// =====================================================

import { reactive } from 'vue'
import type {
  PreparationState,
  GraphProgressEvent,
  PreparationItem,
  PreparationStage,
  CompanyResearchResult,
  JDAnalysisResult
} from '@/types/interview-center'
import { authFetch } from '@/utils/request'
import { usePageGuard } from './usePageGuard'

// 单例状态
let stateInstance: PreparationState | null = null

/** 内部节点 ID 类型（包含工作流控制节点） */
type InternalNodeId = PreparationStage | '__START__' | '__END__' | 'start' | 'end'

/**
 * 节点顺序映射：当前节点完成时，下一个要开始计时的节点
 * __START__ 完成 → company_research 开始
 * company_research 完成 → jd_analysis 开始
 * jd_analysis 完成 → generate_preparation 开始
 * generate_preparation 完成 → 无下一个节点（等待 __END__）
 */
const NEXT_STAGE_MAP: Record<string, PreparationStage | null> = {
  '__START__': 'company_research',
  'company_research': 'jd_analysis',
  'jd_analysis': 'generate_preparation',
  'generate_preparation': null,
  '__END__': null,
  // 以下为前端内部状态，不参与映射
  'start': null,
  'check_company': null,
  'check_job_position': null,
  'end': null
}

// 阶段显示配置
const STAGE_CONFIG: Record<string, { label: string; order: number }> = {
  'start': { label: '开始', order: 0 },
  'check_company': { label: '检查公司信息', order: 1 },
  'company_research': { label: '公司调研', order: 2 },
  'check_job_position': { label: '检查职位信息', order: 3 },
  'jd_analysis': { label: 'JD 分析', order: 4 },
  'generate_preparation': { label: '生成准备事项', order: 5 },
  'end': { label: '完成', order: 6 }
}

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
    errorMessage: null,
    stageHistory: [],
    workflowStartTime: undefined
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

  const { registerGuard, unregisterGuard } = usePageGuard()

  let abortController: AbortController | null = null

  /**
   * 开始执行面试准备工作流
   */
  async function startPreparation(interviewId: string): Promise<void> {
    resetState()
    stateInstance!.isConnecting = true
    stateInstance!.message = '正在连接...'

    // 创建 AbortController
    abortController = new AbortController()

    const url = `/careerforge/interview-center/${interviewId}/preparation/stream`

    try {
      // 使用 authFetch 发起请求
      const response = await authFetch(url, {}, 120000) // 2分钟超时

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      stateInstance!.isConnecting = false
      stateInstance!.isRunning = true
      registerGuard('preparation')
      devLog('[SSE] 连接已建立')

      // 获取 ReadableStream
      const reader = response.body?.getReader()
      if (!reader) {
        throw new Error('无法读取响应流')
      }

      const decoder = new TextDecoder()
      let buffer = ''

      // 读取流
      while (stateInstance!.isRunning && !stateInstance!.isCompleted && !stateInstance!.hasError) {
        const { done, value } = await reader.read()

        if (done) {
          devLog('[SSE] 流结束')
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
              const data: GraphProgressEvent = JSON.parse(jsonStr)
              handleEvent(data)
            }
          } catch (e) {
            devLog('[SSE] 解析事件失败:', e, trimmed)
          }
        }
      }

      reader.releaseLock()
    } catch (error) {
      devLog('[SSE] 连接错误', error)
      stateInstance!.hasError = true
      stateInstance!.errorMessage = error instanceof Error ? error.message : '连接失败，请稍后重试'
      closeConnection()
    }
  }

  /**
   * 开始某个节点的计时
   */
  function startStageTimer(stage: PreparationStage, now: number): void {
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
    stage: PreparationStage,
    data: CompanyResearchResult | JDAnalysisResult | PreparationItem[] | null,
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
      // 使用前端本地时间，确保计时准确
      const now = Date.now()
      stateInstance!.currentStage = 'start'
      stateInstance!.progress = 5
      stateInstance!.message = event.message || '开始生成准备事项...'
      // 记录工作流开始时间（使用前端本地时间）
      stateInstance!.workflowStartTime = now
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

      // 处理 __START__: 开始 company_research 计时
      if (nodeId === '__START__') {
        startStageTimer('company_research', now)
        stateInstance!.currentStage = 'company_research'
        stateInstance!.progress = event.progress || stateInstance!.progress
        stateInstance!.message = event.message || stateInstance!.message
        devLog('[SSE] __START__ 完成，开始 company_research 计时')
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
        let parsedData: CompanyResearchResult | JDAnalysisResult | PreparationItem[] | null = null
        if (data) {
          if (nodeId === 'company_research' && data.companyResearch) {
            parsedData = data.companyResearch as CompanyResearchResult
          } else if (nodeId === 'jd_analysis' && data.jdAnalysis) {
            parsedData = data.jdAnalysis as JDAnalysisResult
          } else if (nodeId === 'generate_preparation') {
            if (data.preparationItems) {
              parsedData = data.preparationItems as PreparationItem[]
            } else if (Array.isArray(data)) {
              parsedData = data as PreparationItem[]
            }
          }
        }

        // 更新已完成节点的数据
        updateStageData(nodeId as PreparationStage, parsedData, event.message)

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
      // 使用前端本地时间
      const now = Date.now()
      stateInstance!.isRunning = false
      unregisterGuard('preparation')
      stateInstance!.isCompleted = true
      stateInstance!.currentStage = 'end'
      stateInstance!.progress = 100
      stateInstance!.message = event.message || '准备事项生成完成'

      // 标记所有阶段完成，使用前端本地时间
      stateInstance!.stageHistory.forEach(h => {
        h.completed = true
        if (!h.endTime) {
          h.endTime = now
        }
      })

      // 不再重复存储 preparationItems 到顶层状态
      // 数据已在 progress 事件中存储到 stageHistory，通过展开节点查看

      closeConnection()
      return
    }

    if (event.event === 'error') {
      stateInstance!.hasError = true
      stateInstance!.errorMessage = event.errorMessage || '生成失败'
      stateInstance!.isRunning = false
      unregisterGuard('preparation')
      closeConnection()
    }
  }

  /**
   * 切换阶段展开状态
   */
  function toggleExpand(stage: PreparationStage): void {
    const item = stateInstance!.stageHistory.find(h => h.stage === stage)
    if (item) {
      item.expanded = !item.expanded
    }
  }

  /**
   * 获取阶段标签
   */
  function getStageLabel(stage: string): string {
    return STAGE_CONFIG[stage]?.label || stage
  }

  /**
   * 关闭 SSE 连接
   */
  function closeConnection(): void {
    if (abortController) {
      abortController.abort()
      abortController = null
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
    closeConnection,
    toggleExpand,
    getStageLabel,
    STAGE_CONFIG
  }
}
