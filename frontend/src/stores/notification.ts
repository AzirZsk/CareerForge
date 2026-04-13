/**
 * 任务通知 Store
 * 管理异步任务状态，支持轮询更新
 *
 * @author Azir
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AsyncTask, TaskStatus, AudioTranscribeResult } from '@/types/notification'
import * as taskApi from '@/api/task'

export const useNotificationStore = defineStore('notification', () => {
  // 状态
  const tasks = ref<AsyncTask[]>([])
  const isLoading = ref(false)
  const isPolling = ref(false)

  // 轮询定时器
  let pollingTimer: ReturnType<typeof setInterval> | null = null

  // 轮询间隔（毫秒）
  const POLLING_INTERVAL = 3000

  // 计算属性
  const hasUnread = computed(() => {
    return tasks.value.some(t => t.status === 'running' || t.status === 'pending')
  })

  const runningTasks = computed(() => {
    return tasks.value.filter(t => t.status === 'running' || t.status === 'pending')
  })

  const completedTasks = computed(() => {
    return tasks.value.filter(t => t.status === 'completed' || t.status === 'failed')
  })

  const unreadCount = computed(() => runningTasks.value.length)

  // 方法
  async function fetchTasks(status?: TaskStatus) {
    isLoading.value = true
    try {
      const result = await taskApi.getTasks({ status })
      tasks.value = result.list
    } catch (error) {
      console.error('[NotificationStore] 获取任务列表失败:', error)
    } finally {
      isLoading.value = false
    }
  }

  async function fetchTaskById(taskId: string): Promise<AsyncTask | null> {
    try {
      return await taskApi.getTaskStatus(taskId)
    } catch (error) {
      console.error('[NotificationStore] 获取任务失败:', error)
      return null
    }
  }

  async function createAudioTranscribeTask(
    interviewId: string,
    file: File
  ): Promise<string | null> {
    try {
      const result = await taskApi.createAudioTranscribeTask(interviewId, file)
      // 添加到列表头部
      tasks.value.unshift({
        id: result.taskId,
        taskType: 'audio_transcribe',
        taskTypeLabel: '音频转录',
        businessId: interviewId,
        status: result.status as TaskStatus,
        statusLabel: '等待中',
        progress: 0,
        message: '任务已创建',
        result: null,
        errorMessage: null,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      })
      // 开始轮询
      startPolling()
      return result.taskId
    } catch (error) {
      console.error('[NotificationStore] 创建任务失败:', error)
      return null
    }
  }

  function updateTask(updatedTask: AsyncTask) {
    const index = tasks.value.findIndex(t => t.id === updatedTask.id)
    if (index !== -1) {
      tasks.value[index] = updatedTask
    } else {
      tasks.value.unshift(updatedTask)
    }
  }

  function removeTask(taskId: string) {
    tasks.value = tasks.value.filter(t => t.id !== taskId)
  }

  async function clearCompletedTasks() {
    try {
      await taskApi.clearCompletedTasks()
      tasks.value = tasks.value.filter(
        t => t.status !== 'completed' && t.status !== 'failed'
      )
    } catch (error) {
      console.error('[NotificationStore] 清理任务失败:', error)
    }
  }

  // 轮询逻辑
  function startPolling() {
    if (pollingTimer) return

    isPolling.value = true

    const poll = async () => {
      // 只轮询进行中的任务
      const activeTasks = tasks.value.filter(
        t => t.status === 'pending' || t.status === 'running'
      )

      if (activeTasks.length === 0) {
        stopPolling()
        return
      }

      try {
        // 批量更新任务状态
        const updates = await Promise.all(
          activeTasks.map(t => fetchTaskById(t.id))
        )

        updates.forEach((task) => {
          if (task) {
            updateTask(task)
          }
        })
      } catch (error) {
        console.error('[NotificationStore] 轮询失败:', error)
      }
    }

    // 立即执行一次
    poll()

    // 定时轮询
    pollingTimer = setInterval(poll, POLLING_INTERVAL)
  }

  function stopPolling() {
    if (pollingTimer) {
      clearInterval(pollingTimer)
      pollingTimer = null
    }
    isPolling.value = false
  }

  // 页面可见性变化时恢复轮询
  function handleVisibilityChange() {
    if (document.visibilityState === 'visible') {
      const hasActiveTasks = tasks.value.some(
        t => t.status === 'pending' || t.status === 'running'
      )
      if (hasActiveTasks) {
        startPolling()
      }
    } else {
      // 页面隐藏时暂停轮询（节省资源）
      stopPolling()
    }
  }

  // 初始化
  function init() {
    // 加载任务列表
    fetchTasks()

    // 监听页面可见性
    document.addEventListener('visibilitychange', handleVisibilityChange)

    // 如果有进行中的任务，开始轮询
    if (hasUnread.value) {
      startPolling()
    }
  }

  // 清理
  function dispose() {
    stopPolling()
    document.removeEventListener('visibilitychange', handleVisibilityChange)
  }

  // 获取转录结果
  function getTranscribeResult(interviewId: string): AudioTranscribeResult | null {
    const task = tasks.value.find(
      t => t.taskType === 'audio_transcribe'
        && t.businessId === interviewId
        && t.status === 'completed'
        && t.result
    )
    if (task?.result) {
      try {
        return JSON.parse(task.result) as AudioTranscribeResult
      } catch (e) {
        console.error('[NotificationStore] 解析转录结果失败:', e)
        return null
      }
    }
    return null
  }

  // 标记转录结果已应用（删除任务）
  function markTranscribeApplied(interviewId: string) {
    const task = tasks.value.find(
      t => t.taskType === 'audio_transcribe'
        && t.businessId === interviewId
        && t.status === 'completed'
    )
    if (task) {
      removeTask(task.id)
    }
  }

  return {
    // 状态
    tasks,
    isLoading,
    isPolling,
    // 计算属性
    hasUnread,
    runningTasks,
    completedTasks,
    unreadCount,
    // 方法
    fetchTasks,
    fetchTaskById,
    createAudioTranscribeTask,
    updateTask,
    removeTask,
    clearCompletedTasks,
    startPolling,
    stopPolling,
    handleVisibilityChange,
    init,
    dispose,
    getTranscribeResult,
    markTranscribeApplied
  }
})
