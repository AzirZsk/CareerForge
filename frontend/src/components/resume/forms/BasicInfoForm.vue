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
        placeholder="请输入姓名"
      />
    </div>
    <div class="form-row">
      <div class="form-group">
        <label class="form-label">性别</label>
        <select v-model="localData.gender" class="form-select">
          <option value="">请选择</option>
          <option value="男">男</option>
          <option value="女">女</option>
        </select>
      </div>
      <div class="form-group">
        <label class="form-label required">电话</label>
        <input
          v-model="localData.phone"
          type="tel"
          class="form-input"
          placeholder="请输入手机号"
        />
      </div>
    </div>
    <div class="form-group">
      <label class="form-label required">邮箱</label>
      <input
        v-model="localData.email"
        type="email"
        class="form-input"
        placeholder="请输入邮箱地址"
      />
    </div>
    <div class="form-group">
      <label class="form-label">目标岗位</label>
      <input
        v-model="localData.targetPosition"
        type="text"
        class="form-input"
        placeholder="请输入目标岗位"
      />
    </div>
    <div class="form-group">
      <label class="form-label">个人简介</label>
      <textarea
        v-model="localData.summary"
        class="form-textarea"
        rows="4"
        placeholder="请输入个人简介"
      ></textarea>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

interface Props {
  modelValue: Record<string, unknown>
}

interface Emits {
  (e: 'update:modelValue', value: Record<string, unknown>): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 本地数据
const localData = ref<Record<string, string>>({})

// 初始化数据
function initData(): void {
  const data: Record<string, string> = {
    name: '',
    gender: '',
    phone: '',
    email: '',
    targetPosition: '',
    summary: ''
  }
  for (const [key, value] of Object.entries(props.modelValue)) {
    if (typeof value === 'string') {
      data[key] = value
    }
  }
  localData.value = data
}

// 监听外部变化
watch(
  () => props.modelValue,
  () => initData(),
  { immediate: true, deep: true }
)

// 监听本地变化，同步到父组件
watch(
  localData,
  (newVal) => {
    emit('update:modelValue', { ...newVal })
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

.form-select {
  cursor: pointer;
}

.form-textarea {
  resize: vertical;
  min-height: 100px;
}
</style>
