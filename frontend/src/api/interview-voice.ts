// =====================================================
// LandIt 语音面试 API
// @author Azir
// =====================================================

import type { RecordingInfo } from '@/types/interview-voice'
import request from '@/utils/request'
import { authFetch } from '@/utils/request'
import { API_BASE } from './config'

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
  regenerateQuestions?: boolean
}

/** 创建语音面试会话响应 */
export interface CreateSessionResponse {
  sessionId: string
  interviewId: string
  position: string
  voiceMode: string
  totalQuestions: number
  assistLimit: number
  questionsReused?: boolean
}

/**
 * 创建语音面试会话
 * 从真实面试详情页进入，关联 Interview
 * @param req 创建请求
 */
export async function createSession(req: CreateSessionRequest): Promise<CreateSessionResponse> {
  return request({
    url: '/interviews/sessions',
    method: 'POST',
    data: req
  })
}

// ============================================================================
// 录音回放 API
// ============================================================================

/**
 * 获取录音回放信息
 * @param sessionId 面试会话 ID
 */
export async function getRecordingInfo(sessionId: string): Promise<RecordingInfo> {
  return request({
    url: `/recordings/${sessionId}`,
    method: 'GET'
  })
}

/**
 * 获取合并后的音频流
 * @param sessionId 面试会话 ID
 * @returns 音频 Blob URL
 */
export async function getMergedAudio(sessionId: string): Promise<string> {
  const response = await authFetch(`${API_BASE}/recordings/${sessionId}/audio`)
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
  const response = await authFetch(
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
  return request({
    url: `/interviews/sessions/${sessionId}/assist/remaining`,
    method: 'GET'
  })
}

