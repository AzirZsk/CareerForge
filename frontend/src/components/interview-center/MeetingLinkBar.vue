<template>
  <div class="meeting-link-bar">
    <div class="link-content">
      <font-awesome-icon icon="fa-solid fa-link" class="link-icon" />
      <span class="link-label">会议链接:</span>
      <a :href="link" target="_blank" class="link-url">{{ link }}</a>
    </div>
    <div class="link-actions">
      <template v-if="password">
        <button class="action-btn" @click="handleCopy(password, '密码')">
          <font-awesome-icon icon="fa-solid fa-key" />
          复制密码
        </button>
      </template>
      <button class="action-btn" @click="handleCopy(link, '链接')">
        <font-awesome-icon icon="fa-solid fa-copy" />
        复制
      </button>
      <button class="action-btn primary" @click="handleJoin">
        <font-awesome-icon icon="fa-solid fa-arrow-up-right-from-square" />
        入会
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useToast } from '@/composables/useToast'

interface Props {
  link: string
  password?: string
}

const props = defineProps<Props>()
const toast = useToast()

function handleCopy(text: string, label: string) {
  navigator.clipboard.writeText(text).then(() => {
    toast.success(`${label}已复制到剪贴板`)
  }).catch(() => {
    toast.error(`复制失败，请手动复制`)
  })
}

function handleJoin() {
  window.open(props.link, '_blank')
}
</script>

<style scoped lang="scss">
.meeting-link-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-sm $spacing-md;
  background: $color-info-bg;
  border-radius: $radius-md;
  border: 1px solid rgba($color-info, 0.2);
}

.link-content {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  flex: 1;
  min-width: 0;
}

.link-icon {
  color: $color-info;
  font-size: 0.875rem;
  flex-shrink: 0;
}

.link-label {
  color: $color-text-tertiary;
  font-size: 0.875rem;
  flex-shrink: 0;
}

.link-url {
  color: $color-info;
  font-size: 0.875rem;
  text-decoration: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  &:hover {
    text-decoration: underline;
  }
}

.link-actions {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  flex-shrink: 0;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: 4px 10px;
  font-size: 0.75rem;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-sm;
  color: $color-text-secondary;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: $color-info;
    color: $color-info;
  }

  &.primary {
    background: $color-info;
    border-color: $color-info;
    color: $color-bg-primary;

    &:hover {
      opacity: 0.9;
    }
  }
}
</style>
