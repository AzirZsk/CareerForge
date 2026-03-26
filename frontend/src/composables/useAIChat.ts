// =====================================================
// LandIt AI聊天 Composable
// @author Azir
// =====================================================

import { reactive, onUnmounted } from 'vue'
import type { ChatMessage, SectionChange, AIChatState } from '@/types/ai-chat'
import { MAX_IMAGE_COUNT } from '@/types/ai-chat'
import { streamChat, applyChanges as apiApplyChanges, getChatHistory, clearChatHistory } from '@/api/aiChat'
import { getResumes } from '@/api/resume'

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
      resumeList: [],
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

  async function loadResumeList(): Promise<void> {
    try {
      const list = await getResumes()
      state.resumeList = list.map(r => ({
        id: r.id,
        name: r.name || '未命名简历'
      }))
    } catch (error) {
      console.error('[AIChat] 加载简历列表失败', error)
      state.error = '加载简历列表失败'
    }
  }

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
        timestamp: new Date(m.createdAt).getTime()
      }))
    } catch (error) {
      console.error('[AIChat] 加载历史失败', error)
      state.messages = []
    }
  }

  async function sendMessage(): Promise<void> {
    const message = state.currentInput.trim()
    if (!message && state.selectedImages.length === 0) return

    // 移除简历检查，允许通用聊天模式

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
      // sessionId 每次会话必须传递
      // 简历模式：sessionId = resumeId
      // 通用模式：sessionId = UUID
      for await (const event of streamChat(
        message,
        state.sessionId!,
        state.currentResumeId,
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
        handleSuggestionEvent(event.content as SectionChange[])
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

  function handleSuggestionEvent(changes: SectionChange[]): void {
    state.pendingChanges = changes
    state.showApplyDialog = true
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

  /**
   * 切换简历或切换到通用聊天模式
   */
  async function handleResumeChange(resumeId: string | null): Promise<void> {
    state.currentResumeId = resumeId
    state.pendingChanges = []
    state.showApplyDialog = false

    if (resumeId) {
      // 简历模式：sessionId = resumeId
      state.chatMode = 'resume'
      state.sessionId = resumeId

      // 从后端加载历史消息
      await loadHistory(resumeId)

      // 如果没有历史消息，添加欢迎提示
      if (state.messages.length === 0) {
        const resume = state.resumeList.find(r => r.id === resumeId)
        state.messages.push({
          id: generateId(),
          role: 'system',
          content: `已选择简历「${resume?.name}」，您现在可以询问关于这份简历的任何问题。`,
          timestamp: Date.now()
        })
      }
    } else {
      // 通用聊天模式：生成新的 sessionId
      state.chatMode = 'general'
      state.sessionId = generateSessionId()
      state.messages = []

      // 添加通用模式欢迎提示
      state.messages.push({
        id: generateId(),
        role: 'system',
        content: '欢迎使用 LandIt 求职助手！我可以帮您解答求职相关问题，或者帮您创建简历。',
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

    try {
      await apiApplyChanges(state.currentResumeId, state.pendingChanges)
      state.messages.push({
        id: generateId(),
        role: 'system',
        content: '✅ 修改已应用成功！',
        timestamp: Date.now()
      })
      state.showApplyDialog = false
      state.pendingChanges = []
    } catch (error) {
      console.error('[AIChat] 应用修改失败', error)
      state.messages.push({
        id: generateId(),
        role: 'system',
        content: '❌ 应用修改失败，请稍后重试',
        timestamp: Date.now()
      })
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
      // 调用后端清空历史
      await clearChatHistory(currentSessionId)

      // 生成新的 sessionId
      state.sessionId = generateSessionId()

      // 清空前端消息列表
      state.messages = []
      state.pendingChanges = []
      state.showApplyDialog = false

      // 添加欢迎提示
      if (state.chatMode === 'resume' && state.currentResumeId) {
        const resume = state.resumeList.find(r => r.id === state.currentResumeId)
        state.messages.push({
          id: generateId(),
          role: 'system',
          content: `已开始新会话，您可以继续询问关于「${resume?.name}」的问题。`,
          timestamp: Date.now()
        })
      } else {
        state.messages.push({
          id: generateId(),
          role: 'system',
          content: '已开始新会话，请问有什么可以帮您的？',
          timestamp: Date.now()
        })
      }
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
    // 窗口打开时加载简历列表
    if (state.isWindowOpen && state.resumeList.length === 0) {
      loadResumeList()
    }
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
    loadResumeList,
    loadHistory,
    sendMessage,
    handleResumeChange,
    handleQuickCommand,
    handleApplyChanges,
    handleRegenerate,
    handleCancelChanges,
    handleImageSelect,
    handleImageRemove,
    handleImageRemoveAll,
    toggleWindow,
    closeWindow,
    startNewSession
  }
}
