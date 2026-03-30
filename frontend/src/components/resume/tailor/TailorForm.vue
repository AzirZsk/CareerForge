<!--=====================================================
  LandIt 定制简历表单组件
  @author Azir
=====================================================-->

<template>
  <div class="form-section">
    <div class="form-group">
      <label class="form-label required">目标职位</label>
      <input
        :value="formData.targetPosition"
        type="text"
        class="form-input"
        placeholder="如：高级Java工程师"
        @input="updateField('targetPosition', ($event.target as HTMLInputElement).value)"
      >
    </div>

    <div class="form-group">
      <label class="form-label required">职位描述</label>
      <textarea
        :value="formData.jobDescription"
        class="form-textarea"
        placeholder="粘贴目标职位的 JD 内容..."
        rows="8"
        @input="updateField('jobDescription', ($event.target as HTMLTextAreaElement).value)"
      />
    </div>

    <div class="form-group">
      <label class="form-label">简历名称（可选）</label>
      <input
        :value="formData.resumeName"
        type="text"
        class="form-input"
        placeholder="如：高级Java工程师定制版"
        @input="updateField('resumeName', ($event.target as HTMLInputElement).value)"
      >
    </div>

    <div class="form-actions">
      <button
        class="btn-cancel"
        @click="$emit('cancel')"
      >
        取消
      </button>
      <button
        class="btn-submit"
        :disabled="!isFormValid"
        @click="$emit('submit')"
      >
        <svg
          width="18"
          height="18"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
        </svg>
        生成定制简历
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

export interface TailorFormData {
  targetPosition: string
  jobDescription: string
  resumeName: string
}

const props = defineProps<{
  formData: TailorFormData
}>()

const emit = defineEmits<{
  'update:formData': [value: TailorFormData]
  'submit': []
  'cancel': []
}>()

// 表单验证
const isFormValid = computed(() => {
  return props.formData.targetPosition.trim() && props.formData.jobDescription.trim()
})

// 更新表单字段
function updateField(field: keyof TailorFormData, value: string) {
  emit('update:formData', {
    ...props.formData,
    [field]: value
  })
}
</script>

<style lang="scss" scoped>
.form-section {
  padding: $spacing-lg;
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
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
  background: $color-bg-tertiary;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-md;
  padding: $spacing-md;
  color: $color-text-primary;
  font-size: $text-base;
  transition: all $transition-fast;

  &::placeholder {
    color: $color-text-tertiary;
  }

  &:focus {
    outline: none;
    border-color: $color-accent;
  }
}

.form-textarea {
  resize: vertical;
  min-height: 120px;
  font-family: inherit;
}

.form-actions {
  display: flex;
  gap: $spacing-md;
  justify-content: flex-end;
  margin-top: $spacing-md;
}

.btn-cancel,
.btn-submit {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-lg;
  font-size: $text-sm;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  transition: all $transition-fast;
}

.btn-cancel {
  background: transparent;
  color: $color-text-secondary;

  &:hover {
    background: rgba(255, 255, 255, 0.05);
    color: $color-text-primary;
  }
}

.btn-submit {
  background: $gradient-gold;
  color: $color-bg-deep;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &:not(:disabled):hover {
    box-shadow: 0 4px 16px rgba(212, 168, 83, 0.3);
  }
}
</style>
