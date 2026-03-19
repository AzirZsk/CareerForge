<!--=====================================================
  LandIt 自定义区块内容项表单
  用于添加/编辑自定义区块的单个内容项（不含区块标题）
  @author Azir
=====================================================-->

<template>
  <div class="custom-item-form">
    <div class="form-group">
      <label class="form-label required">名称</label>
      <input
        v-model="localData.name"
        type="text"
        class="form-input"
        :class="{ 'form-input--error': hasError('name') }"
        placeholder="请输入名称"
      />
    </div>
    <div class="form-row">
      <div class="form-group">
        <label class="form-label">角色/职位</label>
        <input
          v-model="localData.role"
          type="text"
          class="form-input"
          placeholder="请输入角色或职位"
        />
      </div>
      <div class="form-group">
        <label class="form-label">时间段</label>
        <input
          v-model="localData.period"
          type="text"
          class="form-input"
          placeholder="例如：2023.01 - 2023.06"
        />
      </div>
    </div>
    <div class="form-group">
      <label class="form-label">详细描述</label>
      <textarea
        v-model="localData.description"
        class="form-textarea"
        rows="3"
        placeholder="请输入详细描述"
      ></textarea>
    </div>
    <div class="form-group">
      <label class="form-label">成果/要点</label>
      <DynamicListInput
        v-model="localHighlights"
        item-placeholder="请输入成果或要点"
        add-button-text="添加要点"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useValidationInject } from '@/composables/useFormValidation'
import DynamicListInput from './DynamicListInput.vue'

interface Props {
  modelValue: Record<string, unknown>
}

interface Emits {
  (e: 'update:modelValue', value: Record<string, unknown>): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 获取校验上下文
const validation = useValidationInject()
const hasError = (field: string) => validation?.hasError(field) ?? false

// 本地数据
const localData = ref<Record<string, string>>({})
const localHighlights = ref<string[]>([])

// 初始化数据
function initData(): void {
  const data: Record<string, string> = {}
  for (const [key, value] of Object.entries(props.modelValue)) {
    // 排除 highlights 数组
    if (key !== 'highlights' && typeof value === 'string') {
      data[key] = value
    }
  }
  localData.value = data

  // 处理 highlights 数组
  const highlights = props.modelValue?.highlights
  localHighlights.value = Array.isArray(highlights) ? [...highlights] : []
}

// 标记是否正在同步，避免无限循环
let isSyncing = false

// 监听外部变化
watch(
  () => props.modelValue,
  () => {
    if (!isSyncing) {
      initData()
    }
  },
  { immediate: true, deep: true }
)

// 监听本地数据变化，同步到父组件
watch(
  [localData, localHighlights],
  () => {
    const data: Record<string, unknown> = {
      ...localData.value,
      highlights: localHighlights.value.filter((h) => h.trim())
    }
    isSyncing = true
    emit('update:modelValue', data)
    // 使用 nextTick 确保 Vue 完成更新后再重置标志
    setTimeout(() => {
      isSyncing = false
    }, 0)
  },
  { deep: true }
)
</script>

<style lang="scss" scoped>
.custom-item-form {
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: $spacing-lg;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.form-label {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-secondary;
  &.required::after {
    content: '*';
    color: $color-error;
    margin-left: 4px;
  }
}

.form-input,
.form-textarea {
  padding: $spacing-sm $spacing-md;
  font-size: $text-sm;
  font-family: $font-body;
  color: $color-text-primary;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-sm;
  transition: all $transition-fast;
  &:focus {
    outline: none;
    border-color: $color-accent;
    background: rgba(255, 255, 255, 0.05);
  }
  &::placeholder {
    color: $color-text-tertiary;
  }
}

// 错误状态样式
.form-input--error,
.form-textarea--error {
  border-color: $color-error !important;
  background: rgba(248, 113, 113, 0.05) !important;
  &:focus {
    border-color: $color-error !important;
    box-shadow: 0 0 0 2px rgba(248, 113, 113, 0.2);
  }
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}
</style>
