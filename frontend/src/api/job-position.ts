// =====================================================
// 职位管理 API 模块
// @author Azir
// =====================================================

import type {
  JobPositionListItem,
  JobPositionDetail,
  CreateJobPositionRequest,
  UpdateJobPositionRequest,
  InterviewBrief
} from '@/types/job-position'
import type { ApiResponse } from '@/types'
import { API_BASE } from './config'

/**
 * 通用请求处理函数
 */
async function request<T>(url: string, options?: RequestInit, errorMsg?: string): Promise<T> {
  const response = await fetch(url, options)
  const result: ApiResponse<T> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || errorMsg || '请求失败')
  }
  return result.data
}

// ==================== 职位管理 API ====================

/**
 * 获取职位列表（含面试统计）
 */
export async function getJobPositionList(params?: {
  page?: number
  size?: number
}): Promise<{ list: JobPositionListItem[]; total: number }> {
  const query = new URLSearchParams()
  if (params?.page) query.set('page', String(params.page))
  if (params?.size) query.set('size', String(params.size))

  return request<{ list: JobPositionListItem[]; total: number }>(
    `${API_BASE}/job-positions?${query.toString()}`,
    undefined,
    '获取职位列表失败'
  )
}

/**
 * 创建职位
 */
export async function createJobPosition(data: CreateJobPositionRequest): Promise<JobPositionDetail> {
  return request<JobPositionDetail>(
    `${API_BASE}/job-positions`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    },
    '创建职位失败'
  )
}

/**
 * 获取职位详情
 */
export async function getJobPositionDetail(id: string): Promise<JobPositionDetail> {
  return request<JobPositionDetail>(
    `${API_BASE}/job-positions/${id}`,
    undefined,
    '获取职位详情失败'
  )
}

/**
 * 更新职位
 */
export async function updateJobPosition(id: string, data: UpdateJobPositionRequest): Promise<JobPositionDetail> {
  return request<JobPositionDetail>(
    `${API_BASE}/job-positions/${id}`,
    {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    },
    '更新职位失败'
  )
}

/**
 * 删除职位
 */
export async function deleteJobPosition(id: string): Promise<void> {
  await request<void>(
    `${API_BASE}/job-positions/${id}`,
    { method: 'DELETE' },
    '删除职位失败'
  )
}

/**
 * 获取职位下的面试列表
 */
export async function getJobPositionInterviews(id: string): Promise<InterviewBrief[]> {
  return request<InterviewBrief[]>(
    `${API_BASE}/job-positions/${id}/interviews`,
    undefined,
    '获取面试列表失败'
  )
}
