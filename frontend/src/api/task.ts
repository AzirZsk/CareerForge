/**
 * 异步任务 API
 *
 * @author Azir
 */

import type {
  AsyncTask,
  TaskType,
  TaskStatus
} from '@/types/notification'
import request from '@/utils/request'

/**
 * 获取任务列表
 */
export async function getTasks(params?: {
  type?: TaskType
  status?: TaskStatus
}): Promise<{ list: AsyncTask[]; total: number }> {
  return request({
    url: '/tasks',
    method: 'GET',
    params
  })
}

/**
 * 获取单个任务状态
 */
export async function getTaskStatus(taskId: string): Promise<AsyncTask> {
  return request({
    url: `/tasks/${taskId}`,
    method: 'GET'
  })
}

/**
 * 删除任务
 */
export async function deleteTask(taskId: string): Promise<void> {
  return request({
    url: `/tasks/${taskId}`,
    method: 'DELETE'
  })
}

/**
 * 清理已完成的任务
 */
export async function clearCompletedTasks(): Promise<void> {
  return request({
    url: '/tasks/completed',
    method: 'DELETE'
  })
}

/**
 * 按业务ID查询任务
 */
export async function getTaskByBusinessId(params: {
  businessId: string
  taskType: TaskType
}): Promise<AsyncTask | null> {
  return request({
    url: '/tasks/by-business',
    method: 'GET',
    params
  })
}
