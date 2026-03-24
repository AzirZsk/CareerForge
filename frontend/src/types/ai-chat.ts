// =====================================================
// LandIt AI聊天类型定义
// @author Azir
// =====================================================

/**
 * 聊天消息
 */
export interface ChatMessage {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  image?: File | null
  imageUrl?: string | null
  timestamp: number
  isStreaming?: boolean
}

/**
 * SSE事件类型
 */
export type ChatEventType = 'chunk' | 'suggestion' | 'complete' | 'error'

/**
 * SSE事件
 */
export interface ChatEvent {
  type: ChatEventType
  content: string | SectionChange[]
  timestamp: number
}

/**
 * 区块变更
 */
export interface SectionChange {
  sectionId?: string
  sectionType?: string
  sectionTitle?: string
  changeType: 'update' | 'add' | 'delete'
  beforeContent?: string
  afterContent: string
  description: string
}

/**
 * 变更类型枚举
 */
export const ChangeType = {
  UPDATE: 'update',
  ADD: 'add',
  DELETE: 'delete'
} as const

/**
 * 快捷指令类型
 */
export type QuickCommandCategory = 'optimize' | 'adjust' | 'diagnose' | 'create'

/**
 * 快捷指令
 */
export interface QuickCommandItem {
  id: string
  category: QuickCommandCategory
  label: string
  prompt: string
}

/**
 * 简历选项
 */
export interface ResumeOption {
  id: string
  name: string
  status?: string
}

/**
 * AI聊天状态
 */
export interface AIChatState {
  // 窗口状态
  isWindowOpen: boolean

  // 简历上下文
  currentResumeId: string | null
  resumeList: ResumeOption[]

  // 消息
  messages: ChatMessage[]
  isStreaming: boolean

  // 修改建议
  showApplyDialog: boolean
  pendingChanges: SectionChange[]

  // 输入
  currentInput: string
  selectedImage: File | null

  // 错误
  error: string | null
}

/**
 * 预设的快捷指令
 */
export const QUICK_COMMANDS: QuickCommandItem[] = [
  // 区块优化类
  {
    id: 'optimize-work',
    category: 'optimize',
    label: '优化工作经历',
    prompt: '请帮我优化工作经历部分的描述，使其更加专业和有说服力'
  },
  {
    id: 'optimize-project',
    category: 'optimize',
    label: '润色项目描述',
    prompt: '请润色我的项目经验描述，突出技术亮点和成果'
  },
  {
    id: 'optimize-skills',
    category: 'optimize',
    label: '精简技能列表',
    prompt: '请帮我精简和优化技能列表，去除冗余并突出核心技能'
  },

  // 内容调整类
  {
    id: 'quantify-content',
    category: 'adjust',
    label: '改为量化描述',
    prompt: '请将这段内容改为量化描述，使用具体的数字和成果'
  },
  {
    id: 'add-keywords',
    category: 'adjust',
    label: '增加技术关键词',
    prompt: '请为这段内容增加相关的技术关键词，提高ATS匹配度'
  },

  // 诊断分析类
  {
    id: 'analyze-strengths',
    category: 'diagnose',
    label: '分析优势和不足',
    prompt: '请分析我简历的优势和不足之处，并给出改进建议'
  },
  {
    id: 'improvement-suggestions',
    category: 'diagnose',
    label: '给出改进建议',
    prompt: '请针对我的简历给出具体的改进建议'
  },

  // 创建生成类
  {
    id: 'create-work',
    category: 'create',
    label: '从零写工作经历',
    prompt: '请根据我的背景帮我写一段工作经历'
  },
  {
    id: 'create-summary',
    category: 'create',
    label: '创建个人简介',
    prompt: '请帮我写一段专业的个人简介'
  }
]
