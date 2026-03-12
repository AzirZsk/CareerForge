<!--=====================================================
  工作经历模块展示组件
  @author Azir
=====================================================-->

<template>
  <div class="content-block work-block">
    <div class="experience-item" v-for="(item, index) in items" :key="item.id">
      <div class="exp-header">
        <h4 class="exp-title">{{ item.content.company }}</h4>
        <div class="exp-actions">
          <span class="exp-period" v-if="item.content.period">{{ item.content.period }}</span>
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
      <div class="exp-meta">
        <span class="exp-position" v-if="item.content.position">{{ item.content.position }}</span>
        <span class="exp-location" v-if="item.content.location">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
            <circle cx="12" cy="10" r="3"></circle>
          </svg>
          {{ item.content.location }}
        </span>
        <span class="exp-industry" v-if="item.content.industry">{{ item.content.industry }}</span>
      </div>
      <p class="exp-desc" v-if="item.content.description">{{ item.content.description }}</p>
      <div v-if="item.content.achievements?.length" class="exp-achievements">
        <span v-for="a in item.content.achievements" :key="a" class="achievement-tag">{{ a }}</span>
      </div>
      <div v-if="item.content.technologies?.length" class="exp-technologies">
        <span v-for="tech in item.content.technologies" :key="tech" class="tech-tag">{{ tech }}</span>
      </div>
      <div v-if="item.content.products?.length" class="exp-products">
        <span class="products-label">代表产品:</span>
        <span v-for="p in item.content.products" :key="p" class="product-tag">{{ p }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ResumeSectionItem, WorkExperience } from '@/types'

defineProps<{
  items: ResumeSectionItem<WorkExperience>[]
}>()

defineEmits<{
  'edit-item': [index: number]
  'delete-item': [index: number]
}>()
</script>

<style lang="scss" scoped>
.work-block {
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  padding: $spacing-lg;
}

.experience-item {
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

.exp-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xs;
}

.exp-title {
  font-size: $text-base;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.exp-actions {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.exp-period {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.exp-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-sm;
}

.exp-position {
  font-size: $text-sm;
  color: $color-accent;
}

.exp-location {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.exp-industry {
  font-size: $text-xs;
  color: $color-text-tertiary;
  padding: 2px $spacing-sm;
  background: rgba(255, 255, 255, 0.05);
  border-radius: $radius-sm;
}

.exp-desc {
  font-size: $text-sm;
  color: $color-text-secondary;
  line-height: $leading-relaxed;
  margin-bottom: $spacing-sm;
  white-space: pre-wrap;
  word-break: break-word;
}

.exp-achievements {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
  margin-bottom: $spacing-sm;
}

.achievement-tag {
  width: fit-content;
  padding: $spacing-xs $spacing-sm;
  background: $color-success-bg;
  color: $color-success;
  font-size: $text-xs;
  border-radius: $radius-sm;
}

.exp-technologies {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
  margin-bottom: $spacing-sm;
}

.tech-tag {
  padding: $spacing-xs $spacing-sm;
  background: rgba(96, 165, 250, 0.1);
  color: $color-info;
  font-size: $text-xs;
  border-radius: $radius-sm;
}

.exp-products {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: $spacing-xs;
}

.products-label {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.product-tag {
  padding: $spacing-xs $spacing-sm;
  background: rgba(212, 168, 83, 0.15);
  color: $color-accent;
  font-size: $text-xs;
  border-radius: $radius-sm;
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

.experience-item:hover .item-btn {
  opacity: 1;
}
</style>
