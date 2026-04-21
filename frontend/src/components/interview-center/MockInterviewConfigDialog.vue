<template>
  <Teleport to="body">
    <div class="dialog-overlay" @click.self="$emit('close')">
      <div class="dialog-content">
        <header class="dialog-header">
          <h2>模拟面试配置</h2>
          <button class="close-btn" @click="$emit('close')">×</button>
        </header>

        <form class="dialog-form" @submit.prevent="handleSubmit">
          <div class="form-group">
            <label class="form-label">题目数量</label>
            <select v-model="config.totalQuestions" class="form-select">
              <option :value="5">5 题</option>
              <option :value="8">8 题</option>
              <option :value="10">10 题</option>
              <option :value="12">12 题</option>
              <option :value="15">15 题</option>
              <option :value="20">20 题</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">求助次数</label>
            <select v-model="config.assistLimit" class="form-select">
              <option :value="3">3 次</option>
              <option :value="5">5 次</option>
              <option :value="7">7 次</option>
              <option :value="10">10 次</option>
            </select>
            <p class="form-hint">
              面试中可向 AI 助手求助的次数，用于获取提示、解释概念或润色回答
            </p>
          </div>

          <div class="form-group">
            <label class="form-label">语音模式</label>
            <div class="mode-switch">
              <button
                type="button"
                :class="['mode-btn', { active: config.voiceMode === 'half_voice' }]"
                @click="config.voiceMode = 'half_voice'"
              >
                语音作答
              </button>
              <button
                type="button"
                :class="['mode-btn', { active: config.voiceMode === 'full_voice' }]"
                @click="config.voiceMode = 'full_voice'"
              >
                全程对话
              </button>
            </div>
            <p class="form-hint">
              {{ config.voiceMode === 'half_voice' ? '你说语音，AI 回文字' : '全程语音对话，沉浸式面试体验' }}
            </p>
          </div>

          <div class="form-group">
            <label class="form-label">面试官风格</label>
            <div class="style-cards">
              <button
                v-for="style in interviewerStyles"
                :key="style.value"
                type="button"
                :class="['style-card', { active: config.interviewerStyle === style.value }]"
                @click="config.interviewerStyle = style.value"
              >
                <div class="style-icon">
                  <font-awesome-icon :icon="style.icon" />
                </div>
                <div class="style-name">{{ style.label }}</div>
                <div class="style-desc">{{ style.desc }}</div>
              </button>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">面试问题</label>
            <div class="mode-switch">
              <button
                type="button"
                :class="['mode-btn', { active: config.regenerateQuestions }]"
                @click="config.regenerateQuestions = true"
              >
                重新生成
              </button>
              <button
                type="button"
                :class="['mode-btn', { active: !config.regenerateQuestions }]"
                @click="config.regenerateQuestions = false"
              >
                复用上次
              </button>
            </div>
            <p class="form-hint">
              {{ config.regenerateQuestions
                ? '基于简历和 JD 重新生成全新的面试问题'
                : '复用同一面试上次生成的问题'
              }}
            </p>
          </div>
        </form>

        <footer class="dialog-footer">
          <button type="button" class="btn btn-secondary" @click="$emit('close')">取消</button>
          <button
            type="button"
            class="btn btn-primary"
            @click="handleSubmit"
          >
            开始面试
          </button>
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { reactive, onMounted, onUnmounted } from 'vue'
import { useScrollLock } from '@vueuse/core'

export interface MockInterviewConfig {
  totalQuestions: number
  assistLimit: number
  voiceMode: string
  interviewerStyle: string
  regenerateQuestions: boolean
}

const emit = defineEmits<{
  close: []
  submit: [config: MockInterviewConfig]
}>()

const isScrollLocked = useScrollLock(document.body)

const config = reactive<MockInterviewConfig>({
  totalQuestions: 10,
  assistLimit: 5,
  voiceMode: 'half_voice',
  interviewerStyle: 'professional',
  regenerateQuestions: false
})

const interviewerStyles = [
  {
    value: 'professional',
    label: '专业严肃型',
    icon: 'fa-solid fa-bullseye',
    desc: '像大厂面试官，严谨专业，追问深入'
  },
  {
    value: 'friendly',
    label: '亲和引导型',
    icon: 'fa-solid fa-mug-hot',
    desc: '像导师面试，温和引导，给予鼓励'
  },
  {
    value: 'challenging',
    label: '压力挑战型',
    icon: 'fa-solid fa-fire',
    desc: '像压力面试，刻意追问，测试应变'
  }
]

function handleSubmit() {
  emit('submit', { ...config })
}

onMounted(() => {
  isScrollLocked.value = true
})

onUnmounted(() => {
  isScrollLocked.value = false
})
</script>

<style scoped lang="scss">
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: $z-modal-overlay;
}

.dialog-content {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  width: 90%;
  max-width: 520px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-lg;
  border-bottom: 1px solid $color-bg-tertiary;
  flex-shrink: 0;

  h2 {
    font-size: 1.25rem;
    font-weight: 600;
    color: $color-text-primary;
  }

  .close-btn {
    background: none;
    border: none;
    color: $color-text-tertiary;
    font-size: 1.5rem;
    cursor: pointer;
    line-height: 1;

    &:hover {
      color: $color-text-primary;
    }
  }
}

.dialog-form {
  padding: $spacing-lg;
  flex: 1;
  overflow-y: auto;
}

.form-group {
  margin-bottom: $spacing-lg;

  &:last-child {
    margin-bottom: 0;
  }
}

.form-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: $color-text-secondary;
  margin-bottom: $spacing-sm;
}

.form-select {
  width: 100%;
  padding: $spacing-md;
  background: $color-bg-tertiary;
  border: 1px solid $color-bg-tertiary;
  border-radius: $radius-md;
  color: $color-text-primary;
  font-size: 0.875rem;
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2371717a' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 32px;
  transition: border-color 0.2s;

  &:focus {
    outline: none;
    border-color: $color-accent;
  }
}

.mode-switch {
  display: flex;
  gap: $spacing-sm;
  background: $color-bg-tertiary;
  padding: 4px;
  border-radius: $radius-md;
}

.mode-btn {
  flex: 1;
  padding: $spacing-sm $spacing-md;
  border: none;
  background: transparent;
  color: $color-text-secondary;
  font-size: 0.875rem;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: all 0.2s;

  &.active {
    background: $color-accent;
    color: $color-bg-primary;
  }

  &:hover:not(.active) {
    color: $color-text-primary;
  }
}

.form-hint {
  margin-top: $spacing-xs;
  font-size: 0.75rem;
  color: $color-text-tertiary;
  line-height: 1.4;
}

.style-cards {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.style-card {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-md;
  background: $color-bg-tertiary;
  border: 2px solid transparent;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;

  &:hover {
    border-color: $color-bg-elevated;
  }

  &.active {
    border-color: $color-accent;
    background: rgba($color-accent, 0.1);
  }
}

.style-icon {
  font-size: 1.5rem;
  flex-shrink: 0;
}

.style-name {
  font-size: 0.9375rem;
  font-weight: 500;
  color: $color-text-primary;
  margin-bottom: 2px;
}

.style-desc {
  font-size: 0.75rem;
  color: $color-text-tertiary;
  line-height: 1.3;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: $spacing-md;
  padding: $spacing-lg;
  border-top: 1px solid $color-bg-tertiary;
  flex-shrink: 0;
}
</style>
