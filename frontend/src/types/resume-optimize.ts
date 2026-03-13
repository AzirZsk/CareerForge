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
  | 'diagnose_quick'
  | 'diagnose_precise'
  | 'generate_suggestions'
  | 'optimize_section'
  | 'human_review'
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

/** 维度分数集合（后端直接返回数字） */
export interface DimensionScores {
  content?: number
  structure?: number
  matching?: number
  competitiveness?: number
  [key: string]: number | undefined
}

/** 劣势项 */
export interface WeaknessItem {
  severity?: 'high' | 'medium' | 'low'
  content: string
}

/** 诊断数据（适配后端实际返回结构） */
export interface DiagnoseData {
  overallScore: number
  dimensionScores: DimensionScores
  suggestions?: SuggestionItem[]
  strengths: string[]
  weaknesses: string[] | WeaknessItem[]
  quickWins: string[]
  preciseAnalysis?: any
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

/** 建议类型 */
export type SuggestionType = 'critical' | 'improvement' | 'enhancement'

/** 影响程度 */
export type SuggestionImpact = 'high' | 'medium' | 'low'

/** 优化建议项（适配后端实际返回结构） */
export interface SuggestionItem {
  type: SuggestionType           // 建议类型：critical/improvement/enhancement
  impact: SuggestionImpact       // 影响程度：high/medium/low
  category: string
  sectionId: string              // 关联的区块ID
  position: string               // 位置描述
  title: string
  current: string                // 当前问题
  suggestion: string             // 改进建议
  value: string                  // 价值说明
}

/** 生成建议数据 */
export interface GenerateSuggestionsData {
  suggestions: SuggestionItem[]
  quickWins: QuickWinItem[]
  estimatedImprovement: string
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
  /** 优化前的值（新格式：直接为 string 或 string[]） */
  beforeValue: string | string[] | null
  /** 优化后的值（新格式：直接为 string 或 string[]） */
  afterValue: string | string[] | null
}

/** 获取实际值（辅助函数） */
export function getChangeActualValue(v: ChangeValue | null): string | string[] | null {
  if (!v) return null
  return v.arrayValue?.length ? v.arrayValue : v.stringValue ?? null
}

/** 格式化显示（辅助函数） */
export function formatChangeValue(v: ChangeValue | null): string {
  if (!v) return '-'
  return v.arrayValue?.length ? v.arrayValue.join(', ') : v.stringValue ?? '-'
}

/** 简历区块类型 */
export type ResumeSectionType =
  | 'BASIC_INFO'
  | 'EDUCATION'
  | 'WORK'
  | 'PROJECT'
  | 'SKILLS'
  | 'CERTIFICATE'
  | 'OPEN_SOURCE'

/** 简历区块 - 统一使用 content 字段 */
export interface ResumeSection {
  id: string
  resumeId: string
  type: ResumeSectionType
  title: string
  /** content 是 JSON 字符串，需解析 */
  content: string | null
  score: number
  suggestions: string[] | null
  /** 优化后新增的字段 */
  awards?: string
  description?: string
  achievements?: string
}

/** 内容优化数据 */
export interface OptimizeSectionData {
  changes: ChangeItem[]
  improvementScore: number
  tips: string[]
  confidence: 'high' | 'medium' | 'low'
  needsReview: boolean
  changeCount: number
  /** 优化前的原始简历（旧格式，兼容） */
  beforeResume?: any
  /** 优化后的内容（包含 description 和 highlights） */
  optimizedContent?: {
    description: string
    highlights: string[]
  }
  /** 优化前的区块数据（新格式） */
  beforeSection?: ResumeSection[]
  /** 优化后的区块数据（新格式） */
  afterSection?: ResumeSection[]
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
  'diagnose_quick': { label: '快速诊断', order: 1 },
  'diagnose_precise': { label: '精准诊断', order: 2 },
  'generate_suggestions': { label: '生成建议', order: 3 },
  'optimize_section': { label: '内容优化', order: 4 },
  'human_review': { label: '人工审核', order: 5 },
  'end': { label: '完成', order: 6 }
}

/** 维度标签映射 */
export const DIMENSION_LABELS: Record<string, string> = {
  'format': '格式规范',
  'content': '内容质量',
  'keywords': '关键词',
  'structure': '结构规范',
  'matching': '岗位匹配',
  'competitiveness': '竞争力'
}

/** 获取阶段标签 */
export function getStageLabel(stage: OptimizeStage): string {
  return STAGE_CONFIG[stage]?.label || stage
}

/** 获取维度标签 */
export function getDimensionLabel(key: string): string {
  return DIMENSION_LABELS[key] || key
}
