<!--=====================================================
  AI聊天窗口主容器组件
  包含头部、消息列表、输入区域
  @author Azir
=====================================================-->

<template>
  <Teleport to="body">
    <Transition name="slide-up">
      <div v-if="open" class="chat-window-overlay" @click.self="handleClose">
        <div class="chat-window">
          <!-- 头部 -->
          <ChatHeader
            :resume-list="state.resumeList"
            :current-resume-id="state.currentResumeId"
            @resume-change="handleResumeChange"
            @close="handleClose"
          />

          <!-- 消息列表 -->
          <ChatMessageList
            :messages="state.messages"
            :is-streaming="state.isStreaming"
          />

          <!-- 快捷指令 -->
          <QuickCommands
            v-if="state.messages.length <= 1"
            @select="handleQuickCommand"
          />

          <!-- 输入区域 -->
          <ChatInputArea
            v-model="state.currentInput"
            :selected-image="state.selectedImage"
            :is-streaming="state.isStreaming"
            @send="sendMessage"
            @image-select="handleImageSelect"
            @image-remove="handleImageRemove"
          />

          <!-- 修改确认对话框 -->
          <ApplyChangesDialog
            v-model:visible="state.showApplyDialog"
            :changes="state.pendingChanges"
            @confirm="handleApplyChanges"
            @regenerate="handleRegenerate"
            @cancel="handleCancelChanges"
          />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useAIChat } from '@/composables/useAIChat'
import ChatHeader from './ChatHeader.vue'
import ChatMessageList from './ChatMessageList.vue'
import QuickCommands from './QuickCommands.vue'
import ChatInputArea from './ChatInputArea.vue'
import ApplyChangesDialog from './ApplyChangesDialog.vue'

interface Props {
  open: boolean
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'close'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) {
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
  },
  { immediate: true }
)

const {
  state,
  sendMessage,
  handleResumeChange,
  handleQuickCommand,
  handleApplyChanges,
  handleRegenerate,
  handleCancelChanges,
  handleImageSelect,
  handleImageRemove
} = useAIChat()

function handleClose() {
  emit('update:open', false)
  emit('close')
}
</script>

<style lang="scss" scoped>
.chat-window-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  overscroll-behavior: contain;
}

.chat-window {
  width: 90%;
  max-width: 600px;
  height: 80vh;
  max-height: 700px;
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;

  .chat-window {
    transition: all 0.3s ease;
  }
}

.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;

  .chat-window {
    transform: translateY(20px) scale(0.95);
    opacity: 0;
  }
}
</style>
