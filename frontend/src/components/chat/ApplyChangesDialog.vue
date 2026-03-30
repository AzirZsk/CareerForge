<!--=====================================================
  修改确认对话框组件
  显示AI建议的修改内容，让用户确认是否应用
  @author Azir
=====================================================-->

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="visible"
        class="dialog-overlay"
        @click.self="handleCancel"
      >
        <div class="dialog-container">
          <!-- 头部 -->
          <header class="dialog-header">
            <h3 class="dialog-title">
              应用修改建议
            </h3>
            <button
              class="close-btn"
              @click="handleCancel"
            >
              <svg
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <line
                  x1="18"
                  y1="6"
                  x2="6"
                  y2="18"
                />
                <line
                  x1="6"
                  y1="6"
                  x2="18"
                  y2="18"
                />
              </svg>
            </button>
          </header>

          <!-- 内容 -->
          <div class="dialog-body">
            <div class="changes-list">
              <div
                v-for="(change, index) in changes"
                :key="index"
                class="change-item"
              >
                <!-- 变更类型标签 -->
                <div class="change-header">
                  <span
                    class="change-type"
                    :class="change.changeType"
                  >
                    {{ getTypeLabel(change.changeType) }}
                  </span>
                  <span class="change-section">
                    {{ change.sectionTitle || change.sectionType || '内容修改' }}
                  </span>
                </div>

                <!-- 变更说明 -->
                <div class="change-description">
                  💡 {{ change.description }}
                </div>

                <!-- 修改对比 -->
                <div
                  v-if="change.beforeContent || change.afterContent"
                  class="change-comparison"
                >
                  <div
                    v-if="change.beforeContent"
                    class="comparison-before"
                  >
                    <span class="comparison-label">修改前：</span>
                    <div class="comparison-content">
                      {{ truncateContent(change.beforeContent) }}
                    </div>
                  </div>
                  <div
                    v-if="change.afterContent"
                    class="comparison-after"
                  >
                    <span class="comparison-label">修改后：</span>
                    <div class="comparison-content">
                      {{ truncateContent(change.afterContent) }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 底部按钮 -->
          <footer class="dialog-footer">
            <button
              class="btn-regenerate"
              @click="handleRegenerate"
            >
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <polyline points="23 4 23 10 17 10" />
                <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
              </svg>
              重新生成
            </button>
            <button
              class="btn-cancel"
              @click="handleCancel"
            >
              取消
            </button>
            <button
              class="btn-confirm"
              @click="handleConfirm"
            >
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <polyline points="20 6 9 17 4 12" />
              </svg>
              应用修改
            </button>
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import type { SectionChange } from '@/types/ai-chat'

interface Props {
  visible: boolean
  changes: SectionChange[]
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'confirm'): void
  (e: 'regenerate'): void
  (e: 'cancel'): void
}

defineProps<Props>()
const emit = defineEmits<Emits>()

function getTypeLabel(type: string): string {
  const labels: Record<string, string> = {
    'update': '修改',
    'add': '新增',
    'delete': '删除'
  }
  return labels[type] || type
}

function truncateContent(content: string): string {
  if (!content) return ''
  try {
    // 尝试解析JSON并格式化
    const parsed = JSON.parse(content)
    const str = JSON.stringify(parsed, null, 2)
    return str.length > 200 ? str.slice(0, 200) + '...' : str
  } catch {
    // 如果不是JSON，直接截断
    return content.length > 200 ? content.slice(0, 200) + '...' : content
  }
}

function handleConfirm() {
  emit('confirm')
  emit('update:visible', false)
}

function handleRegenerate() {
  emit('regenerate')
  emit('update:visible', false)
}

function handleCancel() {
  emit('cancel')
  emit('update:visible', false)
}
</script>

<style lang="scss" scoped>
.dialog-overlay {
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
  z-index: 1020;
}

.dialog-container {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.08);
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
  display: flex;
  flex-direction: column;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-lg;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.dialog-title {
  font-family: $font-display;
  font-size: $text-lg;
  font-weight: $weight-semibold;
  color: $color-text-primary;
}

.close-btn {
  color: $color-text-tertiary;
  padding: $spacing-sm;
  border-radius: $radius-sm;
  transition: all 0.2s ease;

  &:hover {
    color: $color-text-primary;
    background: rgba(255, 255, 255, 0.05);
  }
}

.dialog-body {
  padding: $spacing-lg;
  overflow-y: auto;
  flex: 1;
}

.changes-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.change-item {
  padding: $spacing-md;
  background: $color-bg-tertiary;
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: $radius-md;
}

.change-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-sm;
}

.change-type {
  padding: 2px 8px;
  border-radius: $radius-sm;
  font-size: $text-xs;
  font-weight: $weight-medium;

  &.update {
    background: rgba($color-accent, 0.2);
    color: $color-accent;
  }

  &.add {
    background: rgba($color-success, 0.2);
    color: $color-success;
  }

  &.delete {
    background: rgba($color-error, 0.2);
    color: $color-error;
  }
}

.change-section {
  font-size: $text-sm;
  color: $color-text-secondary;
  font-weight: $weight-medium;
}

.change-description {
  font-size: $text-sm;
  color: $color-text-secondary;
  line-height: $leading-relaxed;
  margin-bottom: $spacing-sm;
}

.change-comparison {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  padding: $spacing-sm;
  background: rgba(0, 0, 0, 0.2);
  border-radius: $radius-sm;
}

.comparison-before,
.comparison-after {
  .comparison-label {
    font-size: $text-xs;
    color: $color-text-tertiary;
    display: block;
    margin-bottom: 2px;
  }

  .comparison-content {
    font-size: $text-sm;
    color: $color-text-secondary;
    font-family: monospace;
    white-space: pre-wrap;
    word-break: break-word;
  }
}

.comparison-after {
  .comparison-label {
    color: $color-accent;
  }

  .comparison-content {
    color: $color-accent;
  }
}

.dialog-footer {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-lg;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.btn-regenerate {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-sm $spacing-lg;
  font-size: $text-sm;
  color: $color-text-secondary;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-sm;
  margin-right: auto;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.2);
    color: $color-text-primary;
  }
}

.btn-cancel {
  padding: $spacing-sm $spacing-lg;
  font-size: $text-sm;
  color: $color-text-secondary;
  background: rgba(255, 255, 255, 0.05);
  border-radius: $radius-sm;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: $color-text-primary;
  }
}

.btn-confirm {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-sm $spacing-lg;
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-primary;
  background: $color-accent;
  border-radius: $radius-sm;
  transition: all 0.2s ease;

  &:hover {
    background: $color-accent-light;
  }
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.25s ease;

  .dialog-container {
    transition: transform 0.25s ease, opacity 0.25s ease;
  }
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;

  .dialog-container {
    transform: scale(0.95) translateY(-10px);
    opacity: 0;
  }
}
</style>
