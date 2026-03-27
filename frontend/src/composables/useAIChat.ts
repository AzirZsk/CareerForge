// =====================================================
// LandIt AI聊天 Composable
// @author Azir
// =====================================================

import { reactive, onUnmounted } from 'vue'
import type { ChatMessage, SectionChange, AIChatState, ResumeSelectedContent } from '@/types/ai-chat'
import { MAX_IMAGE_COUNT } from '@/types/ai-chat'
import { streamChat, applyChanges as apiApplyChanges, getChatHistory, clearChatHistory, updateActionStatus as apiUpdateActionStatus } from '@/api/aiChat'
import { useToast } from './useToast'

function generateId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
}

function generateSessionId(): string {
  return crypto.randomUUID ? crypto.randomUUID() : generateId()
}

// 单例状态
let stateInstance: AIChatState | null = null
let currentAiMessage: ChatMessage | null = null

function getState(): AIChatState {
  if (!stateInstance) {
    stateInstance = reactive<AIChatState>({
      isWindowOpen: false,
      chatMode: 'general',
      sessionId: generateSessionId(),
      currentResumeId: null,
      detectedResume: null,
      messages: [],
      isStreaming: false,
      showApplyDialog: false,
      pendingChanges: [],
      currentInput: '',
      selectedImages: [],
      error: null
    })
  }
  return stateInstance
}

export function useAIChat() {
  const state = getState()

  /**
   * 从后端加载聊天历史
   */
  async function loadHistory(sessionId: string): Promise<void> {
    try {
      const history = await getChatHistory(sessionId)
      state.messages = history.map(m => ({
        id: m.id,
        role: m.role as 'user' | 'assistant' | 'system',
        content: m.content,
        timestamp: new Date(m.createdAt).getTime(),
        actions: m.actions,
        actionStatus: m.actionStatus
      }))
    } catch (error) {
      console.error('[AIChat] 加载历史失败', error)
      state.messages = []
    }
  }

  async function sendMessage(): Promise<void> {
    const message = state.currentInput.trim()
    if (!message && state.selectedImages.length === 0) return

    const userMessage: ChatMessage = {
      id: generateId(),
      role: 'user',
      content: message,
      images: [...state.selectedImages],
      imageUrls: state.selectedImages.map(f => URL.createObjectURL(f)),
      timestamp: Date.now()
    }
    state.messages.push(userMessage)

    state.currentInput = ''
    const imagesToSend = [...state.selectedImages]
    state.selectedImages = []

    state.isStreaming = true
    currentAiMessage = null

    try {
      // 不再传递 resumeId，让 AI 自动识别
      for await (const event of streamChat(
        message,
        state.sessionId!,
        imagesToSend
      )) {
        handleEvent(event)
      }
    } catch (error) {
      console.error('[AIChat] 发送消息失败', error)
      state.messages.push({
        id: generateId(),
        role: 'system',
        content: '消息发送失败，请稍后重试',
        timestamp: Date.now()
      })
    } finally {
      state.isStreaming = false
      currentAiMessage = null
    }
  }

  function handleEvent(event: any): void {
    switch (event.type) {
      case 'chunk':
        handleChunkEvent(event.content as string)
        break
      case 'suggestion':
        handleActionEvent(event.content as SectionChange[])
        break
      case 'resume_selected':
        handleResumeSelectedEvent(event.content as ResumeSelectedContent)
        break
      case 'complete':
        handleCompleteEvent()
        break
      case 'error':
        handleErrorEvent(event.content as string)
        break
    }
  }

  function handleChunkEvent(content: string): void {
    if (!currentAiMessage) {
      const newMessage: ChatMessage = {
        id: generateId(),
        role: 'assistant',
        content: '',
        timestamp: Date.now(),
        isStreaming: true
      }
      state.messages.push(newMessage)
      currentAiMessage = state.messages[state.messages.length - 1]
    }
    currentAiMessage.content += content
  }

  function handleActionEvent(changes: SectionChange[]): void {
    if (currentAiMessage) {
      // 追加模式：支持多轮工具调用分批到达
      if (!currentAiMessage.actions) {
        currentAiMessage.actions = []
      }
      currentAiMessage.actions.push(...changes)
      currentAiMessage.actionStatus = 'pending'
    } else {
      // suggestion 先于 chunk 到达（罕见），创建新消息并绑定指针
      const newMessage: ChatMessage = {
        id: generateId(),
        role: 'assistant',
        content: '',
        actions: [...changes],
        actionStatus: 'pending',
        timestamp: Date.now()
      }
      state.messages.push(newMessage)
      currentAiMessage = newMessage
    }
  }

  /**
   * 处理简历选择事件
   * AI 选择简历后，更新当前简历上下文
   */
  function handleResumeSelectedEvent(content: ResumeSelectedContent): void {
    console.log('[AIChat] AI选择简历:', content)
    state.currentResumeId = content.resumeId
    state.chatMode = 'resume'
    state.detectedResume = {
      id: content.resumeId,
      name: content.resumeName
    }
    // 添加系统提示
    state.messages.push({
      id: generateId(),
      role: 'system',
      content: `已切换到「${content.resumeName}」`,
      timestamp: Date.now()
    })
  }

  async function applyActionsFromMessage(messageId: string): Promise<void> {
    const message = state.messages.find(m => m.id === messageId)
    if (!message?.actions || !state.currentResumeId) return

    const toast = useToast()

    try {
      await apiApplyChanges(state.currentResumeId, message.actions)
      message.actionStatus = 'applied'
      await apiUpdateActionStatus(messageId, 'applied')
      toast.success('修改已应用成功！')
    } catch (error) {
      console.error('[AIChat] 应用修改失败', error)
      message.actionStatus = 'failed'
      await apiUpdateActionStatus(messageId, 'failed')
      toast.error('应用修改失败，请稍后重试')
    }
  }

  function handleCompleteEvent(): void {
    if (currentAiMessage) {
      currentAiMessage.isStreaming = false
    }
  }

  function handleErrorEvent(errorMessage: string): void {
    if (currentAiMessage) {
      currentAiMessage.content += `\n\n❌ 错误：${errorMessage}`
      currentAiMessage.isStreaming = false
    } else {
      state.messages.push({
        id: generateId(),
        role: 'system',
        content: `❌ 错误：${errorMessage}`,
        timestamp: Date.now()
      })
    }
  }

  function handleQuickCommand(prompt: string): void {
    state.currentInput = prompt
    sendMessage()
  }

  async function handleApplyChanges(): Promise<void> {
    if (!state.currentResumeId || state.pendingChanges.length === 0) return

    const toast = useToast()

    try {
      await apiApplyChanges(state.currentResumeId, state.pendingChanges)
      toast.success('修改已应用成功！')
      state.showApplyDialog = false
      state.pendingChanges = []
    } catch (error) {
      console.error('[AIChat] 应用修改失败', error)
      toast.error('应用修改失败，请稍后重试')
    }
  }

  function handleRegenerate(): void {
    state.showApplyDialog = false
    state.pendingChanges = []
    state.currentInput = '请重新生成建议，换一个优化方向'
    sendMessage()
  }

  function handleCancelChanges(): void {
    state.showApplyDialog = false
    state.pendingChanges = []
  }

  /**
   * 开始新会话 - 清空当前对话历史
   */
  async function startNewSession(): Promise<void> {
    const currentSessionId = state.sessionId
    if (!currentSessionId) return

    try {
      await clearChatHistory(currentSessionId)

      // 重置状态
      state.sessionId = generateSessionId()
      state.currentResumeId = null
      state.detectedResume = null
      state.chatMode = 'general'
      state.messages = []
      state.pendingChanges = []
      state.showApplyDialog = false

      // 添加欢迎提示
      state.messages.push({
        id: generateId(),
        role: 'system',
        content: '欢迎使用 LandIt 求职助手！我可以帮您解答求职相关问题，优化简历，或提供面试建议。您可以直接告诉我您想做什么。',
        timestamp: Date.now()
      })
    } catch (error) {
      console.error('[AIChat] 清空历史失败', error)
      state.messages.push({
        id: generateId(),
        role: 'system',
        content: '❌ 开始新会话失败，请稍后重试',
        timestamp: Date.now()
      })
    }
  }

  function handleImageSelect(file: File): void {
    if (state.selectedImages.length >= MAX_IMAGE_COUNT) {
      state.error = `最多只能上传 ${MAX_IMAGE_COUNT} 张图片`
      return
    }
    state.selectedImages.push(file)
    state.error = null
  }

  function handleImageRemove(index: number): void {
    state.selectedImages.splice(index, 1)
  }

  function handleImageRemoveAll(): void {
    state.selectedImages = []
  }

  function toggleWindow(): void {
    state.isWindowOpen = !state.isWindowOpen
  }

  function closeWindow(): void {
    state.isWindowOpen = false
  }

  onUnmounted(() => {
    state.messages.forEach(m => {
      if (m.imageUrls) {
        m.imageUrls.forEach(url => URL.revokeObjectURL(url))
      }
    })
  })

  return {
    state,
    loadHistory,
    sendMessage,
    handleQuickCommand,
    handleApplyChanges,
    handleRegenerate,
    handleCancelChanges,
    applyActionsFromMessage,
    handleImageSelect,
    handleImageRemove,
    handleImageRemoveAll,
    toggleWindow,
    closeWindow,
    startNewSession
  }
}
