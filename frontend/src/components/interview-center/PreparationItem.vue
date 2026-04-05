<script setup lang="ts">
import { ref, computed } from 'vue'
import type { PreparationVO, PreparationResource } from '@/types/interview-center'
import { PRIORITY_CONFIG } from '@/types/interview-center'

const props = defineProps<{
  preparation: PreparationVO
}>()

const emit = defineEmits<{
  toggle: [id: string]
  delete: [id: string]
}>()

const showResources = ref(false)

const priorityConfig = computed(() => {
  return PRIORITY_CONFIG[props.preparation.priority || 'recommended']
})

const priorityClass = computed(() => {
  const classMap: Record<string, string> = {
    required: 'high',
    recommended: 'medium',
    optional: 'low'
  }
  return classMap[props.preparation.priority || 'recommended'] || 'medium'
})

const priorityLabel = computed(() => {
  return priorityConfig.value.label
})

const hasResources = computed(() => {
  return props.preparation.resources && props.preparation.resources.length > 0
})

// 解析 content 为数组
const contentItems = computed(() => {
  const content = props.preparation.content
  if (!content) return null

  try {
    const parsed = JSON.parse(content)
    if (Array.isArray(parsed) && parsed.length > 0) {
      return parsed
    }
    return null
  } catch {
    return null
  }
})

function handleToggle() {
  emit('toggle', props.preparation.id)
}

function handleDelete() {
  emit('delete', props.preparation.id)
}

function toggleResources() {
  showResources.value = !showResources.value
}

function openResource(resource: PreparationResource) {
  if (resource.url) {
    window.open(resource.url, '_blank')
  }
}

function getResourceIcon(type: string): string {
  const icons: Record<string, string> = {
    link: '🔗',
    note: '📝',
    code: '💻',
    video: '🎬'
  }
  return icons[type] || '📎'
}
</script>

<template>
  <div class="preparation-item" :class="{ completed: preparation.completed }">
    <div class="priority-border" :class="priorityClass" />
    <div class="item-main">
      <button class="checkbox" :class="{ checked: preparation.completed }" @click="handleToggle">
        <span v-if="preparation.completed" class="check-icon">✓</span>
      </button>
      <div class="item-content">
        <div class="item-title">{{ preparation.title }}</div>
        <ul v-if="contentItems" class="item-list">
          <li v-for="(item, index) in contentItems" :key="index">{{ item }}</li>
        </ul>
        <div v-else-if="preparation.content" class="item-description">{{ preparation.content }}</div>
      </div>
      <div class="item-actions">
        <span class="priority-tag" :class="priorityClass">{{ priorityLabel }}</span>
        <button
          v-if="hasResources"
          class="action-btn resources-btn"
          :class="{ active: showResources }"
          @click="toggleResources"
        >
          📎 {{ preparation.resources?.length || 0 }}
        </button>
        <button class="action-btn delete-btn" title="删除" @click="handleDelete">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="3 6 5 6 21 6"></polyline>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
          </svg>
        </button>
      </div>
    </div>
    <div v-if="showResources && hasResources" class="resources-panel">
      <div class="resources-header">📎 参考资料</div>
      <div class="resources-list">
        <div
          v-for="(resource, index) in preparation.resources"
          :key="index"
          class="resource-item"
          :class="{ clickable: resource.url }"
          @click="openResource(resource)"
        >
          <span class="resource-icon">{{ getResourceIcon(resource.type) }}</span>
          <span class="resource-title">{{ resource.title }}</span>
          <span v-if="resource.url" class="external-link-icon">↗</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
export default {
  name: 'PreparationItem'
}
</script>

<style scoped lang="scss">
.preparation-item {
  position: relative;
  background: $color-bg-tertiary;
  border-radius: $radius-md;
  padding: $spacing-md;
  padding-left: calc($spacing-md + 3px);
  margin-bottom: $spacing-sm;
  transition: all 0.2s ease;

  &.completed {
    opacity: 0.6;
    .item-title {
      text-decoration: line-through;
      color: $color-text-tertiary;
    }
  }
}

.priority-border {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  border-radius: $radius-md 0 0 $radius-md;

  &.high {
    background: rgba($color-error, 0.8);
  }

  &.medium {
    background: rgba($color-warning, 0.8);
  }

  &.low {
    background: rgba($color-success, 0.8);
  }
}

.item-main {
  display: flex;
  align-items: flex-start;
  gap: $spacing-sm;
}

.checkbox {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  border: 2px solid $color-border;
  border-radius: $radius-sm;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  margin-top: 2px;

  &:hover {
    border-color: $color-accent;
  }

  &.checked {
    background: $color-accent;
    border-color: $color-accent;
  }

  .check-icon {
    color: $color-bg-primary;
    font-size: 0.75rem;
    font-weight: bold;
  }
}

.priority-indicator {
  flex-shrink: 0;
  font-size: 1rem;
  margin-top: 1px;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  color: $color-text-primary;
  font-size: 0.9375rem;
  font-weight: 500;
  margin-bottom: 2px;
}

.item-description {
  color: $color-text-tertiary;
  font-size: 0.8125rem;
  line-height: 1.4;
  margin-top: $spacing-xs;
}

.item-list {
  margin: $spacing-xs 0 0 0;
  padding-left: $spacing-lg;
  color: $color-text-tertiary;
  font-size: 0.8125rem;
  line-height: 1.5;
  list-style-type: disc;

  li {
    margin-bottom: 4px;
    padding-left: 4px;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

.item-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: $spacing-xs;
}

.priority-tag {
  font-size: 11px;
  font-weight: $weight-medium;
  padding: 3px 10px;
  border-radius: 12px;
  border: 1px solid transparent;

  &.high {
    background: rgba($color-error, 0.12);
    color: $color-error;
    border-color: rgba($color-error, 0.25);
  }

  &.medium {
    background: rgba($color-warning, 0.12);
    color: $color-warning;
    border-color: rgba($color-warning, 0.25);
  }

  &.low {
    background: rgba($color-success, 0.12);
    color: $color-success;
    border-color: rgba($color-success, 0.25);
  }
}

.priority-tag {
  font-size: 11px;
  font-weight: $weight-medium;
  padding: 3px 10px;
  border-radius: 12px;
  border: 1px solid transparent;

  &.high {
    background: rgba($color-error, 0.12);
    color: $color-error;
    border-color: rgba($color-error, 0.25);
  }

  &.medium {
    background: rgba($color-warning, 0.12);
    color: $color-warning;
    border-color: rgba($color-warning, 0.25);
  }

  &.low {
    background: rgba($color-success, 0.12);
    color: $color-success;
    border-color: rgba($color-success, 0.25);
  }
}

.action-btn {
  background: transparent;
  border: none;
  padding: $spacing-xs $spacing-sm;
  border-radius: $radius-sm;
  cursor: pointer;
  font-size: 0.8125rem;
  color: $color-text-tertiary;
  transition: all 0.2s ease;

  &:hover {
    background: $color-bg-elevated;
    color: $color-text-primary;
  }
}

.resources-btn.active {
  background: $color-accent;
  color: $color-bg-primary;
}

.delete-btn {
  opacity: 0.5;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 6px;

  &:hover {
    opacity: 1;
    color: $color-error;
    background: rgba($color-error, 0.1);
  }
}

.resources-panel {
  margin-top: $spacing-md;
  padding-top: $spacing-md;
  border-top: 1px solid $color-border;
}

.resources-header {
  font-size: 0.75rem;
  color: $color-text-tertiary;
  margin-bottom: $spacing-sm;
}

.resources-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.resource-item {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm;
  background: $color-bg-secondary;
  border-radius: $radius-sm;
  font-size: 0.8125rem;

  &.clickable {
    cursor: pointer;

    &:hover {
      background: $color-bg-elevated;
    }
  }
}

.resource-icon {
  flex-shrink: 0;
}

.resource-title {
  flex: 1;
  color: $color-text-secondary;
}

.external-link-icon {
  color: $color-text-tertiary;
  font-size: 0.75rem;
}
</style>
