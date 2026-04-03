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

// 轮次类型（面试属性）
export type RoundType = 'technical_1' | 'technical_2' | 'hr' | 'director' | 'cto' | 'final' | 'custom'

// 面试类型
export type InterviewType = 'onsite' | 'online'

// 准备项类型
export type PreparationItemType =
  | 'company_research'
  | 'jd_keywords'
  | 'tech_prep'
  | 'behavioral'
  | 'case_study'
  | 'todo'
  | 'manual'

// 准备项来源
export type PreparationSource = 'ai_generated' | 'manual'

// 准备项优先级
export type PreparationPriority = 'required' | 'recommended' | 'optional'

// 资源类型
export type ResourceType = 'link' | 'note' | 'code' | 'video'

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
  roundType?: RoundType
  interviewType?: InterviewType
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
  roundType?: RoundType
  roundName?: string
  interviewType?: InterviewType
  location?: string
  onlineLink?: string
  meetingPassword?: string
  jdContent?: string
  notes?: string
  jobPositionId?: string
  resumeId?: string
  resumeName?: string
  companyResearch?: string
  jdAnalysis?: string
  preparations: PreparationVO[]
  reviewNote?: ReviewNoteVO
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
  priority: PreparationPriority
  resources?: PreparationResource[]
  sortOrder: number
  createdAt: string
  updatedAt: string
}

// 准备项资源
export interface PreparationResource {
  type: ResourceType
  title: string
  url?: string
  content?: string
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
  resumeId?: string
  companyName: string
  position: string
  interviewDate: string
  roundType: RoundType
  roundName?: string
  interviewType?: InterviewType
  location?: string
  onlineLink?: string
  meetingPassword?: string
  jdContent?: string
  notes?: string
}

// 更新面试请求
export interface UpdateInterviewRequest {
  companyName?: string
  position?: string
  interviewDate?: string
  roundType?: RoundType
  roundName?: string
  interviewType?: InterviewType
  location?: string
  onlineLink?: string
  meetingPassword?: string
  jdContent?: string
  notes?: string
  status?: InterviewStatus
  overallResult?: InterviewResult
  resumeId?: string
}

// 添加准备事项请求
export interface AddPreparationRequest {
  title: string
  content?: string
  priority?: PreparationPriority
  resources?: PreparationResource[]
}

// 更新准备事项请求
export interface UpdatePreparationRequest {
  title?: string
  content?: string
  completed?: boolean
  priority?: PreparationPriority
  resources?: PreparationResource[]
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

// 面试类型标签
export const INTERVIEW_TYPE_LABELS: Record<InterviewType, string> = {
  onsite: '现场面试',
  online: '线上面试'
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

// 优先级标签（含颜色）
export const PRIORITY_CONFIG: Record<
  PreparationPriority,
  { label: string; color: string; icon: string }
> = {
  required: { label: '必做', color: '#f87171', icon: '🔴' },
  recommended: { label: '推荐', color: '#fbbf24', icon: '🟡' },
  optional: { label: '可选', color: '#34d399', icon: '🟢' }
}

// 准备项类型分组标签
export const ITEM_TYPE_CONFIG: Record<string, { label: string; icon: string }> = {
  company_research: { label: '公司调研', icon: '🏢' },
  jd_keywords: { label: 'JD 关键词', icon: '📋' },
  tech_prep: { label: '技术准备', icon: '💻' },
  behavioral: { label: '行为面试', icon: '🗣️' },
  case_study: { label: '案例准备', icon: '📦' },
  todo: { label: '准备事项', icon: '📝' },
  manual: { label: '其他准备', icon: '📌' }
}

// 准备项类型排序顺序
export const ITEM_TYPE_ORDER: PreparationItemType[] = [
  'company_research',
  'jd_keywords',
  'tech_prep',
  'behavioral',
  'case_study',
  'todo',
  'manual'
]

// 优先级排序权重
export const PRIORITY_ORDER: Record<PreparationPriority, number> = {
  required: 0,
  recommended: 1,
  optional: 2
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

// ==================== 工作流进度事件（SSE） ====================

// 工作流进度事件（后端返回的通用格式）
export interface GraphProgressEvent {
  event: 'start' | 'progress' | 'complete' | 'error'
  nodeId?: string
  threadId?: string
  progress?: number
  message?: string
  cached?: boolean  // 是否使用缓存（节点跳过执行）
  data?: Record<string, unknown>
  errorMessage?: string
  timestamp: number
}

// 阶段历史项（用于追踪每个阶段的执行状态和计时）
export interface PreparationStageHistoryItem {
  stage: PreparationStage
  message: string
  timestamp: number
  startTime?: number
  endTime?: number
  completed: boolean
  cached?: boolean  // 是否使用缓存（跳过实际执行）
  data: CompanyResearchResult | JDAnalysisResult | PreparationItem[] | null
  expanded: boolean
}

// 面试准备工作流状态
export interface PreparationState {
  isConnecting: boolean
  isRunning: boolean
  isCompleted: boolean
  hasError: boolean
  currentStage: string
  progress: number
  message: string
  preparationItems: PreparationItem[]
  errorMessage: string | null
  stageHistory: PreparationStageHistoryItem[]
}

// 复盘分析工作流状态
export interface ReviewAnalysisState {
  isConnecting: boolean
  isRunning: boolean
  isCompleted: boolean
  hasError: boolean
  currentStage: string
  progress: number
  message: string
  adviceList: AdviceItem[]
  errorMessage: string | null
}

// 准备事项（工作流生成/后端返回的实体格式）
export interface PreparationItem {
  id: string
  interviewId: string
  itemType: string        // 类型：company_research/jd_keywords/tech_prep/case_study/behavioral/todo
  title: string
  content: string         // 内容（后端用 content，不是 description）
  completed: boolean
  source: string          // 来源：ai_generated/manual
  sortOrder: number
  priority: string        // 优先级：required/recommended/optional
  resources?: string      // 关联资源（JSON字符串）
  createdAt?: string
  updatedAt?: string
}

// 改进建议（工作流生成）
export interface AdviceItem {
  title: string
  description: string
  category?: string
  priority?: 'high' | 'medium' | 'low'
}
