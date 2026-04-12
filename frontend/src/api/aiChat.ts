// =====================================================
// LandIt AI聊天 API
// @author Azir
// =====================================================

import type { ActionStatusType, ChatEvent, ContentSegment, SectionChange } from '@/types/ai-chat'
import { authFetch } from '@/utils/request'
import { API_BASE } from './config'

/**
 * 聊天历史消息类型
 */
export interface ChatHistoryMessage {
  id: string
  role: string
  content: string
  createdAt: string
  actions?: SectionChange[]
  actionStatus?: ActionStatusType
  segments?: ContentSegment[]
  resumeId?: string // 该消息关联的简历ID
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
 * AI 会自动识别用户意图并选择相应的简历
 * sessionId 用于维护对话上下文
 */
export async function* streamChat(
  message: string,
  sessionId: string,
  images: File[] = []
): AsyncGenerator<ChatEvent, void, unknown> {
  const formData = new FormData()

  // sessionId 每次会话必须传递
  formData.append('sessionId', sessionId)

  formData.append('currentUserMessage', message)

  // 多图处理
  if (images.length > 0) {
    images.forEach((image) => {
      formData.append('images', image)
    })
  }

  const response = await authFetch(`${API_BASE}/chat/stream`, {
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
 * 从后端数据库加载指定会话的聊天历史
 */
export async function getChatHistory(sessionId: string): Promise<ChatHistoryMessage[]> {
  const response = await authFetch(`${API_BASE}/chat/history/${sessionId}`)
  const result = await response.json()

  if (result.code !== 200) {
    throw new Error(result.message || '获取历史失败')
  }

  return result.data
}

/**
 * 清空聊天历史
 */
export async function clearChatHistory(sessionId: string): Promise<void> {
  const response = await authFetch(`${API_BASE}/chat/history/${sessionId}`, {
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
  const response = await authFetch(`${API_BASE}/chat/apply`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ resumeId, changes })
  })

  const result = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '应用修改失败')
  }
}

/**
 * 更新消息操作状态
 * @param messageId 消息ID
 * @param status 操作状态（pending / applied / failed）
 */
export async function updateActionStatus(
  messageId: string,
  status: ActionStatusType
): Promise<void> {
  const response = await authFetch(
    `${API_BASE}/chat/messages/${messageId}/status?status=${status}`,
    { method: 'PATCH' }
  )
  const result = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '更新状态失败')
  }
}
