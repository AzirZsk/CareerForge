<!--=====================================================
  LandIt 技能表单
  @author Azir
=====================================================-->

<template>
  <div class="skills-form">
    <div class="form-group">
      <label class="form-label">专业技能</label>
      <p class="form-hint">输入技能后按回车添加，点击技能标签可删除</p>
      <div class="skill-input-wrapper">
        <input
          v-model="newSkill"
          type="text"
          class="form-input"
          placeholder="输入技能名称，按回车添加"
          @keyup.enter="addSkill"
        />
        <button class="add-btn" @click="addSkill">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
        </button>
      </div>
    </div>
    <div class="skill-tags">
      <TransitionGroup name="tag">
        <span
          v-for="(skill, index) in localSkills"
          :key="skill"
          class="skill-tag"
          @click="removeSkill(index)"
        >
          {{ skill }}
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </span>
      </TransitionGroup>
      <p v-if="localSkills.length === 0" class="empty-hint">暂无技能，请添加</p>
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

// 本地技能列表
const localSkills = ref<string[]>([])
const newSkill = ref('')

// 初始化数据
function initData(): void {
  const content = props.modelValue as { skills?: string[] }
  localSkills.value = Array.isArray(content?.skills) ? [...content.skills] : []
}

// 监听外部变化
watch(
  () => props.modelValue,
  () => initData(),
  { immediate: true, deep: true }
)

// 同步到父组件
function syncToParent(): void {
  emit('update:modelValue', { skills: [...localSkills.value] })
}

// 添加技能
function addSkill(): void {
  const skill = newSkill.value.trim()
  if (skill && !localSkills.value.includes(skill)) {
    localSkills.value.push(skill)
    newSkill.value = ''
    syncToParent()
  }
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
}

.form-hint {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.skill-input-wrapper {
  display: flex;
  gap: $spacing-sm;
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

.add-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $color-accent-glow;
  color: $color-accent;
  border-radius: $radius-sm;
  transition: all $transition-fast;
  &:hover {
    background: rgba(212, 168, 83, 0.2);
  }
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
  min-height: 40px;
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-sm;
}

.skill-tag {
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-xs $spacing-md;
  font-size: $text-sm;
  color: $color-accent;
  background: $color-accent-glow;
  border-radius: $radius-full;
  cursor: pointer;
  transition: all $transition-fast;
  svg {
    opacity: 0;
    transition: opacity $transition-fast;
  }
  &:hover {
    background: rgba(212, 168, 83, 0.2);
    svg {
      opacity: 1;
    }
  }
}

.empty-hint {
  font-size: $text-sm;
  color: $color-text-tertiary;
}

// 动画
.tag-enter-active,
.tag-leave-active {
  transition: all 0.2s ease;
}
.tag-enter-from,
.tag-leave-to {
  opacity: 0;
  transform: scale(0.8);
}
</style>
