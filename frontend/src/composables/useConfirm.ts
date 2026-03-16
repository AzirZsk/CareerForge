// =====================================================
// 确认对话框 Composable
// @author Azir
// =====================================================

import { ref } from 'vue'

export interface ConfirmOptions {
  title?: string
  message: string
  confirmText?: string
  danger?: boolean
}

/**
 * 确认对话框 Composable
 * 封装 ConfirmModal 的使用逻辑，提供 Promise 接口
 *
 * @example
 * ```typescript
 * const { confirm } = useConfirm()
 *
 * async function handleDelete() {
 *   const result = await confirm({
 *     title: '删除确认',
 *     message: '确定要删除这条记录吗？',
 *     danger: true
 *   })
 *   if (result) {
 *     // 执行删除
 *   }
 * }
 * ```
 */
export function useConfirm() {
  const visible = ref(false)
  const title = ref('确认')
  const message = ref('')
  const confirmText = ref('确定')
  const danger = ref(false)

  // 存储确认对话框的 resolve 函数（每个实例独立）
  let confirmResolve: ((value: boolean) => void) | null = null

  /**
   * 显示确认对话框
   */
  function confirm(options: ConfirmOptions): Promise<boolean> {
    title.value = options.title || '确认'
    message.value = options.message
    confirmText.value = options.confirmText || '确定'
    danger.value = options.danger || false
    visible.value = true

    return new Promise((resolve) => {
      confirmResolve = resolve
    })
  }

  /**
   * 处理确认点击
   */
  function handleConfirm(): void {
    visible.value = false
    if (confirmResolve) {
      confirmResolve(true)
      confirmResolve = null
    }
  }

  /**
   * 处理取消点击
   */
  function handleCancel(): void {
    visible.value = false
    if (confirmResolve) {
      confirmResolve(false)
      confirmResolve = null
    }
  }

  return {
    visible,
    title,
    message,
    confirmText,
    danger,
    confirm,
    handleConfirm,
    handleCancel
  }
}
