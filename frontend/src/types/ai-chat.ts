// =====================================================
// LandIt AI聊天类型定义
// @author Azir
// =====================================================

/**
 * 聊天模式
 */
export type ChatMode = 'general' | 'resume'

/**
 * 操作状态类型（消息级 + 单条变更级共用）
 */
export type ActionStatusType = 'pending' | 'applied' | 'rejected' | 'failed'

/**
 * 内容分片 - 记录文字和操作卡片的穿插顺序
 */
export type ContentSegment =
  | { type: 'text'; content: string }
  | { type: 'action'; actionIndex: number }

/**
 * 最大图片数量
 */
export const MAX_IMAGE_COUNT = 10

/**
 * 聊天消息
 */
export interface ChatMessage {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  images?: File[]
  imageUrls?: string[]
  timestamp: number
  isStreaming?: boolean
  actions?: SectionChange[]
  actionStatus?: ActionStatusType
  segments?: ContentSegment[]
}

/**
 * SSE事件类型
 */
export type ChatEventType = 'chunk' | 'suggestion' | 'complete' | 'error' | 'resume_selected'

/**
 * 简历选择事件内容
 */
export interface ResumeSelectedContent {
  resumeId: string
  resumeName: string
}

/**
 * SSE事件
 */
export interface ChatEvent {
  type: ChatEventType
  content: string | SectionChange[] | ResumeSelectedContent
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
  status?: ActionStatusType
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
export type QuickCommandCategory = 'optimize' | 'adjust' | 'diagnose' | 'create' | 'general'

/**
 * 快捷指令
 */
export interface QuickCommandItem {
  id: string
  category: QuickCommandCategory
  label: string
  prompt: string
  mode?: ChatMode // 指定适用的模式
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

  // 聊天模式
  chatMode: ChatMode

  // 会话ID
  sessionId: string | null

  // 简历上下文（AI自动识别或用户在对话中指定）
  currentResumeId: string | null
  // AI识别到的简历信息（用于头部显示）
  detectedResume: {
    id: string
    name: string
  } | null

  // 消息
  messages: ChatMessage[]
  isStreaming: boolean

  // 修改建议
  showApplyDialog: boolean
  pendingChanges: SectionChange[]

  // 输入
  currentInput: string
  selectedImages: File[]

  // 错误
  error: string | null
}

/**
 * 预设的快捷指令 - 通用聊天模式
 */
export const GENERAL_QUICK_COMMANDS: QuickCommandItem[] = [
  {
    id: 'create-resume',
    category: 'create',
    label: '创建简历',
    prompt: '请帮我创建一份新的简历',
    mode: 'general'
  },
  {
    id: 'interview-tips',
    category: 'general',
    label: '面试技巧',
    prompt: '请分享一些面试技巧和注意事项',
    mode: 'general'
  },
  {
    id: 'job-advice',
    category: 'general',
    label: '求职建议',
    prompt: '我现在正在找工作，请问有什么建议吗？',
    mode: 'general'
  },
  {
    id: 'career-planning',
    category: 'general',
    label: '职业规划',
    prompt: '请帮我分析一下职业发展方向',
    mode: 'general'
  }
]

/**
 * 预设的快捷指令 - 简历模式
 */
export const RESUME_QUICK_COMMANDS: QuickCommandItem[] = [
  // 区块优化类
  {
    id: 'optimize-work',
    category: 'optimize',
    label: '优化工作经历',
    prompt: '请帮我优化工作经历部分的描述，使其更加专业和有说服力',
    mode: 'resume'
  },
  {
    id: 'optimize-project',
    category: 'optimize',
    label: '润色项目描述',
    prompt: '请润色我的项目经验描述，突出技术亮点和成果',
    mode: 'resume'
  },
  {
    id: 'optimize-skills',
    category: 'optimize',
    label: '精简技能列表',
    prompt: '请帮我精简和优化技能列表，去除冗余并突出核心技能',
    mode: 'resume'
  },

  // 内容调整类
  {
    id: 'quantify-content',
    category: 'adjust',
    label: '改为量化描述',
    prompt: '请将这段内容改为量化描述，使用具体的数字和成果',
    mode: 'resume'
  },
  {
    id: 'add-keywords',
    category: 'adjust',
    label: '增加技术关键词',
    prompt: '请为这段内容增加相关的技术关键词，提高ATS匹配度',
    mode: 'resume'
  },

  // 诊断分析类
  {
    id: 'analyze-strengths',
    category: 'diagnose',
    label: '分析优势和不足',
    prompt: '请分析我简历的优势和不足之处，并给出改进建议',
    mode: 'resume'
  },
  {
    id: 'improvement-suggestions',
    category: 'diagnose',
    label: '给出改进建议',
    prompt: '请针对我的简历给出具体的改进建议',
    mode: 'resume'
  },

  // 创建生成类
  {
    id: 'create-work',
    category: 'create',
    label: '从零写工作经历',
    prompt: '请根据我的背景帮我写一段工作经历',
    mode: 'resume'
  },
  {
    id: 'create-summary',
    category: 'create',
    label: '创建个人简介',
    prompt: '请帮我写一段专业的个人简介',
    mode: 'resume'
  }
]
