<script setup lang="ts">
import { ref, computed } from 'vue'
import type { PreparationVO, PreparationItemType } from '@/types/interview-center'
import { ITEM_TYPE_CONFIG } from '@/types/interview-center'
import PreparationItem from './PreparationItem.vue'

const props = defineProps<{
  groupType: PreparationItemType | string
  preparations: PreparationVO[]
}>()

const emit = defineEmits<{
  toggle: [id: string]
  delete: [id: string]
}>()

const isExpanded = ref(true)

const groupConfig = computed(() => {
  return ITEM_TYPE_CONFIG[props.groupType] || { label: '其他准备', icon: 'fa-solid fa-thumbtack' }
})

const completedCount = computed(() => {
  return props.preparations.filter((p) => p.completed).length
})

const totalCount = computed(() => {
  return props.preparations.length
})

const progressText = computed(() => {
  return `${completedCount.value}/${totalCount.value}`
})

function toggleGroup() {
  isExpanded.value = !isExpanded.value
}

function handleToggle(id: string) {
  emit('toggle', id)
}

function handleDelete(id: string) {
  emit('delete', id)
}
</script>

<template>
  <div class="preparation-group">
    <div class="group-header" @click="toggleGroup">
      <div class="group-info">
        <font-awesome-icon :icon="groupConfig.icon" class="group-icon" />
        <span class="group-title">{{ groupConfig.label }}</span>
        <span class="group-progress">{{ progressText }}</span>
      </div>
      <button class="toggle-btn" :class="{ expanded: isExpanded }">
        <font-awesome-icon icon="fa-solid fa-chevron-down" class="toggle-icon" />
      </button>
    </div>
    <Transition name="collapse">
      <div v-show="isExpanded" class="group-items">
        <PreparationItem
          v-for="item in preparations"
          :key="item.id"
          :preparation="item"
          @toggle="handleToggle"
          @delete="handleDelete"
        />
        <div v-if="preparations.length === 0" class="empty-hint">暂无准备事项</div>
      </div>
    </Transition>
  </div>
</template>

<script lang="ts">
export default {
  name: 'PreparationGroup'
}
</script>

<style scoped lang="scss">
.preparation-group {
  background: $color-bg-secondary;
  border-radius: $radius-md;
  overflow: hidden;
  margin-bottom: $spacing-md;
}

.group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-md $spacing-lg;
  cursor: pointer;
  transition: background 0.2s ease;

  &:hover {
    background: $color-bg-tertiary;
  }
}

.group-info {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.group-icon {
  font-size: 1.25rem;
}

.group-title {
  color: $color-text-primary;
  font-size: 1rem;
  font-weight: 500;
}

.group-progress {
  color: $color-text-tertiary;
  font-size: 0.875rem;
  margin-left: $spacing-xs;
}

.toggle-btn {
  background: transparent;
  border: none;
  padding: $spacing-xs;
  cursor: pointer;
  color: $color-text-tertiary;
  transition: transform 0.2s ease;

  &.expanded {
    transform: rotate(0deg);
  }

  &:not(.expanded) {
    transform: rotate(-90deg);
  }
}

.toggle-icon {
  font-size: 0.75rem;
}

.group-items {
  padding: 0 $spacing-md $spacing-md;
}

.empty-hint {
  text-align: center;
  color: $color-text-tertiary;
  padding: $spacing-xl;
  font-size: 0.875rem;
}

// 折叠动画
.collapse-enter-active,
.collapse-leave-active {
  transition: all 0.3s ease;
  max-height: 2000px;
  overflow: hidden;
}

.collapse-enter-from,
.collapse-leave-to {
  max-height: 0;
  opacity: 0;
}
</style>
