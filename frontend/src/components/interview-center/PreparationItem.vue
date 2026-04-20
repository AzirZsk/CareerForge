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
const showDescription = ref(false)

function toggleDescription() {
  showDescription.value = !showDescription.value
}

const descriptionLines = computed(() => {
  if (!props.preparation.description) return []
  return props.preparation.description.split('\n').filter(line => line.trim().length > 0)
})

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
    link: 'fa-solid fa-link',
    note: 'fa-solid fa-pen',
    code: 'fa-solid fa-laptop',
    video: 'fa-solid fa-video'
  }
  return icons[type] || 'fa-solid fa-paperclip'
}
</script>

<template>
  <div class="preparation-item" :class="{ completed: preparation.completed }">
    <div class="priority-border" :class="priorityClass" />
    <div class="item-main">
      <input
        type="checkbox"
        :checked="preparation.completed"
        class="item-checkbox"
        @click.stop
        @change="handleToggle"
      />
      <div class="item-content">
        <div class="item-title">{{ preparation.title }}</div>
        <ul v-if="contentItems" class="item-list">
          <li v-for="(item, index) in contentItems" :key="index">{{ item }}</li>
        </ul>
        <div v-else-if="preparation.content" class="item-description">{{ preparation.content }}</div>
        <!-- 详细说明（可折叠） -->
        <div v-if="preparation.description" class="item-detail-section">
          <button class="detail-toggle" @click.stop="toggleDescription">
            <font-awesome-icon icon="fa-solid fa-lightbulb" />
            <span>{{ showDescription ? '收起说明' : '查看说明' }}</span>
            <font-awesome-icon
              icon="fa-solid fa-chevron-down"
              :class="{ rotated: showDescription }"
              class="toggle-icon"
            />
          </button>
          <Transition name="expand">
            <div v-if="showDescription" class="detail-content">
              <p v-for="(line, idx) in descriptionLines" :key="idx">{{ line }}</p>
            </div>
          </Transition>
        </div>
      </div>
      <div class="item-actions">
        <span class="priority-tag" :class="priorityClass">{{ priorityLabel }}</span>
        <button
          v-if="hasResources"
          class="action-btn resources-btn"
          :class="{ active: showResources }"
          @click="toggleResources"
        >
          <font-awesome-icon icon="fa-solid fa-paperclip" /> {{ preparation.resources?.length || 0 }}
        </button>
        <button class="action-btn delete-btn" title="删除" @click="handleDelete">
          <font-awesome-icon icon="fa-solid fa-trash" />
        </button>
      </div>
    </div>
    <div v-if="showResources && hasResources" class="resources-panel">
      <div class="resources-header">
        <font-awesome-icon icon="fa-solid fa-paperclip" /> 参考资料
      </div>
      <div class="resources-list">
        <div
          v-for="(resource, index) in preparation.resources"
          :key="index"
          class="resource-item"
          :class="{ clickable: resource.url }"
          @click="openResource(resource)"
        >
          <font-awesome-icon :icon="getResourceIcon(resource.type)" class="resource-icon" />
          <span class="resource-title">{{ resource.title }}</span>
          <font-awesome-icon v-if="resource.url" icon="fa-solid fa-arrow-up-right-from-square" class="external-link-icon" />
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

.item-checkbox {
  width: 18px;
  height: 18px;
  accent-color: $color-accent;
  flex-shrink: 0;
  cursor: pointer;
  margin-top: 2px;
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

.item-detail-section {
  margin-top: $spacing-xs;
}

.detail-toggle {
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $text-xs;
  color: $color-accent;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 0;
  opacity: 0.8;
  transition: opacity $transition-fast;

  &:hover {
    opacity: 1;
  }

  .toggle-icon {
    font-size: 10px;
    transition: transform $transition-fast;

    &.rotated {
      transform: rotate(180deg);
    }
  }
}

.detail-content {
  margin-top: $spacing-xs;
  padding: $spacing-sm;
  background: rgba($color-accent, 0.05);
  border-radius: $radius-sm;
  border-left: 2px solid rgba($color-accent, 0.3);

  p {
    font-size: $text-xs;
    color: $color-text-secondary;
    line-height: 1.6;
    margin-bottom: 4px;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

.expand-enter-active {
  transition: all 0.2s ease-out;
}

.expand-leave-active {
  transition: all 0.15s ease-in;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
}
</style>
