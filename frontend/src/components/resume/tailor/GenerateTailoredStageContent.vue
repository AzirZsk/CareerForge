<!--=====================================================
  LandIt 定制简历结果阶段内容组件
  @author Azir
=====================================================-->

<template>
  <div class="data-content">
    <!-- 头部：左边统计，右边操作按钮 -->
    <div class="tailor-summary">
      <div class="tailor-summary-left">
        <span class="tailor-improvement" v-if="data.improvementScore">
          预计提升 {{ data.improvementScore }} 分
        </span>
        <span class="tailor-match" v-if="data.matchScore">
          匹配度 {{ data.matchScore }}%
        </span>
      </div>
      <button
        v-if="canShowComparison"
        class="comparison-btn"
        @click="$emit('showComparison')"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
          <line x1="12" y1="3" x2="12" y2="21"/>
        </svg>
        对比&编辑
      </button>
    </div>

    <!-- 定制说明 -->
    <div class="tailor-notes" v-if="data.tailorNotes?.length">
      <div class="notes-title">定制说明</div>
      <ul class="notes-list">
        <li v-for="(note, idx) in data.tailorNotes" :key="idx">{{ note }}</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  data: {
    improvementScore?: number
    matchScore?: number
    tailorNotes?: string[]
  }
  canShowComparison: boolean
}>()

defineEmits<{
  showComparison: []
}>()
</script>

<style lang="scss" scoped>
.data-content {
  font-size: $text-sm;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-sm;
  padding: $spacing-md;
}

.tailor-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-md;
  padding-bottom: $spacing-md;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);

  .tailor-summary-left {
    display: flex;
    align-items: center;
    gap: $spacing-md;
  }

  .tailor-improvement {
    font-size: $text-sm;
    color: $color-success;
    font-weight: $weight-medium;
  }

  .tailor-match {
    font-size: $text-sm;
    color: $color-accent;
    font-weight: $weight-medium;
  }
}

.comparison-btn {
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-xs $spacing-md;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-sm;
  font-size: $text-xs;
  color: $color-text-secondary;
  cursor: pointer;
  transition: all $transition-fast;
  white-space: nowrap;

  &:hover {
    background: rgba(212, 168, 83, 0.1);
    border-color: rgba(212, 168, 83, 0.3);
    color: $color-accent;
  }
}

.tailor-notes {
  .notes-title {
    font-size: $text-xs;
    color: $color-text-tertiary;
    margin-bottom: $spacing-sm;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .notes-list {
    list-style: none;
    padding: 0;
    margin: 0;

    li {
      position: relative;
      padding-left: $spacing-lg;
      font-size: $text-sm;
      color: $color-text-secondary;
      margin-bottom: $spacing-xs;

      &::before {
        content: '✓';
        position: absolute;
        left: 0;
        color: $color-success;
      }
    }
  }
}
</style>
