// =====================================================
// 面试中心类型定义
// @author Azir
// =====================================================

// 面试来源类型
export type InterviewSource = 'real' | 'mock'

// 面试状态
export type InterviewStatus = 'preparing' | 'in_progress' | 'completed' | 'cancelled'

// 面试最终结果
export type InterviewResult = 'passed' | 'failed' | 'pending'

// 轮次类型
export type RoundType = 'technical_1' | 'technical_2' | 'hr' | 'director' | 'cto' | 'final' | 'custom'

// 轮次状态
export type RoundStatus = 'pending' | 'in_progress' | 'passed' | 'failed' | 'pending_result' | 'cancelled'

// 准备项类型
export type PreparationItemType = 'company_research' | 'jd_analysis' | 'todo' | 'manual'

// 准备项来源
export type PreparationSource = 'ai_generated' | 'manual'

// 复盘笔记类型
export type ReviewNoteType = 'manual' | 'ai_analysis'

// ==================== 面试相关接口 ====================

// 面试列表项
export interface InterviewListItem {
  id: string
  source: InterviewSource
  companyName: string
  position: string
  interviewDate: string
  status: InterviewStatus
  overallResult?: InterviewResult
  roundCount: number
  completedRounds: number
  createdAt: string
  updatedAt: string
}

// 面试详情
export interface InterviewDetail {
  id: string
  source: InterviewSource
  companyName: string
  position: string
  interviewDate: string
  status: InterviewStatus
  overallResult?: InterviewResult
  jdContent?: string
  notes?: string
  jobPositionId?: string
  companyResearch?: string
  jdAnalysis?: string
  rounds: RoundVO[]
  preparations: PreparationVO[]
  reviewNote?: ReviewNoteVO
  createdAt: string
  updatedAt: string
}

// 轮次 VO
export interface RoundVO {
  id: string
  interviewId: string
  roundType: RoundType
  roundName?: string
  roundOrder: number
  status: RoundStatus
  scheduledDate?: string
  actualDate?: string
  notes?: string
  selfRating?: number
  resultNote?: string
  createdAt: string
  updatedAt: string
}

// 准备项 VO
export interface PreparationVO {
  id: string
  interviewId: string
  itemType: PreparationItemType
  title: string
  content?: string
  completed: boolean
  source: PreparationSource
  sortOrder: number
  createdAt: string
  updatedAt: string
}

// 复盘笔记 VO
export interface ReviewNoteVO {
  id: string
  interviewId: string
  type: ReviewNoteType
  overallFeeling?: string
  highPoints?: string
  weakPoints?: string
  lessonsLearned?: string
  suggestions?: string
  createdAt: string
  updatedAt: string
}

// ==================== 请求接口 ====================

// 创建面试请求
export interface CreateInterviewRequest {
  jobPositionId?: string
  companyName: string
  position: string
  interviewDate: string
  jdContent?: string
  rounds?: AddRoundRequest[]
  notes?: string
}

// 更新面试请求
export interface UpdateInterviewRequest {
  companyName?: string
  position?: string
  interviewDate?: string
  jdContent?: string
  notes?: string
}

// 添加轮次请求
export interface AddRoundRequest {
  roundType: RoundType
  roundName?: string
  scheduledDate?: string
  notes?: string
}

// 更新轮次请求
export interface UpdateRoundRequest {
  roundType?: RoundType
  roundName?: string
  scheduledDate?: string
  actualDate?: string
  notes?: string
  selfRating?: number
  resultNote?: string
}

// 添加准备事项请求
export interface AddPreparationRequest {
  title: string
  content?: string
}

// 更新准备事项请求
export interface UpdatePreparationRequest {
  title?: string
  content?: string
  completed?: boolean
  sortOrder?: number
}

// 保存复盘笔记请求
export interface SaveReviewNoteRequest {
  overallFeeling?: string
  highPoints?: string
  weakPoints?: string
  lessonsLearned?: string
}

// ==================== 枚举标签映射 ====================

// 轮次类型标签
export const ROUND_TYPE_LABELS: Record<RoundType, string> = {
  technical_1: '技术一面',
  technical_2: '技术二面',
  hr: 'HR 面',
  director: '总监面',
  cto: 'CTO/VP 面',
  final: '终面',
  custom: '自定义'
}

// 轮次状态标签
export const ROUND_STATUS_LABELS: Record<RoundStatus, string> = {
  pending: '待面试',
  in_progress: '进行中',
  passed: '已通过',
  failed: '未通过',
  pending_result: '待定',
  cancelled: '已取消'
}

// 面试状态标签
export const INTERVIEW_STATUS_LABELS: Record<InterviewStatus, string> = {
  preparing: '准备中',
  in_progress: '进行中',
  completed: '已完成',
  cancelled: '已取消'
}

// 面试结果标签
export const INTERVIEW_RESULT_LABELS: Record<InterviewResult, string> = {
  passed: '已通过',
  failed: '未通过',
  pending: '待定'
}

// 面试来源标签
export const INTERVIEW_SOURCE_LABELS: Record<InterviewSource, string> = {
  real: '真实面试',
  mock: '模拟面试'
}

// ==================== 工作流相关类型 ====================

// 准备工作流阶段
export type PreparationStage =
  | 'check_company'
  | 'company_research'
  | 'check_job_position'
  | 'jd_analysis'
  | 'generate_preparation'

// 复盘分析工作流阶段
export type ReviewAnalysisStage =
  | 'collect_data'
  | 'analyze_interview'
  | 'generate_advice'

// 准备工作流 SSE 事件
export interface PreparationSSEEvent {
  type: 'progress' | 'complete' | 'error'
  node?: PreparationStage
  progress?: number
  message?: string
  data?: unknown
  error?: string
}

// 复盘分析工作流 SSE 事件
export interface ReviewAnalysisSSEEvent {
  type: 'progress' | 'complete' | 'error'
  node?: ReviewAnalysisStage
  progress?: number
  message?: string
  data?: unknown
  error?: string
}

// 公司调研结果
export interface CompanyResearchResult {
  overview: string
  coreBusiness: string[]
  culture: string
  techStack: string[]
  interviewCharacteristics: string[]
  recentNews: string[]
  preparationTips: string[]
}

// JD分析结果
export interface JDAnalysisResult {
  overview: string
  requiredSkills: string[]
  plusSkills: string[]
  keywords: string[]
  responsibilities: string[]
  requirements: string[]
  interviewFocus: string[]
  preparationTips: string[]
}

// 改进建议
export interface ImprovementAdvice {
  category: string
  title: string
  description: string
  priority: 'high' | 'medium' | 'low'
  actionItems: string[]
}
