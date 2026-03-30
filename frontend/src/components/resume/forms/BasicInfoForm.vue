<!--=====================================================
  LandIt 基本信息表单
  @author Azir
=====================================================-->

<template>
  <div class="basic-info-form">
    <div class="form-group">
      <label class="form-label required">姓名</label>
      <input
        v-model="localData.name"
        type="text"
        class="form-input"
        :class="{ 'form-input--error': hasError('name') }"
        placeholder="请输入姓名"
      >
    </div>
    <div class="form-row">
      <div class="form-group">
        <label class="form-label">性别</label>
        <select
          v-model="localData.gender"
          class="form-select"
        >
          <option value="">
            请选择
          </option>
          <option value="男">
            男
          </option>
          <option value="女">
            女
          </option>
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">出生日期</label>
        <input
          v-model="localData.birthday"
          type="text"
          class="form-input"
          placeholder="如 1995-03 或 1995-03-15"
        >
      </div>
      <div class="form-group">
        <label class="form-label">年龄</label>
        <input
          v-model="localData.age"
          type="text"
          class="form-input"
          placeholder="如 28"
        >
      </div>
    </div>
    <div class="form-row">
      <div class="form-group">
        <label class="form-label required">电话</label>
        <input
          v-model="localData.phone"
          type="tel"
          class="form-input"
          :class="{ 'form-input--error': hasError('phone') }"
          placeholder="请输入手机号"
        >
      </div>
      <div class="form-group">
        <label class="form-label required">邮箱</label>
        <input
          v-model="localData.email"
          type="email"
          class="form-input"
          :class="{ 'form-input--error': hasError('email') }"
          placeholder="请输入邮箱地址"
        >
      </div>
    </div>
    <div class="form-group">
      <label class="form-label">目标岗位</label>
      <input
        v-model="localData.targetPosition"
        type="text"
        class="form-input"
        placeholder="请输入目标岗位"
      >
    </div>
    <div class="form-group">
      <label class="form-label">个人简介</label>
      <textarea
        v-model="localData.summary"
        class="form-textarea"
        rows="4"
        placeholder="请输入个人简介"
      />
    </div>
    <div class="form-group">
      <label class="form-label">所在地</label>
      <input
        v-model="localData.location"
        type="text"
        class="form-input"
        placeholder="如 北京、上海、深圳"
      >
    </div>
    <div class="form-row">
      <div class="form-group">
        <label class="form-label">LinkedIn</label>
        <input
          v-model="localData.linkedin"
          type="text"
          class="form-input"
          placeholder="LinkedIn 主页链接"
        >
      </div>
      <div class="form-group">
        <label class="form-label">GitHub</label>
        <input
          v-model="localData.github"
          type="text"
          class="form-input"
          placeholder="GitHub 主页链接"
        >
      </div>
    </div>
    <div class="form-group">
      <label class="form-label">个人网站</label>
      <input
        v-model="localData.website"
        type="text"
        class="form-input"
        placeholder="个人网站或博客链接"
      >
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useValidationInject } from '@/composables/useFormValidation'

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
const localData = ref<Record<string, string>>({
  name: '',
  gender: '',
  birthday: '',
  age: '',
  phone: '',
  email: '',
  targetPosition: '',
  summary: '',
  location: '',
  linkedin: '',
  github: '',
  website: ''
})

// 初始化数据
function initData(): void {
  const data: Record<string, string> = {
    name: '',
    gender: '',
    birthday: '',
    age: '',
    phone: '',
    email: '',
    targetPosition: '',
    summary: '',
    location: '',
    linkedin: '',
    github: '',
    website: ''
  }
  for (const [key, value] of Object.entries(props.modelValue)) {
    if (typeof value === 'string') {
      data[key] = value
    }
  }
  localData.value = data
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
  localData,
  (newVal) => {
    isSyncing = true
    emit('update:modelValue', { ...newVal })
    setTimeout(() => {
      isSyncing = false
    }, 0)
  },
  { deep: true }
)
</script>

<style lang="scss" scoped>
.basic-info-form {
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $spacing-lg;
  &:has(.form-group:nth-child(2):last-child) {
    grid-template-columns: 1fr 1fr;
  }
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
.form-select,
.form-textarea {
  padding: $spacing-sm $spacing-md;
  font-size: $text-sm;
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
.form-select--error,
.form-textarea--error {
  border-color: $color-error !important;
  background: rgba(248, 113, 113, 0.05) !important;
  &:focus {
    border-color: $color-error !important;
    box-shadow: 0 0 0 2px rgba(248, 113, 113, 0.2);
  }
}

.form-select {
  cursor: pointer;
  option {
    background: $color-bg-secondary;
    color: $color-text-primary;
  }
}

.form-textarea {
  resize: vertical;
  min-height: 140px;
}
</style>
