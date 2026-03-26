/**
 * 流式音频播放 Composable
 * 支持边接收边播放，实现低延迟音频播放
 *
 * @author Azir
 */

import { ref, computed, onUnmounted } from 'vue'
import type { PlaybackState, AudioPlayerState } from '@/types/interview-voice'

/** 默认采样率 */
const DEFAULT_SAMPLE_RATE = 16000

/** 默认音量 */
const DEFAULT_VOLUME = 0.8

/**
 * 流式音频播放器
 * 支持队列播放，自动衔接多个音频片段
 */
export function useStreamingAudio() {
  // ============================================================================
  // 状态
  // ============================================================================

  /** AudioContext 实例 */
  let audioContext: AudioContext | null = null

  /** 音频队列 */
  const audioQueue: AudioBuffer[] = []

  /** 当前播放源 */
  let currentSource: AudioBufferSourceNode | null = null

  /** 增益节点（用于音量控制） */
  let gainNode: GainNode | null = null

  /** 是否正在播放 */
  const isPlaying = ref(false)

  /** 播放状态 */
  const playbackState = ref<PlaybackState>('idle')

  /** 当前时间 */
  const currentTime = ref(0)

  /** 总时长 */
  const duration = ref(0)

  /** 音量 */
  const volume = ref(DEFAULT_VOLUME)

  /** 是否静音 */
  const muted = ref(false)

  /** 增量文本缓冲 */
  const textBuffer = ref('')

  /** 完整文本 */
  const fullText = ref('')

  /** 错误信息 */
  const error = ref<string | null>(null)

  // ============================================================================
  // 计算属性
  // ============================================================================

  const playerState = computed<AudioPlayerState>(() => ({
    playbackState: playbackState.value,
    currentTime: currentTime.value,
    duration: duration.value,
    volume: volume.value,
    muted: muted.value
  }))

  // ============================================================================
  // 核心方法
  // ============================================================================

  /**
   * 初始化 AudioContext
   */
  function initAudioContext(sampleRate: number = DEFAULT_SAMPLE_RATE): void {
    if (!audioContext) {
      audioContext = new AudioContext({ sampleRate })
      gainNode = audioContext.createGain()
      gainNode.connect(audioContext.destination)
    }
    resumeContext()
  }

  /**
   * 恢复 AudioContext
   */
  function resumeContext(): void {
    if (audioContext?.state === 'suspended') {
      audioContext.resume()
    }
  }

  /**
   * 播放单个音频块
   */
  async function playAudioChunk(
    base64Audio: string,
    _format: 'pcm' | 'wav' = 'wav'
  ): Promise<void> {
    try {
      initAudioContext()

      const audioBuffer = await decodeBase64Audio(base64Audio)
      audioQueue.push(audioBuffer)

      if (!isPlaying.value) {
        playNextChunk()
      }
    } catch (e) {
      console.error('[useStreamingAudio] 音频解码失败:', e)
      error.value = e instanceof Error ? e.message : '音频解码失败'
    }
  }

  /**
   * 解码 Base64 音频
   */
  async function decodeBase64Audio(base64Audio: string): Promise<AudioBuffer> {
    const binaryString = atob(base64Audio)
    const bytes = new Uint8Array(binaryString.length)
    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i)
    }
    return audioContext!.decodeAudioData(bytes.buffer)
  }

  /**
   * 播放下一个音频片段
   */
  function playNextChunk(): void {
    if (audioQueue.length === 0 || !audioContext || !gainNode) {
      isPlaying.value = false
      playbackState.value = 'idle'
      return
    }

    isPlaying.value = true
    playbackState.value = 'playing'

    const buffer = audioQueue.shift()!
    currentSource = audioContext.createBufferSource()
    currentSource.buffer = buffer
    currentSource.connect(gainNode)

    updateGain()

    currentSource.onended = () => {
      duration.value += buffer.duration
      playNextChunk()
    }

    currentSource.start()
  }

  /**
   * 更新增益（音量控制）
   */
  function updateGain(): void {
    if (gainNode) {
      gainNode.gain.value = muted.value ? 0 : volume.value
    }
  }

  /**
   * 添加增量文本
   */
  function appendText(text: string, isComplete: boolean = false): void {
    textBuffer.value = text
    fullText.value = isComplete ? text : fullText.value + text
  }

  /**
   * 清空文本缓冲
   */
  function clearText(): void {
    textBuffer.value = ''
    fullText.value = ''
  }

  /**
   * 暂停播放
   */
  function pause(): void {
    if (currentSource && isPlaying.value) {
      currentSource.stop()
      currentSource = null
      isPlaying.value = false
      playbackState.value = 'paused'
    }
  }

  /**
   * 恢复播放
   */
  function resume(): void {
    if (!isPlaying.value && audioQueue.length > 0) {
      playNextChunk()
    }
  }

  /**
   * 停止播放并清空队列
   */
  function stop(): void {
    if (currentSource) {
      currentSource.stop()
      currentSource = null
    }
    audioQueue.length = 0
    isPlaying.value = false
    playbackState.value = 'idle'
    currentTime.value = 0
    duration.value = 0
  }

  /**
   * 设置音量
   */
  function setVolume(value: number): void {
    volume.value = Math.max(0, Math.min(1, value))
    updateGain()
  }

  /**
   * 切换静音
   */
  function toggleMute(): void {
    muted.value = !muted.value
    updateGain()
  }

  /**
   * 清理资源
   */
  function dispose(): void {
    stop()
    if (audioContext) {
      audioContext.close()
      audioContext = null
      gainNode = null
    }
  }

  onUnmounted(dispose)

  // ============================================================================
  // 返回
  // ============================================================================

  return {
    // 状态
    isPlaying,
    playbackState,
    currentTime,
    duration,
    volume,
    muted,
    textBuffer,
    fullText,
    error,
    playerState,

    // 方法
    initAudioContext,
    playAudioChunk,
    appendText,
    clearText,
    pause,
    resume,
    stop,
    setVolume,
    toggleMute,
    dispose
  }
}
