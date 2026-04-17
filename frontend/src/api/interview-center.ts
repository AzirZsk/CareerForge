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
  SaveReviewNoteRequest,
  MockSessionSummary,
  MockSessionDetail
} from '@/types/interview-center'
import request from '@/utils/request'
import { authFetch } from '@/utils/request'
import { API_BASE } from './config'

// ==================== 面试管理 API ====================

/**
 * 创建真实面试
 */
export async function createInterview(data: CreateInterviewRequest): Promise<InterviewDetail> {
  return request({
    url: '/interview-center',
    method: 'POST',
    data
  })
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
  return request({
    url: '/interview-center',
    method: 'GET',
    params
  })
}

/**
 * 获取面试详情
 */
export async function getInterviewDetail(id: string): Promise<InterviewDetail> {
  return request({
    url: `/interview-center/${id}`,
    method: 'GET'
  })
}

/**
 * 更新面试基本信息
 */
export async function updateInterview(id: string, data: UpdateInterviewRequest): Promise<InterviewDetail> {
  return request({
    url: `/interview-center/${id}`,
    method: 'PUT',
    data
  })
}

/**
 * 删除面试
 */
export async function deleteInterview(id: string): Promise<void> {
  return request({
    url: `/interview-center/${id}`,
    method: 'DELETE'
  })
}

// ==================== 准备管理 API ====================

/**
 * 获取准备清单
 */
export async function getPreparations(interviewId: string): Promise<PreparationVO[]> {
  return request({
    url: `/interview-center/${interviewId}/preparations`,
    method: 'GET'
  })
}

/**
 * 添加准备事项
 */
export async function addPreparation(interviewId: string, data: AddPreparationRequest): Promise<PreparationVO> {
  return request({
    url: `/interview-center/${interviewId}/preparations`,
    method: 'POST',
    data
  })
}

/**
 * 批量添加准备事项（AI生成后保存）
 */
export async function batchAddPreparations(
  interviewId: string,
  items: Array<{ title: string; content: string; itemType?: string; priority?: string }>
): Promise<PreparationVO[]> {
  return request({
    url: `/interview-center/${interviewId}/preparations/batch`,
    method: 'POST',
    data: { items }
  })
}

/**
 * 更新准备事项
 */
export async function updatePreparation(interviewId: string, preparationId: string, data: UpdatePreparationRequest): Promise<PreparationVO> {
  return request({
    url: `/interview-center/${interviewId}/preparations/${preparationId}`,
    method: 'PUT',
    data
  })
}

/**
 * 切换准备事项完成状态
 */
export async function togglePreparationComplete(interviewId: string, preparationId: string): Promise<PreparationVO> {
  return request({
    url: `/interview-center/${interviewId}/preparations/${preparationId}/toggle`,
    method: 'PATCH'
  })
}

/**
 * 删除准备事项
 */
export async function deletePreparation(interviewId: string, preparationId: string): Promise<void> {
  return request({
    url: `/interview-center/${interviewId}/preparations/${preparationId}`,
    method: 'DELETE'
  })
}

// ==================== 复盘管理 API ====================

/**
 * 获取手动复盘笔记
 */
export async function getReviewNote(interviewId: string): Promise<ReviewNoteVO | null> {
  return request({
    url: `/interview-center/${interviewId}/review`,
    method: 'GET'
  })
}

/**
 * 保存手动复盘笔记
 */
export async function saveReviewNote(interviewId: string, data: SaveReviewNoteRequest): Promise<ReviewNoteVO> {
  return request({
    url: `/interview-center/${interviewId}/review`,
    method: 'POST',
    data
  })
}

// ==================== 工作流 API ====================

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

// ==================== 模拟面试历史 API ====================

/**
 * 获取模拟面试历史列表
 */
export async function getMockSessions(interviewId: string): Promise<MockSessionSummary[]> {
  return request({
    url: `/interview-center/${interviewId}/mock-sessions`,
    method: 'GET'
  })
}

/**
 * 获取模拟面试详情
 */
export async function getMockSessionDetail(interviewId: string, mockInterviewId: string): Promise<MockSessionDetail> {
  return request({
    url: `/interview-center/${interviewId}/mock-sessions/${mockInterviewId}`,
    method: 'GET'
  })
}
