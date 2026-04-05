/**
 * 音频录制 Composable
 * 支持麦克风采集、PCM 转换、VAD 检测
 *
 * @author Azir
 */

import { ref, computed, onUnmounted } from 'vue'

/** 录制状态 */
export type RecordingState = 'idle' | 'recording' | 'paused'

/**
 * 音频录制器
 */
export function useAudioRecorder() {
  // ============================================================================
  // 状态
  // ============================================================================

  /** 录制状态 */
  const recordingState = ref<RecordingState>('idle')

  /** 是否正在录制 */
  const isRecording = computed(() => recordingState.value === 'recording')

  /** 录制时长（秒） */
  const recordingTime = ref(0)

  /** 音频电平（用于波形显示） */
  const audioLevel = ref(0)

  /** 错误信息 */
  const error = ref<string | null>(null)

  /** 媒体流 */
  let mediaStream: MediaStream | null = null

  /** 音频上下文 */
  let audioContext: AudioContext | null = null

  /** 音频处理器 */
  let processor: ScriptProcessorNode | null = null

  /** 录制计时器 */
  let recordingTimer: number | null = null

  /** 音频数据回调 */
  let onAudioData: ((data: Int16Array) => void) | null = null

  /** VAD 静音检测计时器 */
  let vadTimer: number | null = null

  /** VAD 静音阈值（毫秒） */
  let vadSilenceMs = 1500

  /** 是否启用 VAD */
  let vadEnabled = true

  // ============================================================================
  // 核心方法
  // ============================================================================

  /**
   * 初始化音频录制
   * @param options 配置选项
   */
  async function init(options: {
    onAudioData?: (data: Int16Array) => void
    vadEnabled?: boolean
    vadSilenceMs?: number
  } = {}): Promise<void> {
    onAudioData = options.onAudioData || null
    vadEnabled = options.vadEnabled ?? true
    vadSilenceMs = options.vadSilenceMs ?? 1500

    try {
      // 获取麦克风权限
      mediaStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          sampleRate: 16000,
          channelCount: 1,
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        }
      })

      // 初始化音频上下文
      audioContext = new AudioContext({ sampleRate: 16000 })

      // 确保 AudioContext 处于运行状态（浏览器安全策略要求）
      if (audioContext.state === 'suspended') {
        console.log('[useAudioRecorder] AudioContext suspended, attempting to resume...')
        await audioContext.resume()
        console.log('[useAudioRecorder] AudioContext resumed, state:', audioContext.state)
      }

      const source = audioContext.createMediaStreamSource(mediaStream)

      // 创建音频处理器（4096 缓冲区）
      // 注意：ScriptProcessorNode 已废弃，但 AudioWorkletNode 需要单独的 worker 文件
      // 为了简化部署，暂时保留 ScriptProcessorNode
      processor = audioContext.createScriptProcessor(4096, 1, 1)
      source.connect(processor)
      processor.connect(audioContext.destination)

      // 音频处理回调
      processor.onaudioprocess = handleAudioProcess

      error.value = null
    } catch (e) {
      console.error('[useAudioRecorder] 初始化失败:', e)
      error.value = e instanceof Error ? e.message : '无法访问麦克风'
      throw e
    }
  }

  /**
   * 开始录制
   */
  async function startRecording(): Promise<void> {
    if (recordingState.value === 'recording') {
      return
    }

    // 确保 AudioContext 处于运行状态（浏览器自动播放策略)
    if (audioContext && audioContext.state === 'suspended') {
      try {
        await audioContext.resume()
      } catch (e) {
        console.error('[useAudioRecorder] AudioContext resume failed:', e)
        error.value = '音频上下文启动失败，请重新点击开始按钮'
        return
      }
    }

    recordingState.value = 'recording'
    recordingTime.value = 0

    // 开始计时
    recordingTimer = window.setInterval(() => {
      recordingTime.value++
    }, 1000)

    // 重置 VAD
    resetVad()
  }

  /**
   * 停止录制
   */
  function stopRecording(): void {
    if (recordingState.value !== 'recording') {
      return
    }

    recordingState.value = 'idle'

    // 停止计时
    if (recordingTimer) {
      clearInterval(recordingTimer)
      recordingTimer = null
    }

    // 清除 VAD 计时器
    if (vadTimer) {
      clearTimeout(vadTimer)
      vadTimer = null
    }
  }

  /**
   * 暂停录制
   */
  function pauseRecording(): void {
    if (recordingState.value === 'recording') {
      recordingState.value = 'paused'
      if (recordingTimer) {
        clearInterval(recordingTimer)
        recordingTimer = null
      }
    }
  }

  /**
   * 恢复录制
   */
  function resumeRecording(): void {
    if (recordingState.value === 'paused') {
      recordingState.value = 'recording'
      recordingTimer = window.setInterval(() => {
        recordingTime.value++
      }, 1000)
    }
  }

  /**
   * 清理资源
   */
  function dispose(): void {
    stopRecording()

    if (processor) {
      processor.disconnect()
      processor = null
    }

    if (mediaStream) {
      mediaStream.getTracks().forEach(track => track.stop())
      mediaStream = null
    }

    if (audioContext) {
      audioContext.close()
      audioContext = null
    }
  }

  // ============================================================================
  // 内部方法
  // ============================================================================

  /**
   * 处理音频数据
   */
  function handleAudioProcess(event: AudioProcessingEvent): void {
    if (recordingState.value !== 'recording') {
      return
    }

    const inputData = event.inputBuffer.getChannelData(0)

    // 计算音量电平
    const rms = calculateRms(inputData)
    audioLevel.value = rms

    // VAD 检测
    if (vadEnabled) {
      handleVad(rms)
    }

    // 转换为 16bit PCM
    const pcmData = float32ToPcm(inputData)

    // 回调
    if (onAudioData) {
      onAudioData(pcmData)
    }
  }

  /**
   * 计算 RMS 音量
   */
  function calculateRms(float32Array: Float32Array): number {
    let sum = 0
    for (let i = 0; i < float32Array.length; i++) {
      sum += float32Array[i] * float32Array[i]
    }
    return Math.sqrt(sum / float32Array.length)
  }

  /**
   * Float32 转 16bit PCM
   */
  function float32ToPcm(float32Array: Float32Array): Int16Array {
    const pcmData = new Int16Array(float32Array.length)
    for (let i = 0; i < float32Array.length; i++) {
      const s = Math.max(-1, Math.min(1, float32Array[i]))
      pcmData[i] = s < 0 ? s * 0x8000 : s * 0x7fff
    }
    return pcmData
  }

  /**
   * VAD 静音检测处理
   */
  function handleVad(rms: number): void {
    const threshold = 0.01 // 静音阈值

    if (rms > threshold) {
      // 有声音，重置静音计时器
      resetVad()
    } else {
      // 静音，启动计时器
      if (!vadTimer) {
        vadTimer = window.setTimeout(() => {
          // 静音超过阈值，触发停止事件
          onVadTrigger()
        }, vadSilenceMs)
      }
    }
  }

  /**
   * 重置 VAD 计时器
   */
  function resetVad(): void {
    if (vadTimer) {
      clearTimeout(vadTimer)
      vadTimer = null
    }
  }

  /**
   * VAD 触发回调
   */
  function onVadTrigger(): void {
    // 可以在这里触发自动停止或通知外部
    console.log('[useAudioRecorder] VAD 检测到静音')
  }

  // 组件卸载时清理
  onUnmounted(() => {
    dispose()
  })

  // ============================================================================
  // 返回
  // ============================================================================

  return {
    // 状态
    recordingState,
    isRecording,
    recordingTime,
    audioLevel,
    error,

    // 方法
    init,
    startRecording,
    stopRecording,
    pauseRecording,
    resumeRecording,
    dispose
  }
}
