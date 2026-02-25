// =====================================================
// LandIt 简历优化相关类型定义
// @author Azir
// =====================================================

// ==================== SSE 事件类型 ====================

/** SSE 事件类型 */
export type OptimizeEventType = 'start' | 'progress' | 'complete' | 'error'

/** 优化阶段 */
export type OptimizeStage =
  | 'start'
  | 'parse_resume'
  | 'diagnose_quick'
  | 'diagnose_precise'
  | 'generate_suggestions'
  | 'optimize_section'
  | 'human_review'
  | 'save_version'
  | 'end'

/** SSE 进度事件 */
export interface OptimizeProgressEvent {
  event: OptimizeEventType
  nodeId: OptimizeStage | null
  progress: number | null
  message: string
  threadId: string | null
  data: any
  timestamp: number
}

// ==================== 各阶段数据类型 ====================

/** 模块信息 */
export interface ModuleInfo {
  id: string
  type: string
  title: string
  score: number
  hasContent: boolean
}

/** 解析简历数据 */
export interface ParseResumeData {
  resumeId: string
  targetPosition: string
  modules: ModuleInfo[]
  moduleCount: number
  completeness: number
}

/** 维度评分 */
export interface DimensionScore {
  score: number
  comment: string
}

/** 维度分数集合 */
export interface DimensionScores {
  format?: DimensionScore
  content?: DimensionScore
  keywords?: DimensionScore
  structure?: DimensionScore
  [key: string]: DimensionScore | undefined
}

/** 问题项 */
export interface IssueItem {
  severity: 'high' | 'medium' | 'low'
  type: string
  content: string
}

/** 诊断数据 */
export interface DiagnoseData {
  overallScore: number
  dimensions: DimensionScores
  issues: IssueItem[]
  highlights: string[]
  quickWins: string[]
}

/** 市场要求 */
export interface MarketRequirements {
  required: string[]
  preferred: string[]
  trending: string[]
}

/** 技能匹配 */
export interface SkillMatch {
  matched: string[]
  missing: string[]
  partial: string[]
}

/** 精准诊断数据 */
export interface DiagnosePreciseData extends DiagnoseData {
  matchScore: number
  marketRequirements: MarketRequirements
  skillMatch: SkillMatch
}

/** 快速改进项 */
export interface QuickWinItem {
  action: string
  example: string
}

/** 优化建议项 */
export interface SuggestionItem {
  id: string
  priority: 'high' | 'medium' | 'low'
  category: string
  section: string
  title: string
  current: string
  suggestion: string
  reason: string
  impact: 'high' | 'medium' | 'low'
}

/** 生成建议数据 */
export interface GenerateSuggestionsData {
  suggestions: SuggestionItem[]
  quickWins: QuickWinItem[]
  estimatedImprovement: string
  totalSuggestions: number
}

/** 变更项 */
export interface ChangeItem {
  sectionId: string
  sectionType: string
  sectionTitle: string
  field: string
  before: string
  after: string
  reason: string
}

/** 内容优化数据 */
export interface OptimizeSectionData {
  changes: ChangeItem[]
  improvementScore: number
  tips: string[]
  confidence: 'high' | 'medium' | 'low'
  needsReview: boolean
  changeCount: number
}

/** 保存版本数据 */
export interface SaveVersionData {
  status: 'completed'
  resumeId: string
  versionId: string
  versionName: string
  originalScore: number
  newScore: number
  improvementScore: number
  optimizedAt: string
}

// ==================== 优化状态 ====================

/** 优化模式 */
export type OptimizeMode = 'quick' | 'precise'

/** 阶段历史项 */
export interface StageHistoryItem {
  stage: OptimizeStage
  message: string
  timestamp: number
  completed: boolean
  data?: any
  expanded?: boolean
}

/** 优化状态 */
export interface OptimizeState {
  // 连接状态
  isConnecting: boolean
  isOptimizing: boolean
  isCompleted: boolean
  hasError: boolean

  // 基本信息
  threadId: string | null
  resumeId: string | null
  targetPosition: string
  mode: OptimizeMode

  // 进度信息
  currentStage: OptimizeStage
  progress: number
  message: string

  // 错误信息
  errorMessage: string | null

  // 阶段历史（包含数据和展开状态）
  stageHistory: StageHistoryItem[]
}

// ==================== 阶段配置 ====================

/** 阶段配置项 */
export interface StageConfigItem {
  label: string
  order: number
}

/** 阶段配置映射 */
export const STAGE_CONFIG: Record<OptimizeStage, StageConfigItem> = {
  'start': { label: '开始优化', order: 0 },
  'parse_resume': { label: '解析简历', order: 1 },
  'diagnose_quick': { label: '快速诊断', order: 2 },
  'diagnose_precise': { label: '精准诊断', order: 3 },
  'generate_suggestions': { label: '生成建议', order: 4 },
  'optimize_section': { label: '内容优化', order: 5 },
  'human_review': { label: '人工审核', order: 6 },
  'save_version': { label: '保存版本', order: 7 },
  'end': { label: '完成', order: 8 }
}

/** 维度标签映射 */
export const DIMENSION_LABELS: Record<string, string> = {
  'format': '格式规范',
  'content': '内容质量',
  'keywords': '关键词',
  'structure': '结构逻辑'
}

/** 获取阶段标签 */
export function getStageLabel(stage: OptimizeStage): string {
  return STAGE_CONFIG[stage]?.label || stage
}

/** 获取维度标签 */
export function getDimensionLabel(key: string): string {
  return DIMENSION_LABELS[key] || key
}
