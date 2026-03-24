// =====================================================
// LandIt AI聊天 API
// @author Azir
// =====================================================

import type { ChatEvent, SectionChange } from '@/types/ai-chat'

const API_BASE = '/landit'

export async function* streamChat(
  message: string,
  resumeId: string | null,
  image: File | null,
  history: Array<{ role: string; content: string }>
): AsyncGenerator<ChatEvent, void, unknown> {
  const formData = new FormData()
  formData.append('currentUserMessage', message)

  if (resumeId) {
    formData.append('resumeId', resumeId)
  }

  if (image) {
    formData.append('image', image)
  }

  if (history.length > 0) {
    formData.append('messages', JSON.stringify(history))
  }

  const response = await fetch(`${API_BASE}/chat/stream`, {
    method: 'POST',
    body: formData
  })

  if (!response.ok) {
    throw new Error(`网络错误: ${response.status}`)
  }

  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('无法读取响应流')
  }

  const decoder = new TextDecoder()
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()

      buffer += decoder.decode(value, { stream: true })

      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmedLine = line.trim()
        if (trimmedLine.startsWith('data:')) {
          const jsonStr = trimmedLine.slice(5).trim()
          if (jsonStr) {
            try {
              yield JSON.parse(jsonStr) as ChatEvent
            } catch (e) {
              console.error('[AIChat] 解析事件失败', e, jsonStr)
            }
          }
        }
      }

      if (done) {
        if (buffer.trim()) {
          const remainingLines = buffer.split('\n')
          for (const line of remainingLines) {
            const trimmedLine = line.trim()
            if (trimmedLine.startsWith('data:')) {
              const jsonStr = trimmedLine.slice(5).trim()
              if (jsonStr) {
                try {
                  yield JSON.parse(jsonStr) as ChatEvent
                } catch (e) {
                  console.error('[AIChat] 解析残留事件失败', e, jsonStr)
                }
              }
            }
          }
        }
        break
      }
    }
  } finally {
    reader.releaseLock()
  }
}

export async function applyChanges(
  resumeId: string,
  changes: SectionChange[]
): Promise<void> {
  const response = await fetch(`${API_BASE}/chat/apply`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ resumeId, changes })
  })

  const result = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '应用修改失败')
  }
}
