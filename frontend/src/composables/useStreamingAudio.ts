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

  /** 是否自己创建的 AudioContext（false 表示外部注入） */
  let ownsAudioContext = true

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
   * @param sampleRate 采样率
   * @param externalContext 外部注入的 AudioContext（共享模式）
   * @returns Promise<boolean> 是否成功启动
   */
  async function initAudioContext(
    sampleRate: number = DEFAULT_SAMPLE_RATE,
    externalContext?: AudioContext
  ): Promise<boolean> {
    if (externalContext) {
      audioContext = externalContext
      ownsAudioContext = false
    } else if (!audioContext) {
      audioContext = new AudioContext({ sampleRate })
      ownsAudioContext = true
    }
    if (!gainNode && audioContext) {
      gainNode = audioContext.createGain()
      gainNode.connect(audioContext.destination)
    }
    return await resumeContext()
  }

  /**
   * 注入外部 AudioContext（用于共享模式）
   */
  function setExternalAudioContext(ctx: AudioContext): void {
    if (ownsAudioContext && audioContext) {
      audioContext.close()
    }
    audioContext = ctx
    ownsAudioContext = false
    if (!gainNode) {
      gainNode = audioContext.createGain()
      gainNode.connect(audioContext.destination)
    }
  }

  /**
   * 恢复 AudioContext
   * @returns Promise<boolean> 是否成功恢复
   */
  async function resumeContext(): Promise<boolean> {
    if (audioContext?.state === 'suspended') {
      try {
        await audioContext.resume()
        return true
      } catch (e) {
        console.warn('[useStreamingAudio] AudioContext resume failed:', e)
        return false
      }
    }
    return audioContext?.state === 'running'
  }

  /**
   * 播放单个音频块
   */
  async function playAudioChunk(
    base64Audio: string,
    _format: 'pcm' | 'wav' = 'wav'
  ): Promise<void> {
    try {
      await initAudioContext()

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
   * 检测音频数据是否包含 WAV 头（RIFF 标识）
   * 阿里云 DashScope TTS 流式返回：只有第一个 delta 带 WAV 头，后续都是裸 PCM
   */
  function hasWavHeader(bytes: Uint8Array): boolean {
    return bytes.length >= 4
      && bytes[0] === 0x52 && bytes[1] === 0x49
      && bytes[2] === 0x46 && bytes[3] === 0x46
  }

  /**
   * 将裸 PCM 数据（16-bit signed, mono）解码为 AudioBuffer
   * DashScope TTS 返回的后续 chunk 不带 WAV 头，需要手动解码
   */
  function decodeRawPCM(bytes: Uint8Array, sampleRate: number): AudioBuffer {
    const int16 = new Int16Array(bytes.buffer, bytes.byteOffset, bytes.byteLength / 2)
    const float32 = new Float32Array(int16.length)
    for (let i = 0; i < int16.length; i++) {
      float32[i] = int16[i] / 32768.0
    }
    const buffer = audioContext!.createBuffer(1, float32.length, sampleRate)
    buffer.copyToChannel(float32, 0)
    return buffer
  }

  /**
   * 解码 Base64 音频
   * 自动检测 WAV 头：有则用浏览器解码，无则手动 PCM 解码
   */
  async function decodeBase64Audio(base64Audio: string): Promise<AudioBuffer> {
    const binaryString = atob(base64Audio)
    const bytes = new Uint8Array(binaryString.length)
    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i)
    }
    const decodedBytes = bytes.buffer.byteLength
    // 有 WAV 头走浏览器原生解码，裸 PCM 走手动解码
    let buffer: AudioBuffer
    if (hasWavHeader(bytes)) {
      buffer = await audioContext!.decodeAudioData(bytes.buffer)
    } else {
      buffer = decodeRawPCM(bytes, audioContext!.sampleRate)
    }
    console.log('[StreamingAudio] 解码完成, 输入字节=', decodedBytes,
      '输出duration=', buffer.duration.toFixed(3) + 's',
      '方式=', hasWavHeader(bytes) ? 'WAV' : 'PCM',
      'AudioContext.state=', audioContext!.state)
    return buffer
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
      if (ownsAudioContext) {
        audioContext.close()
      }
      audioContext = null
      gainNode = null
      ownsAudioContext = true
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
    setExternalAudioContext,
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
