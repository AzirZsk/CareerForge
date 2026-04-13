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
import request from '@/utils/request'

// ==================== 职位管理 API ====================

/**
 * 获取职位列表（含面试统计）
 */
export async function getJobPositionList(params?: {
  page?: number
  size?: number
}): Promise<{ list: JobPositionListItem[]; total: number }> {
  return request({
    url: '/job-positions',
    method: 'GET',
    params
  })
}

/**
 * 创建职位
 */
export async function createJobPosition(data: CreateJobPositionRequest): Promise<JobPositionDetail> {
  return request({
    url: '/job-positions',
    method: 'POST',
    data
  })
}

/**
 * 获取职位详情
 */
export async function getJobPositionDetail(id: string): Promise<JobPositionDetail> {
  return request({
    url: `/job-positions/${id}`,
    method: 'GET'
  })
}

/**
 * 更新职位
 */
export async function updateJobPosition(id: string, data: UpdateJobPositionRequest): Promise<JobPositionDetail> {
  return request({
    url: `/job-positions/${id}`,
    method: 'PUT',
    data
  })
}

/**
 * 删除职位
 */
export async function deleteJobPosition(id: string): Promise<void> {
  return request({
    url: `/job-positions/${id}`,
    method: 'DELETE'
  })
}

/**
 * 获取职位下的面试列表
 */
export async function getJobPositionInterviews(id: string): Promise<InterviewBrief[]> {
  return request({
    url: `/job-positions/${id}/interviews`,
    method: 'GET'
  })
}
