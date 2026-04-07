/**
 * 音频转录 Composable
 * 用于面试复盘场景上传音频文件自动转文字
 *
 * @author Azir
 */

import { reactive, readonly } from 'vue'
import type { AudioTranscribeState, TranscribeResult } from '@/types/interview-center'

// 支持的音频格式
const SUPPORTED_FORMATS = ['wav', 'mp3', 'm4a', 'aac', 'ogg', 'flac', 'opus', 'webm', 'mov', 'mp4']
const MAX_FILE_SIZE_MB = 50

/**
 * 音频转录 Composable
 * @param interviewId 面试ID
 */
export function useAudioTranscribe(interviewId: string) {
  // 状态（单例 reactive）
  const state = reactive<AudioTranscribeState>({
    status: 'idle',
    progress: 0,
    message: '',
    transcriptText: '',
    error: null
  })

  let abortController: AbortController | null = null

  /**
   * 获取文件扩展名
   */
  function getFileExtension(filename: string): string {
    if (!filename) return ''
    const dotIndex = filename.lastIndexOf('.')
    return dotIndex > 0 ? filename.substring(dotIndex + 1).toLowerCase() : ''
  }

  /**
   * 验证文件格式
   */
  function validateFile(file: File): string | null {
    const extension = getFileExtension(file.name)

    if (!extension || !SUPPORTED_FORMATS.includes(extension)) {
      return `不支持的音频格式: .${extension || '未知'}，支持 ${SUPPORTED_FORMATS.map(f => `.${f}`).join('/')} 等格式`
    }

    const fileSizeMb = file.size / (1024 * 1024)
    if (fileSizeMb > MAX_FILE_SIZE_MB) {
      return `文件大小超出限制: 最大 ${MAX_FILE_SIZE_MB}MB，当前 ${fileSizeMb.toFixed(1)}MB`
    }

    return null
  }

  /**
   * 转录音频文件
   * @param file 音频文件
   */
  async function transcribe(file: File): Promise<void> {
    // 验证文件
    const validationError = validateFile(file)
    if (validationError) {
      state.status = 'failed'
      state.error = validationError
      return
    }

    // 重置状态
    state.status = 'uploading'
    state.progress = 0
    state.message = '正在上传音频文件...'
    state.transcriptText = ''
    state.error = null

    abortController = new AbortController()

    try {
      const formData = new FormData()
      formData.append('file', file)

      const response = await fetch(
        `/landit/interview-center/${interviewId}/review-analysis/transcribe`,
        {
          method: 'POST',
          body: formData,
          signal: abortController.signal
        }
      )

      if (!response.ok) {
        throw new Error(`上传失败: ${response.status} ${response.statusText}`)
      }

      state.status = 'processing'
      state.message = '正在转录音频...'

      // 读取 SSE 流
      const reader = response.body?.getReader()
      if (!reader) {
        throw new Error('无法读取响应流')
      }

      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        // 解析 SSE 事件
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('event:')) {
            continue
          }

          if (line.startsWith('data:')) {
            const jsonStr = line.slice(5).trim()
            if (!jsonStr) continue

            try {
              const result: TranscribeResult = JSON.parse(jsonStr)
              handleTranscribeResult(result)
            } catch (e) {
              console.warn('[AudioTranscribe] 解析结果失败:', e)
            }
          }
        }
      }

      // 流结束但未收到完成/失败状态时，标记为完成
      const finalStatus = state.status as string
      if (finalStatus === 'uploading' || finalStatus === 'processing') {
        state.status = 'completed'
        state.progress = 100
      }

    } catch (error) {
      if ((error as Error).name === 'AbortError') {
        console.log('[AudioTranscribe] 已取消')
        return
      }

      console.error('[AudioTranscribe] 转录失败:', error)
      state.status = 'failed'
      state.error = (error as Error).message || '转录失败，请稍后重试'
    }
  }

  /**
   * 处理转录结果
   */
  function handleTranscribeResult(result: TranscribeResult): void {
    if (result.status === 'processing') {
      state.progress = result.progress ?? state.progress
      state.message = result.message ?? '正在转录...'
    } else if (result.status === 'completed') {
      state.transcriptText = result.text ?? ''
      state.progress = 100
      state.status = 'completed'
      state.message = '转录完成'
    } else if (result.status === 'failed') {
      state.status = 'failed'
      state.error = result.errorMessage || '转录失败'
    }
  }

  /**
   * 取消转录
   */
  function cancel(): void {
    if (abortController) {
      abortController.abort()
      abortController = null
    }
    state.status = 'idle'
    state.progress = 0
    state.message = ''
  }

  /**
   * 重置状态
   */
  function reset(): void {
    cancel()
    state.transcriptText = ''
    state.error = null
  }

  return {
    state: readonly(state),
    transcribe,
    cancel,
    reset,
    validateFile
  }
}

/**
 * 获取支持的音频格式描述
 */
export function getSupportedFormatsDescription(): string {
  return '支持 wav/mp3/m4a/aac/ogg/flac 等格式，最大 50MB'
}
