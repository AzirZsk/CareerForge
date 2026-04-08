<!--=====================================================
  自定义区块展示组件
  @author Azir
=====================================================-->

<template>
  <div class="content-block custom-block">
    <!-- 单条模式标题 -->
    <h4
      v-if="isSingleItem && title"
      class="custom-section-title"
    >
      {{ title }}
    </h4>
    <!-- 自定义区块 item（从侧边栏选中） -->
    <div
      v-if="isSingleItem"
      class="custom-content-items"
    >
      <div
        v-for="(contentItem, idx) in contentItems"
        :key="idx"
        class="content-item"
      >
        <div class="content-item-header">
          <span class="content-item-name">{{ contentItem.name }}</span>
          <span
            v-if="contentItem.period"
            class="content-item-period"
          >{{ contentItem.period }}</span>
        </div>
        <p
          v-if="contentItem.role"
          class="exp-position"
        >
          {{ contentItem.role }}
        </p>
        <p
          v-if="contentItem.description"
          class="exp-desc"
        >
          {{ contentItem.description }}
        </p>
        <div
          v-if="contentItem.highlights?.length"
          class="content-item-highlights"
        >
          <span
            v-for="h in contentItem.highlights"
            :key="h"
            class="highlight-tag"
          >{{ h }}</span>
        </div>
      </div>
      <!-- 空状态提示 -->
      <div
        v-if="contentItems.length === 0"
        class="empty-content"
      >
        <p class="empty-text">
          暂无内容
        </p>
      </div>
    </div>
    <!-- 自定义区块列表 -->
    <div
      v-else
      class="custom-section-list"
    >
      <div
        v-for="(item, index) in flatContentItems"
        :key="index"
        class="content-item"
      >
        <div class="content-item-header">
          <span class="content-item-name">{{ item.name }}</span>
          <span
            v-if="item.period"
            class="content-item-period"
          >{{ item.period }}</span>
          <div class="exp-actions">
            <button
              class="item-btn edit"
              title="编辑"
              @click="$emit('edit-item', index)"
            >
              <font-awesome-icon icon="fa-solid fa-pen-to-square" />
            </button>
            <button
              class="item-btn delete"
              title="删除"
              @click="$emit('delete-item', index)"
            >
              <font-awesome-icon icon="fa-solid fa-trash" />
            </button>
          </div>
        </div>
        <p
          v-if="item.role"
          class="exp-position"
        >
          {{ item.role }}
        </p>
        <p
          v-if="item.description"
          class="exp-desc"
        >
          {{ item.description }}
        </p>
        <div
          v-if="item.highlights?.length"
          class="content-item-highlights"
        >
          <span
            v-for="h in item.highlights"
            :key="h"
            class="highlight-tag"
          >{{ h }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ResumeSectionItem, ContentItem } from '@/types'
import { useSectionHelper } from '@/composables/useSectionHelper'

const props = withDefaults(defineProps<{
  items?: ResumeSectionItem<ContentItem>[]
  // content 可能是解析后的对象，也可能是 JSON 字符串（来自后端）
  content?: string
  title?: string  // 区块标题
  isSingleItem?: boolean
}>(), {
  items: () => [],
  isSingleItem: false
})

const { parseContent } = useSectionHelper()

defineEmits<{
  'edit-item': [index: number]
  'delete-item': [index: number]
}>()

// 单条 item 的内容项（content 直接是 items 数组）
const contentItems = computed<ContentItem[]>(() => {
  if (props.isSingleItem && props.content) {
    // content 直接是 ContentItem[] 数组
    return parseContent<ContentItem[]>(props.content)
  }
  return []
})

// 列表模式：直接从 items 获取 ContentItem
// 新数据结构：每个 item.content 直接就是 ContentItem
const flatContentItems = computed<ContentItem[]>(() => {
  if (props.isSingleItem) return []
  return props.items.map(item => item.content as ContentItem)
})
</script>

<style lang="scss" scoped>
.custom-block {
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  padding: $spacing-lg;
}

.custom-section-title {
  font-size: $text-base;
  font-weight: $weight-medium;
  color: $color-text-primary;
  margin-bottom: $spacing-md;
}

.custom-section-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.custom-section-item {
  padding: $spacing-lg 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
  &:first-child {
    padding-top: 0;
  }
  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
}

.custom-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-md;
}

.custom-section-title {
  font-size: $text-base;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.custom-content-items {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.content-item {
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-sm;
}

.content-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xs;
}

.content-item-name {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.content-item-period {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.exp-position {
  font-size: $text-sm;
  color: $color-accent;
  margin-bottom: $spacing-sm;
}

.exp-desc {
  font-size: $text-sm;
  color: $color-text-secondary;
  line-height: $leading-relaxed;
  margin-bottom: $spacing-sm;
  white-space: pre-wrap;
  word-break: break-word;
}

.content-item-highlights {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
}

.highlight-tag {
  padding: $spacing-xs $spacing-sm;
  background: rgba(212, 168, 83, 0.15);
  color: $color-accent;
  font-size: $text-xs;
  border-radius: $radius-sm;
}

.exp-actions {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.item-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius-sm;
  transition: all $transition-fast;
  opacity: 0.5;
  &.edit {
    color: $color-text-secondary;
    &:hover {
      opacity: 1;
      background: rgba(255, 255, 255, 0.1);
      color: $color-accent;
    }
  }
  &.delete {
    color: $color-text-secondary;
    &:hover {
      opacity: 1;
      background: rgba(248, 113, 113, 0.1);
      color: $color-error;
    }
  }
}

.custom-section-item:hover .item-btn {
  opacity: 1;
}

// 空状态
.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $spacing-2xl;
  text-align: center;
}

.empty-text {
  font-size: $text-sm;
  color: $color-text-tertiary;
}
</style>
