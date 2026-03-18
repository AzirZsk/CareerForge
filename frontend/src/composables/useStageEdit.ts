// =====================================================
// LandIt 简历优化阶段编辑 Composable
// @author Azir
// =====================================================

import { ref, computed, watch, type Ref, type ComputedRef } from 'vue'
import type { ResumeSection, StageHistoryItem, ComparisonEditEvent } from '@/types/resume-optimize'

/**
 * 阶段编辑 Composable
 * 用于管理优化内容的编辑状态
 */
export function useStageEdit(stageHistory: Ref<StageHistoryItem[]>) {
  // ==================== 状态 ====================

  /** 可编辑的 afterSection 副本（优化内容阶段的数据） */
  const editedAfterSection = ref<ResumeSection[] | null>(null)

  /** 原始 afterSection 的深拷贝（用于比较是否有编辑） */
  const originalAfterSection = ref<string>('')

  /** 编辑弹窗状态 */
  const showEditModal = ref(false)
  const editingSection = ref<ResumeSection | null>(null)
  const editingSectionIndex = ref<number | null>(null)
  const editingItemIndex = ref<number | null>(null)

  // ==================== 计算属性 ====================

  /** 是否有编辑 */
  const hasEdits: ComputedRef<boolean> = computed(() => {
    if (!editedAfterSection.value) return false
    return JSON.stringify(editedAfterSection.value) !== originalAfterSection.value
  })

  // ==================== 监听器 ====================

  // 监听 stageHistory 变化，初始化编辑数据
  watch(
    () => stageHistory.value,
    (history) => {
      const optimizeStage = history.find(h => h.stage === 'optimize_section')
      if (optimizeStage?.data?.afterSection) {
        // 深拷贝数据
        const afterSectionCopy = JSON.parse(JSON.stringify(optimizeStage.data.afterSection))
        editedAfterSection.value = afterSectionCopy
        originalAfterSection.value = JSON.stringify(afterSectionCopy)
      }
    },
    { deep: true, immediate: true }
  )

  // ==================== 方法 ====================

  /**
   * 获取当前显示的 afterSection（可能是编辑后的）
   */
  function getDisplayAfterSection(item: { data?: { afterSection?: ResumeSection[] } }): ResumeSection[] | undefined {
    if (editedAfterSection.value) {
      return editedAfterSection.value
    }
    return item.data?.afterSection
  }

  /**
   * 处理编辑事件
   */
  function handleEditSection(payload: ComparisonEditEvent) {
    if (!editedAfterSection.value) return

    editingSectionIndex.value = payload.sectionIndex
    editingItemIndex.value = payload.itemIndex ?? null

    // 获取要编辑的区块
    const section = editedAfterSection.value[payload.sectionIndex]
    if (!section) return

    // 直接传完整 section，让 EditSectionModal 根据 itemIndex 自行解析
    editingSection.value = JSON.parse(JSON.stringify(section))

    showEditModal.value = true
  }

  /**
   * 保存编辑
   */
  function handleEditSave(data: { content: Record<string, unknown>; itemIndex?: number }) {
    if (!editedAfterSection.value || editingSectionIndex.value === null) return

    const section = editedAfterSection.value[editingSectionIndex.value]

    if (editingItemIndex.value !== null && editingItemIndex.value !== undefined) {
      // 更新聚合类型中的特定项
      const items = JSON.parse(section.content || '[]')
      items[editingItemIndex.value] = data.content
      section.content = JSON.stringify(items)
    } else {
      // 更新整个内容（单对象类型）
      section.content = JSON.stringify(data.content)
    }

    showEditModal.value = false
    editingSection.value = null
    editingSectionIndex.value = null
    editingItemIndex.value = null
  }

  /**
   * 取消编辑
   */
  function handleEditCancel() {
    showEditModal.value = false
    editingSection.value = null
    editingSectionIndex.value = null
    editingItemIndex.value = null
  }

  /**
   * 重置编辑到原始状态
   */
  function resetEdits() {
    const optimizeStage = stageHistory.value.find(h => h.stage === 'optimize_section')
    if (optimizeStage?.data?.afterSection) {
      editedAfterSection.value = JSON.parse(JSON.stringify(optimizeStage.data.afterSection))
    }
  }

  return {
    // 状态
    editedAfterSection,
    hasEdits,
    showEditModal,
    editingSection,
    editingSectionIndex,
    editingItemIndex,

    // 方法
    handleEditSection,
    handleEditSave,
    handleEditCancel,
    resetEdits,
    getDisplayAfterSection
  }
}
