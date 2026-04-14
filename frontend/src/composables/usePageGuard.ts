// =====================================================
// 页面离开保护 Composable
// 管理全局"忙碌"状态注册表，供 App.vue 绑定 beforeunload
// @author Azir
// =====================================================

import { reactive, computed } from 'vue'

// 模块级共享状态（单例模式，同 useAIChat/useInterviewPreparation）
const activeGuards = reactive(new Set<string>())

export function usePageGuard() {
  /**
   * 注册一个保护锁（表示有操作正在进行中）
   * @param key 唯一标识，如 'optimize' / 'tailor' / 'preparation' / 'review' / 'ai-chat' / 'voice-interview' / 'modal:xxx'
   */
  function registerGuard(key: string): void {
    activeGuards.add(key)
  }

  /**
   * 移除一个保护锁
   * @param key 与 registerGuard 使用相同的 key
   */
  function unregisterGuard(key: string): void {
    activeGuards.delete(key)
  }

  // 是否有任何保护锁处于激活状态
  const isBlocked = computed(() => activeGuards.size > 0)

  // 获取当前所有激活的保护锁（调试用）
  const activeKeys = computed(() => [...activeGuards])

  return {
    registerGuard,
    unregisterGuard,
    isBlocked,
    activeKeys
  }
}
