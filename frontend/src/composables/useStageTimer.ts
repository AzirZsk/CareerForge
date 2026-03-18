// =====================================================
// LandIt 简历优化阶段计时器 Composable
// @author Azir
// =====================================================

import { ref, watch, onUnmounted } from 'vue'
import type { StageHistoryItem } from '@/types/resume-optimize'

/**
 * 阶段计时器 Composable
 * 用于跟踪各阶段的运行时间
 */
export function useStageTimer(isOptimizing: () => boolean) {
  // 当前时间戳，用于计算耗时
  const now = ref(Date.now())
  let timerHandle: ReturnType<typeof setInterval> | null = null

  // 启动计时器
  function startTimer() {
    if (!timerHandle) {
      timerHandle = setInterval(() => {
        now.value = Date.now()
      }, 1000)
    }
  }

  // 停止计时器
  function stopTimer() {
    if (timerHandle) {
      clearInterval(timerHandle)
      timerHandle = null
    }
  }

  // 监听优化状态，自动启动/停止计时器
  watch(
    () => isOptimizing(),
    (optimizing) => {
      if (optimizing) {
        startTimer()
      } else {
        stopTimer()
      }
    },
    { immediate: true }
  )

  // 组件卸载时清理
  onUnmounted(() => {
    stopTimer()
  })

  /**
   * 格式化耗时为 mm:ss
   * @param item 阶段历史项
   * @returns 格式化的时间字符串
   */
  function formatElapsed(item: StageHistoryItem): string {
    if (!item.startTime) return ''
    const end = item.endTime ?? now.value
    const elapsed = Math.max(0, Math.floor((end - item.startTime) / 1000))
    const m = String(Math.floor(elapsed / 60)).padStart(2, '0')
    const s = String(elapsed % 60).padStart(2, '0')
    return `${m}:${s}`
  }

  return {
    now,
    formatElapsed,
    startTimer,
    stopTimer
  }
}
