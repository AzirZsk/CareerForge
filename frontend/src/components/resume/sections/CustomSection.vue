<!--=====================================================
  自定义区块展示组件
  @author Azir
=====================================================-->

<template>
  <div class="content-block custom-block">
    <!-- 自定义区块 item（从侧边栏选中） -->
    <div class="custom-content-items" v-if="isSingleItem">
      <div
        class="content-item"
        v-for="(contentItem, idx) in contentItems"
        :key="idx"
      >
        <div class="content-item-header">
          <span class="content-item-name">{{ contentItem.name as string }}</span>
          <span class="content-item-period" v-if="contentItem.period">{{ contentItem.period as string }}</span>
        </div>
        <p class="exp-position" v-if="contentItem.role">{{ contentItem.role as string }}</p>
        <p class="exp-desc" v-if="contentItem.description">{{ contentItem.description as string }}</p>
        <div v-if="(contentItem.highlights as string[])?.length" class="content-item-highlights">
          <span v-for="h in contentItem.highlights" :key="h" class="highlight-tag">{{ h as string }}</span>
        </div>
      </div>
      <!-- 空状态提示 -->
      <div v-if="contentItems.length === 0" class="empty-content">
        <p class="empty-text">暂无内容</p>
      </div>
    </div>
    <!-- 自定义区块列表 -->
    <div v-else class="custom-section-list">
      <div class="custom-section-item" v-for="item in parsedItems" :key="item.id">
        <div class="custom-section-header">
          <h4 class="custom-section-title">{{ item.parsedContent.title }}</h4>
          <div class="exp-actions">
            <button class="item-btn edit" @click="$emit('edit-item', item.id)" title="编辑">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
              </svg>
            </button>
            <button class="item-btn delete" @click="$emit('delete-item', item.id)" title="删除">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
              </svg>
            </button>
          </div>
        </div>
        <div class="custom-content-items">
          <div
            class="content-item"
            v-for="(contentItem, idx) in item.parsedContent.items"
            :key="idx"
          >
            <div class="content-item-header">
              <span class="content-item-name">{{ contentItem.name as string }}</span>
              <span class="content-item-period" v-if="contentItem.period">{{ contentItem.period as string }}</span>
            </div>
            <p class="exp-position" v-if="contentItem.role">{{ contentItem.role as string }}</p>
            <p class="exp-desc" v-if="contentItem.description">{{ contentItem.description as string }}</p>
            <div v-if="(contentItem.highlights as string[])?.length" class="content-item-highlights">
              <span v-for="h in contentItem.highlights" :key="h" class="highlight-tag">{{ h as string }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ResumeSectionItem, ResumeSectionContent } from '@/types'
import { useSectionHelper } from '@/composables/useSectionHelper'

const props = defineProps<{
  items?: ResumeSectionItem[]
  // content 可能是解析后的对象，也可能是 JSON 字符串（来自后端）
  content?: ResumeSectionContent | string
  isSingleItem?: boolean
}>()

const { parseContent } = useSectionHelper()

interface CustomContent {
  title?: string
  items?: Array<Record<string, unknown>>
}

defineEmits<{
  'edit-item': [itemId: string]
  'delete-item': [itemId: string]
}>()

// 单条 item 的内容项
const contentItems = computed(() => {
  if (props.isSingleItem && props.content) {
    // content 可能是 JSON 字符串，需要先解析
    const parsedContent = parseContent<CustomContent>(props.content)
    return (parsedContent.items as Array<Record<string, unknown>>) ?? []
  }
  return []
})

// 解析后的 items
const parsedItems = computed(() => (props.items ?? []).map(item => ({
  ...item,
  parsedContent: parseContent<CustomContent>(item.content)
})))
</script>

<style lang="scss" scoped>
.custom-block {
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  padding: $spacing-lg;
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
  border-radius: $radius-md;
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
</style>
