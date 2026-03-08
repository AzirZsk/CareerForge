<!--=====================================================
  证书模块展示组件
  @author Azir
=====================================================-->

<template>
  <div class="content-block certificate-block">
    <div class="experience-item" v-for="item in parsedItems" :key="item.id">
      <div class="exp-header">
        <h4 class="exp-title">{{ item.parsedContent.name }}</h4>
        <div class="exp-actions">
          <span class="exp-period" v-if="item.parsedContent.date">{{ item.parsedContent.date }}</span>
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
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ResumeSectionItem } from '@/types'
import { useSectionHelper } from '@/composables/useSectionHelper'

const props = defineProps<{
  items: ResumeSectionItem[]
}>()

const { parseContent } = useSectionHelper()

interface CertificateContent {
  name: string
  date?: string
}

const parsedItems = computed(() => props.items.map(item => ({
  ...item,
  parsedContent: parseContent<CertificateContent>(item.content)
})))

defineEmits<{
  'edit-item': [itemId: string]
  'delete-item': [itemId: string]
}>()
</script>

<style lang="scss" scoped>
.certificate-block {
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
