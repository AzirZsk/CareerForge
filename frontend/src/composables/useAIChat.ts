// =====================================================
// LandIt AI聊天 Composable
// @author Azir
// =====================================================

import { reactive, onUnmounted } from 'vue'
import type { ChatMessage, SectionChange, AIChatState } from '@/types/ai-chat'
import { streamChat, applyChanges as apiApplyChanges, getChatHistory } from '@/api/aiChat'
import { getResumes } from '@/api/resume'

function generateId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
}

// 单例状态
let stateInstance: AIChatState | null = null
let currentAiMessage: ChatMessage | null = null

function getState(): AIChatState {
  if (!stateInstance) {
    stateInstance = reactive<AIChatState>({
      isWindowOpen: false,
      currentResumeId: null,
      resumeList: [],
      messages: [],
      isStreaming: false,
      showApplyDialog: false,
      pendingChanges: [],
      currentInput: '',
      selectedImage: null,
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
  async function loadHistory(resumeId: string): Promise<void> {
    try {
      const history = await getChatHistory(resumeId)
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
    if (!message && !state.selectedImage) return

    if (!state.currentResumeId) {
      state.messages.push({
        id: generateId(),
        role: 'system',
        content: '请先选择一份简历，然后开始对话。',
        timestamp: Date.now()
      })
      return
    }

    const userMessage: ChatMessage = {
      id: generateId(),
      role: 'user',
      content: message,
      image: state.selectedImage,
      imageUrl: state.selectedImage ? URL.createObjectURL(state.selectedImage) : null,
      timestamp: Date.now()
    }
    state.messages.push(userMessage)

    state.currentInput = ''
    const imageToSend = state.selectedImage
    state.selectedImage = null

    state.isStreaming = true
    currentAiMessage = null

    try {
      // 不再发送历史消息，历史消息由后端从数据库加载
      for await (const event of streamChat(message, state.currentResumeId, imageToSend)) {
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
   * 切换简历时从后端加载历史消息
   */
  async function handleResumeChange(resumeId: string | null): Promise<void> {
    state.currentResumeId = resumeId
    state.pendingChanges = []
    state.showApplyDialog = false

    if (resumeId) {
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
      state.messages = []
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

  function handleImageSelect(file: File): void {
    state.selectedImage = file
  }

  function handleImageRemove(): void {
    state.selectedImage = null
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
      if (m.imageUrl) {
        URL.revokeObjectURL(m.imageUrl)
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
    toggleWindow,
    closeWindow
  }
}
