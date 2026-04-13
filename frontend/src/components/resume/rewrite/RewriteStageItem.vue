<!--=====================================================
  CareerForge 风格改写阶段项组件
  @author Azir
======================================================-->

<template>
  <div
    class="stage-item"
    :class="{
      active: isActive,
      completed: item.completed
    }"
  >
    <div
      class="stage-main"
      :class="{ clickable: item.completed && item.data }"
      @click="item.completed && item.data && $emit('toggleExpand')"
    >
      <div class="stage-left">
        <!-- 阶段指示器 -->
        <div class="stage-indicator">
          <svg
            v-if="item.completed"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <polyline points="20 6 9 17 4 12" />
          </svg>
          <div
            v-else-if="isActive"
            class="spinner"
          />
          <div
            v-else
            class="dot"
          />
        </div>
        <!-- 阶段标签 -->
        <span class="stage-label">{{ getRewriteStageLabel(item.stage) }}</span>
        <!-- 耗时 -->
        <span
          v-if="item.startTime"
          class="stage-elapsed"
          :class="{ running: !item.endTime && isActive }"
        >
          {{ elapsed }}
        </span>
      </div>
      <!-- 展开指示器 -->
      <div
        v-if="item.completed && item.data"
        class="expand-indicator"
        :class="{ expanded: item.expanded }"
      >
        <svg
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <polyline points="6 9 12 15" />
        </svg>
      </div>
    </div>

    <!-- 展开内容 -->
    <div
      v-if="item.expanded"
      class="data-content"
    >
      <!-- 风格分析内容 -->
      <div
        v-if="item.stage === 'analyze_style' && item.data"
        class="content-block style-analysis"
      >
        <h4 class="content-title">风格分析结果</h4>
        <!-- 语气和句式信息 -->
        <div
          v-if="item.data.toneDescription || item.data.voicePattern"
          class="analysis-summary"
        >
          <div
            v-if="item.data.toneDescription"
            class="summary-item"
          >
            <span class="summary-label">语气风格：</span>
            <span class="summary-value">{{ item.data.toneDescription }}</span>
          </div>
          <div
            v-if="item.data.verbStyle"
            class="summary-item"
          >
            <span class="summary-label">动词风格：</span>
            <span class="summary-value">{{ item.data.verbStyle }}</span>
          </div>
        </div>
        <!-- 风格示例列表 -->
        <div
          v-if="item.data.styleExamples && item.data.styleExamples.length > 0"
          class="examples-section"
        >
          <h5 class="section-subtitle">风格示例</h5>
          <div
            v-for="(example, idx) in item.data.styleExamples"
            :key="idx"
            class="example-item"
          >
            <div class="example-header">
              <span class="example-type">{{ example.sectionType }}</span>
            </div>
            <div class="example-text">{{ example.referenceText }}</div>
            <div class="example-point">{{ example.stylePoint }}</div>
          </div>
        </div>
      </div>

      <!-- 风格差异建议内容 -->
      <div
        v-if="item.stage === 'generate_style_diff' && item.data"
        class="content-block style-diff"
      >
        <h4 class="content-title">风格差异建议</h4>
        <div
          v-for="(suggestion, idx) in item.data?.suggestions"
          :key="idx"
          class="diff-item"
        >
          <div class="diff-header">
            <span
              class="diff-type"
              :class="suggestion.type"
            >{{ suggestion.title }}</span>
          </div>
          <div class="diff-content">{{ suggestion.problem }}</div>
          <div
            v-if="suggestion.direction"
            class="diff-direction"
          >
            建议：{{ suggestion.direction }}
          </div>
        </div>
      </div>

      <!-- 区块改写内容 -->
      <div
        v-if="item.stage === 'rewrite_section' && item.data"
        class="content-block rewrite-content"
      >
        <h4 class="content-title">区块改写结果</h4>
        <!-- 变更列表 -->
        <div
          v-if="item.data?.changes && item.data.changes.length > 0"
          class="changes-list"
        >
          <div
            v-for="(change, idx) in item.data.changes"
            :key="idx"
            class="change-item"
          >
            <div class="change-header">
              <span
                class="change-type"
                :class="change.type"
              >{{ change.typeLabel || change.type }}</span>
              <span class="change-section">{{ change.fieldLabel || change.field }}</span>
            </div>
            <div
              v-if="change.reason"
              class="change-content"
            >
              {{ change.reason }}
            </div>
          </div>
        </div>
        <!-- 差异对比 -->
        <div
          v-if="item.data?.beforeSection && item.data?.afterSection"
          class="section-diff"
        >
          <div class="diff-column before">
            <h5 class="section-title">改写前</h5>
            <div class="diff-grid">
              <div
                v-for="(section, idx) in item.data.beforeSection"
                :key="idx"
                class="diff-row"
              >
                <div class="diff-label">{{ section.title }}</div>
                <div class="diff-value">{{ section.content }}</div>
              </div>
            </div>
          </div>
          <div class="diff-column after">
            <h5 class="section-title">改写后</h5>
            <div class="diff-grid">
              <div
                v-for="(section, idx) in item.data.afterSection"
                :key="idx"
                class="diff-row"
              >
                <div class="diff-label">{{ section.title }}</div>
                <div class="diff-value">{{ section.content }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RewriteStageHistoryItem } from '@/types/resume-rewrite'
import { getRewriteStageLabel } from '@/types/resume-rewrite'

defineProps<{
  item: RewriteStageHistoryItem
  isActive: boolean
  completed: boolean
  elapsed: string
}>()

defineEmits<{
  toggleExpand: []
}>()
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.stage-item {
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  border: 1px solid rgba(255, 255, 255, 0.05);
  margin-bottom: $spacing-sm;
  transition: all $transition-fast;

  &.active {
    border-color: rgba($color-accent, 0.3);
  }

  &.completed {
    border-color: rgba(52, 211, 153, 0.2);
  }
}

.stage-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-md;

  &.clickable {
    cursor: pointer;

    &:hover {
      background: rgba(255, 255, 255, 0.03);
    }
  }
}

.stage-left {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.stage-indicator {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;

  svg {
    color: $color-success;
  }

  .spinner {
    width: 16px;
    height: 16px;
    border: 2px solid rgba($color-accent, 0.3);
    border-top-color: $color-accent;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  .dot {
    width: 8px;
    height: 8px;
    background: rgba($color-accent, 0.3);
    border-radius: 50%;
  }
}

.stage-label {
  flex: 1;
  font-size: $text-sm;
  color: $color-text-primary;
}

.stage-elapsed {
  font-size: $text-xs;
  color: $color-text-tertiary;

  &.running {
    color: $color-accent;
  }
}

.expand-indicator {
  color: $color-text-tertiary;
  transition: transform 0.3s ease;

  &.expanded {
    transform: rotate(180deg);
  }
}

.data-content {
  padding: $spacing-md;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.content-block {
  margin-bottom: $spacing-md;

  &:last-child {
    margin-bottom: 0;
  }
}

.content-title {
  font-size: $text-sm;
  color: $color-text-secondary;
  margin-bottom: $spacing-sm;
  font-weight: 500;
}

// 风格分析样式
.style-analysis {
  .analysis-summary {
    margin-bottom: $spacing-md;
    padding: $spacing-md;
    background: rgba($color-accent, 0.05);
    border-radius: $radius-md;
  }

  .summary-item {
    margin-bottom: $spacing-xs;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .summary-label {
    font-size: $text-xs;
    color: $color-text-tertiary;
    margin-right: $spacing-xs;
  }

  .summary-value {
    font-size: $text-sm;
    color: $color-text-secondary;
  }

  .examples-section {
    margin-top: $spacing-md;
  }

  .section-subtitle {
    font-size: $text-xs;
    color: $color-text-tertiary;
    margin-bottom: $spacing-sm;
    font-weight: 500;
  }

  .example-item {
    padding: $spacing-sm;
    background: rgba(255, 255, 255, 0.02);
    border-radius: $radius-sm;
    margin-bottom: $spacing-sm;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .example-header {
    margin-bottom: $spacing-xs;
  }

  .example-type {
    font-size: $text-xs;
    color: $color-accent;
    font-weight: 500;
  }

  .example-text {
    font-size: $text-sm;
    color: $color-text-secondary;
    line-height: 1.5;
    margin-bottom: $spacing-xs;
  }

  .example-point {
    font-size: $text-xs;
    color: $color-text-tertiary;
    font-style: italic;
  }
}

// 风格差异建议样式
.style-diff {
  .diff-item {
    padding: $spacing-sm;
    background: rgba(255, 255, 255, 0.02);
    border-radius: $radius-sm;
    margin-bottom: $spacing-sm;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .diff-header {
    margin-bottom: $spacing-xs;
  }

  .diff-type {
    font-size: $text-xs;
    color: $color-accent;
    font-weight: 500;

    &.critical {
      color: $color-error;
    }

    &.improvement {
      color: $color-warning;
    }

    &.enhancement {
      color: $color-info;
    }
  }

  .diff-content {
    font-size: $text-sm;
    color: $color-text-secondary;
    line-height: 1.5;
    margin-bottom: $spacing-xs;
  }

  .diff-direction {
    font-size: $text-xs;
    color: $color-text-tertiary;
    font-style: italic;
  }
}

// 区块改写样式
.rewrite-content {
  .changes-list {
    margin-bottom: $spacing-md;
  }

  .change-item {
    padding: $spacing-sm;
    background: rgba(255, 255, 255, 0.02);
    border-radius: $radius-sm;
    margin-bottom: $spacing-sm;
  }

  .change-header {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    margin-bottom: $spacing-xs;
  }

  .change-type {
    font-size: $text-xs;
    padding: 2px 6px;
    border-radius: $radius-sm;
    font-weight: 500;

    &.modified {
      background: rgba($color-info, 0.2);
      color: $color-info;
    }

    &.added {
      background: rgba($color-success, 0.2);
      color: $color-success;
    }

    &.removed {
      background: rgba($color-error, 0.2);
      color: $color-error;
    }
  }

  .change-section {
    font-size: $text-xs;
    color: $color-text-tertiary;
  }

  .change-content {
    font-size: $text-sm;
    color: $color-text-secondary;
    line-height: 1.5;
    margin-bottom: $spacing-xs;
  }
}

// 差异对比样式
.section-diff {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: $spacing-md;
}

.diff-column {
  &.before {
    .section-title {
      color: $color-text-tertiary;
    }

    .diff-value {
      background: rgba($color-error, 0.1);
    }
  }

  &.after {
    .section-title {
      color: $color-accent;
    }

    .diff-value {
      background: rgba($color-success, 0.1);
    }
  }
}

.section-title {
  font-size: $text-xs;
  font-weight: 500;
  margin-bottom: $spacing-sm;
}

.diff-grid {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.diff-row {
  display: flex;
  align-items: flex-start;
  gap: $spacing-sm;
  padding: $spacing-xs;
  border-radius: $radius-sm;
}

.diff-label {
  font-size: $text-xs;
  color: $color-text-tertiary;
  min-width: 80px;
  flex-shrink: 0;
}

.diff-value {
  flex: 1;
  font-size: $text-xs;
  color: $color-text-secondary;
  padding: $spacing-xs;
  border-radius: $radius-sm;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>
