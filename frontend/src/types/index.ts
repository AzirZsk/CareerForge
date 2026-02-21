// =====================================================
// LandIt 业务类型定义
// @author Azir
// =====================================================

// 性别类型
export type Gender = 'MALE' | 'FEMALE'

// 用户信息
export interface User {
  id: string
  name: string
  gender: Gender | null
  avatar: string | null
  createdAt: string
}

// 用户状态响应（新版）
export interface UserStatusResponse {
  exists: boolean
  user?: {
    id: number
    name: string
    gender: string
    avatar: string | null
  }
}

// 用户初始化响应
export interface UserInitResponse {
  name: string
  gender: Gender | null
}

// API 统一响应格式
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 简历状态
export type ResumeStatus = 'optimized' | 'draft'

// 简历列表项
export interface Resume {
  id: string
  name: string
  targetPosition: string
  updatedAt: string
  status: ResumeStatus
  score: number
  completeness: number
  isPrimary: boolean
}

// 简历建议类型
export type SuggestionType = 'critical' | 'improvement' | 'enhancement'

// 简历优化建议
export interface ResumeSuggestionItem {
  type: SuggestionType
  content: string
}

// 工作经历内容
export interface WorkExperience {
  company: string
  position: string
  period: string
  description: string
}

// 项目经历内容
export interface ProjectExperience {
  name: string
  role: string
  period: string
  description: string
  achievements: string[]
}

// 简历模块内容类型
export type ResumeSectionContent = Record<string, unknown> | WorkExperience[] | ProjectExperience[] | string[]

// 简历模块
export interface ResumeSection {
  id: string
  type: string
  title: string
  content: ResumeSectionContent
  score: number
  suggestions: ResumeSuggestionItem[]
}

// 简历详情
export interface ResumeDetail {
  id: string
  name: string
  targetPosition: string
  sections: ResumeSection[]
  overallScore: number
  keywordMatch: number
  formatScore: number
  contentScore: number
}

// 简历优化建议
export interface ResumeSuggestion {
  id: string
  type: SuggestionType
  category: string
  title: string
  description: string
  impact: string
  position: string
}

// 面试类型
export type InterviewType = 'technical' | 'behavioral'

// 面试状态
export type InterviewStatus = 'completed' | 'in_progress'

// 面试记录
export interface Interview {
  id: string
  type: InterviewType
  position: string
  company: string
  date: string
  duration: number
  score: number
  status: InterviewStatus
  questions: number
  correctAnswers: number
}

// 面试问题难度
export type QuestionDifficulty = 'easy' | 'medium' | 'hard'

// 面试题目
export interface InterviewQuestion {
  id: string
  category: string
  difficulty: QuestionDifficulty
  question: string
  followUp: string
  keyPoints: string[]
  sampleAnswer: string
}

// 面试问题分类
export interface InterviewQuestions {
  technical: InterviewQuestion[]
  behavioral: InterviewQuestion[]
}

// 对话角色
export type ConversationRole = 'interviewer' | 'candidate'

// 面试对话
export interface Conversation {
  id: string
  role: ConversationRole
  content: string
  timestamp: string
  score?: number
  feedback?: string
}

// 面试分析
export interface InterviewAnalysis {
  strengths: string[]
  weaknesses: string[]
  overallFeedback: string
}

// 面试详情
export interface InterviewDetail {
  id: string
  type: InterviewType
  position: string
  company: string
  date: string
  duration: number
  score: number
  conversation: Conversation[]
  analysis: InterviewAnalysis
}

// 复盘维度
export interface ReviewDimension {
  name: string
  score: number
  maxScore: number
  feedback: string
}

// 问题分析
export interface QuestionAnalysis {
  question: string
  yourAnswer: string
  score: number
  keyPointsCovered: string[]
  keyPointsMissed: string[]
  suggestion: string
}

// 改进计划项
export interface ImprovementPlan {
  category: string
  items: string[]
}

// 面试复盘
export interface InterviewReview {
  id: string
  interviewId: string
  overallScore: number
  analysis: InterviewAnalysis
  dimensions: ReviewDimension[]
  questionAnalysis: QuestionAnalysis[]
  improvementPlan: ImprovementPlan[]
}

// 周进度
export interface WeeklyProgress {
  week: string
  score: number
  interviews: number
}

// 技能雷达
export interface SkillRadar {
  skill: string
  score: number
}

// 活动类型
export type ActivityType = 'interview' | 'resume' | 'practice' | 'review'

// 最近活动
export interface RecentActivity {
  type: ActivityType
  content: string
  time: string
  score: number | null
}

// 统计概览
export interface StatisticsOverview {
  totalInterviews: number
  averageScore: number
  improvementRate: number
  studyHours: number
}

// 统计数据
export interface Statistics {
  overview: StatisticsOverview
  weeklyProgress: WeeklyProgress[]
  skillRadar: SkillRadar[]
  recentActivity: RecentActivity[]
}

// 职位推荐
export interface Job {
  id: string
  company: string
  companyLogo: string | null
  position: string
  salary: string
  location: string
  experience: string
  education: string
  tags: string[]
  matchScore: number
  publishedAt: string
  description: string
}

// 导航项
export interface NavItem {
  key: string
  label: string
  path: string
  icon: string
  badge?: string
}

// 面试设置
export interface InterviewSettings {
  position: string
  difficulty: QuestionDifficulty
  questionCount: number
}

// 会话问题
export interface SessionQuestion {
  id: number
  category: string
  question: string
  keyPoints: string[]
}

// 会话消息
export interface SessionMessage {
  id: number
  role: ConversationRole
  content: string
  timestamp: Date
  score?: number
  feedback?: string
}

// 用户信息更新
export interface UserUpdateInfo {
  name?: string
  gender?: Gender | null
}
