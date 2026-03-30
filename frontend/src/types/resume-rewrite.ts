// =====================================================
// LandIt 简历风格改写相关类型定义
// @author Azir
// =====================================================

// 从主类型文件导入并重新导出
import type { ResumeSection } from './index'
export type { ResumeSection }

// ==================== SSE 事件类型 ====================

/** SSE 事件类型 */
export type RewriteEventType = 'start' | 'progress' | 'complete' | 'error'

/** 风格改写阶段 */
export type RewriteStage =
  | 'start'
  | 'analyze_style'
  | 'generate_style_diff'
  | 'rewrite_section'
  | 'end'

/** SSE 进度事件 */
export interface RewriteProgressEvent {
  event: RewriteEventType
  nodeId: RewriteStage | null
  progress: number | null
  message: string
  threadId: string | null
  data: any
  timestamp: number
}

// ==================== 各阶段数据类型 ====================

/** 风格示例 */
export interface StyleExample {
  sectionType: string
  referenceText: string
  stylePoint: string
}

/** 风格分析数据 */
export interface StyleAnalysisData {
  toneDescription: string
  voicePattern: string
  sentenceStructure: string
  verbStyle: string
  quantificationDensity: string
  contentOrganization: string
  sectionOrdering: string[]
  styleRules: string[]
  styleExamples: StyleExample[]
}

/** 建议类型 */
export type SuggestionType = 'critical' | 'improvement' | 'enhancement'

/** 影响程度 */
export type SuggestionImpact = 'high' | 'medium' | 'low'

/** 风格差异建议项 */
export interface StyleDiffSuggestion {
  type: SuggestionType
  impact: SuggestionImpact
  category: string
  sectionId: string
  position: string
  title: string
  problem: string
  direction: string
  example?: string
  value: string
}

/** 生成风格差异数据 */
export interface GenerateStyleDiffData {
  suggestions: StyleDiffSuggestion[]
  quickWins: string[]
  estimatedImprovement: number
  totalSuggestions: number
}

/** 值类型 */
export type ChangeValueType = 'string' | 'string_array'

/** 变更值容器 */
export interface ChangeValue {
  stringValue?: string
  arrayValue?: string[]
}

/** 变更项 */
export interface ChangeItem {
  type: 'added' | 'modified' | 'removed'
  typeLabel: string
  field: string
  fieldLabel: string
  valueType: ChangeValueType
  before: ChangeValue | null
  after: ChangeValue | null
  reason: string
}

/** 区块改写数据 */
export interface RewriteSectionData {
  changes: ChangeItem[]
  improvementScore: number
  tips: string[]
  confidence: 'high' | 'medium' | 'low'
  needsReview: boolean
  changeCount: number
  beforeSection?: ResumeSection[]
  afterSection?: ResumeSection[]
}

// ==================== 改写状态 ====================

/** 阶段历史项 */
export interface RewriteStageHistoryItem {
  stage: RewriteStage
  message: string
  timestamp: number
  completed: boolean
  data?: any
  expanded?: boolean
  startTime?: number
  endTime?: number
}

/** 风格改写状态 */
export interface RewriteState {
  // 连接状态
  isConnecting: boolean
  isRewriting: boolean
  isCompleted: boolean
  hasError: boolean

  // 基本信息
  threadId: string | null
  resumeId: string | null
  tempKey: string | null

  // 进度信息
  currentStage: RewriteStage
  progress: number
  message: string

  // 错误信息
  errorMessage: string | null

  // 阶段历史
  stageHistory: RewriteStageHistoryItem[]

  // 参考简历信息
  referenceFileName: string | null
}

// ==================== 阶段配置 ====================

/** 阶段配置项 */
export interface RewriteStageConfigItem {
  label: string
  order: number
}

/** 阶段配置映射 */
export const REWRITE_STAGE_CONFIG: Record<RewriteStage, RewriteStageConfigItem> = {
  'start': { label: '开始', order: 0 },
  'analyze_style': { label: '分析参考风格', order: 1 },
  'generate_style_diff': { label: '生成风格差异', order: 2 },
  'rewrite_section': { label: '应用风格改写', order: 3 },
  'end': { label: '完成', order: 4 }
}

/** 获取阶段标签 */
export function getRewriteStageLabel(stage: RewriteStage): string {
  return REWRITE_STAGE_CONFIG[stage]?.label || stage
}

// ==================== API 请求/响应类型 ====================

/** 解析参考简历响应 */
export interface ParseReferenceResponse {
  tempKey: string
  fileName: string
}
