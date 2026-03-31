// =====================================================
// 面试中心 API 模块
// @author Azir
// =====================================================

import type {
  InterviewListItem,
  InterviewDetail,
  RoundVO,
  PreparationVO,
  ReviewNoteVO,
  CreateInterviewRequest,
  UpdateInterviewRequest,
  AddRoundRequest,
  UpdateRoundRequest,
  AddPreparationRequest,
  UpdatePreparationRequest,
  SaveReviewNoteRequest
} from '@/types/interview-center'
import type { ApiResponse } from '@/types'

const API_BASE = '/landit'

// ==================== 面试管理 API ====================

/**
 * 创建真实面试
 */
export async function createInterview(data: CreateInterviewRequest): Promise<InterviewDetail> {
  const response = await fetch(`${API_BASE}/interview-center`, {
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

  const response = await fetch(`${API_BASE}/interview-center?${query.toString()}`)
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
  const response = await fetch(`${API_BASE}/interview-center/${id}`)
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
  const response = await fetch(`${API_BASE}/interview-center/${id}`, {
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
  const response = await fetch(`${API_BASE}/interview-center/${id}`, {
    method: 'DELETE'
  })
  const result: ApiResponse<void> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '删除面试失败')
  }
}

// ==================== 轮次管理 API ====================

/**
 * 新增面试轮次
 */
export async function addRound(interviewId: string, data: AddRoundRequest): Promise<RoundVO> {
  const response = await fetch(`${API_BASE}/interview-center/${interviewId}/rounds`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  const result: ApiResponse<RoundVO> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '添加轮次失败')
  }
  return result.data
}

/**
 * 更新轮次信息
 */
export async function updateRound(interviewId: string, roundId: string, data: UpdateRoundRequest): Promise<RoundVO> {
  const response = await fetch(`${API_BASE}/interview-center/${interviewId}/rounds/${roundId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  const result: ApiResponse<RoundVO> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '更新轮次失败')
  }
  return result.data
}

/**
 * 更新轮次状态
 */
export async function updateRoundStatus(interviewId: string, roundId: string, status: string): Promise<RoundVO> {
  const response = await fetch(`${API_BASE}/interview-center/${interviewId}/rounds/${roundId}/status`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status })
  })
  const result: ApiResponse<RoundVO> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '更新轮次状态失败')
  }
  return result.data
}

/**
 * 删除轮次
 */
export async function deleteRound(interviewId: string, roundId: string): Promise<void> {
  const response = await fetch(`${API_BASE}/interview-center/${interviewId}/rounds/${roundId}`, {
    method: 'DELETE'
  })
  const result: ApiResponse<void> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '删除轮次失败')
  }
}

/**
 * 重排轮次顺序
 */
export async function reorderRounds(interviewId: string, roundIds: string[]): Promise<RoundVO[]> {
  const response = await fetch(`${API_BASE}/interview-center/${interviewId}/rounds/reorder`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ roundIds })
  })
  const result: ApiResponse<RoundVO[]> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '重排轮次失败')
  }
  return result.data
}

// ==================== 准备管理 API ====================

/**
 * 获取准备清单
 */
export async function getPreparations(interviewId: string): Promise<PreparationVO[]> {
  const response = await fetch(`${API_BASE}/interview-center/${interviewId}/preparations`)
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
  const response = await fetch(`${API_BASE}/interview-center/${interviewId}/preparations`, {
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
 * 更新准备事项
 */
export async function updatePreparation(interviewId: string, preparationId: string, data: UpdatePreparationRequest): Promise<PreparationVO> {
  const response = await fetch(`${API_BASE}/interview-center/${interviewId}/preparations/${preparationId}`, {
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
  const response = await fetch(`${API_BASE}/interview-center/${interviewId}/preparations/${preparationId}/toggle`, {
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
  const response = await fetch(`${API_BASE}/interview-center/${interviewId}/preparations/${preparationId}`, {
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
  const response = await fetch(`${API_BASE}/interview-center/${interviewId}/review`)
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
  const response = await fetch(`${API_BASE}/interview-center/${interviewId}/review`, {
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
 * SSE 流式返回工作流进度
 */
export function streamPreparation(interviewId: string): EventSource {
  const url = `${API_BASE}/interview-center/${interviewId}/preparation/stream`
  return new EventSource(url)
}

/**
 * 流式执行复盘分析工作流
 * SSE 流式返回工作流进度
 */
export function streamReviewAnalysis(interviewId: string): EventSource {
  const url = `${API_BASE}/interview-center/${interviewId}/review-analysis/stream`
  return new EventSource(url)
}
