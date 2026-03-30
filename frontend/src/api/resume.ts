// =====================================================
// LandIt 简历模块 API
// @author Azir
// =====================================================

import type { ApiResponse, PrimaryResumeVO, ResumeDetail, ResumeListItem, CreateResumeRequest, ResumeSuggestion, ResumeSuggestionsGroup } from '@/types'
import type { DeriveResumeRequest, SaveTailoredResumeRequest } from '@/types/resume-tailor'
import type { ParseReferenceResponse } from '@/types/resume-rewrite'

// API 基础路径
const API_BASE = '/landit'

/**
 * 获取所有简历列表
 * @param status 简历状态筛选（可选，"optimized"/"draft"）
 */
export async function getResumes(status?: string): Promise<ResumeListItem[]> {
  const params = status ? `?status=${status}` : ''
  const response = await fetch(`${API_BASE}/resumes${params}`)
  const result: ApiResponse<ResumeListItem[]> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '获取简历列表失败')
  }
  return result.data
}

/**
 * 创建空白简历
 */
export async function createResume(data: CreateResumeRequest): Promise<ResumeDetail> {
  const response = await fetch(`${API_BASE}/resumes`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  const result: ApiResponse<ResumeDetail> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '创建简历失败')
  }
  return result.data
}

/**
 * 删除简历
 */
export async function deleteResume(id: string): Promise<void> {
  const response = await fetch(`${API_BASE}/resumes/${id}`, {
    method: 'DELETE'
  })
  const result: ApiResponse<void> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '删除简历失败')
  }
}

/**
 * 设置主简历
 */
export async function setPrimaryResume(id: string): Promise<PrimaryResumeVO> {
  const response = await fetch(`${API_BASE}/resumes/${id}/primary`, {
    method: 'PUT'
  })
  const result: ApiResponse<PrimaryResumeVO> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '设置主简历失败')
  }
  return result.data
}

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
 * 更新简历基本信息
 * @param id 简历ID
 * @param data 更新数据（名称和目标岗位）
 */
export async function updateResume(
  id: string,
  data: { name: string; targetPosition?: string }
): Promise<ResumeDetail> {
  const response = await fetch(`${API_BASE}/resumes/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  const result: ApiResponse<ResumeDetail> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '更新简历失败')
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
    body: JSON.stringify({
      ...data,
      content: JSON.stringify(data.content)
    })
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

/**
 * 派生岗位定制简历
 * @param sourceResumeId 源简历ID
 * @param data 派生请求参数
 */
export async function deriveResume(
  sourceResumeId: string,
  data: DeriveResumeRequest
): Promise<ResumeDetail> {
  const response = await fetch(`${API_BASE}/resumes/${sourceResumeId}/derive`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  const result: ApiResponse<ResumeDetail> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '派生简历失败')
  }
  return result.data
}

/**
 * 创建职位适配 SSE 连接
 * @param resumeId 简历ID
 * @param targetPosition 目标职位
 * @param jobDescription 职位描述
 * @returns EventSource 实例
 */
export function createTailorResumeStream(
  resumeId: string,
  targetPosition: string,
  jobDescription: string
): EventSource {
  const params = new URLSearchParams({
    targetPosition,
    jobDescription
  })
  const url = `${API_BASE}/resumes/${resumeId}/tailor/stream?${params.toString()}`
  return new EventSource(url)
}

/** 区块数据项（用于应用优化变更） */
export interface SectionDataItem {
  id: string
  type?: string
  title?: string
  content: string
}

/**
 * 应用优化变更
 * 批量更新优化后的简历区块内容
 * @param resumeId 简历ID
 * @param data 包含 beforeSection 和 afterSection 的数据
 */
export async function applyOptimizeChanges(
  resumeId: string,
  data: { beforeSection: SectionDataItem[]; afterSection: SectionDataItem[] }
): Promise<ResumeDetail> {
  const response = await fetch(`${API_BASE}/resumes/${resumeId}/optimize/apply`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  const result: ApiResponse<ResumeDetail> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '应用变更失败')
  }
  return result.data
}

/**
 * 删除优化建议
 * @param resumeId 简历ID
 * @param suggestionId 建议ID
 */
export async function deleteSuggestion(
  resumeId: string,
  suggestionId: string
): Promise<void> {
  const response = await fetch(`${API_BASE}/resumes/${resumeId}/suggestions/${suggestionId}`, {
    method: 'DELETE'
  })
  const result: ApiResponse<void> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '删除建议失败')
  }
}

/**
 * 获取简历优化建议列表
 * @param resumeId 简历ID
 */
export async function getSuggestions(resumeId: string): Promise<ResumeSuggestion[]> {
  const response = await fetch(`${API_BASE}/suggestions/resume/${resumeId}`)
  const result: ApiResponse<ResumeSuggestion[]> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '获取建议列表失败')
  }
  return result.data
}

/**
 * 获取所有简历的优化建议（按简历分组）
 */
export async function getAllSuggestions(): Promise<ResumeSuggestionsGroup[]> {
  const response = await fetch(`${API_BASE}/suggestions/all`)
  const result: ApiResponse<ResumeSuggestionsGroup[]> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '获取建议失败')
  }
  return result.data
}

/**
 * 保存定制简历
 * 创建新的派生简历并应用定制内容
 * @param sourceResumeId 源简历ID
 * @param data 保存请求数据
 */
export async function saveTailoredResume(
  sourceResumeId: string,
  data: SaveTailoredResumeRequest
): Promise<ResumeDetail> {
  const response = await fetch(`${API_BASE}/resumes/${sourceResumeId}/tailor/save`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  const result: ApiResponse<ResumeDetail> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '保存定制简历失败')
  }
  return result.data
}

// ==================== 风格改写 API ====================

/**
 * 解析参考简历文件
 * 上传参考简历（PDF/Word），AI解析后返回 tempKey 用于后续改写
 * @param resumeId 简历ID
 * @param file 参考简历文件
 */
export async function parseReferenceResume(
  resumeId: string,
  file: File
): Promise<ParseReferenceResponse> {
  const formData = new FormData()
  formData.append('file', file)
  const response = await fetch(`${API_BASE}/resumes/${resumeId}/rewrite/parse-reference`, {
    method: 'POST',
    body: formData
  })
  const result: ApiResponse<ParseReferenceResponse> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '解析参考简历失败')
  }
  return result.data
}

/**
 * 创建风格改写 SSE 连接
 * @param resumeId 简历ID
 * @param tempKey 参考简历缓存key
 * @returns EventSource 实例
 */
export function createRewriteResumeStream(
  resumeId: string,
  tempKey: string
): EventSource {
  const url = `${API_BASE}/resumes/${resumeId}/rewrite/stream?tempKey=${encodeURIComponent(tempKey)}`
  return new EventSource(url)
}
