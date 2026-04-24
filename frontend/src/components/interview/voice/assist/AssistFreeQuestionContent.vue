<template>
  <div class="assist-free">
    <!-- 降级模式 -->
    <div v-if="isFallback" class="fallback-content">
      <p>{{ fallbackText }}</p>
    </div>
    <!-- 正常结构化渲染 -->
    <template v-else>
      <!-- AI 回答 -->
      <div v-if="data.answer" class="answer-block">
        <p class="answer-text">{{ data.answer }}</p>
      </div>
      <!-- 建议 -->
      <div v-if="data.suggestions?.length" class="section">
        <h4 class="section-title">建议</h4>
        <ul class="suggestion-list">
          <li v-for="(s, i) in data.suggestions" :key="i">
            <font-awesome-icon icon="fa-solid fa-arrow-right" class="arrow-icon" />
            <span>{{ s }}</span>
          </li>
        </ul>
      </div>
      <!-- 相关话题 -->
      <div v-if="data.relatedTopics?.length" class="section">
        <h4 class="section-title">相关话题</h4>
        <div class="tag-list">
          <span v-for="(topic, i) in data.relatedTopics" :key="i" class="topic-tag">{{ topic }}</span>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { FreeQuestionData, FallbackData } from '@/types/interview-voice'

interface Props {
  data: Record<string, any>
}

const props = defineProps<Props>()

const isFallback = computed(() => 'fallbackText' in props.data)
const fallbackText = computed(() => (props.data as FallbackData).fallbackText)
const data = computed(() => props.data as FreeQuestionData)
</script>

<style scoped lang="scss">
.assist-free {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.answer-block {
  padding: $spacing-sm $spacing-md;
  background: $color-bg-tertiary;
  border-radius: $radius-md;
  border-left: 3px solid $color-text-secondary;

  .answer-text {
    color: $color-text-primary;
    font-size: $text-sm;
    line-height: $leading-relaxed;
    margin: 0;
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

.suggestion-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;

  li {
    display: flex;
    align-items: flex-start;
    gap: $spacing-sm;
    color: $color-text-primary;
    font-size: $text-sm;
    line-height: $leading-relaxed;
  }

  .arrow-icon {
    color: $color-text-tertiary;
    margin-top: 4px;
    flex-shrink: 0;
    font-size: 12px;
  }
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
}

.topic-tag {
  display: inline-block;
  padding: 2px 10px;
  background: $color-bg-tertiary;
  color: $color-text-secondary;
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
