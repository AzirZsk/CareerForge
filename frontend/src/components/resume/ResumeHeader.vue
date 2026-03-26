<!--=====================================================
  简历头部组件
  @author Azir
=====================================================-->

<template>
  <header
    class="resume-header animate-in"
    style="--delay: 1"
  >
    <div class="header-left">
      <!-- 编辑模式 -->
      <template v-if="isEditing">
        <div class="edit-form">
          <input
            v-model="editName"
            type="text"
            class="edit-input title-input"
            placeholder="简历名称"
            maxlength="50"
          >
          <input
            v-model="editTargetPosition"
            type="text"
            class="edit-input target-input"
            placeholder="目标岗位（可选）"
            maxlength="50"
          >
        </div>
        <div class="edit-actions">
          <button
            class="edit-btn save"
            :disabled="!editName.trim()"
            @click="handleSave"
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
            保存
          </button>
          <button
            class="edit-btn cancel"
            @click="handleCancel"
          >
            取消
          </button>
        </div>
      </template>
      <!-- 展示模式 -->
      <template v-else>
        <div class="title-row">
          <h1 class="resume-title">
            {{ name }}
          </h1>
          <button
            v-if="resumeId"
            class="edit-trigger"
            title="编辑名称"
            @click="startEdit"
          >
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
            </svg>
          </button>
        </div>
        <p class="resume-target">
          目标岗位：{{ targetPosition || '暂未设置目标职位' }}
        </p>
      </template>
    </div>
    <div class="header-right">
      <div class="score-overview">
        <div class="score-main">
          <div
            class="score-ring"
            :style="{ '--score': analyzed ? overallScore : 0 }"
          >
            <span>{{ analyzed ? overallScore : '~' }}</span>
          </div>
          <div class="score-labels">
            <span class="score-title">综合评分</span>
            <span class="score-detail">结构规范 {{ analyzed ? structureScore + '%' : '~' }}</span>
          </div>
        </div>
      </div>
      <div class="header-actions">
        <button
          class="action-btn primary"
          :class="{ disabled: !hasContent }"
          :disabled="!hasContent"
          :title="hasContent ? '' : '请先填写简历内容'"
          @click="hasContent && $emit('optimize')"
        >
          <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
          </svg>
          {{ hasContent ? (analyzed ? '一键优化' : 'AI分析') : '请先填写内容' }}
        </button>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  name: string
  targetPosition: string
  analyzed: boolean
  overallScore: number
  structureScore: number
  hasContent?: boolean
  resumeId?: string
}>()

const emit = defineEmits<{
  optimize: []
  update: [data: { name: string; targetPosition?: string }]
}>()

// 编辑状态
const isEditing = ref(false)
const editName = ref('')
const editTargetPosition = ref('')

// 监听 props 变化，同步到编辑状态
watch(() => props.name, (newName) => {
  if (!isEditing.value) {
    editName.value = newName
  }
}, { immediate: true })

watch(() => props.targetPosition, (newTarget) => {
  if (!isEditing.value) {
    editTargetPosition.value = newTarget || ''
  }
}, { immediate: true })

// 开始编辑
function startEdit(): void {
  editName.value = props.name
  editTargetPosition.value = props.targetPosition || ''
  isEditing.value = true
}

// 保存编辑
function handleSave(): void {
  if (!editName.value.trim()) return
  emit('update', {
    name: editName.value.trim(),
    targetPosition: editTargetPosition.value.trim() || undefined
  })
  isEditing.value = false
}

// 取消编辑
function handleCancel(): void {
  editName.value = props.name
  editTargetPosition.value = props.targetPosition || ''
  isEditing.value = false
}
</script>

<style lang="scss" scoped>
.resume-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: $spacing-xl;
  background: $gradient-card;
  border-radius: $radius-xl;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.header-left {
  flex: 1;
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-xs;
}

.resume-title {
  font-family: $font-display;
  font-size: $text-3xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin: 0;
}

.edit-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: transparent;
  border: none;
  border-radius: $radius-sm;
  color: $color-text-tertiary;
  cursor: pointer;
  transition: all $transition-fast;
  opacity: 0;

  .title-row:hover & {
    opacity: 1;
  }

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: $color-accent;
  }
}

.resume-target {
  font-size: $text-base;
  color: $color-text-secondary;
  margin: 0;
}

// 编辑表单样式
.edit-form {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  margin-bottom: $spacing-sm;
}

.edit-input {
  width: 100%;
  max-width: 360px;
  padding: $spacing-sm $spacing-md;
  font-size: $text-base;
  color: $color-text-primary;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-sm;
  transition: all $transition-fast;

  &::placeholder {
    color: $color-text-tertiary;
  }

  &:focus {
    outline: none;
    border-color: $color-accent;
    background: rgba(255, 255, 255, 0.08);
  }

  &.title-input {
    font-size: $text-xl;
    font-weight: $weight-semibold;
  }
}

.edit-actions {
  display: flex;
  gap: $spacing-sm;
}

.edit-btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-xs $spacing-md;
  font-size: $text-sm;
  font-weight: $weight-medium;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: all $transition-fast;

  &.save {
    background: $gradient-gold;
    color: $color-bg-deep;
    border: none;

    &:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 2px 8px rgba(212, 168, 83, 0.3);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }

  &.cancel {
    background: transparent;
    color: $color-text-secondary;
    border: 1px solid rgba(255, 255, 255, 0.1);

    &:hover {
      background: rgba(255, 255, 255, 0.05);
      color: $color-text-primary;
    }
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: $spacing-2xl;
  flex-shrink: 0;
}

.score-main {
  display: flex;
  align-items: center;
  gap: $spacing-lg;
}

.score-ring {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: conic-gradient(
    $color-accent calc(var(--score) * 3.6deg),
    rgba(255, 255, 255, 0.1) calc(var(--score) * 3.6deg)
  );
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  &::before {
    content: '';
    position: absolute;
    inset: 6px;
    background: $color-bg-tertiary;
    border-radius: 50%;
  }
  span {
    position: relative;
    z-index: 1;
    font-family: $font-display;
    font-size: $text-2xl;
    font-weight: $weight-bold;
    color: $color-accent;
  }
}

.score-labels {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.score-title {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.score-detail {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.header-actions {
  display: flex;
  gap: $spacing-md;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-lg;
  font-size: $text-sm;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  transition: all $transition-fast;
  cursor: pointer;
  &.primary {
    background: $gradient-gold;
    color: $color-bg-deep;
    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 4px 16px rgba(212, 168, 83, 0.3);
    }
    &.disabled,
    &:disabled {
      background: rgba(255, 255, 255, 0.1);
      color: $color-text-tertiary;
      cursor: not-allowed;
      transform: none;
      box-shadow: none;
    }
  }
  &.secondary {
    background: rgba(255, 255, 255, 0.05);
    color: $color-text-secondary;
    border: 1px solid rgba(255, 255, 255, 0.1);
    &:hover {
      background: rgba(255, 255, 255, 0.1);
    }
  }
}

.animate-in {
  animation: slideUp 0.6s ease forwards;
  animation-delay: calc(var(--delay) * 0.1s);
  opacity: 0;
}
</style>
