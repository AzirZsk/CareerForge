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

const hasResources = computed(() => {
  return props.preparation.resources && props.preparation.resources.length > 0
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
    <div class="item-main">
      <button class="checkbox" :class="{ checked: preparation.completed }" @click="handleToggle">
        <span v-if="preparation.completed" class="check-icon">✓</span>
      </button>
      <span class="priority-indicator" :style="{ color: priorityConfig.color }">
        {{ priorityConfig.icon }}
      </span>
      <div class="item-content">
        <div class="item-title">{{ preparation.title }}</div>
        <div v-if="preparation.content" class="item-description">{{ preparation.content }}</div>
      </div>
      <div class="item-actions">
        <button
          v-if="hasResources"
          class="action-btn resources-btn"
          :class="{ active: showResources }"
          @click="toggleResources"
        >
          📎 {{ preparation.resources?.length || 0 }}
        </button>
        <button class="action-btn delete-btn" @click="handleDelete">🗑️</button>
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
  background: $color-bg-tertiary;
  border-radius: $radius-md;
  padding: $spacing-md;
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

.item-actions {
  flex-shrink: 0;
  display: flex;
  gap: $spacing-xs;
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

.delete-btn:hover {
  color: $color-error;
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
