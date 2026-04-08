<template>
  <div class="meeting-link-bar" v-if="link">
    <span class="link-icon">🔗</span>
    <span class="link-label">会议链接:</span>
    <span class="link-url">{{ link }}</span>
    <div class="link-actions">
      <button class="action-btn" @click="handleCopy">
        {{ copied ? '已复制' : '复制' }}
      </button>
      <a :href="link" target="_blank" rel="noopener noreferrer" class="action-btn join-btn">
        入会
      </a>
    </div>
    <div v-if="password" class="password-section">
      <span class="password-label">密码:</span>
      <span class="password-value">{{ password }}</span>
      <button class="action-btn small" @click="handleCopyPassword">
        {{ passwordCopied ? '已复制' : '复制' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useToast } from '@/composables/useToast'

const props = defineProps<{
  link?: string
  password?: string
}>()

const toast = useToast()
const copied = ref(false)
const passwordCopied = ref(false)

async function handleCopy() {
  if (!props.link) return
  try {
    await navigator.clipboard.writeText(props.link)
    copied.value = true
    toast.success('链接已复制')
    setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch {
    toast.error('复制失败，请手动复制')
  }
}

async function handleCopyPassword() {
  if (!props.password) return
  try {
    await navigator.clipboard.writeText(props.password)
    passwordCopied.value = true
    toast.success('密码已复制')
    setTimeout(() => {
      passwordCopied.value = false
    }, 2000)
  } catch {
    toast.error('复制失败，请手动复制')
  }
}
</script>

<style scoped lang="scss">
.meeting-link-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  background: rgba($color-info, 0.1);
  border-radius: $radius-md;
  margin-top: $spacing-sm;

  .link-icon {
    font-size: 1rem;
  }

  .link-label {
    font-size: 0.875rem;
    color: $color-text-tertiary;
  }

  .link-url {
    flex: 1;
    font-size: 0.875rem;
    color: $color-text-primary;
    word-break: break-all;
    min-width: 0;
  }

  .link-actions {
    display: flex;
    gap: $spacing-xs;
  }

  .action-btn {
    padding: 4px 10px;
    font-size: 0.75rem;
    background: transparent;
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: $radius-sm;
    color: $color-text-secondary;
    cursor: pointer;
    transition: all 0.2s;
    text-decoration: none;

    &:hover {
      border-color: $color-accent;
      color: $color-accent;
    }

    &.join-btn {
      background: rgba($color-accent, 0.1);
      border-color: $color-accent;
      color: $color-accent;

      &:hover {
        background: rgba($color-accent, 0.2);
      }
    }

    &.small {
      padding: 2px 6px;
      font-size: 0.7rem;
    }
  }

  .password-section {
    display: flex;
    align-items: center;
    gap: $spacing-xs;
    margin-left: $spacing-md;
    padding-left: $spacing-md;
    border-left: 1px solid rgba(255, 255, 255, 0.1);

    .password-label {
      font-size: 0.75rem;
      color: $color-text-tertiary;
    }

    .password-value {
      font-size: 0.875rem;
      color: $color-text-primary;
      font-family: monospace;
    }
  }
}

@media (max-width: 480px) {
  .meeting-link-bar {
    flex-direction: column;
    align-items: flex-start;

    .link-url {
      width: 100%;
      order: 2;
    }

    .link-actions {
      order: 3;
      margin-top: $spacing-xs;
    }

    .password-section {
      margin-left: 0;
      padding-left: 0;
      border-left: none;
      margin-top: $spacing-xs;
      width: 100%;
      order: 4;
    }
  }
}
</style>
