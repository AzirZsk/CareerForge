<!--=====================================================
  LandIt 面试进行中页面
  @author Azir
=====================================================-->

<template>
  <div class="interview-session-page">
    <div class="session-container">
      <!-- 顶部状态栏 -->
      <header
        class="session-header animate-in"
        style="--delay: 0"
      >
        <div class="header-left">
          <span class="session-badge">面试进行中</span>
          <h2 class="session-title">
            技术面试 - 高级前端工程师
          </h2>
        </div>
        <div class="header-right">
          <div class="timer">
            <svg
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <circle
                cx="12"
                cy="12"
                r="10"
              />
              <polyline points="12 6 12 12 16 14" />
            </svg>
            <span class="timer-value">{{ formatTime(elapsedTime) }}</span>
          </div>
          <div class="progress-indicator">
            <span class="progress-current">{{ currentQuestionIndex + 1 }}</span>
            <span class="progress-sep">/</span>
            <span class="progress-total">{{ totalQuestions }}</span>
          </div>
          <button
            class="end-btn"
            @click="confirmEnd"
          >
            结束面试
          </button>
        </div>
      </header>

      <!-- 进度条 -->
      <div
        class="progress-bar animate-in"
        style="--delay: 1"
      >
        <div
          v-for="(_q, i) in questions"
          :key="i"
          class="progress-dot"
          :class="{ active: i === currentQuestionIndex, completed: i < currentQuestionIndex }"
          @click="goToQuestion(i)"
        >
          <span v-if="i < currentQuestionIndex">✓</span>
          <span v-else>{{ i + 1 }}</span>
        </div>
      </div>

      <!-- 对话区域 -->
      <main
        class="conversation-area animate-in"
        style="--delay: 2"
      >
        <div
          ref="messagesContainer"
          class="messages-container"
        >
          <div
            v-for="(msg, index) in messages"
            :key="msg.id"
            class="message"
            :class="msg.role"
            :style="{ '--index': index }"
          >
            <div class="message-avatar">
              <span v-if="msg.role === 'interviewer'">AI</span>
              <span v-else>我</span>
            </div>
            <div class="message-content">
              <p class="message-text">
                {{ msg.content }}
              </p>
              <span class="message-time">{{ formatMessageTime(msg.timestamp) }}</span>
              <div
                v-if="msg.score"
                class="message-feedback"
              >
                <span class="feedback-score">{{ msg.score }}分</span>
                <span class="feedback-text">{{ msg.feedback }}</span>
              </div>
            </div>
          </div>
          <div
            v-if="isAIResponding"
            class="message interviewer typing"
          >
            <div class="message-avatar">
              AI
            </div>
            <div class="message-content">
              <div class="typing-indicator">
                <span />
                <span />
                <span />
              </div>
            </div>
          </div>
        </div>
      </main>

      <!-- 输入区域 -->
      <footer
        class="input-area animate-in"
        style="--delay: 3"
      >
        <div class="input-container">
          <textarea
            v-model="userInput"
            placeholder="输入你的回答..."
            class="answer-input"
            :disabled="isAIResponding"
            rows="3"
            @keydown.enter.exact.prevent="submitAnswer"
          />
          <div class="input-actions">
            <div class="action-hints">
              <span class="hint">按 Enter 发送</span>
              <span class="hint">Shift + Enter 换行</span>
            </div>
            <button
              class="submit-btn"
              :disabled="!userInput.trim() || isAIResponding"
              @click="submitAnswer"
            >
              <svg
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <line
                  x1="22"
                  y1="2"
                  x2="11"
                  y2="13"
                />
                <polygon points="22 2 15 22 11 13 2 9 22 2" />
              </svg>
              发送回答
            </button>
          </div>
        </div>
      </footer>

      <!-- 工具栏 -->
      <aside
        class="tools-panel animate-in"
        style="--delay: 4"
      >
        <div class="tool-card">
          <h4 class="tool-title">
            面试提示
          </h4>
          <div class="tips-list">
            <div class="tip-item">
              <span class="tip-icon">💡</span>
              <p class="tip-text">
                回答时注意结构化，可以先总述再分点说明
              </p>
            </div>
            <div class="tip-item">
              <span class="tip-icon">🎯</span>
              <p class="tip-text">
                结合实际项目经验会让回答更有说服力
              </p>
            </div>
            <div class="tip-item">
              <span class="tip-icon">⏱️</span>
              <p class="tip-text">
                遇到不会的问题可以诚实说明，展示学习态度
              </p>
            </div>
          </div>
        </div>
        <div class="tool-card">
          <h4 class="tool-title">
            当前问题要点
          </h4>
          <ul class="key-points-list">
            <li
              v-for="point in currentKeyPoints"
              :key="point"
            >
              <span class="point-check" />
              {{ point }}
            </li>
          </ul>
        </div>
        <button
          class="hint-btn"
          @click="requestHint"
        >
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <circle
              cx="12"
              cy="12"
              r="10"
            />
            <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" />
            <line
              x1="12"
              y1="17"
              x2="12.01"
              y2="17"
            />
          </svg>
          请求提示
        </button>
      </aside>
    </div>

    <!-- 结束确认弹窗 -->
    <div
      v-if="showEndModal"
      class="modal-overlay"
      @click.self="showEndModal = false"
    >
      <div class="modal-content">
        <h3 class="modal-title">
          确认结束面试？
        </h3>
        <p class="modal-desc">
          当前面试尚未完成，结束后将根据已回答的问题生成复盘报告。
        </p>
        <div class="modal-actions">
          <button
            class="modal-btn secondary"
            @click="showEndModal = false"
          >
            继续面试
          </button>
          <button
            class="modal-btn primary"
            @click="endInterview"
          >
            确认结束
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import type { SessionQuestion, SessionMessage } from '@/types'

const router = useRouter()

const userInput = ref<string>('')
const elapsedTime = ref<number>(0)
const currentQuestionIndex = ref<number>(0)
const isAIResponding = ref<boolean>(false)
const showEndModal = ref<boolean>(false)
const messagesContainer = ref<HTMLElement | null>(null)

const totalQuestions: number = 10

const questions = ref<SessionQuestion[]>([
  { id: '1', category: 'Vue.js', question: '请解释 Vue 3 的响应式原理', keyPoints: ['Proxy', 'Reflect', '依赖收集', '触发更新'] },
  { id: '2', category: 'JavaScript', question: '请解释事件循环机制', keyPoints: ['调用栈', '任务队列', '宏任务', '微任务'] },
  { id: '3', category: '性能优化', question: '如何优化大型单页应用的首次加载', keyPoints: ['代码分割', '懒加载', '缓存', 'CDN'] }
])

const messages = ref<SessionMessage[]>([
  {
    id: '1',
    role: 'interviewer',
    content: '你好，我是今天的面试官。我们将进行一场前端技术面试，主要考察Vue.js、JavaScript基础和性能优化方面的知识。准备好了吗？',
    timestamp: new Date(Date.now() - 60000)
  },
  {
    id: '2',
    role: 'candidate',
    content: '准备好了，请开始吧！',
    timestamp: new Date(Date.now() - 55000)
  },
  {
    id: '3',
    role: 'interviewer',
    content: '好的，首先请你介绍一下自己，重点说说你的技术背景和项目经验。',
    timestamp: new Date(Date.now() - 50000)
  },
  {
    id: '4',
    role: 'candidate',
    content: '我叫Azir，有5年前端开发经验，目前在字节跳动负责抖音创作者平台的前端架构工作。主要技术栈是Vue和React，对性能优化和工程化有深入研究。',
    timestamp: new Date(Date.now() - 40000),
    score: 90,
    feedback: '自我介绍清晰，技术重点突出'
  }
])

const currentKeyPoints = computed<string[]>(() => {
  return questions.value[currentQuestionIndex.value]?.keyPoints || []
})

let timer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  timer = setInterval(() => {
    elapsedTime.value++
  }, 1000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})

function formatTime(seconds: number): string {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

function formatMessageTime(date: Date): string {
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

async function submitAnswer(): Promise<void> {
  if (!userInput.value.trim() || isAIResponding.value) return
  messages.value.push({
    id: String(messages.value.length + 1),
    role: 'candidate',
    content: userInput.value,
    timestamp: new Date()
  })
  userInput.value = ''
  await nextTick()
  scrollToBottom()
  isAIResponding.value = true
  setTimeout(() => {
    isAIResponding.value = false
    const nextQuestion = questions.value[currentQuestionIndex.value + 1]
    messages.value.push({
      id: String(messages.value.length + 1),
      role: 'interviewer',
      content: '好的回答。那我们继续下一个问题：' + (nextQuestion?.question || ''),
      timestamp: new Date()
    })
    currentQuestionIndex.value++
    scrollToBottom()
  }, 2000)
}

function scrollToBottom(): void {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

function goToQuestion(index: number): void {
  if (index <= currentQuestionIndex.value) {
    currentQuestionIndex.value = index
  }
}

function confirmEnd(): void {
  showEndModal.value = true
}

function endInterview(): void {
  if (timer) {
    clearInterval(timer)
  }
  router.push('/review')
}

function requestHint(): void {
  messages.value.push({
    id: String(messages.value.length + 1),
    role: 'interviewer',
    content: '提示：在回答时可以从以下角度考虑 - ' + currentKeyPoints.value.join('、'),
    timestamp: new Date()
  })
  scrollToBottom()
}
</script>

<style lang="scss" scoped>
.interview-session-page {
  height: 100vh;
  display: flex;
  background: $color-bg-deep;
}

.session-container {
  flex: 1;
  display: grid;
  grid-template-rows: auto auto 1fr auto;
  grid-template-columns: 1fr 280px;
  gap: 0;
  max-width: 1400px;
  margin: 0 auto;
  padding: $spacing-lg;
}

// 头部
.session-header {
  grid-column: 1 / -1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md $spacing-lg;
  background: $gradient-card;
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.session-badge {
  padding: $spacing-xs $spacing-sm;
  background: rgba(52, 211, 153, 0.15);
  color: $color-success;
  font-size: $text-xs;
  font-weight: $weight-medium;
  border-radius: $radius-sm;
  animation: pulse 2s ease-in-out infinite;
}

.session-title {
  font-size: $text-lg;
  font-weight: $weight-medium;
  color: $color-text-primary;
  margin-left: $spacing-md;
}

.header-right {
  display: flex;
  align-items: center;
  gap: $spacing-xl;
}

.timer {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  color: $color-text-secondary;
}

.timer-value {
  font-family: $font-display;
  font-size: $text-lg;
  font-weight: $weight-semibold;
  color: $color-accent;
}

.progress-indicator {
  font-family: $font-display;
  font-size: $text-lg;
  .progress-current {
    color: $color-accent;
    font-weight: $weight-semibold;
  }
  .progress-sep, .progress-total {
    color: $color-text-tertiary;
  }
}

.end-btn {
  padding: $spacing-sm $spacing-lg;
  background: rgba(248, 113, 113, 0.15);
  color: $color-error;
  font-size: $text-sm;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  transition: all $transition-fast;
  &:hover {
    background: rgba(248, 113, 113, 0.25);
  }
}

// 进度条
.progress-bar {
  grid-column: 1 / -1;
  display: flex;
  justify-content: center;
  gap: $spacing-sm;
  padding: $spacing-md;
  background: $gradient-card;
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.progress-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $text-sm;
  color: $color-text-tertiary;
  cursor: pointer;
  transition: all $transition-fast;
  &.active {
    background: $gradient-gold;
    color: $color-bg-deep;
    font-weight: $weight-semibold;
    transform: scale(1.1);
  }
  &.completed {
    background: rgba(52, 211, 153, 0.2);
    color: $color-success;
  }
  &:hover:not(.active) {
    background: rgba(255, 255, 255, 0.15);
  }
}

// 对话区域
.conversation-area {
  grid-column: 1;
  background: $gradient-card;
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-lg;
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.message {
  display: flex;
  gap: $spacing-md;
  max-width: 80%;
  animation: slideUp 0.3s ease forwards;
  animation-delay: calc(var(--index) * 0.05s);
  opacity: 0;
  &.interviewer {
    align-self: flex-start;
    .message-avatar {
      background: $gradient-gold;
      color: $color-bg-deep;
    }
  }
  &.candidate {
    align-self: flex-end;
    flex-direction: row-reverse;
    .message-avatar {
      background: rgba(96, 165, 250, 0.2);
      color: #60a5fa;
    }
    .message-content {
      align-items: flex-end;
    }
  }
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $text-sm;
  font-weight: $weight-semibold;
  flex-shrink: 0;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.message-text {
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.05);
  border-radius: $radius-md;
  font-size: $text-sm;
  color: $color-text-primary;
  line-height: $leading-relaxed;
}

.message-time {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.message-feedback {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-xs $spacing-sm;
  background: $color-success-bg;
  border-radius: $radius-sm;
}

.feedback-score {
  font-size: $text-xs;
  font-weight: $weight-semibold;
  color: $color-success;
}

.feedback-text {
  font-size: $text-xs;
  color: $color-success;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.05);
  border-radius: $radius-md;
  span {
    width: 8px;
    height: 8px;
    background: $color-accent;
    border-radius: 50%;
    animation: bounce 1.4s ease-in-out infinite;
    &:nth-child(1) { animation-delay: 0s; }
    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-8px); }
}

// 输入区域
.input-area {
  grid-column: 1;
  background: $gradient-card;
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
  padding: $spacing-lg;
}

.input-container {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.answer-input {
  width: 100%;
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-md;
  color: $color-text-primary;
  font-size: $text-sm;
  resize: none;
  font-family: $font-body;
  &:focus {
    outline: none;
    border-color: $color-accent;
  }
  &::placeholder {
    color: $color-text-tertiary;
  }
  &:disabled {
    opacity: 0.5;
  }
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-hints {
  display: flex;
  gap: $spacing-md;
}

.hint {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.submit-btn {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-lg;
  background: $gradient-gold;
  color: $color-bg-deep;
  font-size: $text-sm;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  transition: all $transition-fast;
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  &:not(:disabled):hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(212, 168, 83, 0.3);
  }
}

// 工具栏
.tools-panel {
  grid-row: 3 / 5;
  grid-column: 2;
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
  padding-left: $spacing-lg;
}

.tool-card {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.tool-title {
  font-size: $text-sm;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-md;
}

.tips-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.tip-item {
  display: flex;
  gap: $spacing-sm;
}

.tip-icon {
  font-size: $text-base;
}

.tip-text {
  font-size: $text-xs;
  color: $color-text-secondary;
  line-height: $leading-relaxed;
}

.key-points-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  li {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-size: $text-sm;
    color: $color-text-secondary;
  }
}

.point-check {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: $radius-sm;
}

.hint-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.05);
  color: $color-text-secondary;
  font-size: $text-sm;
  border-radius: $radius-md;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all $transition-fast;
  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: $color-accent;
    border-color: rgba(212, 168, 83, 0.3);
  }
}

// 弹窗
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: $z-modal;
  animation: fadeIn 0.2s ease;
}

.modal-content {
  background: $color-bg-secondary;
  border-radius: $radius-xl;
  padding: $spacing-2xl;
  max-width: 400px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.modal-title {
  font-family: $font-display;
  font-size: $text-xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-md;
}

.modal-desc {
  font-size: $text-sm;
  color: $color-text-secondary;
  line-height: $leading-relaxed;
  margin-bottom: $spacing-xl;
}

.modal-actions {
  display: flex;
  gap: $spacing-md;
}

.modal-btn {
  flex: 1;
  padding: $spacing-md;
  font-size: $text-sm;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  transition: all $transition-fast;
  &.primary {
    background: $gradient-gold;
    color: $color-bg-deep;
    &:hover {
      transform: translateY(-2px);
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

// 动画
.animate-in {
  animation: slideUp 0.5s ease forwards;
  animation-delay: calc(var(--delay) * 0.1s);
  opacity: 0;
}
</style>
