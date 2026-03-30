<!--=====================================================
  简历模块列表组件
  统一从 content 解析数据
  @author Azir
=====================================================-->

<template>
  <section
    class="sections-panel animate-in"
    style="--delay: 3"
  >
    <div class="panel-header">
      <h2 class="panel-title">
        简历模块
      </h2>
      <button
        class="add-section-btn"
        @click="$emit('add-section')"
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
        添加模块
      </button>
    </div>
    <div class="sections-list">
      <div
        v-for="(section, index) in sections"
        :key="section.id"
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
          <div
            class="section-score"
            :class="analyzed ? getScoreClass(section.score) : ''"
          >
            {{ analyzed ? section.score : '~' }}
          </div>
        </div>
        <p class="section-preview">
          {{ getSectionPreview(section) }}
        </p>
        <div
          v-if="section.suggestions?.length"
          class="section-hint"
          :class="getHighestPriorityClass(section.suggestions)"
        >
          <span class="hint-icon">{{ getSuggestionIcon(section.suggestions) }}</span>
          {{ section.suggestions.length }} 条建议
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useSectionHelper } from '@/composables/useSectionHelper'
import type { ResumeSection, ResumeSuggestionItem } from '@/types'

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
  getScoreClass
} = useSectionHelper()

// 根据最高优先级获取图标
function getSuggestionIcon(suggestions: ResumeSuggestionItem[]): string {
  const hasCritical = suggestions.some(s => s.type === 'critical')
  const hasImprovement = suggestions.some(s => s.type === 'improvement')
  if (hasCritical) return '⚠️'
  if (hasImprovement) return '💡'
  return '✨'
}

// 根据最高优先级获取样式类
function getHighestPriorityClass(suggestions: ResumeSuggestionItem[]): string {
  const hasCritical = suggestions.some(s => s.type === 'critical')
  const hasImprovement = suggestions.some(s => s.type === 'improvement')
  if (hasCritical) return 'critical'
  if (hasImprovement) return 'improvement'
  return 'enhancement'
}
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
  padding: 2px $spacing-sm;
  border-radius: $radius-sm;
  width: fit-content;

  &.critical {
    background: rgba(248, 113, 113, 0.1);
    color: $color-error;
  }

  &.improvement {
    background: rgba(251, 191, 36, 0.1);
    color: $color-warning;
  }

  &.enhancement {
    background: rgba(96, 165, 250, 0.1);
    color: $color-info;
  }
}

.hint-icon {
  font-size: 12px;
}

.animate-in {
  animation: slideUp 0.6s ease forwards;
  animation-delay: calc(var(--delay) * 0.1s);
  opacity: 0;
}
</style>
