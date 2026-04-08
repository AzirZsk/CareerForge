// =====================================================
// LandIt 简历优化阶段辅助工具函数
// @author Azir
// =====================================================

import type { ChangeItem, WeaknessItem } from '@/types/resume-optimize'

// ==================== 严重性相关 ====================

/** 严重性图标映射 - Font Awesome 图标类名 */
export const SEVERITY_ICONS: Record<string, string> = {
  high: 'fa-solid fa-triangle-exclamation',
  medium: 'fa-solid fa-lightbulb',
  low: 'fa-solid fa-wand-magic-sparkles'
}

/** 获取严重性图标 */
export function getSeverityIcon(severity: string): string {
  return SEVERITY_ICONS[severity] || 'fa-solid fa-circle'
}

/** 获取劣势项内容 */
export function getWeaknessContent(weakness: string | WeaknessItem): string {
  return typeof weakness === 'string' ? weakness : weakness.content
}

/** 获取劣势项严重性 */
export function getWeaknessSeverity(weakness: string | WeaknessItem): string {
  return typeof weakness === 'string' ? 'medium' : (weakness.severity || 'medium')
}

// ==================== 建议相关 ====================

/** 建议类型标签映射 */
export const SUGGESTION_TYPE_LABELS: Record<string, string> = {
  critical: '严重',
  improvement: '改进',
  enhancement: '优化'
}

/** 建议影响程度标签映射 */
export const SUGGESTION_IMPACT_LABELS: Record<string, string> = {
  high: '高影响',
  medium: '中影响',
  low: '低影响'
}

/** 获取建议类型标签 */
export function getSuggestionTypeLabel(type: string): string {
  return SUGGESTION_TYPE_LABELS[type] || type
}

/** 获取建议影响程度标签 */
export function getSuggestionImpactLabel(impact: string): string {
  return SUGGESTION_IMPACT_LABELS[impact] || impact
}

// ==================== 变更相关 ====================

/** 判断是否为数组 */
export function isArray(value: unknown): value is string[] {
  return Array.isArray(value)
}

/**
 * 判断变更项是否有值需要展示
 * 删除类型不展示值，只展示原因；新增类型需要 afterValue，修改类型需要至少一个有值
 */
export function hasValueToShow(change: ChangeItem): boolean {
  if (change.type === 'removed') {
    return false  // 删除类型不展示值，只展示原因
  }
  if (change.type === 'added') {
    return change.afterValue !== null
  }
  return change.beforeValue !== null || change.afterValue !== null
}
