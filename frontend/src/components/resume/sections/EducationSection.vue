<!--=====================================================
  教育经历模块展示组件
  @author Azir
=====================================================-->

<template>
  <div class="content-block education-block">
    <div
      v-for="(item, index) in items"
      :key="item.id"
      class="experience-item"
    >
      <div class="exp-header">
        <h4 class="exp-title">
          {{ item.content.school }}
        </h4>
        <div class="exp-actions">
          <span
            v-if="item.content.period"
            class="exp-period"
          >{{ item.content.period }}</span>
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
          v-if="item.content.degree"
          class="exp-degree"
        >{{ item.content.degree }}</span>
        <span
          v-if="item.content.major"
          class="exp-major"
        >{{ item.content.major }}</span>
        <span
          v-if="item.content.gpa"
          class="exp-gpa"
        >GPA: {{ item.content.gpa }}</span>
      </div>
      <div
        v-if="item.content.courses?.length"
        class="exp-courses"
      >
        <span
          v-for="course in item.content.courses"
          :key="course"
          class="course-tag"
        >{{ course }}</span>
      </div>
      <div
        v-if="item.content.honors?.length"
        class="exp-honors"
      >
        <span
          v-for="honor in item.content.honors"
          :key="honor"
          class="honor-tag"
        >{{ honor }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ResumeSectionItem, EducationContent } from '@/types'

defineProps<{
  items: ResumeSectionItem<EducationContent>[]
}>()

defineEmits<{
  'edit-item': [index: number]
  'delete-item': [index: number]
}>()
</script>

<style lang="scss" scoped>
.education-block {
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
  gap: $spacing-xs;
  margin-bottom: $spacing-sm;
}

.exp-degree {
  font-size: $text-sm;
  color: $color-accent;
}

.exp-major {
  font-size: $text-sm;
  color: $color-text-secondary;
  &::before {
    content: '·';
    margin-right: $spacing-xs;
    color: $color-text-tertiary;
  }
}

.exp-gpa {
  font-size: $text-xs;
  color: $color-success;
  background: $color-success-bg;
  padding: 2px $spacing-sm;
  border-radius: $radius-sm;
}

.exp-courses {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
  margin-bottom: $spacing-sm;
}

.course-tag {
  padding: $spacing-xs $spacing-sm;
  background: rgba(96, 165, 250, 0.1);
  color: $color-info;
  font-size: $text-xs;
  border-radius: $radius-sm;
}

.exp-honors {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.honor-tag {
  padding: $spacing-xs $spacing-sm;
  background: rgba(212, 168, 83, 0.12);
  color: $color-accent;
  font-size: $text-xs;
  border-radius: $radius-sm;
  width: fit-content;
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
