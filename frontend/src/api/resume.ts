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

/**
 * 更新简历模块
 * @param resumeId 简历ID
 * @param sectionId 模块ID
 * @param content 模块内容（对象或数组）
 */
export async function updateSection(
  resumeId: string,
  sectionId: string,
  content: Record<string, unknown> | Record<string, unknown>[]
): Promise<ResumeDetail> {
  const response = await fetch(`${API_BASE}/resumes/${resumeId}/sections/${sectionId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ content: JSON.stringify(content) })
  })
  const result: ApiResponse<ResumeDetail> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '更新模块失败')
  }
  return result.data
}

/**
 * 新增简历模块
 * @param resumeId 简历ID
 * @param data 模块数据
 */
export async function createSection(
  resumeId: string,
  data: { type: string; title: string; content: Record<string, unknown> | Record<string, unknown>[] }
): Promise<ResumeDetail> {
  const response = await fetch(`${API_BASE}/resumes/${resumeId}/sections`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  const result: ApiResponse<ResumeDetail> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '新增模块失败')
  }
  return result.data
}

/**
 * 删除简历模块
 * @param resumeId 简历ID
 * @param sectionId 模块ID
 */
export async function deleteSection(
  resumeId: string,
  sectionId: string
): Promise<ResumeDetail> {
  const response = await fetch(`${API_BASE}/resumes/${resumeId}/sections/${sectionId}`, {
    method: 'DELETE'
  })
  const result: ApiResponse<ResumeDetail> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '删除模块失败')
  }
  return result.data
}

/**
 * 新增聚合类型的条目
 * @param resumeId 简历ID
 * @param type 模块类型
 * @param content 条目内容
 */
export async function createSectionItem(
  resumeId: string,
  type: string,
  content: Record<string, unknown>
): Promise<ResumeDetail> {
  // 从 content 中获取标题字段作为模块标题
  const title = (content.name || content.school || content.company || '新条目') as string
  const response = await fetch(`${API_BASE}/resumes/${resumeId}/sections`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ type, title, content })
  })
  const result: ApiResponse<ResumeDetail> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '新增条目失败')
  }
  return result.data
}
