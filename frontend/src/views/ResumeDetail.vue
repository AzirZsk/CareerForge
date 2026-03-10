<!--=====================================================
  LandIt 简历详情页面
  @author Azir
=====================================================-->

<template>
  <div class="resume-detail-page">
    <div class="container">
      <!-- 返回导航 -->
      <nav class="back-nav animate-in" style="--delay: 0">
        <router-link to="/resume" class="back-link">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="19" y1="12" x2="5" y2="12"></line>
            <polyline points="12 19 5 12 12 5"></polyline>
          </svg>
          返回简历列表
        </router-link>
      </nav>

      <!-- 简历头部 -->
      <ResumeHeader
        :name="store.currentResume.name"
        :target-position="store.currentResume.targetPosition"
        :analyzed="store.currentResume.analyzed"
        :overall-score="store.currentResume.overallScore"
        :structure-score="store.currentResume.structureScore"
        @optimize="optimizeResume"
        @export="exportResume"
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
        />

        <!-- 详情面板 -->
        <section class="detail-panel animate-in" style="--delay: 4">
          <div class="panel-header">
            <h2 class="panel-title">{{ currentSectionDetail?.title }}</h2>
            <div class="panel-actions">
              <!-- 单条类型或 CUSTOM_ITEM：显示编辑按钮 -->
              <button v-if="!isAggregateSection || isCustomItem" class="panel-btn" @click="openEditModal">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                </svg>
                编辑
              </button>
              <!-- 聚合类型（非 CUSTOM_ITEM）：显示添加按钮 -->
              <button v-else class="panel-btn primary" @click="openAddItemModal">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
                添加
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

          <!-- 优化建议 -->
          <div v-if="hasSuggestions" class="suggestions-block">
            <h4 class="suggestions-title">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"></path>
              </svg>
              优化建议
            </h4>
            <div v-for="sug in sectionSuggestions" :key="sug.content" class="suggestion-item" :class="sug.type">
              <span class="sug-icon">{{ sug.type === 'critical' ? '⚠️' : sug.type === 'improvement' ? '💡' : '✨' }}</span>
              <p class="sug-text">{{ sug.content }}</p>
              <button class="apply-sug-btn">应用</button>
            </div>
          </div>
        </section>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <EditSectionModal
      v-model:visible="isEditModalVisible"
      :section="currentSectionDetail ?? null"
      :item-index="editItemIndex"
      :is-new="isNewItem"
      :saving="isSaving"
      @save="handleSave"
      @cancel="closeEditModal"
    />

    <!-- 优化进度弹窗 -->
    <OptimizeProgressModal
      v-model:visible="showOptimizeModal"
      :state="optimizeState"
      @cancel="cancelOptimize"
      @retry="retryOptimize"
      @toggle-expand="toggleStageExpanded"
      @complete="handleOptimizeComplete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores'
import EditSectionModal from '@/components/resume/EditSectionModal.vue'
import OptimizeProgressModal from '@/components/resume/OptimizeProgressModal.vue'
import ResumeHeader from '@/components/resume/ResumeHeader.vue'
import MetricsSection from '@/components/resume/MetricsSection.vue'
import SectionList from '@/components/resume/SectionList.vue'
import SectionContent from '@/components/resume/SectionContent.vue'
import { useResumeOptimize } from '@/composables/useResumeOptimize'
import { useSectionEdit } from '@/composables/useSectionEdit'
import { useSectionHelper } from '@/composables/useSectionHelper'
import type { ResumeSection, ResumeSuggestionItem } from '@/types'

const store = useAppStore()
const { parseContent } = useSectionHelper()
const route = useRoute()
const activeSection = ref<string>('')
const resumeId = ref<string>('')

// 优化相关状态
const showOptimizeModal = ref<boolean>(false)
const {
  state: optimizeState,
  startOptimize,
  cancelOptimize,
  retryOptimize,
  toggleStageExpanded
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
  const section = store.currentResume.sections.find((s: ResumeSection) => s.id === activeSection.value)
  return section
})

// 使用编辑 composable
const {
  isEditModalVisible,
  isSaving,
  editItemIndex,
  isNewItem,
  isAggregateSection,
  isCustomItem,
  openEditModal,
  openEditItemModal,
  openAddItemModal,
  closeEditModal,
  handleSave,
  deleteItem
} = useSectionEdit(resumeId, activeSection, currentSectionDetail)

// 优化建议
const sectionSuggestions = computed<ResumeSuggestionItem[]>(() => {
  return currentSectionDetail.value?.suggestions ?? []
})

const hasSuggestions = computed<boolean>(() => {
  return (currentSectionDetail.value?.suggestions?.length ?? 0) > 0
})

// 页面加载时获取简历详情
onMounted(async () => {
  const id = route.params.id as string
  if (id) {
    resumeId.value = id
    await store.fetchResumeDetail(id)
    // 默认选中第一个模块
    if (store.currentResume.sections.length > 0) {
      activeSection.value = String(store.currentResume.sections[0].id)
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

// 导出 PDF
function exportResume(): void {
  // TODO: 实现导出 PDF 功能
  alert('导出 PDF 功能开发中...')
}

// 优化完成后刷新简历详情
async function handleOptimizeComplete(): Promise<void> {
  if (resumeId.value) {
    await store.fetchResumeDetail(resumeId.value)
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
}

// 优化建议
.suggestions-block {
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  padding: $spacing-lg;
}

.suggestions-title {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  font-size: $text-base;
  font-weight: $weight-medium;
  color: $color-text-primary;
  margin-bottom: $spacing-lg;
}

.suggestion-item {
  display: flex;
  align-items: flex-start;
  gap: $spacing-md;
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  margin-bottom: $spacing-sm;
  border-left: 3px solid;
  &.critical {
    border-color: $color-error;
  }
  &.improvement {
    border-color: $color-warning;
  }
  &.enhancement {
    border-color: $color-info;
  }
}

.sug-icon {
  font-size: $text-lg;
}

.sug-text {
  flex: 1;
  font-size: $text-sm;
  color: $color-text-secondary;
  line-height: $leading-relaxed;
}

.apply-sug-btn {
  padding: $spacing-xs $spacing-md;
  background: $color-accent-glow;
  color: $color-accent;
  font-size: $text-xs;
  font-weight: $weight-medium;
  border-radius: $radius-sm;
  transition: all $transition-fast;
  white-space: nowrap;
  &:hover {
    background: rgba(212, 168, 83, 0.2);
  }
}

// 动画
.animate-in {
  animation: slideUp 0.6s ease forwards;
  animation-delay: calc(var(--delay) * 0.1s);
  opacity: 0;
}
</style>
