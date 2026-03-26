<template>
  <div class="assistant-panel">
    <!-- 面板头部 -->
    <div class="panel-header">
      <div class="header-left">
        <span class="icon">🤖</span>
        <span class="title">AI 助手</span>
      </div>
      <button class="close-btn" @click="handleReturn">返回面试</button>
    </div>

    <!-- 助手回复内容 -->
    <div class="panel-content">
      <!-- 加载状态 -->
      <div v-if="isLoading" class="loading-state">
        <div class="spinner"></div>
        <p>正在思考中...</p>
      </div>

      <!-- 回复内容 -->
      <div v-else-if="content" class="response-content">
        <div class="text-content" v-html="formattedContent"></div>

        <!-- 音频播放器 -->
        <div v-if="audioQueue.length > 0" class="audio-player">
          <button
            class="play-btn"
            :class="{ playing: isPlaying }"
            @click="togglePlay"
          >
            {{ isPlaying ? '⏸️' : '▶️' }}
          </button>
          <div class="progress-bar">
            <div class="progress" :style="{ width: playProgress + '%' }"></div>
          </div>
          <span class="duration">{{ formatDuration(currentTime) }} / {{ formatDuration(totalDuration) }}</span>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <span class="empty-icon">💬</span>
        <p>助手回复将在这里显示...</p>
      </div>
    </div>

    <!-- 继续求助按钮 -->
    <div class="panel-footer">
      <QuickAssistButtons
        :remaining="assistRemaining"
        :limit="assistLimit"
        @assist="handleAssist"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import QuickAssistButtons from './QuickAssistButtons.vue'
import type { AssistType } from '@/types/interview-voice'

// Props
interface Props {
  content?: string
  isLoading?: boolean
  assistRemaining?: number
  assistLimit?: number
}

const props = withDefaults(defineProps<Props>(), {
  content: '',
  isLoading: false,
  assistRemaining: 5,
  assistLimit: 5
})

// Emits
const emit = defineEmits<{
  return: []
  assist: [type: AssistType, question?: string]
}>()

// 音频播放状态
const audioQueue = ref<ArrayBuffer[]>([])
const isPlaying = ref(false)
const currentTime = ref(0)
const totalDuration = ref(0)

// 播放进度
const playProgress = computed(() => {
  if (totalDuration.value === 0) return 0
  return (currentTime.value / totalDuration.value) * 100
})

// 格式化内容（支持 Markdown）
const formattedContent = computed(() => {
  if (!props.content) return ''
  // 简单的换行处理，实际可使用 markdown 解析器
  return props.content
    .split('\n')
    .map(line => `<p>${line}</p>`)
    .join('')
})

// 返回面试
function handleReturn() {
  emit('return')
}

// 处理求助
function handleAssist(type: AssistType, question?: string) {
  emit('assist', type, question)
}

// 切换播放
function togglePlay() {
  isPlaying.value = !isPlaying.value
  // TODO: 实现音频播放逻辑
}

// 格式化时长
function formatDuration(seconds: number): string {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins}:${secs.toString().padStart(2, '0')}`
}
</script>

<style scoped lang="scss">
.assistant-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg-primary);
  border-radius: 12px;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);

  .header-left {
    display: flex;
    align-items: center;
    gap: 8px;

    .icon {
      font-size: 20px;
    }

    .title {
      font-weight: 600;
      font-size: 16px;
    }
  }

  .close-btn {
    padding: 8px 16px;
    background: var(--primary-color);
    color: white;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    font-size: 14px;

    &:hover {
      opacity: 0.9;
    }
  }
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;

  .loading-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    gap: 16px;

    .spinner {
      width: 40px;
      height: 40px;
      border: 3px solid var(--border-color);
      border-top-color: var(--primary-color);
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }
  }

  .response-content {
    .text-content {
      line-height: 1.8;
      color: var(--text-primary);

      p {
        margin-bottom: 12px;
      }
    }

    .audio-player {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-top: 16px;
      padding: 12px;
      background: var(--bg-secondary);
      border-radius: 8px;

      .play-btn {
        width: 40px;
        height: 40px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--primary-color);
        color: white;
        border: none;
        border-radius: 50%;
        cursor: pointer;
        font-size: 16px;

        &.playing {
          background: var(--danger-color);
        }
      }

      .progress-bar {
        flex: 1;
        height: 4px;
        background: var(--border-color);
        border-radius: 2px;
        overflow: hidden;

        .progress {
          height: 100%;
          background: var(--primary-color);
          transition: width 0.1s;
        }
      }

      .duration {
        font-size: 12px;
        color: var(--text-secondary);
        min-width: 80px;
        text-align: right;
      }
    }
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    gap: 12px;
    color: var(--text-secondary);

    .empty-icon {
      font-size: 48px;
      opacity: 0.5;
    }
  }
}

.panel-footer {
  padding: 16px;
  border-top: 1px solid var(--border-color);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
