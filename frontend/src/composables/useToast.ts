// =====================================================
// Toast 通知 Composable
// @author Azir
// =====================================================

import { ref } from 'vue'
import type { ToastType } from '@/components/common/Toast.vue'

// 全局 Toast 实例引用
const toastInstance = ref<{
  success: (message: string, duration?: number) => number
  error: (message: string, duration?: number) => number
  warning: (message: string, duration?: number) => number
  info: (message: string, duration?: number) => number
} | null>(null)

/**
 * 设置全局 Toast 实例
 * 应在 App.vue 中挂载 Toast 组件后调用
 */
export function setToastInstance(instance: typeof toastInstance.value): void {
  toastInstance.value = instance
}

/**
 * 获取全局 Toast 实例
 */
export function getToastInstance(): typeof toastInstance.value {
  return toastInstance.value
}

/**
 * Toast 通知 Composable
 * 提供全局可用的 Toast 方法
 *
 * @example
 * ```typescript
 * const toast = useToast()
 * toast.success('操作成功')
 * toast.error('操作失败，请重试')
 * toast.warning('请注意')
 * toast.info('提示信息')
 * ```
 */
export function useToast() {
  const createMethod = (type: ToastType) => (message: string, duration?: number): number => {
    if (!toastInstance.value) {
      console.warn('Toast instance not initialized. Make sure to mount Toast component in App.vue')
      return -1
    }
    return toastInstance.value[type](message, duration)
  }

  return {
    success: createMethod('success'),
    error: createMethod('error'),
    warning: createMethod('warning'),
    info: createMethod('info')
  }
}
