<!--=====================================================
  LandIt 区块编辑按钮组件
  统一的编辑按钮（区块级/条目级）
  @author Azir
=====================================================-->

<template>
  <button
    class="edit-btn"
    :class="[sizeClass, { 'is-visible': visible }]"
    :title="title"
    @click.stop="handleClick"
  >
    <svg
      :width="iconSize"
      :height="iconSize"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
    >
      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
    </svg>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  /** 按钮尺寸：small(24px) / medium(28px) */
  size?: 'small' | 'medium'
  /** 是否可见（用于 hover 显示） */
  visible?: boolean
  /** 按钮标题 */
  title?: string
}

const props = withDefaults(defineProps<Props>(), {
  size: 'small',
  visible: false,
  title: '编辑'
})

const emit = defineEmits<{
  (e: 'click', event: MouseEvent): void
}>()

const sizeClass = computed(() => `size-${props.size}`)

const iconSize = computed(() => (props.size === 'medium' ? 14 : 12))

function handleClick(event: MouseEvent) {
  emit('click', event)
}
</script>

<style lang="scss" scoped>
.edit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius-sm;
  color: $color-text-tertiary;
  background: rgba(255, 255, 255, 0.05);
  transition: all $transition-fast;
  cursor: pointer;
  border: none;
  padding: 0;

  // 尺寸变体 - 条目级按钮（绝对定位到右上角）
  &.size-small {
    position: absolute;
    top: $spacing-sm;
    right: $spacing-sm;
    width: 24px;
    height: 24px;
    opacity: 0;
    z-index: 1;

    &.is-visible {
      opacity: 1;
    }
  }

  &.size-medium {
    width: 28px;
    height: 28px;
  }

  &:hover {
    background: rgba(212, 168, 83, 0.15);
    color: $color-accent;
  }
}
</style>
