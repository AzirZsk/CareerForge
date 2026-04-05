/**
 * 麦克风权限管理 Composable
 * 支持权限检查、请求、浏览器设置引导
 *
 * @author Azir
 */

import { ref } from 'vue'

/** 麦克风权限状态 */
export type MicrophonePermissionState = 'granted' | 'denied' | 'prompt' | 'unknown'

/** 权限检查结果 */
export interface PermissionCheckResult {
  state: MicrophonePermissionState
  canRequest: boolean
  isPermanentlyDenied: boolean
  stream?: MediaStream
}

/** 浏览器设置引导信息 */
export interface BrowserSettingsGuide {
  browser: string
  steps: string[]
}

/**
 * 麦克风权限管理 Composable
 */
export function useMicrophonePermission() {
  // ============================================================================
  // 状态
  // ============================================================================

  const permissionState = ref<MicrophonePermissionState>('unknown')
  const isChecking = ref(false)
  const error = ref<string | null>(null)

  // ============================================================================
  // 核心方法
  // ============================================================================

  /**
   * 检查麦克风权限状态
   * 优先使用 Permissions API，不支持则回退到实际请求
   */
  async function checkPermission(): Promise<PermissionCheckResult> {
    isChecking.value = true
    error.value = null

    try {
      // 尝试使用 Permissions API
      if ('permissions' in navigator) {
        try {
          // 某些浏览器（如 Safari）不支持 'microphone' 权限查询
          const permissionStatus = await navigator.permissions.query({
            name: 'microphone' as PermissionName
          })

          permissionState.value = permissionStatus.state as MicrophonePermissionState

          // 监听权限状态变化
          permissionStatus.onchange = () => {
            permissionState.value = permissionStatus.state as MicrophonePermissionState
          }

          isChecking.value = false
          return {
            state: permissionState.value,
            canRequest: permissionState.value !== 'denied',
            isPermanentlyDenied: false
          }
        } catch {
          // Permissions API 不支持 microphone 查询，回退到实际请求
          console.log('[useMicrophonePermission] Permissions API 不支持 microphone，回退到实际请求')
        }
      }

      // 回退方案：通过轻量级请求来检查（立即停止，不占用设备）
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
        // 立即停止所有轨道
        stream.getTracks().forEach(track => track.stop())

        permissionState.value = 'granted'
        isChecking.value = false

        return {
          state: 'granted',
          canRequest: false,
          isPermanentlyDenied: false
        }
      } catch (e: unknown) {
        const err = e as DOMException
        permissionState.value = 'denied'
        isChecking.value = false

        return {
          state: 'denied',
          canRequest: false,
          isPermanentlyDenied: err.name === 'NotAllowedError'
        }
      }

    } catch (e) {
      console.error('[useMicrophonePermission] 检查权限失败:', e)
      error.value = e instanceof Error ? e.message : '检查权限失败'
      isChecking.value = false

      return {
        state: 'unknown',
        canRequest: true,
        isPermanentlyDenied: false
      }
    }
  }

  /**
   * 请求麦克风权限
   * 注意：必须在用户手势（如点击）触发的上下文中调用
   */
  async function requestPermission(): Promise<PermissionCheckResult> {
    isChecking.value = true
    error.value = null

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          sampleRate: 16000,
          channelCount: 1,
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        }
      })

      permissionState.value = 'granted'
      isChecking.value = false

      return {
        state: 'granted',
        canRequest: false,
        isPermanentlyDenied: false,
        stream
      }

    } catch (e: unknown) {
      const err = e as DOMException
      console.error('[useMicrophonePermission] 请求权限失败:', err)

      isChecking.value = false

      // 判断错误类型
      if (err.name === 'NotAllowedError') {
        // 用户拒绝了权限
        error.value = '麦克风权限被拒绝'
        permissionState.value = 'denied'

        return {
          state: 'denied',
          canRequest: false,
          // 无法真正区分临时拒绝和永久拒绝，假设需要引导用户手动设置
          isPermanentlyDenied: true
        }
      } else if (err.name === 'NotFoundError') {
        error.value = '未找到麦克风设备'
      } else if (err.name === 'NotReadableError') {
        error.value = '麦克风被其他应用占用'
      } else if (err.name === 'OverconstrainedError') {
        error.value = '麦克风不支持所需的音频设置'
      } else if (err.name === 'SecurityError') {
        error.value = '安全限制：请在 HTTPS 环境下使用'
      } else if (err.name === 'NotSupportedError') {
        error.value = '浏览器不支持麦克风访问'
      } else {
        error.value = err.message || '请求麦克风权限失败'
      }

      return {
        state: 'denied',
        canRequest: false,
        isPermanentlyDenied: err.name === 'NotAllowedError'
      }
    }
  }

  /**
   * 释放麦克风流
   */
  function releaseStream(stream: MediaStream): void {
    stream.getTracks().forEach(track => track.stop())
  }

  /**
   * 获取浏览器设置引导信息
   * 根据用户代理返回对应浏览器的设置步骤
   */
  function getBrowserSettingsGuide(): BrowserSettingsGuide {
    const ua = navigator.userAgent

    if (ua.includes('Edg/')) {
      return {
        browser: 'Edge',
        steps: [
          '1. 点击地址栏左侧的锁形图标或"不安全"文字',
          '2. 找到"麦克风"选项',
          '3. 将其改为"允许"',
          '4. 刷新页面后点击"我已设置，重新检查"'
        ]
      }
    }

    if (ua.includes('Chrome/')) {
      return {
        browser: 'Chrome',
        steps: [
          '1. 点击地址栏左侧的🔒图标或"不安全"文字',
          '2. 找到"麦克风"选项',
          '3. 将其改为"允许"',
          '4. 刷新页面后点击"我已设置，重新检查"'
        ]
      }
    }

    if (ua.includes('Firefox/')) {
      return {
        browser: 'Firefox',
        steps: [
          '1. 点击地址栏左侧的权限图标',
          '2. 找到"使用麦克风"权限',
          '3. 选择"允许"',
          '4. 刷新页面后点击"我已设置，重新检查"'
        ]
      }
    }

    if (ua.includes('Safari/') && !ua.includes('Chrome/')) {
      return {
        browser: 'Safari',
        steps: [
          '1. 点击菜单栏 Safari > 偏好设置（或设置）',
          '2. 选择"网站"标签页',
          '3. 左侧找到"麦克风"',
          '4. 找到本网站，选择"允许"',
          '5. 刷新页面后点击"我已设置，重新检查"'
        ]
      }
    }

    return {
      browser: '浏览器',
      steps: [
        '1. 打开浏览器设置',
        '2. 找到隐私/权限设置',
        '3. 允许本网站访问麦克风',
        '4. 刷新页面后点击"我已设置，重新检查"'
      ]
    }
  }

  // ============================================================================
  // 返回
  // ============================================================================

  return {
    // 状态
    permissionState,
    isChecking,
    error,

    // 方法
    checkPermission,
    requestPermission,
    releaseStream,
    getBrowserSettingsGuide
  }
}
