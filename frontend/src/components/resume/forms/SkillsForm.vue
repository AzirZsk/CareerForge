<!--=====================================================
  LandIt 技能表单
  @author Azir
=====================================================-->

<template>
  <div class="skills-form">
    <div class="form-group">
      <label class="form-label">专业技能</label>
      <p class="form-hint">添加技能名称、描述、熟练度和分类</p>
    </div>
    <TransitionGroup name="skill-item" tag="div" class="skill-list">
      <div v-for="(skill, index) in localSkills" :key="index" class="skill-item">
        <div class="skill-row">
          <input
            v-model="skill.name"
            type="text"
            class="form-input skill-name"
            :class="{ 'form-input--error': hasError(`skills.${index}.name`) }"
            placeholder="技能名称"
            @input="syncToParent"
          />
          <select v-model="skill.level" class="form-select skill-level" @change="syncToParent">
            <option value="">熟练度</option>
            <option value="了解">了解</option>
            <option value="熟悉">熟悉</option>
            <option value="熟练">熟练</option>
            <option value="精通">精通</option>
          </select>
          <button class="remove-btn" @click="removeSkill(index)" title="删除">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
        <div class="skill-row-secondary">
          <input
            v-model="skill.category"
            type="text"
            class="form-input skill-category"
            placeholder="分类（如：编程语言、框架、工具）"
            @input="syncToParent"
          />
          <input
            v-model="skill.description"
            type="text"
            class="form-input skill-desc"
            placeholder="技能描述（如：5年经验，精通并发编程）"
            @input="syncToParent"
          />
        </div>
      </div>
    </TransitionGroup>
    <button class="add-btn" @click="addSkill">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <line x1="12" y1="5" x2="12" y2="19"></line>
        <line x1="5" y1="12" x2="19" y2="12"></line>
      </svg>
      添加技能
    </button>
    <p v-if="localSkills.length === 0" class="empty-hint">暂无技能，请点击上方按钮添加</p>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { Skill } from '@/types'
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

// 本地技能列表
const localSkills = ref<Skill[]>([])

// 初始化数据
function initData(): void {
  const content = props.modelValue as { skills?: Skill[] }
  if (Array.isArray(content?.skills)) {
    // 兼容旧数据：如果元素是字符串，转换为对象
    localSkills.value = content.skills.map((s) => {
      if (typeof s === 'string') {
        return { name: s, description: '', level: '', category: '' }
      }
      return { ...s }
    })
  } else {
    localSkills.value = []
  }
}

// 监听外部变化
watch(
  () => props.modelValue,
  () => initData(),
  { immediate: true, deep: true }
)

// 同步到父组件
function syncToParent(): void {
  emit('update:modelValue', { skills: localSkills.value.map((s) => ({ ...s })) })
}

// 添加技能
function addSkill(): void {
  localSkills.value.push({
    name: '',
    description: '',
    level: '',
    category: ''
  })
}

// 删除技能
function removeSkill(index: number): void {
  localSkills.value.splice(index, 1)
  syncToParent()
}
</script>

<style lang="scss" scoped>
.skills-form {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
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
}

.form-hint {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.skill-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.skill-item {
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: $radius-md;
  transition: all $transition-fast;
  &:hover {
    border-color: rgba(255, 255, 255, 0.15);
  }
}

.skill-row {
  display: flex;
  gap: $spacing-sm;
  align-items: center;
}

.skill-row-secondary {
  display: flex;
  gap: $spacing-sm;
  margin-top: $spacing-sm;
}

.form-input {
  flex: 1;
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

.skill-name {
  flex: 2;
}

.skill-level {
  width: 100px;
}

.skill-category {
  flex: 1;
}

.skill-desc {
  flex: 2;
}

.form-select {
  padding: $spacing-sm $spacing-md;
  font-size: $text-sm;
  color: $color-text-primary;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-sm;
  cursor: pointer;
  transition: all $transition-fast;
  &:focus {
    outline: none;
    border-color: $color-accent;
  }
  option {
    background: $color-bg-secondary;
    color: $color-text-primary;
  }
}

.remove-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $color-text-tertiary;
  background: transparent;
  border-radius: $radius-sm;
  transition: all $transition-fast;
  flex-shrink: 0;
  &:hover {
    color: $color-error;
    background: rgba(248, 113, 113, 0.1);
  }
}

.add-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  font-size: $text-sm;
  color: $color-accent;
  background: $color-accent-glow;
  border-radius: $radius-sm;
  transition: all $transition-fast;
  &:hover {
    background: rgba(212, 168, 83, 0.2);
  }
}

.empty-hint {
  font-size: $text-sm;
  color: $color-text-tertiary;
  text-align: center;
  padding: $spacing-md;
}

// 动画
.skill-item-enter-active,
.skill-item-leave-active {
  transition: all 0.2s ease;
}
.skill-item-enter-from,
.skill-item-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}
</style>
