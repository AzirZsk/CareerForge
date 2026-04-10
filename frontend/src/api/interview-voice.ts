// =====================================================
// LandIt 语音面试 API
// @author Azir
// =====================================================

import request from '@/utils/request'
import type { ApiResponse } from '@/types'
import type { RecordingInfo } from '@/types/interview-voice'

// ============================================================================
// 创建会话 API
// ============================================================================

/** 创建语音面试会话请求 */
export interface CreateSessionRequest {
  interviewId: string
  totalQuestions?: number
  assistLimit?: number
  voiceMode?: string
  interviewerStyle?: string
}

/** 创建语音面试会话响应 */
export interface CreateSessionResponse {
  sessionId: string
  interviewId: string
  position: string
  voiceMode: string
  totalQuestions: number
  assistLimit: number
}

/**
 * 创建语音面试会话
 * 从真实面试详情页进入，关联 Interview
 * @param request 创建请求
 */
export async function createSession(request: CreateSessionRequest): Promise<CreateSessionResponse> {
  const response = await fetch(`${API_BASE}/interviews/sessions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request)
  })
  const result: ApiResponse<CreateSessionResponse> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '创建会话失败')
  }
  return result.data
}

// ============================================================================
// 录音回放 API
// ============================================================================

/**
 * 获取录音回放信息
 * @param sessionId 面试会话 ID
 */
export async function getRecordingInfo(sessionId: string): Promise<RecordingInfo> {
  const response = await fetch(`${API_BASE}/recordings/${sessionId}`)
  const result: ApiResponse<RecordingInfo> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '获取录音信息失败')
  }
  return result.data
}

/**
 * 获取合并后的音频流
 * @param sessionId 面试会话 ID
 * @returns 音频 Blob URL
 */
export async function getMergedAudio(sessionId: string): Promise<string> {
  const response = await fetch(`${API_BASE}/recordings/${sessionId}/audio`)
  if (!response.ok) {
    throw new Error('获取录音音频失败')
  }
  const blob = await response.blob()
  return URL.createObjectURL(blob)
}

/**
 * 获取单个片段音频
 * @param sessionId 面试会话 ID
 * @param segmentIndex 片段序号
 * @returns 音频 Blob URL
 */
export async function getSegmentAudio(
  sessionId: string,
  segmentIndex: number
): Promise<string> {
  const response = await fetch(
    `${API_BASE}/recordings/${sessionId}/segments/${segmentIndex}/audio`
  )
  if (!response.ok) {
    throw new Error('获取片段音频失败')
  }
  const blob = await response.blob()
  return URL.createObjectURL(blob)
}

// ============================================================================
// 求助次数 API
// ============================================================================

/** 求助次数响应 */
export interface AssistRemainingResponse {
  remaining: number
  limit: number
}

/**
 * 获取求助剩余次数
 * @param sessionId 面试会话 ID
 */
export async function getAssistRemaining(
  sessionId: string
): Promise<AssistRemainingResponse> {
  const response = await fetch(`${API_BASE}/interviews/sessions/${sessionId}/assist/remaining`)
  const result: ApiResponse<AssistRemainingResponse> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '获取求助次数失败')
  }
  return result.data
}

// ============================================================================
// SSE 流式求助 API
// ============================================================================

/** SSE 事件类型 */
export type SSEEventType = 'text' | 'audio' | 'done' | 'error'

/** SSE 事件数据 */
export interface SSEEventData {
  type: SSEEventType
  data: unknown
}

/**
 * 创建 SSE 流式求助连接
 * @param sessionId 面试会话 ID
 * @param assistType 求助类型
 * @param options 可选参数
 */
export function createAssistStream(
  sessionId: string,
  assistType: string,
  options?: {
    question?: string
    candidateDraft?: string
  }
): EventSource {
  const params = new URLSearchParams({
    type: assistType
  })

  if (options?.question) {
    params.append('question', options.question)
  }
  if (options?.candidateDraft) {
    params.append('candidateDraft', options.candidateDraft)
  }

  const url = `${API_BASE}/interviews/sessions/${sessionId}/assist/stream?${params.toString()}`
  return new EventSource(url)
}
