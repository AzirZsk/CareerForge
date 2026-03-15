// =====================================================
// 简历区块差异对比工具
// 通过对比 beforeSection 和 afterSection 的实际数据来判断高亮
// @author Azir
// =====================================================

import { computed, type Ref } from 'vue'
import type { ResumeSection } from '@/types'

/** 差异类型 */
export type DiffType = 'added' | 'modified' | 'removed' | ''

/**
 * 解析 section 的 content 字段
 */
function parseContent(content: string | null | undefined): unknown {
  if (!content) return null
  if (typeof content === 'object') return content
  try {
    return JSON.parse(content)
  } catch {
    return content
  }
}

/**
 * 深度比较两个值是否相等
 */
function deepEqual(a: unknown, b: unknown): boolean {
  if (a === b) return true
  if (a == null && b == null) return true
  if (a == null || b == null) return false

  // 都是数组
  if (Array.isArray(a) && Array.isArray(b)) {
    if (a.length !== b.length) return false
    return a.every((item, i) => deepEqual(item, b[i]))
  }

  // 都是对象
  if (typeof a === 'object' && typeof b === 'object') {
    const aObj = a as Record<string, unknown>
    const bObj = b as Record<string, unknown>
    const allKeys = new Set([...Object.keys(aObj), ...Object.keys(bObj)])
    for (const key of allKeys) {
      if (!deepEqual(aObj[key], bObj[key])) return false
    }
    return true
  }

  return String(a) === String(b)
}

/**
 * 判断值是否为空（空字符串、null、undefined、空数组）
 */
function isEmpty(val: unknown): boolean {
  if (val == null) return true
  if (typeof val === 'string') return val.trim() === ''
  if (Array.isArray(val)) return val.length === 0
  return false
}

export function useSectionDiff(
  beforeSections: Ref<ResumeSection[] | undefined>,
  afterSections: Ref<ResumeSection[] | undefined>
) {
  // 构建 before 数据的索引：sectionType -> parsed content
  const beforeMap = computed(() => {
    const map = new Map<string, { content: unknown; exists: boolean }>()
    if (!beforeSections.value) return map
    for (const section of beforeSections.value) {
      map.set(section.type, {
        content: parseContent(section.content),
        exists: true
      })
    }
    return map
  })

  // 构建 after 数据的索引
  const afterMap = computed(() => {
    const map = new Map<string, { content: unknown; exists: boolean }>()
    if (!afterSections.value) return map
    for (const section of afterSections.value) {
      map.set(section.type, {
        content: parseContent(section.content),
        exists: true
      })
    }
    return map
  })

  /**
   * 判断某个区块是否整体是新增的（before 中不存在）
   */
  function isSectionAdded(sectionType: string): boolean {
    return !beforeMap.value.has(sectionType) && afterMap.value.has(sectionType)
  }

  /**
   * 判断某个区块是否整体被删除（after 中不存在）
   */
  function isSectionRemoved(sectionType: string): boolean {
    return beforeMap.value.has(sectionType) && !afterMap.value.has(sectionType)
  }

  /**
   * 获取单条类型（BASIC_INFO）某个字段的差异类型
   * @param sectionType 区块类型，如 'BASIC_INFO'
   * @param fieldKey 字段名，如 'name', 'summary'
   */
  function getFieldDiff(sectionType: string, fieldKey: string): DiffType {
    // 整个区块是新增的
    if (isSectionAdded(sectionType)) return 'added'
    if (isSectionRemoved(sectionType)) return 'removed'

    const beforeData = beforeMap.value.get(sectionType)?.content as Record<string, unknown> | null
    const afterData = afterMap.value.get(sectionType)?.content as Record<string, unknown> | null

    if (!beforeData && !afterData) return ''

    const beforeVal = beforeData?.[fieldKey]
    const afterVal = afterData?.[fieldKey]

    // before 为空，after 有值 → 新增
    if (isEmpty(beforeVal) && !isEmpty(afterVal)) return 'added'
    // before 有值，after 为空 → 删除
    if (!isEmpty(beforeVal) && isEmpty(afterVal)) return 'removed'
    // 两者都有值但不同 → 修改
    if (!deepEqual(beforeVal, afterVal)) return 'modified'

    return ''
  }

  /**
   * 获取聚合类型（数组）中某个条目某个字段的差异类型
   * @param sectionType 区块类型，如 'WORK', 'EDUCATION'
   * @param itemIndex 数组索引
   * @param fieldKey 字段名（可选，不传则比较整个条目）
   */
  function getItemFieldDiff(sectionType: string, itemIndex: number, fieldKey?: string): DiffType {
    // 整个区块是新增的
    if (isSectionAdded(sectionType)) return 'added'
    if (isSectionRemoved(sectionType)) return 'removed'

    const beforeArr = beforeMap.value.get(sectionType)?.content
    const afterArr = afterMap.value.get(sectionType)?.content

    const bList = Array.isArray(beforeArr) ? beforeArr : []
    const aList = Array.isArray(afterArr) ? afterArr : []

    const beforeItem = bList[itemIndex] as Record<string, unknown> | undefined
    const afterItem = aList[itemIndex] as Record<string, unknown> | undefined

    // 该索引在 before 中不存在 → 整条新增
    if (!beforeItem && afterItem) return 'added'
    // 该索引在 after 中不存在 → 整条删除
    if (beforeItem && !afterItem) return 'removed'
    // 两边都不存在
    if (!beforeItem && !afterItem) return ''

    // 不指定字段，比较整个条目
    if (!fieldKey) {
      return deepEqual(beforeItem, afterItem) ? '' : 'modified'
    }

    // 比较具体字段
    const beforeVal = beforeItem![fieldKey]
    const afterVal = afterItem![fieldKey]

    if (isEmpty(beforeVal) && !isEmpty(afterVal)) return 'added'
    if (!isEmpty(beforeVal) && isEmpty(afterVal)) return 'removed'
    if (!deepEqual(beforeVal, afterVal)) return 'modified'

    return ''
  }

  /**
   * 统一的高亮 class 获取函数
   * 根据 side（before/after）和 diffType 返回对应的 CSS class
   */
  function getDiffClass(diffType: DiffType, side: 'before' | 'after'): string {
    if (!diffType) return ''
    if (side === 'after') {
      return diffType === 'removed' ? 'highlight-removed' : 'highlight-added'
    } else {
      // before 侧：modified 和 removed 都标红，added 不标（因为 before 中没有）
      return diffType === 'added' ? '' : 'highlight-removed'
    }
  }

  /**
   * 便捷方法：获取单条类型字段的高亮 class
   */
  function fieldClass(sectionType: string, fieldKey: string, side: 'before' | 'after'): string {
    return getDiffClass(getFieldDiff(sectionType, fieldKey), side)
  }

  /**
   * 便捷方法：获取聚合类型条目字段的高亮 class
   */
  function itemFieldClass(sectionType: string, itemIndex: number, fieldKey: string | undefined, side: 'before' | 'after'): string {
    return getDiffClass(getItemFieldDiff(sectionType, itemIndex, fieldKey), side)
  }

  return {
    isSectionAdded,
    isSectionRemoved,
    getFieldDiff,
    getItemFieldDiff,
    getDiffClass,
    fieldClass,
    itemFieldClass
  }
}
