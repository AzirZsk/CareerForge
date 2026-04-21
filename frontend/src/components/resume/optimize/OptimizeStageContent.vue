<!--=====================================================
  CareerForge 优化阶段内容组件
  @author Azir
=====================================================-->

<template>
  <div class="data-content">
    <!-- 变更详情视图 -->
    <div class="changes-view">
      <!-- 变更摘要 -->
      <div class="changes-summary">
        <div class="changes-summary-left">
          <span class="changes-count">{{ changeCount }} 处变更</span>
          <span
            v-if="data.improvementScore"
            class="changes-improvement"
          >预计提升 {{ data.improvementScore }} 分</span>
        </div>
        <button
          v-if="hasBeforeData"
          class="comparison-btn"
          @click="$emit('showComparison')"
        >
          <svg
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <rect
              x="2"
              y="3"
              width="20"
              height="18"
              rx="2"
            />
            <line
              x1="12"
              y1="3"
              x2="12"
              y2="21"
            />
          </svg>
          对比 & 编辑
        </button>
      </div>

      <!-- 置信度徽章 -->
      <div
        v-if="data.confidence"
        class="confidence-badge"
        :class="data.confidence || 'medium'"
      >
        置信度: {{ data.confidence }}
      </div>

      <!-- 变更列表 -->
      <div
        v-if="data.changes?.length"
        class="changes-list"
      >
        <div
          v-for="(change, idx) in data.changes"
          :key="idx"
          class="change-item"
        >
          <div class="change-header">
            <span
              class="change-type"
              :class="change.type"
            >{{ change.typeLabel || change.type || '修改' }}</span>
            <span class="change-field">{{ change.fieldLabel || change.field }}</span>
          </div>
          <div
            v-if="hasValueToShow(change)"
            class="change-content"
          >
            <!-- 新增类型：只显示新增的值 -->
            <div
              v-if="change.type === 'added'"
              class="change-added"
            >
              <ol
                v-if="isArray(change.afterValue) && hasJsonItems(change.afterValue)"
                class="change-value-list"
              >
                <li
                  v-for="(item, vIdx) in parseArrayItems(change.afterValue)"
                  :key="vIdx"
                  class="structured-item"
                >
                  <StructuredItemRenderer :item="item" :field="change.field" />
                </li>
              </ol>
              <ol
                v-else-if="isArray(change.afterValue)"
                class="change-value-list"
              >
                <li
                  v-for="(val, vIdx) in change.afterValue"
                  :key="vIdx"
                >
                  {{ val }}
                </li>
              </ol>
              <pre
                v-else
                class="change-value-text"
              >{{ change.afterValue }}</pre>
            </div>

            <!-- 修改类型：保持前后对比 -->
            <template v-else>
              <div
                v-if="change.beforeValue !== null"
                class="change-before"
              >
                <span class="change-label">前:</span>
                <ol
                  v-if="isArray(change.beforeValue) && hasJsonItems(change.beforeValue)"
                  class="change-value-list"
                >
                  <li
                    v-for="(item, vIdx) in parseArrayItems(change.beforeValue)"
                    :key="vIdx"
                    class="structured-item"
                  >
                    <StructuredItemRenderer :item="item" :field="change.field" />
                  </li>
                </ol>
                <ol
                  v-else-if="isArray(change.beforeValue)"
                  class="change-value-list"
                >
                  <li
                    v-for="(val, vIdx) in change.beforeValue"
                    :key="vIdx"
                  >
                    {{ val }}
                  </li>
                </ol>
                <pre
                  v-else
                  class="change-value-text"
                >{{ change.beforeValue }}</pre>
              </div>
              <div
                v-if="change.afterValue !== null"
                class="change-after"
              >
                <span class="change-label">后:</span>
                <ol
                  v-if="isArray(change.afterValue) && hasJsonItems(change.afterValue)"
                  class="change-value-list"
                >
                  <li
                    v-for="(item, vIdx) in parseArrayItems(change.afterValue)"
                    :key="vIdx"
                    class="structured-item"
                  >
                    <StructuredItemRenderer :item="item" :field="change.field" />
                  </li>
                </ol>
                <ol
                  v-else-if="isArray(change.afterValue)"
                  class="change-value-list"
                >
                  <li
                    v-for="(val, vIdx) in change.afterValue"
                    :key="vIdx"
                  >
                    {{ val }}
                  </li>
                </ol>
                <pre
                  v-else
                  class="change-value-text"
                >{{ change.afterValue }}</pre>
              </div>
            </template>
          </div>
          <div
            v-if="change.reason"
            class="change-reason"
          >
            {{ change.reason }}
          </div>
        </div>
      </div>

      <!-- 优化提示 -->
      <div
        v-if="data.tips?.length"
        class="tips-section"
      >
        <div class="tips-title">
          优化提示
        </div>
        <div
          v-for="(tip, idx) in data.tips"
          :key="idx"
          class="tip-item"
        >
          • {{ tip }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { OptimizeSectionData } from '@/types/resume-optimize'
import { isArray, hasValueToShow, isJsonObjectStr, tryParseJson } from '@/utils/stageHelpers'
import StructuredItemRenderer from './StructuredItemRenderer.vue'

interface ParsedItem {
  raw: string
  parsed: Record<string, any> | null
}

const props = defineProps<{
  data: OptimizeSectionData
}>()

defineEmits<{
  showComparison: []
}>()

// 解析数组中的 JSON 对象
function parseArrayItems(values: string[]): ParsedItem[] {
  return values.map(v => ({
    raw: v,
    parsed: isJsonObjectStr(v) ? tryParseJson(v) : null
  }))
}

// 判断数组是否包含 JSON 对象元素
function hasJsonItems(values: string[] | null): boolean {
  if (!values || !isArray(values)) return false
  return values.length > 0 && isJsonObjectStr(values[0])
}

// 变更数量
const changeCount = computed(() => {
  return props.data.changeCount || props.data.changes?.length || 0
})

// 是否有前置数据（用于对比）
const hasBeforeData = computed(() => {
  return props.data.beforeSection?.length || props.data.beforeResume
})
</script>

<style lang="scss" scoped>
.data-content {
  font-size: $text-sm;
}

// 变更摘要
.changes-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-sm;
}

.changes-summary-left {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.changes-count {
  font-size: $text-sm;
  color: $color-text-primary;
  font-weight: $weight-medium;
}

.changes-improvement {
  font-size: $text-xs;
  color: $color-success;
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

.changes-view {
  margin-top: $spacing-md;
}

// 置信度徽章
.confidence-badge {
  display: inline-block;
  padding: 2px 8px;
  margin-bottom: $spacing-sm;
  border-radius: $radius-sm;
  font-size: $text-xs;

  &.high {
    background: rgba(52, 211, 153, 0.15);
    color: $color-success;
  }
  &.medium {
    background: rgba(251, 191, 36, 0.15);
    color: $color-warning;
  }
  &.low {
    background: rgba(248, 113, 113, 0.15);
    color: $color-error;
  }
}

// 变更项
.change-item {
  padding: $spacing-sm;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-sm;
  margin-bottom: $spacing-xs;
}

.change-header {
  display: flex;
  gap: $spacing-sm;
  margin-bottom: 4px;
}

.change-type {
  padding: 2px 6px;
  border-radius: $radius-sm;
  font-size: 10px;
  text-transform: uppercase;

  &.modified {
    background: rgba(96, 165, 250, 0.15);
    color: $color-info;
  }
  &.added {
    background: rgba(52, 211, 153, 0.15);
    color: $color-success;
  }
  &.removed {
    background: rgba(248, 113, 113, 0.15);
    color: $color-error;
  }
}

.change-field {
  font-size: $text-xs;
  color: $color-accent;
}

.change-content {
  margin-bottom: 4px;
}

.change-label {
  font-size: $text-xs;
  font-weight: $weight-medium;
  margin-right: $spacing-xs;
  opacity: 0.7;
}

.change-before,
.change-after {
  font-size: $text-xs;
  padding: $spacing-xs;
  border-radius: $radius-sm;
  margin-bottom: 4px;
}

.change-before {
  color: $color-error;
  background: rgba(248, 113, 113, 0.05);
}

.change-after {
  color: $color-success;
  background: rgba(52, 211, 153, 0.05);
}

// 新增类型的单值展示样式
.change-added {
  font-size: $text-xs;
  padding: $spacing-xs;
  border-radius: $radius-sm;
  margin-bottom: 4px;
  color: $color-success;
  background: rgba(52, 211, 153, 0.05);
}

// 值列表样式（使用数字序号）
.change-value-list {
  padding: 0 0 0 $spacing-lg;
  list-style: decimal;

  li {
    padding: 2px 0;
    font-size: $text-xs;
    line-height: 1.5;

    &::marker {
      color: inherit;
      opacity: 0.6;
    }
  }

  // 结构化渲染项增加间距
  li.structured-item {
    padding: 4px 0;
  }
}

// 值文本样式（保留换行）
.change-value-text {
  margin: $spacing-xs 0 0 0;
  padding: $spacing-xs;
  font-size: $text-xs;
  font-family: inherit;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
  background: rgba(0, 0, 0, 0.1);
  border-radius: $radius-sm;
}

.change-reason {
  font-size: $text-xs;
  color: $color-text-tertiary;
  font-style: italic;
}

// 优化提示
.tips-section {
  margin-top: $spacing-sm;
  padding-top: $spacing-sm;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.tips-title {
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-bottom: $spacing-xs;
}

.tip-item {
  font-size: $text-xs;
  color: $color-text-secondary;
  padding: $spacing-xs;
  line-height: 1.4;
}
</style>
