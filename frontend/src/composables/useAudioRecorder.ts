/**
 * 音频录制 Composable
 * 支持麦克风采集、PCM 转换
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

  /** 是否自己创建的 AudioContext（false 表示外部注入） */
  let ownsAudioContext = true

  /** 音频处理器 */
  let processor: ScriptProcessorNode | null = null

  /** 录制计时器 */
  let recordingTimer: number | null = null

  /** 音频数据回调 */
  let onAudioData: ((data: Int16Array) => void) | null = null


  // ============================================================================
  // 核心方法
  // ============================================================================

  /**
   * 初始化音频录制
   * @param options 配置选项
   */
  async function init(options: {
    onAudioData?: (data: Int16Array) => void
    audioContext?: AudioContext
  } = {}): Promise<void> {
    onAudioData = options.onAudioData || null

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

      // 使用外部注入或自行创建 AudioContext
      if (options.audioContext) {
        audioContext = options.audioContext
        ownsAudioContext = false
      } else {
        audioContext = new AudioContext({ sampleRate: 16000 })
        ownsAudioContext = true
      }

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
      if (ownsAudioContext) {
        audioContext.close()
      }
      audioContext = null
      ownsAudioContext = true
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

    // 计算音量电平（始终执行，UI波形显示需要）
    const rms = calculateRms(inputData)
    audioLevel.value = rms

    // 直接发送所有音频帧，静音判断交给后端
    sendPcmFrame(inputData)
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
   * 将 Float32 音频帧转换为 PCM 并通过回调发送
   */
  function sendPcmFrame(inputData: Float32Array): void {
    const pcmData = float32ToPcm(inputData)
    if (onAudioData) {
      onAudioData(pcmData)
    }
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
