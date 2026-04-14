<!--=====================================================
  CareerForge 简历详情页面
  @author Azir
=====================================================-->

<template>
  <div class="resume-detail-page">
    <div class="container">
      <!-- 返回导航 -->
      <nav
        class="back-nav animate-in"
        style="--delay: 0"
      >
        <router-link
          to="/resume"
          class="back-link"
        >
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <line
              x1="19"
              y1="12"
              x2="5"
              y2="12"
            />
            <polyline points="12 19 5 12 12 5" />
          </svg>
          返回简历列表
        </router-link>
      </nav>

      <!-- 简历头部 -->
      <ResumeHeader
        :resume-id="resumeId"
        :name="store.currentResume.name"
        :target-position="store.currentResume.targetPosition"
        :analyzed="store.currentResume.analyzed"
        :overall-score="store.currentResume.overallScore"
        :structure-score="store.currentResume.structureScore"
        :has-content="hasAnalyzableContent"
        @optimize="optimizeResume"
        @rewrite="startRewrite"
        @update="handleUpdateResumeBasicInfo"
      />

      <!-- 评分指标 -->
      <MetricsSection
        :analyzed="store.currentResume.analyzed"
        :content-score="store.currentResume.contentScore"
        :structure-score="store.currentResume.structureScore"
        :matching-score="store.currentResume.matchingScore"
        :competitiveness-score="store.currentResume.competitivenessScore"
      />

      <!-- 简历内容 -->
      <div class="content-grid">
        <!-- 模块列表 -->
        <SectionList
          :sections="store.currentResume.sections"
          :active-section="activeSection"
          :analyzed="store.currentResume.analyzed"
          @update:active-section="activeSection = $event"
          @add-section="showAddSectionModal = true"
        />

        <!-- 详情面板 -->
        <section
          v-if="currentSectionDetail"
          class="detail-panel animate-in"
          style="--delay: 4"
        >
          <!-- 优化建议区块（头部） -->
          <SuggestionsBlock
            v-if="currentSectionDetail"
            :suggestions="sectionSuggestions"
            @ignore="handleIgnoreSuggestion"
            @delete="handleDeleteSuggestion"
          />

          <div class="panel-header">
            <h2 class="panel-title">
              {{ currentSectionDetail?.title }}
            </h2>
            <div class="panel-actions">
              <!-- 单条类型或 CUSTOM_ITEM：显示编辑按钮 -->
              <button
                v-if="!isAggregateSection || isCustomItem"
                class="panel-btn"
                @click="openEditModal"
              >
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                </svg>
                编辑
              </button>
              <!-- 聚合类型（非 CUSTOM_ITEM）：显示添加按钮 -->
              <button
                v-else
                class="panel-btn primary"
                @click="openAddItemModal"
              >
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <line
                    x1="12"
                    y1="5"
                    x2="12"
                    y2="19"
                  />
                  <line
                    x1="5"
                    y1="12"
                    x2="19"
                    y2="12"
                  />
                </svg>
                添加
              </button>
              <!-- 删除按钮：除了基本信息之外都可以删除 -->
              <button
                v-if="canDeleteSection"
                class="panel-btn danger"
                @click="handleDeleteSection"
              >
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <polyline points="3 6 5 6 21 6" />
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                </svg>
                删除
              </button>
            </div>
          </div>

          <!-- 模块内容 -->
          <SectionContent
            :section="currentSectionDetail"
            @edit="openEditModal"
            @edit-item="openEditItemModal"
            @add-item="openAddItemModal"
            @delete-item="deleteItem"
          />
        </section>

        <!-- 空状态提示 -->
        <section
          v-else
          class="detail-panel empty-panel animate-in"
          style="--delay: 4"
        >
          <div class="empty-state">
            <svg
              width="48"
              height="48"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.5"
            >
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
              <polyline points="14 2 14 8 20 8" />
            </svg>
            <p class="empty-text">
              暂无简历模块，请在左侧添加
            </p>
          </div>
        </section>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <EditSectionModal
      v-model:visible="isEditModalVisible"
      :section="editingSection ?? null"
      :item-index="editItemIndex"
      :is-new="isNewItem"
      :saving="isSaving"
      @save="handleSave"
      @cancel="closeEditModal"
    />

    <!-- 添加模块弹窗 -->
    <AddSectionModal
      v-model:visible="showAddSectionModal"
      :existing-types="existingSectionTypes"
      @select="handleSelectSectionType"
    />

    <!-- 优化进度弹窗 -->
    <OptimizeProgressModal
      v-model:visible="showOptimizeModal"
      :state="optimizeState"
      @cancel="cancelOptimize"
      @retry="retryOptimize"
      @apply="handleApplyChanges"
      @toggle-expand="toggleStageExpanded"
      @complete="handleOptimizeComplete"
    />

    <!-- 删除确认弹窗 -->
    <ConfirmModal
      v-model:visible="showDeleteConfirmModal"
      title="删除确认"
      :message="deleteConfirmMessage"
      confirm-text="删除"
      :danger="true"
      @confirm="confirmDeleteCustomItem"
    />

    <!-- 删除建议确认弹窗 -->
    <ConfirmModal
      v-model:visible="showDeleteSuggestionModal"
      title="删除建议"
      message="确定要删除这条优化建议吗？删除后将无法恢复。"
      confirm-text="删除"
      :danger="true"
      @confirm="confirmDeleteSuggestion"
    />

    <!-- 删除条目确认弹窗（来自 useSectionEdit） -->
    <ConfirmModal
      v-model:visible="confirmVisible"
      :title="confirmTitle"
      :message="confirmMessage"
      :confirm-text="confirmText"
      :danger="confirmDanger"
      @confirm="handleConfirm"
      @cancel="handleConfirmCancel"
    />

    <!-- 风格改写弹窗 -->
    <RewriteResumeModal
      v-model:visible="showRewriteModal"
      :resume-id="resumeId"
      :overlay="true"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores'
import EditSectionModal from '@/components/resume/EditSectionModal.vue'
import AddSectionModal from '@/components/resume/AddSectionModal.vue'
import OptimizeProgressModal from '@/components/resume/OptimizeProgressModal.vue'
import RewriteResumeModal from '@/components/resume/RewriteResumeModal.vue'
import ConfirmModal from '@/components/common/ConfirmModal.vue'
import ResumeHeader from '@/components/resume/ResumeHeader.vue'
import MetricsSection from '@/components/resume/MetricsSection.vue'
import SectionList from '@/components/resume/SectionList.vue'
import SectionContent from '@/components/resume/SectionContent.vue'
import SuggestionsBlock from '@/components/resume/SuggestionsBlock.vue'
import { useResumeOptimize } from '@/composables/useResumeOptimize'
import { useAIChat } from '@/composables/useAIChat'
import { useSectionEdit } from '@/composables/useSectionEdit'
import { useSectionHelper } from '@/composables/useSectionHelper'
import { useToast } from '@/composables/useToast'
import { usePageGuard } from '@/composables/usePageGuard'
import type { ResumeSection, ResumeSuggestionItem, SectionType } from '@/types'

const store = useAppStore()
const { parseContent } = useSectionHelper()
const toast = useToast()
const route = useRoute()
const activeSection = ref<string>('')
const resumeId = ref<string>('')

// 优化相关状态
const showOptimizeModal = ref<boolean>(false)
const { state: aiChatState } = useAIChat()
// 优化弹窗打开时隐藏AI悬浮球
watch(showOptimizeModal, (val) => {
  aiChatState.hideFloat = val
})

// 风格改写相关状态
const showRewriteModal = ref<boolean>(false)
// 风格改写弹窗打开时隐藏AI悬浮球
watch(showRewriteModal, (val) => {
  aiChatState.hideFloat = val
})

// 删除确认弹窗状态
const showDeleteConfirmModal = ref<boolean>(false)

// 删除建议确认弹窗状态
const showDeleteSuggestionModal = ref<boolean>(false)
const pendingDeleteSuggestionId = ref<string | null>(null)

// 添加模块相关状态
const showAddSectionModal = ref<boolean>(false)

// 弹窗打开时注册页面离开保护
const { registerGuard, unregisterGuard } = usePageGuard()

watch(showAddSectionModal, (open) => {
  open ? registerGuard('modal:add-section') : unregisterGuard('modal:add-section')
})

watch(showOptimizeModal, (open) => {
  open ? registerGuard('modal:optimize') : unregisterGuard('modal:optimize')
})

watch(showRewriteModal, (open) => {
  open ? registerGuard('modal:rewrite') : unregisterGuard('modal:rewrite')
})

// 已存在的模块类型列表
const existingSectionTypes = computed<SectionType[]>(() => {
  return store.currentResume.sections.map((s: ResumeSection) => s.type as SectionType)
})
const {
  state: optimizeState,
  startOptimize,
  cancelOptimize,
  retryOptimize,
  toggleStageExpanded,
  applyChanges
} = useResumeOptimize()

// 当前选中的模块详情
const currentSectionDetail = computed<ResumeSection | undefined>(() => {
  // 首先检查是否是 CUSTOM 类型的 item
  for (const s of store.currentResume.sections) {
    if (s.type === 'CUSTOM' && s.content) {
      // 解析 CUSTOM 类型的 content 数组
      const customItems = parseContent<Array<{ id?: string; title?: string } & Record<string, unknown>>>(s.content)
      if (Array.isArray(customItems)) {
        const item = customItems.find((i, idx) => (i.id || `custom_${idx}`) === activeSection.value)
        if (item) {
          const itemIndex = customItems.indexOf(item)
          // 返回一个虚拟的 section，只包含选中的 item
          return {
            id: item.id || `custom_${itemIndex}`,
            resumeId: s.resumeId,
            type: 'CUSTOM_ITEM',
            title: (item.title as string) || '自定义区块',
            content: JSON.stringify(item),
            score: (item.score as number) ?? 0,
            suggestions: null
          } as ResumeSection
        }
      }
    }
  }
  // 非 CUSTOM 类型：直接匹配 section.id
  return store.currentResume.sections.find((s: ResumeSection) => s.id === activeSection.value)
})

// 编辑弹窗使用的 section（支持新建模块时的虚拟 section）
const editingSection = computed<ResumeSection | undefined>(() => {
  // 新建模块模式：返回虚拟 section
  if (isNewSection.value && pendingSectionType.value) {
    return {
      id: 'new_section',
      resumeId: resumeId.value,
      type: pendingSectionType.value,
      title: '新模块',
      content: null,
      score: 0,
      suggestions: null
    } as ResumeSection
  }
  // 编辑模式：返回当前选中的 section
  return currentSectionDetail.value
})

// 使用编辑 composable
const {
  isEditModalVisible,
  isSaving,
  editItemIndex,
  isNewItem,
  isNewSection,
  pendingSectionType,
  isAggregateSection,
  isCustomItem,
  canDeleteSection,
  confirmVisible,
  confirmTitle,
  confirmMessage,
  confirmText,
  confirmDanger,
  handleConfirm,
  handleConfirmCancel,
  openEditModal,
  openEditItemModal,
  openAddItemModal,
  openNewSectionModal,
  closeEditModal,
  handleSave,
  deleteItem
} = useSectionEdit(resumeId, activeSection, currentSectionDetail)

// 编辑弹窗保护
watch(isEditModalVisible, (open) => {
  open ? registerGuard('modal:edit-section') : unregisterGuard('modal:edit-section')
})

// 优化建议
const sectionSuggestions = computed<ResumeSuggestionItem[]>(() => {
  return currentSectionDetail.value?.suggestions ?? []
})

// 判断简历是否有可分析的内容（用于控制AI分析按钮的显示）
const hasAnalyzableContent = computed<boolean>(() => {
  const sections = store.currentResume.sections
  if (!sections || sections.length === 0) return false
  // 至少有一个区块有实际内容
  return sections.some((s: ResumeSection) => {
    if (!s.content) return false
    const content = s.content.trim()
    return content !== '{}' && content !== '[]' && content !== ''
  })
})

// 忽略建议（占位实现）
function handleIgnoreSuggestion(suggestion: ResumeSuggestionItem): void {
  console.log('忽略建议:', suggestion)
  // TODO: 实现忽略建议功能（需要后端 API 支持）
  toast.info('忽略建议功能开发中...')
}

// 删除建议（显示确认弹窗）
function handleDeleteSuggestion(suggestionId: string): void {
  pendingDeleteSuggestionId.value = suggestionId
  showDeleteSuggestionModal.value = true
}

// 确认删除建议
async function confirmDeleteSuggestion(): Promise<void> {
  if (!pendingDeleteSuggestionId.value) return

  try {
    await store.deleteSuggestion(resumeId.value, pendingDeleteSuggestionId.value)
    toast.success('建议已删除')
  } catch (error) {
    console.error('删除建议失败:', error)
    toast.error('删除建议失败')
  } finally {
    pendingDeleteSuggestionId.value = null
  }
}

// 删除确认弹窗消息
const deleteConfirmMessage = computed<string>(() => {
  const title = currentSectionDetail.value?.title || '模块'
  return `确定要删除「${title}」吗？此操作不可撤销。`
})

// 判断模块是否有实际数据
function hasSectionData(section: ResumeSection): boolean {
  if (!section.content) return false
  const content = section.content.trim()
  return content !== '{}' && content !== '[]' && content !== ''
}

// 页面加载时获取简历详情
onMounted(async () => {
  const id = route.params.id as string
  if (id) {
    resumeId.value = id
    await store.fetchResumeDetail(id)
    // 默认选中第一个有数据的模块
    const sections = store.currentResume.sections
    if (sections.length > 0) {
      const firstSectionWithData = sections.find((s: ResumeSection) => hasSectionData(s))
      activeSection.value = firstSectionWithData
        ? String(firstSectionWithData.id)
        : String(sections[0].id)
    }
  }
})

// 开始 AI 优化
function optimizeResume(): void {
  if (!resumeId.value) {
    return
  }
  showOptimizeModal.value = true
  // 获取目标岗位
  const targetPosition = store.currentResume.targetPosition || undefined
  // 开始 SSE 优化
  startOptimize(resumeId.value, {
    mode: 'quick',
    targetPosition
  })
}

// 开始风格改写
function startRewrite(): void {
  if (!resumeId.value) return
  showRewriteModal.value = true
}

// 更新简历基本信息
async function handleUpdateResumeBasicInfo(data: { name: string; targetPosition?: string }): Promise<void> {
  if (!resumeId.value) return
  try {
    await store.updateResumeBasicInfo(resumeId.value, data)
    toast.success('简历信息已更新')
  } catch (error) {
    console.error('更新简历信息失败:', error)
    toast.error('更新失败，请重试')
  }
}

// 选择要添加的模块类型
function handleSelectSectionType(type: SectionType): void {
  openNewSectionModal(type)
}

// 删除模块（除了基本信息之外都可以删除）
function handleDeleteSection(): void {
  if (!resumeId.value || !activeSection.value) return
  showDeleteConfirmModal.value = true
}

// 确认删除模块
async function confirmDeleteCustomItem(): Promise<void> {
  if (!resumeId.value || !activeSection.value) return

  try {
    // 判断是 CUSTOM_ITEM 还是普通 section
    if (isCustomItem.value) {
      // CUSTOM_ITEM：删除单个 item
      const match = activeSection.value.match(/^custom_(\d+)$/)
      if (!match) return

      const itemIndex = parseInt(match[1], 10)
      const customSection = store.currentResume.sections.find((s: ResumeSection) => s.type === 'CUSTOM')
      if (!customSection) return

      await store.deleteResumeSectionItem(resumeId.value, customSection.id, itemIndex)
    } else {
      // 普通 section：删除整个模块
      await store.deleteResumeSection(resumeId.value, activeSection.value)
    }

    // 删除后选中第一个模块
    if (store.currentResume.sections.length > 0) {
      activeSection.value = String(store.currentResume.sections[0].id)
    } else {
      activeSection.value = ''
    }
  } catch (error) {
    console.error('删除失败:', error)
    toast.error('删除失败，请重试')
  }
}

// 优化完成后刷新简历详情
async function handleOptimizeComplete(): Promise<void> {
  if (resumeId.value) {
    await store.fetchResumeDetail(resumeId.value)
  }
}

// 应用优化变更
async function handleApplyChanges(editedAfterSection?: ResumeSection[] | null): Promise<void> {
  if (!resumeId.value) return

  const success = await applyChanges(editedAfterSection ?? undefined)
  if (success) {
    // 应用成功，刷新简历详情并关闭弹窗
    await store.fetchResumeDetail(resumeId.value)
    showOptimizeModal.value = false
    toast.success('变更已应用，评分已更新')
  } else {
    // 应用失败，显示错误提示（弹窗保持打开状态）
    console.error('应用变更失败:', optimizeState.applyError)
    toast.error(optimizeState.applyError || '应用变更失败，请重试')
  }
}
</script>

<style lang="scss" scoped>
.resume-detail-page {
  padding: $spacing-2xl;
  max-width: 1400px;
  margin: 0 auto;
}

.container {
  display: flex;
  flex-direction: column;
  gap: $spacing-xl;
}

.back-nav {
  margin-bottom: $spacing-sm;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: $spacing-sm;
  font-size: $text-sm;
  color: $color-text-tertiary;
  transition: color $transition-fast;
  &:hover {
    color: $color-accent;
  }
}

// 内容区域
.content-grid {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: $spacing-xl;
}

// 详情面板
.detail-panel {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-lg;
}

.panel-title {
  font-family: $font-display;
  font-size: $text-xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.panel-btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-xs $spacing-md;
  font-size: $text-sm;
  color: $color-text-secondary;
  background: rgba(255, 255, 255, 0.05);
  border-radius: $radius-sm;
  transition: all $transition-fast;
  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: $color-text-primary;
  }
  &.primary {
    background: $color-accent-glow;
    color: $color-accent;
    &:hover {
      background: rgba(212, 168, 83, 0.2);
    }
  }
  &.danger {
    color: $color-error;
    &:hover {
      background: rgba(248, 113, 113, 0.1);
    }
  }
}

// 空状态
.empty-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-md;
  color: $color-text-tertiary;
}

.empty-text {
  font-size: $text-sm;
}

// 动画
.animate-in {
  animation: slideUp 0.6s ease forwards;
  animation-delay: calc(var(--delay) * 0.1s);
  opacity: 0;
}
</style>
