<!--=====================================================
  简历模块列表组件
  统一从 content 解析数据
  @author Azir
=====================================================-->

<template>
  <section class="sections-panel animate-in" style="--delay: 3">
    <div class="panel-header">
      <h2 class="panel-title">简历模块</h2>
      <button class="add-section-btn" @click="$emit('add-section')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19"></line>
          <line x1="5" y1="12" x2="19" y2="12"></line>
        </svg>
        添加模块
      </button>
    </div>
    <div class="sections-list">
      <template v-for="(section, index) in sections" :key="section.id">
        <!-- CUSTOM 类型：展开显示每个 item -->
        <template v-if="section.type === 'CUSTOM'">
          <div
            v-for="(item, itemIndex) in getCustomItems(section)"
            :key="item.id"
            class="section-card"
            :class="{ active: activeSection === item.id }"
            :style="{ '--index': index + itemIndex }"
            @click="$emit('update:activeSection', item.id)"
          >
            <div class="section-header">
              <div class="section-info">
                <span class="section-icon">{{ getSectionIcon(section.type) }}</span>
                <span class="section-name">{{ item.content?.title || '自定义区块' }}</span>
              </div>
              <div class="section-actions">
                <div class="section-score" :class="analyzed && item.score != null ? getScoreClass(item.score!) : ''">
                  {{ analyzed ? (item.score ?? '~') : '~' }}
                </div>
                <!-- CUSTOM 类型显示删除按钮 -->
                <button
                  class="delete-btn"
                  @click.stop="$emit('delete-section', section.id)"
                  title="删除区块"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                  </svg>
                </button>
              </div>
            </div>
            <p class="section-preview">{{ getCustomItemPreview({ content: JSON.stringify(item.content) }) }}</p>
          </div>
        </template>
        <!-- 非 CUSTOM 类型：正常显示 -->
        <div
          v-else
          class="section-card"
          :class="{ active: activeSection === section.id }"
          :style="{ '--index': index }"
          @click="$emit('update:activeSection', section.id)"
        >
          <div class="section-header">
            <div class="section-info">
              <span class="section-icon">{{ getSectionIcon(section.type) }}</span>
              <span class="section-name">{{ section.title }}</span>
            </div>
            <div class="section-score" :class="analyzed ? getScoreClass(section.score) : ''">
              {{ analyzed ? section.score : '~' }}
            </div>
          </div>
          <p class="section-preview">{{ getSectionPreview(section) }}</p>
          <div v-if="section.suggestions?.length" class="section-hint">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="16" x2="12" y2="12"></line>
              <line x1="12" y1="8" x2="12.01" y2="8"></line>
            </svg>
            {{ section.suggestions.length }} 条优化建议
          </div>
        </div>
      </template>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useSectionHelper } from '@/composables/useSectionHelper'
import type { ResumeSection } from '@/types'

defineProps<{
  sections: ResumeSection[]
  activeSection: string
  analyzed: boolean
}>()

defineEmits<{
  'update:activeSection': [id: string]
  'add-section': []
  'delete-section': [sectionId: string]
}>()

const {
  getSectionIcon,
  getSectionPreview,
  getCustomItemPreview,
  getScoreClass,
  getCustomItems
} = useSectionHelper()
</script>

<style lang="scss" scoped>
.sections-panel {
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

.add-section-btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $text-sm;
  color: $color-accent;
  transition: color $transition-fast;
  &:hover {
    color: $color-accent-light;
  }
}

.sections-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.section-card {
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all $transition-fast;
  animation: slideUp 0.4s ease forwards;
  animation-delay: calc(var(--index) * 0.1s);
  opacity: 0;
  &:hover {
    background: rgba(255, 255, 255, 0.04);
  }
  &.active {
    background: $color-accent-glow;
    border-color: rgba(212, 168, 83, 0.3);
  }
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xs;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.delete-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  color: $color-text-tertiary;
  background: transparent;
  border-radius: $radius-sm;
  opacity: 0;
  transition: all $transition-fast;

  &:hover {
    color: $color-error;
    background: rgba(248, 113, 113, 0.1);
  }
}

.section-card:hover .delete-btn {
  opacity: 1;
}

.section-info {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.section-icon {
  font-size: $text-lg;
}

.section-name {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.section-score {
  font-size: $text-sm;
  font-weight: $weight-semibold;
  padding: $spacing-xs $spacing-sm;
  border-radius: $radius-sm;
  &.excellent {
    background: $color-success-bg;
    color: $color-success;
  }
  &.good {
    background: $color-accent-glow;
    color: $color-accent;
  }
  &.average {
    background: $color-warning-bg;
    color: $color-warning;
  }
}

.section-preview {
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-bottom: $spacing-sm;
}

.section-hint {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $text-xs;
  color: $color-warning;
}

.animate-in {
  animation: slideUp 0.6s ease forwards;
  animation-delay: calc(var(--delay) * 0.1s);
  opacity: 0;
}
</style>
