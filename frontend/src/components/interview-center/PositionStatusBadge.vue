<template>
  <div class="status-selector" ref="selectorRef">
    <span
      class="status-badge"
      :class="[status, { pulse: config.pulse, clickable: editable }]"
      @click.stop="toggleDropdown"
    >
      <span class="status-dot"></span>
      <span class="status-text">{{ config.label }}</span>
      <font-awesome-icon v-if="editable" icon="fa-solid fa-chevron-down" class="dropdown-arrow" />
    </span>
    <Transition name="dropdown">
      <div v-if="showDropdown && editable" class="status-dropdown">
        <div
          v-for="option in statusOptions"
          :key="option.value"
          class="dropdown-item"
          :class="{ active: option.value === status }"
          @click.stop="selectStatus(option.value)"
        >
          <span class="item-dot" :style="{ background: option.color }"></span>
          <span class="item-label">{{ option.label }}</span>
          <font-awesome-icon v-if="option.value === status" icon="fa-solid fa-check" class="item-check" />
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import type { PositionStatus } from '@/types/job-position'
import { POSITION_STATUS_CONFIG, getStatusConfig } from '@/constants/position-status'

const props = withDefaults(defineProps<{
  status: PositionStatus
  editable?: boolean
}>(), {
  editable: false
})

const emit = defineEmits<{
  change: [status: PositionStatus]
}>()

const showDropdown = ref(false)
const selectorRef = ref<HTMLElement | null>(null)

const config = computed(() => getStatusConfig(props.status))

const statusOptions = computed(() => {
  return (Object.keys(POSITION_STATUS_CONFIG) as PositionStatus[]).map(key => ({
    value: key,
    label: POSITION_STATUS_CONFIG[key].label,
    color: POSITION_STATUS_CONFIG[key].color
  }))
})

function toggleDropdown() {
  if (!props.editable) return
  showDropdown.value = !showDropdown.value
}

function selectStatus(status: PositionStatus) {
  showDropdown.value = false
  if (status !== props.status) {
    emit('change', status)
  }
}

function handleClickOutside(e: MouseEvent) {
  if (selectorRef.value && !selectorRef.value.contains(e.target as Node)) {
    showDropdown.value = false
  }
}

onMounted(() => document.addEventListener('click', handleClickOutside))
onBeforeUnmount(() => document.removeEventListener('click', handleClickOutside))
</script>

<style scoped lang="scss">
.status-selector {
  position: relative;
  display: inline-block;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: $radius-full;
  font-size: 0.75rem;
  font-weight: 500;
  white-space: nowrap;
  transition: all 0.2s;

  &.clickable {
    cursor: pointer;

    &:hover {
      filter: brightness(1.1);
    }
  }

  // 状态颜色
  &.draft {
    color: #71717a;
    background: rgba(113, 113, 122, 0.15);
    border: 1px solid rgba(113, 113, 122, 0.3);
    .status-dot { background: #71717a; }
  }

  &.applied {
    color: #60a5fa;
    background: rgba(96, 165, 250, 0.15);
    border: 1px solid rgba(96, 165, 250, 0.3);
    .status-dot { background: #60a5fa; }
  }

  &.interviewing {
    color: #fbbf24;
    background: rgba(251, 191, 36, 0.15);
    border: 1px solid rgba(251, 191, 36, 0.3);
    .status-dot { background: #fbbf24; }
    &.pulse .status-dot { animation: pulse 2s infinite; }
  }

  &.offered {
    color: #34d399;
    background: rgba(52, 211, 153, 0.15);
    border: 1px solid rgba(52, 211, 153, 0.3);
    .status-dot { background: #34d399; }
  }

  &.rejected {
    color: #f87171;
    background: rgba(248, 113, 113, 0.15);
    border: 1px solid rgba(248, 113, 113, 0.3);
    .status-dot { background: #f87171; }
  }

  &.withdrawn {
    color: #71717a;
    background: rgba(113, 113, 122, 0.15);
    border: 1px solid rgba(113, 113, 122, 0.3);
    .status-dot { background: #71717a; }
  }
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-text {
  line-height: 1;
}

.dropdown-arrow {
  font-size: 0.6rem;
  margin-left: 2px;
  opacity: 0.6;
  transition: transform 0.2s;
}

.status-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  z-index: 100;
  min-width: 160px;
  padding: $spacing-xs;
  background: $color-bg-elevated;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-md;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  border-radius: $radius-sm;
  cursor: pointer;
  font-size: 0.8125rem;
  color: $color-text-secondary;
  transition: all 0.15s;

  &:hover {
    background: rgba(255, 255, 255, 0.06);
    color: $color-text-primary;
  }

  &.active {
    color: $color-text-primary;
    background: rgba(255, 255, 255, 0.04);
  }
}

.item-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.item-label {
  flex: 1;
}

.item-check {
  font-size: 0.7rem;
  color: $color-accent;
}

// 下拉动画
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.15s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

// 脉动动画
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.2); }
}
</style>
