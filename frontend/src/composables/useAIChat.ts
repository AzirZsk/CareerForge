// =====================================================
// LandIt AI聊天 Composable
// @author Azir
// =====================================================

import { reactive, onUnmounted } from 'vue'
import type { ChatMessage, SectionChange, AIChatState, ResumeSelectedContent } from '@/types/ai-chat'
import { MAX_IMAGE_COUNT } from '@/types/ai-chat'
import { streamChat, applyChanges as apiApplyChanges, getChatHistory, clearChatHistory, updateActionStatus as apiUpdateActionStatus } from '@/api/aiChat'
import { useToast } from './useToast'

const SESSION_ID_KEY = 'aiChat_sessionId'

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
    // 从 localStorage 读取已存在的 sessionId
    const savedSessionId = localStorage.getItem(SESSION_ID_KEY)

    stateInstance = reactive<AIChatState>({
      isWindowOpen: false,
      hideFloat: false,
      chatMode: 'general',
      sessionId: savedSessionId || generateSessionId(),
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

    // 首次初始化时保存 sessionId
    if (!savedSessionId) {
      localStorage.setItem(SESSION_ID_KEY, stateInstance.sessionId!)
    }
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
        actionStatus: m.actionStatus,
        segments: m.segments
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
        segments: [{ type: 'text', content: '' }],
        timestamp: Date.now(),
        isStreaming: true      }
      state.messages.push(newMessage)
      currentAiMessage = state.messages[state.messages.length - 1]
    }
        // 追加文本内容和分片
        currentAiMessage.content += content
        if (!currentAiMessage.segments) {
          currentAiMessage.segments = []
        }
        const lastSeg = currentAiMessage.segments[currentAiMessage.segments.length - 1]
        if (lastSeg && lastSeg.type === 'text') {
          lastSeg.content += content
        } else {
          currentAiMessage.segments.push({ type: 'text', content })
        }
  }

  function handleActionEvent(changes: SectionChange[]): void {
    // 给每个 change 初始化 status
    const changesWithStatus = changes.map(c => ({ ...c, status: 'pending' as const }))

    if (currentAiMessage) {
      // 追加模式：支持多轮工具调用分批到达
      if (!currentAiMessage.actions) {
        currentAiMessage.actions = []
      }
      const startIndex = currentAiMessage.actions.length
      currentAiMessage.actions.push(...changesWithStatus)
      currentAiMessage.actionStatus = 'pending'

      // 追加 action 分片 + 新建空 text 分片
      if (!currentAiMessage.segments) {
        currentAiMessage.segments = []
      }
      for (let i = 0; i < changesWithStatus.length; i++) {
        currentAiMessage.segments.push({ type: 'action', actionIndex: startIndex + i })
      }
      currentAiMessage.segments.push({ type: 'text', content: '' })
    } else {
      // suggestion 先于 chunk 到达（罕见），创建新消息并绑定指针
      const newMessage: ChatMessage = {
        id: generateId(),
        role: 'assistant',
        content: '',
        actions: [...changesWithStatus],
        actionStatus: 'pending',
        segments: [],
        timestamp: Date.now()
      }
      for (let i = 0; i < changesWithStatus.length; i++) {
        newMessage.segments!.push({ type: 'action', actionIndex: i })
      }
      newMessage.segments!.push({ type: 'text', content: '' })
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

  /**
   * 应用单条变更
   */
  async function applySingleChange(messageId: string, index: number): Promise<void> {
    const message = state.messages.find(m => m.id === messageId)
    if (!message?.actions?.[index] || !state.currentResumeId) return

    const change = message.actions[index]
    const toast = useToast()

    try {
      change.status = 'applied'
      await apiApplyChanges(state.currentResumeId, [change])
      toast.success(`「${change.sectionTitle || '修改'}」已应用`)
      syncMessageStatus(message)
      await apiUpdateActionStatus(messageId, message.actionStatus!)
    } catch (error) {
      console.error('[AIChat] 应用单条修改失败', error)
      change.status = 'failed'
      toast.error(`「${change.sectionTitle || '修改'}」应用失败`)
      syncMessageStatus(message)
      await apiUpdateActionStatus(messageId, message.actionStatus!)
    }
  }

  /**
   * 忽略单条变更
   */
  function ignoreChange(messageId: string, index: number): void {
    const message = state.messages.find(m => m.id === messageId)
    if (!message?.actions?.[index]) return

    message.actions[index].status = 'rejected'
    syncMessageStatus(message)
  }

  /**
   * 批量应用所有 pending 变更
   */
  async function applyAllChanges(messageId: string): Promise<void> {
    const message = state.messages.find(m => m.id === messageId)
    if (!message?.actions || !state.currentResumeId) return

    const pendingChanges = message.actions.filter(a => a.status === 'pending')
    if (pendingChanges.length === 0) return

    const toast = useToast()

    try {
      // 先乐观更新状态
      pendingChanges.forEach(c => { c.status = 'applied' })
      await apiApplyChanges(state.currentResumeId, pendingChanges)
      toast.success(`${pendingChanges.length} 项修改已全部应用`)
      syncMessageStatus(message)
      await apiUpdateActionStatus(messageId, message.actionStatus!)
    } catch (error) {
      console.error('[AIChat] 批量应用修改失败', error)
      pendingChanges.forEach(c => { c.status = 'failed' })
      toast.error('批量应用失败，请逐条重试')
      syncMessageStatus(message)
      await apiUpdateActionStatus(messageId, message.actionStatus!)
    }
  }

  /**
   * 同步消息级别状态：检查所有 changes 是否已 resolved
   */
  function syncMessageStatus(message: ChatMessage): void {
    if (!message.actions || message.actions.length === 0) return

    const total = message.actions.length
    const applied = message.actions.filter(a => a.status === 'applied').length
    const rejected = message.actions.filter(a => a.status === 'rejected').length
    const failed = message.actions.filter(a => a.status === 'failed').length
    const resolved = applied + rejected + failed

    if (resolved === total) {
      // 全部已处理
      if (failed > 0) {
        message.actionStatus = 'failed'
      } else if (applied > 0) {
        message.actionStatus = 'applied'
      } else {
        message.actionStatus = 'rejected'
      }
    } else {
      message.actionStatus = 'pending'
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
      localStorage.setItem(SESSION_ID_KEY, state.sessionId!)
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

  function openWindow(): void {
    state.isWindowOpen = true
    // 窗口打开时加载历史（只在消息为空时加载，避免重复加载）
    if (state.sessionId && state.messages.length === 0) {
      loadHistory(state.sessionId)
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
    loadHistory,
    sendMessage,
    handleQuickCommand,
    handleApplyChanges,
    handleRegenerate,
    handleCancelChanges,
    applySingleChange,
    ignoreChange,
    applyAllChanges,
    handleImageSelect,
    handleImageRemove,
    handleImageRemoveAll,
    toggleWindow,
    openWindow,
    closeWindow,
    startNewSession
  }
}
