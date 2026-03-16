<!--=====================================================
  添加模块类型选择器弹窗
  @author Azir
=====================================================-->

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="modal-overlay" @click.self="$emit('update:visible', false)">
        <div class="modal-container">
          <header class="modal-header">
            <h3 class="modal-title">添加模块</h3>
            <button class="close-btn" @click="$emit('update:visible', false)">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </header>

          <div class="modal-body">
            <p v-if="availableTypes.length === 0" class="empty-hint">
              所有模块类型已存在，无需添加
            </p>
            <div v-else class="section-types-grid">
              <button
                v-for="typeInfo in availableTypes"
                :key="typeInfo.type"
                class="type-card"
                @click="handleSelectType(typeInfo.type)"
              >
                <span class="type-icon">{{ typeInfo.icon }}</span>
                <span class="type-label">{{ typeInfo.label }}</span>
                <span v-if="typeInfo.tooltip" class="tooltip">{{ typeInfo.tooltip }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { SectionType } from '@/types'

// 模块类型配置
const SECTION_TYPE_CONFIG: Array<{ type: SectionType; label: string; icon: string; tooltip?: string }> = [
  { type: 'BASIC_INFO', label: '基本信息', icon: '👤' },
  { type: 'EDUCATION', label: '教育经历', icon: '🎓' },
  { type: 'WORK', label: '工作经历', icon: '💼' },
  { type: 'PROJECT', label: '项目经历', icon: '📦' },
  { type: 'SKILLS', label: '专业技能', icon: '⚡' },
  { type: 'CERTIFICATE', label: '证书荣誉', icon: '🏆' },
  { type: 'OPEN_SOURCE', label: '开源贡献', icon: '🔧' },
  { type: 'CUSTOM', label: '自定义区块', icon: '📝', tooltip: '适用于游戏经历、志愿者活动、竞赛经历等' }
]

const props = defineProps<{
  visible: boolean
  existingTypes: SectionType[]
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'select': [type: SectionType]
}>()

// 计算可添加的类型（过滤已存在的类型）
const availableTypes = computed(() => {
  return SECTION_TYPE_CONFIG.filter((item) => {
    // CUSTOM 类型始终可选（用户可以添加多个自定义区块）
    if (item.type === 'CUSTOM') {
      return true
    }
    // 其他类型：过滤已存在的
    return !props.existingTypes.includes(item.type)
  })
})

function handleSelectType(type: SectionType): void {
  emit('select', type)
  emit('update:visible', false)
}
</script>

<style lang="scss" scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: $z-modal;
}

.modal-container {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.08);
  width: 90%;
  max-width: 480px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-lg;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.modal-title {
  font-family: $font-display;
  font-size: $text-xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
}

.close-btn {
  color: $color-text-tertiary;
  transition: color $transition-fast;
  &:hover {
    color: $color-text-primary;
  }
}

.modal-body {
  padding: $spacing-lg;
}

.section-types-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $spacing-md;
}

.type-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-lg $spacing-md;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: $radius-md;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.06);
    border-color: rgba(212, 168, 83, 0.3);
    transform: translateY(-2px);

    .tooltip {
      opacity: 1;
      visibility: visible;
      transform: translateX(-50%) translateY(0);
    }
  }
}

.type-icon {
  font-size: 1.75rem;
}

.type-label {
  font-size: $text-sm;
  color: $color-text-secondary;
  font-weight: $weight-medium;
}

.tooltip {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%) translateY(4px);
  padding: $spacing-sm $spacing-md;
  background: $color-bg-elevated;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-sm;
  font-size: $text-xs;
  color: $color-text-secondary;
  white-space: nowrap;
  opacity: 0;
  visibility: hidden;
  transition: all $transition-fast;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  z-index: 10;
  pointer-events: none;
}

.empty-hint {
  text-align: center;
  color: $color-text-tertiary;
  padding: $spacing-xl 0;
}

// 动画
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.25s ease;

  .modal-container {
    transition: transform 0.25s ease, opacity 0.25s ease;
  }
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;

  .modal-container {
    transform: scale(0.95) translateY(-10px);
    opacity: 0;
  }
}
</style>
