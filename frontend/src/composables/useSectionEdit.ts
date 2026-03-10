// =====================================================
// 简历模块编辑操作
// @author Azir
// =====================================================

import { ref, computed, type Ref } from 'vue'
import { useAppStore } from '@/stores'
import type { ResumeSection } from '@/types'

export function useSectionEdit(
  resumeId: Ref<string>,
  activeSection: Ref<string>,
  currentSectionDetail: Ref<ResumeSection | undefined>
) {
  const store = useAppStore()

  // 编辑状态
  const isEditModalVisible = ref<boolean>(false)
  const isSaving = ref<boolean>(false)
  const editItemIndex = ref<number | null>(null)
  const isNewItem = ref<boolean>(false)

  // 判断当前模块是否为聚合类型
  const isAggregateSection = computed<boolean>(() => {
    const type = currentSectionDetail.value?.type
    return ['EDUCATION', 'WORK', 'PROJECT', 'CERTIFICATE', 'OPEN_SOURCE', 'CUSTOM'].includes(type ?? '')
  })

  // 判断当前是否为 CUSTOM 类型的单个 item
  const isCustomItem = computed<boolean>(() => {
    return currentSectionDetail.value?.type === 'CUSTOM_ITEM'
  })

  // 打开编辑弹窗（单条类型）
  function openEditModal(): void {
    if (!currentSectionDetail.value) {
      return
    }
    // 对于 CUSTOM_ITEM 类型，设置 editItemIndex
    if (currentSectionDetail.value.type === 'CUSTOM_ITEM') {
      // CUSTOM_ITEM 不需要索引，直接设置为 0 表示编辑
      editItemIndex.value = 0
    } else {
      editItemIndex.value = null
    }
    isNewItem.value = false
    isEditModalVisible.value = true
  }

  // 打开编辑弹窗（聚合类型 - 编辑某条记录）
  function openEditItemModal(index: number): void {
    editItemIndex.value = index
    isNewItem.value = false
    isEditModalVisible.value = true
  }

  // 打开新增弹窗（聚合类型 - 新增记录）
  function openAddItemModal(): void {
    editItemIndex.value = null
    isNewItem.value = true
    isEditModalVisible.value = true
  }

  // 关闭编辑弹窗
  function closeEditModal(): void {
    isEditModalVisible.value = false
    editItemIndex.value = null
    isNewItem.value = false
  }

  // 保存编辑
  async function handleSave(data: { content: Record<string, unknown>; itemIndex?: number; isNew: boolean }): Promise<void> {
    if (!resumeId.value || !activeSection.value) {
      return
    }
    isSaving.value = true
    try {
      if (isAggregateSection.value) {
        // 聚合类型：新增或编辑条目，需要更新整个数组
        if (data.isNew) {
          // 新增条目：追加到数组末尾
          await store.addResumeSectionItem(resumeId.value, activeSection.value, data.content)
        } else if (data.itemIndex !== undefined && data.itemIndex !== null) {
          // 编辑条目：找到索引，修改该条目
          await store.updateResumeSectionItem(resumeId.value, activeSection.value, data.itemIndex, data.content)
        }
      } else {
        // 单条类型：直接更新
        await store.updateResumeSection(resumeId.value, activeSection.value, data.content)
      }
      closeEditModal()
    } catch (error) {
      console.error('保存失败', error)
      alert('保存失败，请重试')
    } finally {
      isSaving.value = false
    }
  }

  // 删除条目
  async function deleteItem(index: number): Promise<void> {
    if (!confirm('确定要删除这条记录吗？')) {
      return
    }
    try {
      await store.deleteResumeSectionItem(resumeId.value, activeSection.value, index)
    } catch (error) {
      console.error('删除失败', error)
      alert('删除失败，请重试')
    }
  }

  return {
    // 状态
    isEditModalVisible,
    isSaving,
    editItemIndex,
    isNewItem,
    // 计算属性
    isAggregateSection,
    isCustomItem,
    // 方法
    openEditModal,
    openEditItemModal,
    openAddItemModal,
    closeEditModal,
    handleSave,
    deleteItem
  }
}
