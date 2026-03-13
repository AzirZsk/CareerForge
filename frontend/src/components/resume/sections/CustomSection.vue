<!--=====================================================
  自定义区块展示组件
  @author Azir
=====================================================-->

<template>
  <div class="content-block custom-block">
    <!-- 单条模式标题 -->
    <h4 v-if="isSingleItem && title" class="custom-section-title">{{ title }}</h4>
    <!-- 自定义区块 item（从侧边栏选中） -->
    <div class="custom-content-items" v-if="isSingleItem">
      <div
        class="content-item"
        v-for="(contentItem, idx) in contentItems"
        :key="idx"
      >
        <div class="content-item-header">
          <span class="content-item-name">{{ contentItem.name }}</span>
          <span class="content-item-period" v-if="contentItem.period">{{ contentItem.period }}</span>
        </div>
        <p class="exp-position" v-if="contentItem.role">{{ contentItem.role }}</p>
        <p class="exp-desc" v-if="contentItem.description">{{ contentItem.description }}</p>
        <div v-if="contentItem.highlights?.length" class="content-item-highlights">
          <span v-for="h in contentItem.highlights" :key="h" class="highlight-tag">{{ h }}</span>
        </div>
      </div>
      <!-- 空状态提示 -->
      <div v-if="contentItems.length === 0" class="empty-content">
        <p class="empty-text">暂无内容</p>
      </div>
    </div>
    <!-- 自定义区块列表 -->
    <div v-else class="custom-section-list">
      <div
        class="content-item"
        v-for="(item, index) in flatContentItems"
        :key="index"
      >
        <div class="content-item-header">
          <span class="content-item-name">{{ item.name }}</span>
          <span class="content-item-period" v-if="item.period">{{ item.period }}</span>
          <div class="exp-actions">
            <button class="item-btn edit" @click="$emit('edit-item', index)" title="编辑">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
              </svg>
            </button>
            <button class="item-btn delete" @click="$emit('delete-item', index)" title="删除">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
              </svg>
            </button>
          </div>
        </div>
        <p class="exp-position" v-if="item.role">{{ item.role }}</p>
        <p class="exp-desc" v-if="item.description">{{ item.description }}</p>
        <div v-if="item.highlights?.length" class="content-item-highlights">
          <span v-for="h in item.highlights" :key="h" class="highlight-tag">{{ h }}</span>
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
