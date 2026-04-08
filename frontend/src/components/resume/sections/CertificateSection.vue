<!--=====================================================
  证书模块展示组件
  @author Azir
=====================================================-->

<template>
  <div class="content-block certificate-block">
    <div
      v-for="(item, index) in items"
      :key="item.id"
      class="experience-item"
    >
      <div class="exp-header">
        <h4 class="exp-title">
          {{ item.content.name }}
          <a
            v-if="item.content.url"
            :href="item.content.url"
            target="_blank"
            class="exp-link"
            title="查看证书"
          >
            <font-awesome-icon icon="fa-solid fa-arrow-up-right-from-square" />
          </a>
        </h4>
        <div class="exp-actions">
          <span
            v-if="item.content.date"
            class="exp-period"
          >{{ item.content.date }}</span>
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
      <div class="exp-meta">
        <span
          v-if="item.content.issuer"
          class="exp-issuer"
        >{{ item.content.issuer }}</span>
        <span
          v-if="item.content.credentialId"
          class="exp-credential"
        >
          编号: {{ item.content.credentialId }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ResumeSectionItem, CertificateContent } from '@/types'

defineProps<{
  items: ResumeSectionItem<CertificateContent>[]
}>()

defineEmits<{
  'edit-item': [index: number]
  'delete-item': [index: number]
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
  display: flex;
  align-items: center;
  gap: $spacing-xs;
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
  margin-top: $spacing-xs;
}

.exp-issuer {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.exp-credential {
  font-size: $text-xs;
  color: $color-text-tertiary;
  background: rgba(255, 255, 255, 0.05);
  padding: 2px $spacing-sm;
  border-radius: $radius-sm;
}

.exp-link {
  display: inline-flex;
  align-items: center;
  color: $color-text-tertiary;
  margin-left: $spacing-xs;
  transition: color $transition-fast;
  &:hover {
    color: $color-accent;
  }
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
