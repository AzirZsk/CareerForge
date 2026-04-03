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

// 单例状态
let stateInstance: PreparationState | null = null

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
      // 记录工作流开始时间（使用后端时间戳）
      stateInstance!.workflowStartTime = event.timestamp
      return
    }

    if (event.event === 'progress') {
      if (event.nodeId) {
        const newStage = event.nodeId as PreparationStage
        const prevStage = stateInstance!.currentStage
        const isCached = event.cached === true

        // 阶段切换时记录历史
        if (newStage !== prevStage) {
          // 标记上一阶段完成，使用当前事件的后端时间戳作为 endTime
          const prevHistoryItem = stateInstance!.stageHistory.find(h => h.stage === prevStage)
          if (prevHistoryItem && !prevHistoryItem.completed) {
            prevHistoryItem.completed = true
            prevHistoryItem.endTime = event.timestamp  // 使用后端时间戳
          }

          // 添加新阶段到历史
          const existingIndex = stateInstance!.stageHistory.findIndex(h => h.stage === newStage)
          if (existingIndex === -1) {
            // 计算阶段开始时间：上一阶段的 endTime 或工作流开始时间
            const stageStartTime = prevHistoryItem?.endTime || stateInstance!.workflowStartTime || event.timestamp

            stateInstance!.stageHistory.push({
              stage: newStage,
              message: event.message || '',
              timestamp: event.timestamp,  // 使用后端时间戳
              startTime: stageStartTime,   // 使用计算出的开始时间
              completed: isCached,          // cached 节点直接标记为完成
              endTime: isCached ? event.timestamp : undefined,  // cached 节点设置结束时间
              cached: isCached,             // 记录是否使用缓存
              data: null,
              expanded: false
            })
          } else {
            // 更新现有阶段
            stateInstance!.stageHistory[existingIndex].message = event.message || ''
            if (!stateInstance!.stageHistory[existingIndex].startTime) {
              // 如果还没有 startTime，使用上一阶段的 endTime 或工作流开始时间
              const stageStartTime = prevHistoryItem?.endTime || stateInstance!.workflowStartTime || event.timestamp
              stateInstance!.stageHistory[existingIndex].startTime = stageStartTime
            }
            // cached 节点标记为完成
            if (isCached) {
              stateInstance!.stageHistory[existingIndex].completed = true
              stateInstance!.stageHistory[existingIndex].endTime = event.timestamp
            }
          }
        } else {
          // 同阶段更新消息
          const historyItem = stateInstance!.stageHistory.find(h => h.stage === newStage)
          if (historyItem) {
            historyItem.message = event.message || ''
          }
        }

            // 只有非 cached 节点且是有效的显示阶段才更新 currentStage
        // 排除 __START__、__END__ 等内部节点
        const isValidDisplayStage = newStage in STAGE_CONFIG
        if (!isCached && isValidDisplayStage) {
          stateInstance!.currentStage = newStage
        }
      }

      // 处理阶段数据
      if (event.data && event.nodeId) {
        const historyItem = stateInstance!.stageHistory.find(h => h.stage === event.nodeId)
        if (historyItem) {
          // 根据阶段类型解析数据
          if (event.nodeId === 'company_research' && event.data.companyResearch) {
            historyItem.data = event.data.companyResearch as CompanyResearchResult
          } else if (event.nodeId === 'jd_analysis' && event.data.jdAnalysis) {
            historyItem.data = event.data.jdAnalysis as JDAnalysisResult
          } else if (event.nodeId === 'generate_preparation') {
            // 兼容两种数据格式：
            // 1. { preparationItems: [...] } - 包装格式
            // 2. [...] - 直接数组格式
            if (event.data.preparationItems) {
              historyItem.data = event.data.preparationItems as PreparationItem[]
            } else if (Array.isArray(event.data)) {
              historyItem.data = event.data as PreparationItem[]
            }
          }
        }
      }

      stateInstance!.progress = event.progress || stateInstance!.progress
      stateInstance!.message = event.message || stateInstance!.message
      return
    }

        if (event.event === 'complete') {
      stateInstance!.isRunning = false
      stateInstance!.isCompleted = true
      stateInstance!.currentStage = 'end'
      stateInstance!.progress = 100
      stateInstance!.message = event.message || '准备事项生成完成'

      // 标记所有阶段完成，使用后端时间戳
      stateInstance!.stageHistory.forEach(h => {
        h.completed = true
        if (!h.endTime) {
          h.endTime = event.timestamp  // 使用后端时间戳
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
    closeConnection,
    toggleExpand,
    getStageLabel,
    STAGE_CONFIG
  }
}
