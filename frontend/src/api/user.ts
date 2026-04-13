// =====================================================
// CareerForge 用户模块 API
// @author Azir
// =====================================================

import request from '@/utils/request'
import type { UserStatusResponse, UserInitResponse } from '@/types'

/**
 * 获取用户状态
 * 检查系统是否已初始化用户
 */
export async function getUserStatus(): Promise<UserStatusResponse> {
  return request({
    url: '/user/status',
    method: 'get'
  })
}

/**
 * 初始化用户（上传简历文件解析）
 * 上传简历文件后，AI会解析并自动创建用户
 * @param file 简历文件（支持图片和PDF）
 */
export async function initUser(file: File): Promise<UserInitResponse> {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/user/init',
    method: 'post',
    data: formData,
    timeout: 300000, // 5分钟超时，AI解析简历需要较长时间
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取当前用户信息
 */
export async function getUserProfile(): Promise<UserStatusResponse> {
  return request({
    url: '/user/profile',
    method: 'get'
  })
}

/**
 * 更新用户信息
 * @param data 用户信息
 */
export async function updateUserProfile(data: { name: string; gender?: string }): Promise<void> {
  return request({
    url: '/user/profile',
    method: 'put',
    data
  })
}

/**
 * 上传头像
 * @param file 头像文件
 */
export async function uploadAvatar(file: File): Promise<{ avatarUrl: string }> {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/user/avatar',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
