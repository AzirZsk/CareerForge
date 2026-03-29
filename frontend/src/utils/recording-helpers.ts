/**
 * 录音回放相关辅助函数
 *
 * @author Azir
 */

import type { ConversationRole, RecordingSegment } from '@/types/interview-voice'

/**
 * 角色名称映射
 */
export const roleNames: Record<ConversationRole, string> = {
  interviewer: '面试官',
  candidate: '候选人',
  assistant: 'AI 助手'
}

/**
 * 获取角色显示名称
 */
export function getRoleName(role: ConversationRole): string {
  return roleNames[role] || '未知'
}

/**
 * 格式化时间（秒 -> MM:SS）
 */
export function formatTime(seconds: number): string {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

/**
 * 格式化时间戳（毫秒 -> HH:MM）
 */
export function formatTimestamp(timestamp: number): string {
  if (!timestamp) return '00:00'
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

/**
 * 截断文本
 */
export function truncateText(text: string, maxLength: number): string {
  if (!text) return ''
  return text.length > maxLength ? text.slice(0, maxLength) + '...' : text
}

/**
 * 计算片段的累计时间偏移量
 * 返回每个片段的开始时间（秒）
 */
export function calculateSegmentTimeOffsets(segments: RecordingSegment[]): number[] {
  const offsets: number[] = []
  let accumulatedTime = 0
  for (const segment of segments) {
    offsets.push(accumulatedTime)
    accumulatedTime += (segment.durationMs || 0) / 1000
  }
  return offsets
}

/**
 * 根据当前时间找到对应的片段索引
 */
export function findSegmentIndexByTime(
  segments: RecordingSegment[],
  currentTime: number
): number {
  let accumulatedTime = 0
  for (let i = 0; i < segments.length; i++) {
    const segmentDuration = (segments[i].durationMs || 0) / 1000
    if (currentTime < accumulatedTime + segmentDuration) {
      return i
    }
    accumulatedTime += segmentDuration
  }
  return segments.length - 1
}
