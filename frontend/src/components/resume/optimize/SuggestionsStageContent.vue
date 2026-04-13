<!--=====================================================
  CareerForge 建议阶段内容组件
  @author Azir
=====================================================-->

<template>
  <div class="data-content">
    <!-- 建议统计 -->
    <div class="suggestions-summary">
      <span class="suggestions-count">{{ data.totalSuggestions }} 条建议</span>
    </div>

    <!-- 建议列表 -->
    <div
      v-if="data.suggestions?.length"
      class="suggestions-list"
    >
      <div
        v-for="(sug, idx) in data.suggestions"
        :key="idx"
        class="suggestion-item"
      >
        <div class="sug-header">
          <span
            class="sug-type"
            :class="sug.type"
          >{{ getSuggestionTypeLabel(sug.type) }}</span>
          <span
            class="sug-impact-badge"
            :class="sug.impact"
          >{{ getSuggestionImpactLabel(sug.impact) }}</span>
          <span class="sug-title">{{ sug.title }}</span>
        </div>
        <div class="sug-section">
          位置: {{ sug.position || sug.category }}
        </div>
        <div
          v-if="sug.problem"
          class="sug-problem"
        >
          问题: {{ sug.problem }}
        </div>
        <div class="sug-direction">
          方向：{{ sug.direction }}
        </div>
        <div
          v-if="sug.example"
          class="sug-example"
        >
          示例：{{ sug.example }}
        </div>
        <div
          v-if="sug.value"
          class="sug-value"
        >
          价值: {{ sug.value }}
        </div>
      </div>
    </div>

    <!-- 快速改进项 -->
    <div
      v-if="data.quickWins?.length"
      class="quickwins-section"
    >
      <div class="quickwins-title">
        快速改进项
      </div>
      <div
        v-for="(quickWin, idx) in data.quickWins"
        :key="idx"
        class="quickwin-item"
      >
        ✓ {{ quickWin.action || quickWin }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { GenerateSuggestionsData } from '@/types/resume-optimize'
import { getSuggestionTypeLabel, getSuggestionImpactLabel } from '@/utils/stageHelpers'

defineProps<{
  data: GenerateSuggestionsData
}>()
</script>

<style lang="scss" scoped>
.data-content {
  font-size: $text-sm;
}

// 建议统计
.suggestions-summary {
  display: flex;
  justify-content: space-between;
  margin-bottom: $spacing-sm;
}

.suggestions-count {
  font-size: $text-sm;
  color: $color-text-primary;
  font-weight: $weight-medium;
}

// 建议列表
.suggestion-item {
  padding: $spacing-sm;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-sm;
  margin-bottom: $spacing-xs;
}

.sug-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: 4px;
}

// 建议类型标签
.sug-type {
  padding: 2px 6px;
  border-radius: $radius-sm;
  font-size: $text-xs;

  &.critical {
    background: rgba(248, 113, 113, 0.15);
    color: $color-error;
  }
  &.improvement {
    background: rgba(251, 191, 36, 0.15);
    color: $color-warning;
  }
  &.enhancement {
    background: rgba(96, 165, 250, 0.15);
    color: $color-info;
  }
}

// 建议影响程度标签
.sug-impact-badge {
  padding: 2px 6px;
  border-radius: $radius-sm;
  font-size: $text-xs;

  &.high {
    background: rgba(248, 113, 113, 0.15);
    color: $color-error;
  }
  &.medium {
    background: rgba(251, 191, 36, 0.15);
    color: $color-warning;
  }
  &.low {
    background: rgba(96, 165, 250, 0.15);
    color: $color-info;
  }
}

.sug-title {
  font-size: $text-sm;
  color: $color-text-primary;
}

.sug-section {
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-bottom: 4px;
}

.sug-problem {
  font-size: $text-xs;
  color: $color-error;
  margin-bottom: 4px;
  padding-left: $spacing-sm;
}

.sug-direction {
  font-size: $text-xs;
  color: $color-text-primary;
  margin-bottom: 4px;
  padding-left: $spacing-sm;
  line-height: 1.4;
}

.sug-example {
  font-size: $text-xs;
  color: $color-accent;
  margin-bottom: 4px;
  padding-left: $spacing-sm;
  line-height: 1.4;
  font-style: italic;
}

.sug-value {
  font-size: $text-xs;
  color: $color-success;
  padding-left: $spacing-sm;
  font-style: italic;
}

// 快速改进项
.quickwins-section {
  margin-top: $spacing-sm;
}

.quickwins-title {
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-bottom: $spacing-xs;
}

.quickwin-item {
  display: flex;
  align-items: flex-start;
  gap: $spacing-xs;
  padding: $spacing-xs;
  font-size: $text-xs;
  color: $color-text-secondary;
  background: rgba(251, 191, 36, 0.05);
  border-radius: $radius-sm;
  margin-bottom: 4px;
}
</style>
