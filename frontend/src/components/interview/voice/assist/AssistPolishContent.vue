<template>
  <div class="assist-polish">
    <!-- 降级模式 -->
    <div v-if="isFallback" class="fallback-content">
      <p>{{ fallbackText }}</p>
    </div>
    <!-- 正常结构化渲染 -->
    <template v-else>
      <!-- 润色后的回答 -->
      <div v-if="data.polishedAnswer" class="polished-block">
        <h4 class="section-title">润色后的回答</h4>
        <div class="polished-text">{{ data.polishedAnswer }}</div>
      </div>
      <!-- 改进要点 -->
      <div v-if="data.improvements?.length" class="section">
        <h4 class="section-title">改进要点</h4>
        <div class="improvement-list">
          <div v-for="(item, i) in data.improvements" :key="i" class="improvement-item">
            <div class="improvement-header">
              <font-awesome-icon icon="fa-solid fa-arrow-up" class="improve-icon" />
              <span class="improve-point">{{ item.point }}</span>
            </div>
            <div class="diff-row">
              <span class="diff-label before">原文：</span>
              <span class="diff-text before-text">{{ item.before }}</span>
            </div>
            <div class="diff-row">
              <span class="diff-label after">优化：</span>
              <span class="diff-text after-text">{{ item.after }}</span>
            </div>
          </div>
        </div>
      </div>
      <!-- 面试小贴士 -->
      <div v-if="data.tips?.length" class="section">
        <h4 class="section-title">面试小贴士</h4>
        <div class="tag-list">
          <span v-for="(tip, i) in data.tips" :key="i" class="tip-tag">{{ tip }}</span>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PolishData, FallbackData } from '@/types/interview-voice'

interface Props {
  data: Record<string, any>
}

const props = defineProps<Props>()

const isFallback = computed(() => 'fallbackText' in props.data)
const fallbackText = computed(() => (props.data as FallbackData).fallbackText)
const data = computed(() => props.data as PolishData)
</script>

<style scoped lang="scss">
.assist-polish {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.polished-block {
  padding: $spacing-md;
  background: rgba(212, 168, 83, 0.1);
  border: 1px solid rgba(212, 168, 83, 0.2);
  border-radius: $radius-md;

  .polished-text {
    color: $color-text-primary;
    font-size: $text-sm;
    line-height: $leading-relaxed;
    margin-top: $spacing-xs;
  }
}

.section {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.section-title {
  font-size: $text-sm;
  font-weight: $weight-semibold;
  color: $color-text-secondary;
  margin: 0;
}

.improvement-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.improvement-item {
  padding: $spacing-sm $spacing-md;
  background: $color-bg-tertiary;
  border-radius: $radius-sm;
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.improvement-header {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  margin-bottom: 2px;

  .improve-icon {
    color: $color-accent;
    font-size: 12px;
  }

  .improve-point {
    color: $color-accent;
    font-size: 12px;
    font-weight: $weight-medium;
  }
}

.diff-row {
  display: flex;
  align-items: flex-start;
  gap: $spacing-xs;
  font-size: 13px;
  line-height: $leading-relaxed;

  .diff-label {
    flex-shrink: 0;
    font-weight: $weight-medium;
    font-size: 12px;
  }

  .diff-label.before {
    color: $color-error;
  }

  .diff-label.after {
    color: $color-success;
  }

  .diff-text {
    color: $color-text-primary;
  }

  .before-text {
    opacity: 0.6;
    text-decoration: line-through;
  }

  .after-text {
    color: $color-success;
  }
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
}

.tip-tag {
  display: inline-block;
  padding: 2px 10px;
  background: rgba(212, 168, 83, 0.1);
  color: $color-accent;
  border-radius: $radius-full;
  font-size: 12px;
  line-height: 1.6;
}

.fallback-content {
  color: $color-text-primary;
  font-size: $text-sm;
  line-height: $leading-relaxed;
}
</style>
