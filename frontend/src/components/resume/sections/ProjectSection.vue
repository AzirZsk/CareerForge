<!--=====================================================
  项目经历模块展示组件
  @author Azir
=====================================================-->

<template>
  <div class="content-block project-block">
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
            title="访问项目"
          >
            <font-awesome-icon icon="fa-solid fa-arrow-up-right-from-square" />
          </a>
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
      <p
        v-if="item.content.role"
        class="exp-position"
      >
        {{ item.content.role }}
      </p>
      <p
        v-if="item.content.description"
        class="exp-desc"
      >
        {{ item.content.description }}
      </p>
      <div
        v-if="item.content.technologies?.length"
        class="exp-technologies"
      >
        <span
          v-for="tech in item.content.technologies"
          :key="tech"
          class="tech-tag"
        >{{ tech }}</span>
      </div>
      <div
        v-if="item.content.achievements?.length"
        class="exp-achievements"
      >
        <span
          v-for="a in item.content.achievements"
          :key="a"
          class="achievement-tag"
        >{{ a }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ResumeSectionItem, ProjectExperience } from '@/types'

defineProps<{
  items: ResumeSectionItem<ProjectExperience>[]
}>()

defineEmits<{
  'edit-item': [index: number]
  'delete-item': [index: number]
}>()
</script>

<style lang="scss" scoped>
.project-block {
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
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.exp-link {
  display: inline-flex;
  align-items: center;
  color: $color-text-tertiary;
  transition: color $transition-fast;
  &:hover {
    color: $color-accent;
  }
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

.exp-achievements {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.achievement-tag {
  width: fit-content;
  padding: $spacing-xs $spacing-sm;
  background: $color-success-bg;
  color: $color-success;
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
