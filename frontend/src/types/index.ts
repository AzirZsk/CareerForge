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
    id: string
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
export type ResumeStatus = 'OPTIMIZED' | 'DRAFT'

// 主简历VO
export interface PrimaryResumeVO {
  id: string
  name: string
  targetPosition: string
  status: ResumeStatus
  score: number
  completeness: number
  analyzed: boolean
  createdAt: string
  updatedAt: string
}

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

// 简历模块类型枚举
export type SectionType = 'BASIC_INFO' | 'EDUCATION' | 'WORK' | 'PROJECT' | 'SKILLS' | 'CERTIFICATE' | 'OPEN_SOURCE' | 'CUSTOM'

// 基本信息（后端实际字段）
export interface BasicInfoContent {
  name: string
  gender?: string
  birthday?: string
  age?: string
  phone?: string
  email?: string
  targetPosition?: string
  summary?: string
  location?: string
  linkedin?: string
  github?: string
  website?: string
}

// 教育经历内容
export interface EducationContent {
  school: string
  major?: string
  degree?: string
  period?: string
  gpa?: string
  courses?: string[]
  honors?: string[]
}

// 工作经历内容
export interface WorkExperience {
  company: string
  position?: string
  period?: string
  description?: string
  location?: string
  achievements?: string[]
  technologies?: string[]
}

// 项目经历内容
export interface ProjectExperience {
  name: string
  role?: string
  period?: string
  description?: string
  achievements?: string[]
  technologies?: string[]
  url?: string
}

// 技能内容
export interface Skill {
  name: string
  description?: string
  level?: string
  category?: string
}

// 技能模块内容
export interface SkillsContent {
  skills: Skill[]
}

// 证书内容
export interface CertificateContent {
  name: string
  date?: string
  issuer?: string
  credentialId?: string
  url?: string
}

// 开源贡献内容
export interface OpenSourceContribution {
  projectName: string
  url: string
  role?: string
  period?: string
  description?: string
  achievements?: string[]
}

// 通用内容项（用于自定义区块）
export interface ContentItem {
  name: string
  role?: string
  period?: string
  description?: string
  highlights?: string[]
}

// 自定义区块
export interface CustomSection {
  title: string
  items: ContentItem[]
}

// 简历模块内容类型（支持后端单个对象和 mock 数组格式）
export type ResumeSectionContent =
  | BasicInfoContent
  | EducationContent
  | WorkExperience
  | WorkExperience[]
  | ProjectExperience
  | ProjectExperience[]
  | SkillsContent
  | string[]
  | CertificateContent
  | OpenSourceContribution
  | CustomSection
  | CustomSection[]
  | Record<string, unknown>

// 简历模块聚合项（用于 PROJECT、WORK、EDUCATION、CERTIFICATE 类型）
export interface ResumeSectionItem {
  id: string
  title: string
  content: Record<string, unknown>
  score: number | null
}

// 简历模块
// 支持两种模式：
// - 单条模式：使用 content 字段（BASIC_INFO）
// - 聚合模式：使用 items 字段（PROJECT、WORK、EDUCATION、CERTIFICATE、SKILLS）
export interface ResumeSection {
  id: string
  type: string
  title: string
  content: ResumeSectionContent | null
  items: ResumeSectionItem[] | null
  score: number
  suggestions: ResumeSuggestionItem[] | null
}

// 简历详情
export interface ResumeDetail {
  id: string
  name: string
  targetPosition: string
  sections: ResumeSection[]
  overallScore: number
  contentScore: number
  structureScore: number
  matchingScore: number
  competitivenessScore: number
  analyzed: boolean
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
  id: string
  category: string
  question: string
  keyPoints: string[]
}

// 会话消息
export interface SessionMessage {
  id: string
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
