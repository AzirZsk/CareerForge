<!--=====================================================
  LandIt 动态列表输入组件
  通用的动态添加/删除文本输入项组件
  @author Azir
=====================================================-->

<template>
  <div class="dynamic-list-input">
    <div class="list-container">
      <div
        v-for="(_, index) in localValue"
        :key="index"
        class="list-item"
      >
        <input
          v-model="localValue[index]"
          type="text"
          class="form-input"
          :placeholder="itemPlaceholder"
        >
        <button
          class="remove-btn"
          type="button"
          @click="removeItem(index)"
        >
          <font-awesome-icon icon="fa-solid fa-xmark" />
        </button>
      </div>
    </div>
    <button
      class="add-btn"
      type="button"
      @click="addItem"
    >
      <font-awesome-icon icon="fa-solid fa-plus" />
      {{ addButtonText }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

interface Props {
  modelValue: string[]
  itemPlaceholder?: string
  addButtonText?: string
}

interface Emits {
  (e: 'update:modelValue', value: string[]): void
}

const props = withDefaults(defineProps<Props>(), {
  itemPlaceholder: '请输入内容',
  addButtonText: '添加项目'
})

const emit = defineEmits<Emits>()

// 本地数据，用于 v-model 绑定
const localValue = ref<string[]>([])

// 监听外部变化
watch(
  () => props.modelValue,
  (newValue) => {
    localValue.value = Array.isArray(newValue) ? [...newValue] : []
  },
  { immediate: true }
)

// 监听本地变化，同步到父组件
watch(
  localValue,
  (newValue) => {
    emit('update:modelValue', newValue.filter((item) => item.trim()))
  },
  { deep: true }
)

// 添加项目
function addItem(): void {
  localValue.value.push('')
}

// 删除项目
function removeItem(index: number): void {
  localValue.value.splice(index, 1)
}
</script>

<style lang="scss" scoped>
.dynamic-list-input {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.list-container {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.list-item {
  display: flex;
  gap: $spacing-sm;

  .form-input {
    flex: 1;
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
}

.remove-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $color-text-tertiary;
  border-radius: $radius-sm;
  transition: all $transition-fast;

  &:hover {
    background: rgba(248, 113, 113, 0.1);
    color: $color-error;
  }
}

.add-btn {
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-sm $spacing-md;
  font-size: $text-sm;
  color: $color-accent;
  background: $color-accent-glow;
  border-radius: $radius-sm;
  transition: all $transition-fast;
  align-self: flex-start;

  &:hover {
    background: rgba(212, 168, 83, 0.2);
  }
}
</style>
