<!--=====================================================
  快捷指令组件
  预设常用优化指令
  根据聊天模式显示不同的指令
  @author Azir
=====================================================-->

<template>
  <div class="quick-commands">
    <div class="commands-label">
      快捷指令
    </div>
    <div class="commands-list">
      <button
        v-for="cmd in displayCommands"
        :key="cmd.id"
        class="command-btn"
        @click="$emit('select', cmd.prompt)"
      >
        <font-awesome-icon :icon="getCategoryIcon(cmd.category)" class="command-icon" />
        <span class="command-label">{{ cmd.label }}</span>
      </button>

      <!-- 更多按钮 -->
      <button
        v-if="!showAll && currentCommands.length > 4"
        class="command-btn more-btn"
        @click="showAll = true"
      >
        <span class="command-icon">+</span>
        <span class="command-label">更多...</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { GENERAL_QUICK_COMMANDS, RESUME_QUICK_COMMANDS, type ChatMode } from '@/types/ai-chat'

interface Props {
  chatMode: ChatMode
}

interface Emits {
  (e: 'select', prompt: string): void
}

const props = defineProps<Props>()
defineEmits<Emits>()

const showAll = ref(false)

// 根据模式选择指令列表
const currentCommands = computed(() => {
  return props.chatMode === 'resume' ? RESUME_QUICK_COMMANDS : GENERAL_QUICK_COMMANDS
})

// 显示的指令（默认只显示前4个）
const displayCommands = computed(() => {
  return showAll.value ? currentCommands.value : currentCommands.value.slice(0, 4)
})

// 获取分类图标（FontAwesome 图标名称）
function getCategoryIcon(category: string): string {
  const icons: Record<string, string> = {
    'optimize': 'fa-wand-magic-sparkles',
    'adjust': 'fa-screwdriver-wrench',
    'diagnose': 'fa-magnifying-glass',
    'create': 'fa-pen-fancy',
    'general': 'fa-comment'
  }
  return icons[category] || 'fa-pencil-square'
}
</script>

<style lang="scss" scoped>
.quick-commands {
  padding: $spacing-md $spacing-lg;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba($color-bg-secondary, 0.5);
  width: 100%;
}

.commands-label {
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-bottom: $spacing-sm;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.commands-list {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
}

.command-btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-xs $spacing-md;
  background: $color-bg-tertiary;
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: $radius-full;
  color: $color-text-secondary;
  font-size: $text-sm;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba($color-accent, 0.1);
    border-color: rgba($color-accent, 0.3);
    color: $color-accent;
  }

  .command-icon {
    font-size: $text-sm;
    width: 14px;
    height: 14px;
  }

  .command-label {
    white-space: nowrap;
  }
}

.more-btn {
  background: transparent;
  border-style: dashed;

  &:hover {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.2);
    color: $color-text-primary;
  }
}
</style>
