// =====================================================
// 复盘分析工作流 Composable
// @author Azir
// =====================================================

import { reactive } from 'vue'
import type {
  ReviewAnalysisState,
  ReviewStage,
  GraphProgressEvent
} from '@/types/interview-center'
import { streamReviewAnalysis } from '@/api/interview-center'

// 创建单例状态
const stateInstance = reactive<ReviewAnalysisState>({
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
})

export function useReviewAnalysis() {
  // 重置状态
  function resetState() {
    stateInstance.isConnecting = false
    stateInstance.isRunning = false
    stateInstance.isCompleted = false
    stateInstance.hasError = false
    stateInstance.currentStage = ''
    stateInstance.progress = 0
    stateInstance.message = ''
    stateInstance.adviceList = []
    stateInstance.errorMessage = null
    stateInstance.stageHistory = []
  }

  // 切换阶段展开状态
  function toggleStageExpand(stage: ReviewStage) {
    const historyItem = stateInstance.stageHistory.find(h => h.stage === stage)
    if (historyItem) {
      historyItem.expanded = !historyItem.expanded
    }
  }

  // 开始复盘分析
  async function startAnalysis(interviewId: string, sessionTranscript: string) {
    resetState()
    stateInstance.isConnecting = true
    stateInstance.message = '正在连接...'

    try {
      await streamReviewAnalysis(
        interviewId,
        sessionTranscript,
        (event: GraphProgressEvent) => {
          // 处理工作流进度事件
          switch (event.event) {
            case 'start':
              stateInstance.isConnecting = false
              stateInstance.isRunning = true
              stateInstance.message = event.message || '开始分析...'
              break

            case 'progress':
              stateInstance.currentStage = event.nodeId || ''
              stateInstance.progress = event.progress || 0
              stateInstance.message = event.message || ''

              // 更新或创建阶段历史项
              let historyItem = stateInstance.stageHistory.find(h => h.stage === event.nodeId)
              if (!historyItem && event.nodeId) {
                historyItem = {
                  stage: event.nodeId as ReviewStage,
                  message: event.message || '',
                  timestamp: event.timestamp,
                  startTime: event.timestamp,
                  completed: false,
                  data: null,
                  expanded: false
                }
                stateInstance.stageHistory.push(historyItem)
              } else if (historyItem) {
                historyItem.message = event.message || ''
              }
              break

            case 'complete':
              // 标记阶段完成
              if (event.nodeId) {
                const historyItem = stateInstance.stageHistory.find(h => h.stage === event.nodeId)
                if (historyItem) {
                  historyItem.completed = true
                  historyItem.endTime = event.timestamp
                  historyItem.cached = event.cached
                  // 存储数据（需要根据节点类型进行类型断言）
                  if (event.data) {
                    if (event.nodeId === 'analyze_transcript') {
                      historyItem.data = event.data as any
                    } else if (event.nodeId === 'analyze_interview') {
                      historyItem.data = event.data as any
                    } else if (event.nodeId === 'generate_advice') {
                      historyItem.data = event.data as any
                    }
                  }
                }

                // 根据节点类型存储数据
                if (event.nodeId === 'analyze_transcript' && event.data) {
                  console.log('[useReviewAnalysis] 对话分析完成:', event.data)
                } else if (event.nodeId === 'analyze_interview' && event.data) {
                  console.log('[useReviewAnalysis] 面试分析完成:', event.data)
                } else if (event.nodeId === 'generate_advice' && event.data) {
                  const adviceData = event.data as any
                  if (Array.isArray(adviceData)) {
                    stateInstance.adviceList = adviceData
                  }
                  console.log('[useReviewAnalysis] 建议生成完成:', event.data)
                }
              }

              // 如果是整体完成事件
              if (!event.nodeId) {
                stateInstance.isRunning = false
                stateInstance.isCompleted = true
                stateInstance.progress = 100
                stateInstance.message = '分析完成'
              }
              break

            case 'error':
              stateInstance.isConnecting = false
              stateInstance.isRunning = false
              stateInstance.hasError = true
              stateInstance.errorMessage = event.errorMessage || '分析失败'
              stateInstance.message = `分析失败: ${event.errorMessage}`
              break
          }
        },
        (error: Error) => {
          stateInstance.isConnecting = false
          stateInstance.isRunning = false
          stateInstance.hasError = true
          stateInstance.errorMessage = error.message
          stateInstance.message = `分析失败: ${error.message}`
        },
        () => {
          stateInstance.isRunning = false
          stateInstance.isCompleted = true
          stateInstance.progress = 100
          stateInstance.message = '分析完成'
        }
      )
    } catch (error) {
      stateInstance.isConnecting = false
      stateInstance.isRunning = false
      stateInstance.hasError = true
      stateInstance.errorMessage = error instanceof Error ? error.message : '分析失败'
      stateInstance.message = `分析失败: ${stateInstance.errorMessage}`
    }
  }

  return {
    state: stateInstance,
    startAnalysis,
    resetState,
    toggleStageExpand
  }
}
