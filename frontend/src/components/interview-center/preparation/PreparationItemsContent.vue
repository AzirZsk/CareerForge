<!--=====================================================
  LandIt 准备事项阶段内容组件
  @author Azir
=====================================================-->

<template>
  <div class="preparation-items-content">
    <div class="items-header">
      <span class="items-count">已选 {{ selectedCount }} / {{ items.length }} 项</span>
      <div class="items-actions">
        <button class="action-btn" @click="selectAll">全选</button>
        <button class="action-btn" @click="deselectAll">全不选</button>
      </div>
    </div>
    <div class="items-list">
      <div
        v-for="(item, index) in items"
        :key="item.id || index"
        class="preparation-item"
        :class="{ selected: selectedItems[index] }"
        @click="toggleItem(index)"
      >
        <div class="item-header">
          <input
            type="checkbox"
            :checked="selectedItems[index]"
            class="item-checkbox"
            @click.stop
            @change="toggleItem(index)"
          />
          <span class="item-index">{{ index + 1 }}</span>
          <span class="item-title">{{ item.title }}</span>
          <span v-if="item.priority" class="item-priority" :class="getPriorityClass(item.priority)">
            {{ priorityLabels[item.priority] || item.priority }}
          </span>
        </div>
        <!-- 列表渲染（优先） -->
        <ul v-if="getParsedContentItems(item).length > 0" class="item-steps">
          <li v-for="(step, idx) in getParsedContentItems(item)" :key="idx">{{ step }}</li>
        </ul>
        <!-- 纯文本降级（旧数据兼容） -->
        <p v-else-if="item.content && !isJsonArray(item.content)" class="item-description">{{ item.content }}</p>
        <div class="item-meta">
          <span v-if="item.itemType" class="category-tag">{{ itemTypeLabels[item.itemType] || item.itemType }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PreparationItem } from '@/types/interview-center'

const props = defineProps<{
  items: PreparationItem[]
  selectedItems: Record<number, boolean>
}>()

const emit = defineEmits<{
  'update:selectedItems': [value: Record<number, boolean>]
}>()

// 计算选中数量
const selectedCount = computed(() => {
  return Object.values(props.selectedItems).filter(Boolean).length
})

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

// 全选
function selectAll() {
  const newSelected: Record<number, boolean> = {}
  props.items.forEach((_, index) => {
    newSelected[index] = true
  })
  emit('update:selectedItems', newSelected)
}

// 全不选
function deselectAll() {
  const newSelected: Record<number, boolean> = {}
  props.items.forEach((_, index) => {
    newSelected[index] = false
  })
  emit('update:selectedItems', newSelected)
}

// 切换单项勾选
function toggleItem(index: number) {
  const newSelected = { ...props.selectedItems }
  newSelected[index] = !newSelected[index]
  emit('update:selectedItems', newSelected)
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

// 解析 content 或 contentItems 为数组
function getParsedContentItems(item: PreparationItem): string[] {
  // 优先使用 contentItems
  if (item.contentItems && item.contentItems.length > 0) {
    return item.contentItems
  }
  // 尝试解析 content 为 JSON 数组
  if (item.content) {
    try {
      const parsed = JSON.parse(item.content)
      if (Array.isArray(parsed)) {
        return parsed.filter((s): s is string => typeof s === 'string')
      }
    } catch {
      // 不是 JSON，返回空数组（降级为纯文本显示）
      return []
    }
  }
  return []
}

// 判断 content 是否为 JSON 数组
function isJsonArray(content: string): boolean {
  try {
    const parsed = JSON.parse(content)
    return Array.isArray(parsed)
  } catch {
    return false
  }
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

.items-actions {
  display: flex;
  gap: $spacing-xs;
}

.action-btn {
  font-size: $text-xs;
  padding: 4px $spacing-sm;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-sm;
  color: $color-text-secondary;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: $color-text-primary;
  }
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
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.04);
  }

  &.selected {
    border-color: rgba($color-accent, 0.3);
    background: rgba($color-accent, 0.05);
  }
}

.item-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.item-checkbox {
  width: 18px;
  height: 18px;
  accent-color: $color-accent;
  flex-shrink: 0;
  cursor: pointer;
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
  margin: $spacing-xs 0 0 36px;
}

.item-steps {
  list-style: disc;
  margin: $spacing-xs 0 0 48px;
  padding-left: 0;

  li {
    font-size: $text-xs;
    color: $color-text-secondary;
    line-height: 1.6;
    margin-bottom: 4px;

    &::marker {
      color: $color-accent;
    }
  }
}

.item-meta {
  margin-left: 36px;
}

.category-tag {
  font-size: 10px;
  padding: 2px 6px;
  background: rgba(255, 255, 255, 0.05);
  color: $color-text-tertiary;
  border-radius: $radius-sm;
}
</style>
