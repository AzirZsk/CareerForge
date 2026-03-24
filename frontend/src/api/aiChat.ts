// =====================================================
// LandIt AI聊天 API
// @author Azir
// =====================================================

import type { ChatEvent, SectionChange } from '@/types/ai-chat'

const API_BASE = '/landit'

/**
 * 聊天历史消息类型
 */
export interface ChatHistoryMessage {
  id: string
  role: string
  content: string
  createdAt: string
}

/**
 * 解析 SSE 行为 ChatEvent
 */
function parseSseLine(line: string): ChatEvent | null {
  const trimmedLine = line.trim()
  if (!trimmedLine.startsWith('data:')) {
    return null
  }

  const jsonStr = trimmedLine.slice(5).trim()
  if (!jsonStr) {
    return null
  }

  try {
    return JSON.parse(jsonStr) as ChatEvent
  } catch (e) {
    console.error('[AIChat] 解析事件失败', e, jsonStr)
    return null
  }
}

/**
 * 流式聊天
 * 不再发送历史消息，历史消息由后端从数据库加载
 */
export async function* streamChat(
  message: string,
  resumeId: string | null,
  image: File | null
): AsyncGenerator<ChatEvent, void, unknown> {
  const formData = new FormData()

  if (resumeId) {
    formData.append('resumeId', resumeId)
  }
  formData.append('currentUserMessage', message)

  if (image) {
    formData.append('image', image)
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
        const event = parseSseLine(line)
        if (event) {
          yield event
        }
      }

      if (done) {
        if (buffer.trim()) {
          for (const line of buffer.split('\n')) {
            const event = parseSseLine(line)
            if (event) {
              yield event
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

/**
 * 获取聊天历史
 * 从后端数据库加载指定简历的聊天历史
 */
export async function getChatHistory(resumeId: string): Promise<ChatHistoryMessage[]> {
  const response = await fetch(`${API_BASE}/chat/history/${resumeId}`)
  const result = await response.json()

  if (result.code !== 200) {
    throw new Error(result.message || '获取历史失败')
  }

  return result.data
}

/**
 * 清空聊天历史
 */
export async function clearChatHistory(resumeId: string): Promise<void> {
  const response = await fetch(`${API_BASE}/chat/history/${resumeId}`, {
    method: 'DELETE'
  })
  const result = await response.json()

  if (result.code !== 200) {
    throw new Error(result.message || '清空历史失败')
  }
}

/**
 * 应用修改
 */
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
