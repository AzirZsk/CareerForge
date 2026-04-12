/**
 * 异步任务 API
 *
 * @author Azir
 */

import type {
  AsyncTask,
  CreateTaskResponse,
  TaskType,
  TaskStatus
} from '@/types/notification'
import type { ApiResponse } from '@/types'
import { authFetch } from '@/utils/request'
import { API_BASE } from './config'

/**
 * 创建音频转录任务
 */
export async function createAudioTranscribeTask(
  interviewId: string,
  file: File
): Promise<CreateTaskResponse> {
  const formData = new FormData()
  formData.append('interviewId', interviewId)
  formData.append('file', file)

  const response = await authFetch(`${API_BASE}/tasks/audio-transcribe`, {
    method: 'POST',
    body: formData
  })

  const result: ApiResponse<CreateTaskResponse> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '创建任务失败')
  }
  return result.data
}

/**
 * 获取任务列表
 */
export async function getTasks(params?: {
  type?: TaskType
  status?: TaskStatus
}): Promise<{ list: AsyncTask[]; total: number }> {
  const query = new URLSearchParams()
  if (params?.type) query.set('type', params.type)
  if (params?.status) query.set('status', params.status)

  const response = await authFetch(`${API_BASE}/tasks?${query.toString()}`)
  const result: ApiResponse<{ list: AsyncTask[]; total: number }> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '获取任务列表失败')
  }
  return result.data
}

/**
 * 获取单个任务状态
 */
export async function getTaskStatus(taskId: string): Promise<AsyncTask> {
  const response = await authFetch(`${API_BASE}/tasks/${taskId}`)
  const result: ApiResponse<AsyncTask> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '获取任务失败')
  }
  return result.data
}

/**
 * 删除任务
 */
export async function deleteTask(taskId: string): Promise<void> {
  const response = await authFetch(`${API_BASE}/tasks/${taskId}`, {
    method: 'DELETE'
  })
  const result: ApiResponse<void> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '删除任务失败')
  }
}

/**
 * 清理已完成的任务
 */
export async function clearCompletedTasks(): Promise<void> {
  const response = await authFetch(`${API_BASE}/tasks/completed`, {
    method: 'DELETE'
  })
  const result: ApiResponse<void> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '清理任务失败')
  }
}
