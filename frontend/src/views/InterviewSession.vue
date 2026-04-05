<!--=====================================================
  LandIt 面试进行中页面
  支持语音模式（语音作答/全程对话）
  @author Azir
=====================================================-->

<template>
  <!-- 倒计时遮罩层 -->
  <div v-if="showCountdown" class="countdown-overlay">
    <div class="countdown-content">
      <div class="countdown-title">面试即将开始</div>
      <div class="countdown-number">{{ countdown }}</div>
      <div class="countdown-hint">请准备好麦克风</div>
    </div>
  </div>

  <div class="interview-session-page">
    <div class="session-container">
      <!-- 顶部状态栏 -->
      <header
        class="session-header animate-in"
        style="--delay: 0"
      >
        <div class="header-left">
          <span class="session-badge" :class="sessionBadgeClass">{{ sessionBadgeText }}</span>
          <h2 class="session-title">{{ interviewTitle }}</h2>
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
          v-for="(_q, i) in totalQuestions"
          :key="i"
          class="progress-dot"
          :class="{ active: i === currentQuestionIndex, completed: i < currentQuestionIndex }"
          @click="goToQuestion(i)"
        >
          <span v-if="i < currentQuestionIndex">✓</span>
          <span v-else>{{ i + 1 }}</span>
        </div>
      </div>

      <!-- 主内容区 -->
      <div class="main-content">
        <!-- 对话区域 -->
        <main class="conversation-area animate-in" style="--delay: 2">
          <div class="messages-container" ref="messagesContainer">
            <div
              v-for="(msg, index) in messages"
              :key="msg.id"
              class="message"
              :class="msg.role"
              :style="{ '--index': index }"
            >
              <div class="message-avatar">
                <span v-if="msg.role === 'interviewer'">AI</span>
                <span v-else-if="msg.role === 'assistant'">🤖</span>
                <span v-else>我</span>
              </div>
              <div class="message-content">
                <p class="message-text">{{ msg.content }}</p>
                <span class="message-time">{{ formatMessageTime(msg.timestamp) }}</span>
              </div>
            </div>
            <div v-if="isAIResponding" class="message interviewer typing">
              <div class="message-avatar">AI</div>
              <div class="message-content">
                <div class="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
            <!-- 实时转录显示 -->
            <div v-if="partialTranscript && !partialTranscript.isFinal" class="message partial-transcript">
              <div class="message-avatar">
                <span v-if="partialTranscript.role === 'interviewer'">AI</span>
                <span v-else>我</span>
              </div>
              <div class="message-content">
                <p class="message-text">{{ partialTranscript.text }}<span class="cursor-blink">|</span></p>
              </div>
            </div>
          </div>
        </main>

        <!-- 右侧面板 -->
        <aside class="side-panel animate-in" style="--delay: 3">
          <!-- 语音控制 -->
          <VoiceControls
            v-model="voiceMode"
            :is-recording="isRecording"
            :is-processing="isProcessing"
            :status-text="statusText"
            :status-type="statusType"
            :hide-mode-switch="isInterviewStarted"
            @toggle-recording="toggleRecording"
            @volume-change="handleVolumeChange"
            @mute-change="handleMuteChange"
          />

          <!-- 助手面板（冻结状态时显示） -->
          <AssistantPanel
            v-if="isFrozen"
            :content="assistantContent"
            :is-loading="isAssistLoading"
            :assist-remaining="assistRemaining"
            :assist-limit="assistLimit"
            :audio-chunks="assistantAudioChunks"
            @return="handleResumeInterview"
            @assist="handleAssist"
          />

          <!-- 工具栏（非冻结状态时显示） -->
          <template v-if="!isFrozen">
            <div class="tool-card">
              <h4 class="tool-title">面试提示</h4>
              <div class="tips-list">
                <div class="tip-item">
                  <span class="tip-icon">💡</span>
                  <p class="tip-text">回答时注意结构化，可以先总述再分点说明</p>
                </div>
                <div class="tip-item">
                  <span class="tip-icon">🎯</span>
                  <p class="tip-text">结合实际项目经验会让回答更有说服力</p>
                </div>
              </div>
            </div>

            <!-- 快捷求助按钮 -->
            <QuickAssistButtons
              :remaining="assistRemaining"
              :limit="assistLimit"
              @assist="handleAssist"
            />
          </template>
        </aside>
      </div>
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
          <button class="modal-btn secondary" @click="showEndModal = false">继续面试</button>
          <button class="modal-btn primary" @click="handleEndInterview">确认结束</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import VoiceControls from '@/components/interview/voice/VoiceControls.vue'
import AssistantPanel from '@/components/interview/voice/AssistantPanel.vue'
import QuickAssistButtons from '@/components/interview/voice/QuickAssistButtons.vue'
import { useInterviewVoice } from '@/composables/useInterviewVoice'
import { useStreamAssist } from '@/composables/useStreamAssist'
import type { VoiceMode, AssistType } from '@/types/interview-voice'

const router = useRouter()
const route = useRoute()

// DEBUG: 打印路由参数
console.log('[InterviewSession] route.params:', route.params)
console.log('[InterviewSession] route.params.sessionId:', route.params.sessionId)

// 从路由获取会话ID（路由参数名是 sessionId，不是 id）
const sessionId = computed(() => {
  const id = route.params.sessionId as string || `session_${Date.now()}`
  console.log('[InterviewSession] computed sessionId:', id)
  return id
})

// ============================================================================
// 语音面试相关
// ============================================================================

// 语音模式（默认半语音）
const voiceMode = ref<VoiceMode>('half_voice')

// 语音面试 composable
const voiceInterview = useInterviewVoice(sessionId.value)

// 流式求助 composable
const streamAssist = useStreamAssist(sessionId.value)

// 从 voiceInterview 获取状态
const isRecording = voiceInterview.isRecording
const isFrozen = voiceInterview.isFrozen
const isCompleted = voiceInterview.isCompleted
const currentQuestionIndex = computed(() => voiceInterview.currentQuestion.value)
const totalQuestions = computed(() => voiceInterview.totalQuestions.value)
const elapsedTime = computed(() => voiceInterview.elapsedTime.value)
const messages = voiceInterview.messages
const assistRemaining = voiceInterview.assistRemaining
const assistLimit = voiceInterview.assistLimit
const partialTranscript = voiceInterview.partialTranscript

// 处理状态（基于实时转录状态）
const isProcessing = computed(() => !!partialTranscript.value && !partialTranscript.value.isFinal)
const statusText = computed(() => {
  if (voiceInterview.isPlaying.value) return 'AI 正在说话...'
  if (isRecording.value) return '正在录音...'
  if (isProcessing.value) return '正在识别...'
  if (voiceInterview.error.value) return '连接错误'
  return ''
})
const statusType = computed(() => {
  if (voiceInterview.isPlaying.value) return 'synthesizing'
  if (isRecording.value) return 'recording'
  if (isProcessing.value) return 'recognizing'
  if (voiceInterview.error.value) return 'error'
  return 'idle'
})

// AI 响应状态（与音频播放状态同步）
const isAIResponding = computed(() => voiceInterview.isPlaying.value)

// 面试是否已开始（非 idle 状态时隐藏语音模式切换）
const isInterviewStarted = computed(() => {
  return voiceInterview.sessionState.value !== 'idle'
})

// ============================================================================
// 助手相关
// ============================================================================

const assistantContent = ref('')
const assistantAudioChunks = ref<string[]>([])
const isAssistLoading = ref(false)

// ============================================================================
// UI 状态
// ============================================================================

const showEndModal = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)

// 倒计时相关
const showCountdown = ref(true)
const countdown = ref(3)

// 面试标题
const interviewTitle = computed(() => '技术面试 - 高级前端工程师')

// 会话徽章
const sessionBadgeClass = computed(() => {
  if (isFrozen.value) return 'frozen'
  if (isCompleted.value) return 'completed'
  return 'active'
})

const sessionBadgeText = computed(() => {
  if (isFrozen.value) return '求助中'
  if (isCompleted.value) return '已结束'
  return '面试进行中'
})

// ============================================================================
// 生命周期
// ============================================================================

onMounted(() => {
  // 开始倒计时
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
      showCountdown.value = false
      // 倒计时结束后初始化语音面试
      voiceInterview.init()
    }
  }, 1000)
})

onUnmounted(() => {
  voiceInterview.dispose()
})

// 监听消息变化，自动滚动到底部
watch(() => messages.value.length, () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
})

// ============================================================================
// 录音控制
// ============================================================================

function toggleRecording() {
  // statusText 和 statusType 是 computed 属性，会自动根据 isRecording 状态更新
  if (isRecording.value) {
    voiceInterview.stopRecording()
  } else {
    voiceInterview.startRecording()
  }
}

function handleVolumeChange(volume: number) {
  voiceInterview.setVolume(volume)
}

function handleMuteChange(muted: boolean) {
  if (muted) {
    voiceInterview.toggleMute()
  }
}

// ============================================================================
// 助手相关
// ============================================================================

async function handleAssist(type: AssistType, question?: string) {
  // 冻结面试
  voiceInterview.freeze()
  isAssistLoading.value = true
  assistantContent.value = ''
  assistantAudioChunks.value = []

  try {
    await streamAssist.requestAssist({
      type,
      question
    })
  } catch (e) {
    console.error('Assist error:', e)
  } finally {
    isAssistLoading.value = false
  }
}

// 监听助手内容变化
watch(() => streamAssist.textContent.value, (content) => {
  assistantContent.value = content
})

function handleResumeInterview() {
  voiceInterview.resumeInterview()
  assistantContent.value = ''
  assistantAudioChunks.value = []
}

// ============================================================================
// 面试结束
// ============================================================================

function confirmEnd() {
  showEndModal.value = true
}

function handleEndInterview() {
  voiceInterview.endInterview()
  voiceInterview.dispose()
  router.push('/review')
}

// ============================================================================
// 辅助方法
// ============================================================================

function formatTime(seconds: number): string {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

function formatMessageTime(timestamp: number): string {
  return new Date(timestamp).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function goToQuestion(index: number): void {
  if (index <= currentQuestionIndex.value) {
    // voiceInterview 不支持跳转，这里只是视觉反馈
  }
}
</script>

<style lang="scss" scoped>
// 倒计时遮罩层
.countdown-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.95);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.countdown-content {
  text-align: center;
}

.countdown-title {
  font-family: $font-display;
  font-size: $text-2xl;
  color: $color-text-secondary;
  margin-bottom: $spacing-xl;
}

.countdown-number {
  font-family: $font-display;
  font-size: 120px;
  font-weight: $weight-bold;
  color: $color-accent;
  line-height: 1;
}

.countdown-hint {
  font-size: $text-base;
  color: $color-text-tertiary;
  margin-top: $spacing-xl;
}

@keyframes pulse-scale {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
}

.interview-session-page {
  height: 100vh;
  display: flex;
  background: $color-bg-deep;
}

.session-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  max-width: 1400px;
  margin: 0 auto;
  padding: $spacing-lg;
  gap: $spacing-md;
}

// 头部
.session-header {
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
  font-size: $text-xs;
  font-weight: $weight-medium;
  border-radius: $radius-sm;

  &.active {
    background: rgba(52, 211, 153, 0.15);
    color: $color-success;
    animation: pulse 2s ease-in-out infinite;
  }

  &.frozen {
    background: rgba(251, 191, 36, 0.15);
    color: $color-warning;
  }

  &.completed {
    background: rgba(96, 165, 250, 0.15);
    color: $color-info;
  }
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

// 主内容区
.main-content {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: $spacing-md;
  min-height: 0;
}

// 对话区域
.conversation-area {
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
  &.assistant {
    align-self: flex-start;
    .message-avatar {
      background: rgba(52, 211, 153, 0.2);
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

// 侧边面板
.side-panel {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
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

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

// 实时转录样式
.partial-transcript {
  opacity: 0.7;
  .message-text {
    background: rgba(212, 168, 83, 0.1);
    border: 1px dashed rgba(212, 168, 83, 0.3);
  }
}

.cursor-blink {
  animation: blink 1s step-end infinite;
  color: $color-accent;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
