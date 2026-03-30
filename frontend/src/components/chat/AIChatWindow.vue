<!--=====================================================
  AI聊天窗口主容器组件
  包含头部、消息列表、输入区域
  @author Azir
=====================================================-->

<template>
  <Teleport to="body">
    <Transition name="fullscreen-slide">
      <div
        v-if="open"
        class="chat-fullscreen"
      >
        <div class="chat-container">
          <!-- 头部 -->
          <ChatHeader
            :detected-resume="state.detectedResume"
            @close="handleClose"
            @new-session="startNewSession"
          />

          <!-- 主内容区 -->
          <div class="chat-main">
            <!-- 消息列表 -->
            <ChatMessageList
              :messages="state.messages"
              :is-streaming="state.isStreaming"
              @preview-image="handleImagePreview"
              @apply-single-change="applySingleChange"
              @ignore-change="ignoreChange"
              @apply-all-changes="applyAllChanges"
            />

            <!-- 快捷指令 -->
            <QuickCommands
              v-if="state.messages.length <= 1"
              :chat-mode="state.chatMode"
              @select="handleQuickCommand"
            />
          </div>

          <!-- 输入区域 -->
          <ChatInputArea
            v-model="state.currentInput"
            :selected-images="state.selectedImages"
            :is-streaming="state.isStreaming"
            @send="sendMessage"
            @image-select="handleImageSelect"
            @image-remove="handleImageRemove"
            @preview-image="handleImagePreview"
          />

          <!-- 图片预览弹窗 -->
          <ImagePreviewModal
            v-model:visible="previewVisible"
            :src="previewImageUrl"
          />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { watch, ref } from 'vue'
import { useAIChat } from '@/composables/useAIChat'
import ChatHeader from './ChatHeader.vue'
import ChatMessageList from './ChatMessageList.vue'
import QuickCommands from './QuickCommands.vue'
import ChatInputArea from './ChatInputArea.vue'
import ImagePreviewModal from '@/components/common/ImagePreviewModal.vue'

interface Props {
  open: boolean
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'close'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 图片预览状态
const previewVisible = ref(false)
const previewImageUrl = ref('')

function handleImagePreview(url: string) {
  previewImageUrl.value = url
  previewVisible.value = true
}

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
  handleQuickCommand,
  applySingleChange,
  ignoreChange,
  applyAllChanges,
  handleImageSelect,
  handleImageRemove,
  startNewSession
} = useAIChat()

function handleClose() {
  emit('update:open', false)
  emit('close')
}
</script>

<style lang="scss" scoped>
.chat-fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: $color-bg-primary;
  z-index: $z-chat-window;
  display: flex;
  flex-direction: column;
  overscroll-behavior: contain;
}

.chat-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

// 全屏滑入动画
.fullscreen-slide-enter-active,
.fullscreen-slide-leave-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.fullscreen-slide-enter-from,
.fullscreen-slide-leave-to {
  opacity: 0;
  transform: translateY(30px);
}
</style>
