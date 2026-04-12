// =====================================================
// 面试中心 API 模块
// @author Azir
// =====================================================

import type {
  InterviewListItem,
  InterviewDetail,
  PreparationVO,
  ReviewNoteVO,
  CreateInterviewRequest,
  UpdateInterviewRequest,
  AddPreparationRequest,
  UpdatePreparationRequest,
  SaveReviewNoteRequest
} from '@/types/interview-center'
import type { ApiResponse } from '@/types'
import { authFetch } from '@/utils/request'
import { API_BASE } from './config'

// ==================== 面试管理 API ====================

/**
 * 创建真实面试
 */
export async function createInterview(data: CreateInterviewRequest): Promise<InterviewDetail> {
  const response = await authFetch(`${API_BASE}/interview-center`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  const result: ApiResponse<InterviewDetail> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '创建面试失败')
  }
  return result.data
}

/**
 * 获取面试列表
 */
export async function getInterviewList(params?: {
  type?: 'real' | 'mock' | 'all'
  status?: string
  page?: number
  size?: number
}): Promise<{ list: InterviewListItem[]; total: number }> {
  const query = new URLSearchParams()
  if (params?.type) query.set('type', params.type)
  if (params?.status) query.set('status', params.status)
  if (params?.page) query.set('page', String(params.page))
  if (params?.size) query.set('size', String(params.size))

  const response = await authFetch(`${API_BASE}/interview-center?${query.toString()}`)
  const result: ApiResponse<{ list: InterviewListItem[]; total: number }> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '获取面试列表失败')
  }
  return result.data
}

/**
 * 获取面试详情
 */
export async function getInterviewDetail(id: string): Promise<InterviewDetail> {
  const response = await authFetch(`${API_BASE}/interview-center/${id}`)
  const result: ApiResponse<InterviewDetail> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '获取面试详情失败')
  }
  return result.data
}

/**
 * 更新面试基本信息
 */
export async function updateInterview(id: string, data: UpdateInterviewRequest): Promise<InterviewDetail> {
  const response = await authFetch(`${API_BASE}/interview-center/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  const result: ApiResponse<InterviewDetail> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '更新面试失败')
  }
  return result.data
}

/**
 * 删除面试
 */
export async function deleteInterview(id: string): Promise<void> {
  const response = await authFetch(`${API_BASE}/interview-center/${id}`, {
    method: 'DELETE'
  })
  const result: ApiResponse<void> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '删除面试失败')
  }
}

// ==================== 准备管理 API ====================

/**
 * 获取准备清单
 */
export async function getPreparations(interviewId: string): Promise<PreparationVO[]> {
  const response = await authFetch(`${API_BASE}/interview-center/${interviewId}/preparations`)
  const result: ApiResponse<PreparationVO[]> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '获取准备清单失败')
  }
  return result.data
}

/**
 * 添加准备事项
 */
export async function addPreparation(interviewId: string, data: AddPreparationRequest): Promise<PreparationVO> {
  const response = await authFetch(`${API_BASE}/interview-center/${interviewId}/preparations`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  const result: ApiResponse<PreparationVO> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '添加准备事项失败')
  }
  return result.data
}

/**
 * 批量添加准备事项（AI生成后保存）
 */
export async function batchAddPreparations(
  interviewId: string,
  items: Array<{ title: string; content: string; itemType?: string; priority?: string }>
): Promise<PreparationVO[]> {
  const response = await authFetch(`${API_BASE}/interview-center/${interviewId}/preparations/batch`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ items })
  })
  const result: ApiResponse<PreparationVO[]> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '批量保存失败')
  }
  return result.data
}

/**
 * 更新准备事项
 */
export async function updatePreparation(interviewId: string, preparationId: string, data: UpdatePreparationRequest): Promise<PreparationVO> {
  const response = await authFetch(`${API_BASE}/interview-center/${interviewId}/preparations/${preparationId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  const result: ApiResponse<PreparationVO> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '更新准备事项失败')
  }
  return result.data
}

/**
 * 切换准备事项完成状态
 */
export async function togglePreparationComplete(interviewId: string, preparationId: string): Promise<PreparationVO> {
  const response = await authFetch(`${API_BASE}/interview-center/${interviewId}/preparations/${preparationId}/toggle`, {
    method: 'PATCH'
  })
  const result: ApiResponse<PreparationVO> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '切换状态失败')
  }
  return result.data
}

/**
 * 删除准备事项
 */
export async function deletePreparation(interviewId: string, preparationId: string): Promise<void> {
  const response = await authFetch(`${API_BASE}/interview-center/${interviewId}/preparations/${preparationId}`, {
    method: 'DELETE'
  })
  const result: ApiResponse<void> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '删除准备事项失败')
  }
}

// ==================== 复盘管理 API ====================

/**
 * 获取手动复盘笔记
 */
export async function getReviewNote(interviewId: string): Promise<ReviewNoteVO | null> {
  const response = await authFetch(`${API_BASE}/interview-center/${interviewId}/review`)
  const result: ApiResponse<ReviewNoteVO | null> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '获取复盘笔记失败')
  }
  return result.data
}

/**
 * 保存手动复盘笔记
 */
export async function saveReviewNote(interviewId: string, data: SaveReviewNoteRequest): Promise<ReviewNoteVO> {
  const response = await authFetch(`${API_BASE}/interview-center/${interviewId}/review`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  const result: ApiResponse<ReviewNoteVO> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '保存复盘笔记失败')
  }
  return result.data
}

// ==================== 工作流 API ====================

/**
 * 流式执行面试准备工作流
 * SSE 流式返回工作流进度（GET 请求，使用 EventSource）
 */
export function streamPreparation(interviewId: string): EventSource {
  const url = `${API_BASE}/interview-center/${interviewId}/preparation/stream`
  return new EventSource(url)
}

/**
 * 流式执行复盘分析工作流
 * SSE 流式返回工作流进度（POST 请求，需要传递面试过程文本）
 *
 * 注意：此 API 使用 POST 方法，需要通过 composables/useReviewAnalysis.ts 调用
 * 因为 EventSource 只支持 GET 请求
 *
 * @param interviewId 面试ID
 * @param transcript 面试过程文字记录
 * @param onEvent 事件回调
 * @param onError 错误回调
 * @param onComplete 完成回调
 */
export async function streamReviewAnalysis(
  interviewId: string,
  transcript: string,
  onEvent: (event: import('@/types/interview-center').GraphProgressEvent) => void,
  onError: (error: Error) => void,
  onComplete: () => void
): Promise<void> {
  try {
    const response = await authFetch(`${API_BASE}/interview-center/${interviewId}/review-analysis/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ transcript })
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('无法读取响应流')
    }

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const json = line.slice(5).trim()
          if (json) {
            try {
              const event = JSON.parse(json) as import('@/types/interview-center').GraphProgressEvent
              onEvent(event)
            } catch {
              console.warn('[API] 解析 SSE 事件失败:', json)
            }
          }
        }
      }
    }

    onComplete()
  } catch (error) {
    onError(error as Error)
  }
}
