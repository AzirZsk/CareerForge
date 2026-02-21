// =====================================================
// LandIt 用户模块 API
// @author Azir
// =====================================================

import type { ApiResponse, UserStatusResponse, UserInitResponse } from '@/types'

// API 基础路径（始终使用相对路径，开发环境通过 Vite 代理转发）
const API_BASE = '/landit'

/**
 * 获取用户状态
 * 检查系统是否已初始化用户
 */
export async function getUserStatus(): Promise<UserStatusResponse> {
  const response = await fetch(`${API_BASE}/user/status`)
  const result: ApiResponse<UserStatusResponse> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '获取用户状态失败')
  }
  return result.data
}

/**
 * 初始化用户（上传简历文件解析）
 * 上传简历文件后，AI会解析并自动创建用户
 * @param file 简历文件（支持图片和PDF）
 */
export async function initUser(file: File): Promise<UserInitResponse> {
  const formData = new FormData()
  formData.append('file', file)
  const response = await fetch(`${API_BASE}/user/init`, {
    method: 'POST',
    body: formData
  })
  const result: ApiResponse<UserInitResponse> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '初始化用户失败')
  }
  return result.data
}
