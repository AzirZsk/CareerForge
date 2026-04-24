<template>
  <div class="assist-hints">
    <!-- 降级模式 -->
    <div v-if="isFallback" class="fallback-content">
      <p>{{ fallbackText }}</p>
    </div>
    <!-- 正常结构化渲染 -->
    <template v-else>
      <!-- 思路总结 -->
      <div v-if="data.summary" class="summary-block">
        <font-awesome-icon icon="fa-solid fa-lightbulb" class="block-icon" />
        <span class="summary-text">{{ data.summary }}</span>
      </div>
      <!-- 思考步骤 -->
      <div v-if="data.thinkingSteps?.length" class="section">
        <h4 class="section-title">思考步骤</h4>
        <ol class="step-list">
          <li v-for="(step, i) in data.thinkingSteps" :key="i" class="step-item">
            <span class="step-number">{{ i + 1 }}</span>
            <span class="step-text">{{ step }}</span>
          </li>
        </ol>
      </div>
      <!-- 关键知识点 -->
      <div v-if="data.keyPoints?.length" class="section">
        <h4 class="section-title">关键知识点</h4>
        <div class="tag-list">
          <span v-for="(point, i) in data.keyPoints" :key="i" class="tag">{{ point }}</span>
        </div>
      </div>
      <!-- 建议回答结构 -->
      <div v-if="data.answerStructure" class="section">
        <h4 class="section-title">建议回答结构</h4>
        <div class="structure-block">{{ data.answerStructure }}</div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { HintsData, FallbackData } from '@/types/interview-voice'

interface Props {
  data: Record<string, any>
}

const props = defineProps<Props>()

const isFallback = computed(() => 'fallbackText' in props.data)
const fallbackText = computed(() => (props.data as FallbackData).fallbackText)
const data = computed(() => props.data as HintsData)
</script>

<style scoped lang="scss">
.assist-hints {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.summary-block {
  display: flex;
  align-items: flex-start;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  background: rgba(96, 165, 250, 0.1);
  border-left: 3px solid $color-info;
  border-radius: $radius-sm;

  .block-icon {
    color: $color-info;
    margin-top: 2px;
    flex-shrink: 0;
  }

  .summary-text {
    color: $color-text-primary;
    font-size: $text-sm;
    line-height: $leading-relaxed;
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

.step-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.step-item {
  display: flex;
  align-items: flex-start;
  gap: $spacing-sm;

  .step-number {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: rgba(96, 165, 250, 0.15);
    color: $color-info;
    font-size: 12px;
    font-weight: $weight-semibold;
    flex-shrink: 0;
  }

  .step-text {
    color: $color-text-primary;
    font-size: $text-sm;
    line-height: $leading-relaxed;
  }
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
}

.tag {
  display: inline-block;
  padding: 2px 10px;
  background: rgba(96, 165, 250, 0.1);
  color: $color-info;
  border-radius: $radius-full;
  font-size: 12px;
  line-height: 1.6;
}

.structure-block {
  padding: $spacing-sm $spacing-md;
  background: $color-bg-tertiary;
  border-radius: $radius-sm;
  color: $color-text-primary;
  font-size: $text-sm;
  line-height: $leading-relaxed;
}

.fallback-content {
  color: $color-text-primary;
  font-size: $text-sm;
  line-height: $leading-relaxed;
}
</style>
