/**
 * 任务通知类型定义
 *
 * @author Azir
 */

// =====================================================
// 任务类型和状态
// =====================================================

/**
 * 任务类型
 */
export type TaskType = 'audio_transcribe' | 'resume_optimize' | 'review_analysis'

/**
 * 任务状态
 */
export type TaskStatus = 'pending' | 'running' | 'completed' | 'failed'

// =====================================================
// 任务接口定义
// =====================================================

/**
 * 异步任务
 */
export interface AsyncTask {
  id: string
  taskType: TaskType
  taskTypeLabel: string
  businessId: string | null
  status: TaskStatus
  statusLabel: string
  progress: number
  message: string
  result: string | null
  errorMessage: string | null
  createdAt: string
  updatedAt: string
}

/**
 * 音频转录结果
 */
export interface AudioTranscribeResult {
  transcriptText: string
  duration: number
}

/**
 * 创建任务响应
 */
export interface CreateTaskResponse {
  taskId: string
  status: string
}

// =====================================================
// 任务类型配置
// =====================================================

/**
 * 任务类型标签映射
 */
export const TASK_TYPE_LABELS: Record<TaskType, string> = {
  audio_transcribe: '音频转录',
  resume_optimize: '简历优化',
  review_analysis: '复盘分析'
}

/**
 * 任务状态配置
 */
export const TASK_STATUS_CONFIG: Record<TaskStatus, {
  label: string
  color: string
  icon: string
  bgColor: string
}> = {
  pending: {
    label: '等待中',
    color: '#a1a1aa',
    icon: 'fa-solid fa-hourglass-half',
    bgColor: 'rgba(161, 161, 170, 0.1)'
  },
  running: {
    label: '进行中',
    color: '#d4a853',
    icon: 'fa-solid fa-rotate',
    bgColor: 'rgba(212, 168, 83, 0.1)'
  },
  completed: {
    label: '已完成',
    color: '#34d399',
    icon: 'fa-solid fa-circle-check',
    bgColor: 'rgba(52, 211, 153, 0.1)'
  },
  failed: {
    label: '失败',
    color: '#f87171',
    icon: 'fa-solid fa-circle-xmark',
    bgColor: 'rgba(248, 113, 113, 0.1)'
  }
}
