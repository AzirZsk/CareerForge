<!--=====================================================
  LandIt 准备事项阶段内容组件
  @author Azir
=====================================================-->

<template>
  <div class="preparation-items-content">
    <div class="items-header">
      <span class="items-count">{{ items.length }} 项准备事项</span>
    </div>
    <div class="items-list">
      <div
        v-for="(item, index) in items"
        :key="item.id || index"
        class="preparation-item"
      >
        <div class="item-header">
          <span class="item-index">{{ index + 1 }}</span>
          <span class="item-title">{{ item.title }}</span>
          <span v-if="item.priority" class="item-priority" :class="getPriorityClass(item.priority)">
            {{ priorityLabels[item.priority] || item.priority }}
          </span>
        </div>
        <p v-if="item.content" class="item-description">{{ item.content }}</p>
        <div class="item-meta">
          <span v-if="item.itemType" class="category-tag">{{ itemTypeLabels[item.itemType] || item.itemType }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PreparationItem } from '@/types/interview-center'

defineProps<{
  items: PreparationItem[]
}>()

// 优先级标签映射（后端返回 required/recommended/optional）
const priorityLabels: Record<string, string> = {
  required: '必做',
  recommended: '推荐',
  optional: '可选'
}

// 类型标签映射
const itemTypeLabels: Record<string, string> = {
  company_research: '公司调研',
  jd_keywords: 'JD关键词',
  tech_prep: '技术准备',
  case_study: '案例准备',
  behavioral: '行为面试',
  todo: '待办事项'
}

// 获取优先级样式类
function getPriorityClass(priority: string): string {
  const classMap: Record<string, string> = {
    required: 'high',
    recommended: 'medium',
    optional: 'low'
  }
  return classMap[priority] || 'low'
}
</script>

<style lang="scss" scoped>
.preparation-items-content {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.items-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.items-count {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.items-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  max-height: 300px;
  overflow-y: auto;
}

.preparation-item {
  padding: $spacing-sm;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-sm;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.item-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.item-index {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $text-xs;
  font-weight: $weight-semibold;
  color: $color-text-tertiary;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 50%;
}

.item-title {
  flex: 1;
  font-size: $text-sm;
  color: $color-text-primary;
}

.item-priority {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: $radius-sm;

  &.high {
    background: rgba($color-error, 0.15);
    color: $color-error;
  }

  &.medium {
    background: rgba($color-warning, 0.15);
    color: $color-warning;
  }

  &.low {
    background: rgba($color-info, 0.15);
    color: $color-info;
  }
}

.item-description {
  font-size: $text-xs;
  color: $color-text-tertiary;
  line-height: 1.5;
  margin: $spacing-xs 0 0 28px;
}

.item-category {
  margin: $spacing-xs 0 0 28px;
}

.category-tag {
  font-size: 10px;
  padding: 2px 6px;
  background: rgba(255, 255, 255, 0.05);
  color: $color-text-tertiary;
  border-radius: $radius-sm;
}
</style>
