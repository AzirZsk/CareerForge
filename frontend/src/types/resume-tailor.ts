// =====================================================
// LandIt 职位适配简历相关类型定义
// @author Azir
// =====================================================

// 从主类型文件导入并重新导出
import type { ResumeSection } from './index'
export type { ResumeSection }

// ==================== SSE 事件类型 ====================

/** 定制阶段 */
export type TailorStage =
  | 'start'
  | 'analyze_jd'
  | 'match_resume'
  | 'generate_tailored'
  | 'end'

/** SSE 进度事件（复用优化模块的结构） */
export interface TailorProgressEvent {
  event: 'start' | 'progress' | 'complete' | 'error'
  nodeId: TailorStage | null
  progress: number | null
  message: string
  threadId: string | null
  data: any
  timestamp: number
}

// ==================== 各阶段数据类型 ====================

/** JD 分析结果 */
export interface JobRequirements {
  requiredSkills: string[]
  preferredSkills: string[]
  keywords: string[]
  responsibilities: string[]
  seniorityLevel: string
  industryDomain: string
}

/** 匹配分析结果 */
export interface MatchAnalysis {
  matchScore: number
  matchedSkills: string[]
  missingSkills: string[]
  relevantExperiences: string[]
  adjustmentSuggestions: string[]
}

/** 定制简历响应 */
export interface TailorResumeResponse {
  basicInfo: any
  education: any[]
  work: any[]
  projects: any[]
  skills: any[]
  certificates: any[]
  openSource: any[]
  customSections: any[]
  tailorNotes: string[]
  sectionRelevanceScores: Record<string, number>
}

// ==================== 定制状态 ====================

/** 阶段历史项 */
export interface TailorStageHistoryItem {
  stage: TailorStage
  message: string
  timestamp: number
  completed: boolean
  data?: any
  expanded?: boolean
}

/** 定制状态 */
export interface TailorState {
  // 连接状态
  isConnecting: boolean
  isTailoring: boolean
  isCompleted: boolean
  hasError: boolean

  // 基本信息
  threadId: string | null
  resumeId: string | null
  targetPosition: string
  jobDescription: string

  // 进度信息
  currentStage: TailorStage
  progress: number
  message: string

  // 错误信息
  errorMessage: string | null

  // 阶段数据
  jobRequirements: JobRequirements | null
  matchAnalysis: MatchAnalysis | null
  tailoredResume: TailorResumeResponse | null

  // 阶段历史
  stageHistory: TailorStageHistoryItem[]
}

// ==================== 阶段配置 ====================

/** 阶段配置项 */
export interface TailorStageConfigItem {
  label: string
  order: number
}

/** 阶段配置映射 */
export const TAILOR_STAGE_CONFIG: Record<TailorStage, TailorStageConfigItem> = {
  'start': { label: '开始定制', order: 0 },
  'analyze_jd': { label: '分析职位描述', order: 1 },
  'match_resume': { label: '匹配简历内容', order: 2 },
  'generate_tailored': { label: '生成定制简历', order: 3 },
  'end': { label: '完成', order: 4 }
}

/** 获取阶段标签 */
export function getTailorStageLabel(stage: TailorStage): string {
  return TAILOR_STAGE_CONFIG[stage]?.label || stage
}

// ==================== 请求类型 ====================

/** 派生简历请求 */
export interface DeriveResumeRequest {
  targetPosition: string
  resumeName?: string
  jobDescription: string
}

/** 定制简历对比数据 */
export interface TailorComparisonData {
  beforeSection: ResumeSection[]
  afterSection: ResumeSection[]
  improvementScore: number
  matchScore: number
  tailorNotes: string[]
  sectionRelevanceScores: Record<string, number>
}
