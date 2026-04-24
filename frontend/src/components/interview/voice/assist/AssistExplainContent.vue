<template>
  <div class="assist-explain">
    <!-- 降级模式 -->
    <div v-if="isFallback" class="fallback-content">
      <p>{{ fallbackText }}</p>
    </div>
    <!-- 正常结构化渲染 -->
    <template v-else>
      <!-- 概念定义 -->
      <div v-if="data.definition" class="definition-block">
        <h4 class="section-title">概念定义</h4>
        <p class="definition-text">{{ data.definition }}</p>
      </div>
      <!-- 类比理解 -->
      <div v-if="data.analogy" class="section">
        <h4 class="section-title">类比理解</h4>
        <div class="analogy-block">
          <font-awesome-icon icon="fa-solid fa-quote-left" class="quote-icon" />
          <span>{{ data.analogy }}</span>
        </div>
      </div>
      <!-- 应用场景 -->
      <div v-if="data.applications?.length" class="section">
        <h4 class="section-title">应用场景</h4>
        <ul class="app-list">
          <li v-for="(app, i) in data.applications" :key="i">
            <font-awesome-icon icon="fa-solid fa-check" class="check-icon" />
            <span>{{ app }}</span>
          </li>
        </ul>
      </div>
      <!-- 代码示例 -->
      <div v-if="data.codeExample" class="section">
        <h4 class="section-title">代码示例</h4>
        <pre class="code-block"><code>{{ data.codeExample }}</code></pre>
      </div>
      <!-- 总结 -->
      <div v-if="data.summary" class="summary-block">
        <font-awesome-icon icon="fa-solid fa-bookmark" class="block-icon" />
        <span>{{ data.summary }}</span>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ExplainData, FallbackData } from '@/types/interview-voice'

interface Props {
  data: Record<string, any>
}

const props = defineProps<Props>()

const isFallback = computed(() => 'fallbackText' in props.data)
const fallbackText = computed(() => (props.data as FallbackData).fallbackText)
const data = computed(() => props.data as ExplainData)
</script>

<style scoped lang="scss">
.assist-explain {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.definition-block {
  padding: $spacing-sm $spacing-md;
  background: rgba(52, 211, 153, 0.08);
  border-left: 3px solid $color-success;
  border-radius: $radius-sm;

  .definition-text {
    color: $color-text-primary;
    font-size: $text-sm;
    line-height: $leading-relaxed;
    margin: $spacing-xs 0 0;
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

.analogy-block {
  display: flex;
  align-items: flex-start;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  background: rgba(52, 211, 153, 0.06);
  border-radius: $radius-sm;
  color: $color-text-primary;
  font-size: $text-sm;
  line-height: $leading-relaxed;
  font-style: italic;

  .quote-icon {
    color: $color-success;
    opacity: 0.5;
    margin-top: 3px;
    flex-shrink: 0;
  }
}

.app-list {
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

  .check-icon {
    color: $color-success;
    margin-top: 3px;
    flex-shrink: 0;
    font-size: 12px;
  }
}

.code-block {
  background: $color-bg-deep;
  border-radius: $radius-sm;
  padding: $spacing-sm $spacing-md;
  overflow-x: auto;
  margin: 0;

  code {
    color: $color-success;
    font-size: 12px;
    line-height: 1.6;
    font-family: 'Fira Code', 'Consolas', monospace;
  }
}

.summary-block {
  display: flex;
  align-items: flex-start;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  background: rgba(52, 211, 153, 0.06);
  border-radius: $radius-sm;
  color: $color-text-secondary;
  font-size: $text-sm;

  .block-icon {
    color: $color-success;
    margin-top: 2px;
    flex-shrink: 0;
  }
}

.fallback-content {
  color: $color-text-primary;
  font-size: $text-sm;
  line-height: $leading-relaxed;
}
</style>
