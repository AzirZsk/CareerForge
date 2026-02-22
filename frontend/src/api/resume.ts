// =====================================================
// LandIt 简历模块 API
// @author Azir
// =====================================================

import type { ApiResponse, PrimaryResumeVO, ResumeDetail } from '@/types'

// API 基础路径
const API_BASE = '/landit'

/**
 * 获取主简历信息
 */
export async function getPrimaryResume(): Promise<PrimaryResumeVO | null> {
  const response = await fetch(`${API_BASE}/resumes/primary`)
  const result: ApiResponse<PrimaryResumeVO | null> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '获取主简历失败')
  }
  return result.data
}

/**
 * 获取简历详情
 * @param id 简历ID
 */
export async function getResumeDetail(id: string): Promise<ResumeDetail> {
  const response = await fetch(`${API_BASE}/resumes/${id}`)
  const result: ApiResponse<ResumeDetail> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '获取简历详情失败')
  }
  return result.data
}
