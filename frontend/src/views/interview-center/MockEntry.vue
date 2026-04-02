<template>
  <div class="mock-entry-page">
    <header class="page-header">
      <button class="back-btn" @click="goBack">← 返回</button>
      <h1>模拟面试</h1>
    </header>

    <div class="entry-content">
      <section class="intro-section">
        <div class="intro-icon">🎤</div>
        <h2>AI 模拟面试</h2>
        <p class="intro-desc">
          通过 AI 进行模拟面试练习，帮助你提升面试技巧和自信心。
          支持多种面试模式和题目类型。
        </p>
      </section>

      <section class="options-section">
        <h3>面试设置</h3>

        <div class="form-group">
          <label class="form-label">面试类型</label>
          <div class="option-cards">
            <div
              v-for="type in interviewTypes"
              :key="type.value"
              class="option-card"
              :class="{ active: form.type === type.value }"
              @click="form.type = type.value"
            >
              <span class="option-icon">{{ type.icon }}</span>
              <span class="option-label">{{ type.label }}</span>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">语音模式</label>
          <div class="option-cards">
            <div
              v-for="mode in voiceModes"
              :key="mode.value"
              class="option-card"
              :class="{ active: form.voiceMode === mode.value }"
              @click="form.voiceMode = mode.value"
            >
              <span class="option-icon">{{ mode.icon }}</span>
              <div class="option-content">
                <span class="option-label">{{ mode.label }}</span>
                <span class="option-desc">{{ mode.desc }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">目标岗位（可选）</label>
          <input
            v-model="form.position"
            type="text"
            class="form-input"
            placeholder="如：Java高级工程师"
          />
        </div>
      </section>

      <div class="action-section">
        <button
          class="btn btn-primary btn-lg"
          :disabled="starting"
          @click="startMockInterview"
        >
          {{ starting ? '准备中...' : '开始面试' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const toast = useToast()
const starting = ref(false)

const form = reactive({
  type: 'technical',
  voiceMode: 'text',
  position: ''
})

const interviewTypes = [
  { value: 'technical', label: '技术面试', icon: '💻' },
  { value: 'behavioral', label: '行为面试', icon: '🤝' }
]

const voiceModes = [
  { value: 'text', label: '文字模式', icon: '⌨️', desc: '纯文字交流' },
  { value: 'half', label: '半语音', icon: '🎤', desc: '语音提问，文字回答' },
  { value: 'full', label: '全语音', icon: '🎙️', desc: '全程语音交互' }
]

function goBack() {
  router.push('/interview-center')
}

async function startMockInterview() {
  if (starting.value) return

  starting.value = true
  try {
    // 调用现有的模拟面试开始接口
    const response = await fetch('/landit/interview/sessions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        type: form.type,
        position: form.position || '通用岗位',
        voiceMode: form.voiceMode
      })
    })

    const result = await response.json()
    if (result.code === 200 && result.data?.sessionId) {
      // 跳转到面试页面
      router.push(`/interview-center/mock/${result.data.sessionId}`)
    } else {
      toast.error('启动面试失败，请稍后重试')
    }
  } catch (error) {
    console.error('启动模拟面试失败:', error)
    toast.error('启动失败，请稍后重试')
  } finally {
    starting.value = false
  }
}
</script>

<style scoped lang="scss">
.mock-entry-page {
  padding: $spacing-xl;
  max-width: 700px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: $spacing-lg;
  margin-bottom: $spacing-2xl;

  h1 {
    font-size: 1.5rem;
    font-weight: 700;
    color: $color-text-primary;
  }
}

.back-btn {
  background: none;
  border: none;
  color: $color-text-secondary;
  cursor: pointer;
  font-size: 1rem;

  &:hover {
    color: $color-text-primary;
  }
}

.entry-content {
  display: flex;
  flex-direction: column;
  gap: $spacing-2xl;
}

.intro-section {
  text-align: center;
  padding: $spacing-2xl;
  background: $color-bg-secondary;
  border-radius: $radius-lg;

  .intro-icon {
    font-size: 4rem;
    margin-bottom: $spacing-lg;
  }

  h2 {
    font-size: 1.5rem;
    font-weight: 700;
    color: $color-text-primary;
    margin-bottom: $spacing-md;
  }

  .intro-desc {
    color: $color-text-secondary;
    line-height: 1.6;
  }
}

.options-section {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  padding: $spacing-xl;

  h3 {
    font-size: 1.125rem;
    font-weight: 600;
    color: $color-text-primary;
    margin-bottom: $spacing-xl;
  }
}

.form-group {
  margin-bottom: $spacing-xl;

  &:last-child {
    margin-bottom: 0;
  }
}

.form-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: $color-text-secondary;
  margin-bottom: $spacing-md;
}

.option-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: $spacing-md;
}

.option-card {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-lg;
  background: $color-bg-tertiary;
  border: 2px solid transparent;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: $color-accent;
  }

  &.active {
    border-color: $color-accent;
    background: rgba($color-accent, 0.1);
  }

  .option-icon {
    font-size: 1.5rem;
  }

  .option-label {
    font-size: 0.875rem;
    font-weight: 500;
    color: $color-text-primary;
  }

  .option-content {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .option-desc {
    font-size: 0.75rem;
    color: $color-text-tertiary;
  }
}

.form-input {
  width: 100%;
  padding: $spacing-md;
  background: $color-bg-tertiary;
  border: 1px solid $color-bg-tertiary;
  border-radius: $radius-md;
  color: $color-text-primary;
  font-size: 0.875rem;
  transition: border-color 0.2s;

  &:focus {
    outline: none;
    border-color: $color-accent;
  }

  &::placeholder {
    color: $color-text-tertiary;
  }
}

.action-section {
  text-align: center;
}

.btn-lg {
  padding: $spacing-lg $spacing-2xl;
  font-size: 1.125rem;
}
</style>
