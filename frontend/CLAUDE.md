[根目录](../CLAUDE.md) > **frontend**

---

# Frontend - 前端应用模块

## 变更记录 (Changelog)

| 日期 | 版本 | 变更内容 |
|------|------|----------|
| 2026-04-14 | 2.6.0 | **新增页面离开保护**：新增 usePageGuard composable（全局保护锁注册表 + beforeunload）；工作流运行/AI聊天流式/语音面试/弹窗打开时阻止关闭页面；Composables 数量 19→20 |
| 2026-04-10 | 2.5.0 | **新增多用户登录注册系统**：新增 Login.vue 视图、auth.ts API/Types、路由守卫更新；删除 useAudioTranscribe composable；更新文件统计 |
| 2026-04-07 | 2.4.0 | **清理老版本复盘模块**：删除 Review.vue/ReviewDetail.vue 视图、currentReview 状态、interviewReview Mock 数据；复盘功能通过面试中心详情页实现 |
| 2026-04-02 | 2.3.0 | **新增 useStreamAssist composable**：SSE 流式求助功能；更新 Composables 数量（17->18）；更新文件统计 |
| 2026-04-02 | 2.2.0 | **新增 AI 语音面试前端**：6 个语音组件 + 2 个录音回放组件 + 3 个 Composables（useInterviewVoice/useStreamingAudio/useAudioRecorder）+ API（interview-voice.ts）+ Types（interview-voice.ts）+ Utils（recording-helpers.ts）；新增面试中心组件（7 个）+ Composables（2 个）+ API/Types； 更新文件统计 |
| 2026-03-30 | 2.1.0 | 新增 ImagePreviewModal/AIIcon 组件（common 3->5）、stageHelpers 工具函数、ResumeSuggestionsGroup 组件;聊天组件更新（hideFloat、resume_selected 事件、内容分片机制）;更新文件统计 |
| 2026-03-30 | 2.0.0 | 新增 AI Chat 前端:10 个聊天组件 + useAIChat composable + aiChat API + ai-chat 类型 |
| 2026-03-18 | 1.4.0 | AI 上下文全面扫描更新:更新文件统计（56 个 Vue 组件、 10 个 Composables、 3 个 Types） |
| 2026-03-18 | 1.3.0 | 新增 viewer/tailor 组件目录、补充 composables（Tailor/Diff/Confirm/Toast/FormValidation） |
| 2026-03-08 | 1.2.0 | 更新组件清单、补充 Composables 文档、完善简历区块组件 |
| 2026-03-03 | 1.1.0 | 添加简历优化相关类型、更新组件清单、完善 API 模块文档 |
| 2026-02-19 | 1.0.0 | 初始版本 |

---

## 模块职责

前端应用模块负责提供用户界面，包括：
- 用户登录注册（多用户系统）
- 首页数据展示
- 简历管理与编辑
- 简历优化进度展示（SSE 实时）
- 简历定制工作流展示
- AI 对话式简历优化（悬浮球全屏聊天）
- **AI 语音模拟面试**（实时语音对话 + 求助系统 + 录音回放）
- 面试复盘查看
- 个人中心设置
- 用户引导流程

---

## 入口与启动

### 主入口
- **文件**：`src/main.ts`
- **端口**：5173（Vite 默认）
- **框架初始化**：Vue 3 + Pinia + Vue Router

### 启动命令
```bash
cd frontend
npm run dev
```

### 环境变量
```bash
# .env.development
VITE_API_TARGET=http://localhost:8080
```

### 访问地址
- 开发环境：`http://localhost:5173`
- 生产预览：`npm run preview` 后访问 `http://localhost:4173`

---

## 对外接口
前端通过 HTTP 请求调用后端 API,基础路径： `http://localhost:8080/careerforge/`
同时通过 WebSocket 连接语音面试服务: `ws://localhost:8080/careerforge/ws/interview/voice/{sessionId}`

### API 模块
| 模块 | 文件 | 描述 |
|------|------|------|
| **auth** | `api/auth.ts` | **用户登录注册、登出** |
| user | `api/user.ts` | 用户状态、初始化、信息管理 |
| resume | `api/resume.ts` | 简历 CRUD、模块操作 |
| aiChat | `api/aiChat.ts` | AI 聊天流式对话、历史管理、修改应用 |
| interview-center | `api/interview-center.ts` | 面试中心管理、准备工作流、复盘分析 |
| **interview-voice** | `api/interview-voice.ts` | **语音面试录音回放、求助次数、SSE 求助** |
| job-position | `api/job-position.ts` | 职位信息管理 |

### 页面路由
| 路径 | 组件 | 标题 | 描述 | 权限 |
|------|------|------|------|------|
| `/login` | Login.vue | 登录/注册 | 用户登录注册页 | 公开 |
| `/onboarding` | Onboarding.vue | 欢迎 | 用户引导页 | 登录 |
| `/` | Home.vue | 首页 | 数据概览与快捷入口 | 登录 |
| `/resume` | Resume.vue | 简历管理 | 简历列表 | 登录 |
| `/resume/:id` | ResumeDetail.vue | 简历详情 | 简历编辑与优化 | 登录 |
| `/interview` | Interview.vue | 面试演练 | 面试历史列表 | 登录 |
| `/interview/:id` | InterviewSession.vue | 面试进行中 | 实时面试界面 | 登录 |
| `/recording/:id` | InterviewRecording.vue | 录音回放 | 面试录音播放 | 登录 |
| `/interview-center` | interview-center/Layout.vue | 面试中心 | 真实面试管理 | 登录 |
| `/interview-center/create` | interview-center/CreateInterview.vue | 创建面试 | 新建面试记录 | 登录 |
| `/interview-center/:id` | interview-center/InterviewDetail.vue | 面试详情 | 面试详情与准备 | 登录 |
| `/profile` | Profile.vue | 个人中心 | 用户信息设置 | 登录 |

### 路由守卫
- 未登录用户访问需要登录的页面时跳转到 `/login`
- 登录后未初始化用户跳转到 `/onboarding`

---

## 关键依赖与配置
### NPM 依赖 (package.json)

#### 运行时依赖
| 依赖 | 版本 | 用途 |
|------|------|------|
| vue | ^3.4.21 | 核心框架 |
| vue-router | ^4.3.0 | 路由管理 |
| pinia | ^2.1.7 | 状态管理 |
| @vueuse/core | ^10.9.0 | 组合式工具库 |

#### 开发依赖
| 依赖 | 版本 | 用途 |
|------|------|------|
| @vitejs/plugin-vue | ^5.0.4 | Vite Vue 插件 |
| vite | ^5.2.0 | 构建工具 |
| sass | ^1.72.0 | CSS 预处理器 |
| typescript | ^5.4.0 | 类型系统 |
| vue-tsc | ^2.0.0 | Vue TypeScript 编译器 |
| @types/node | ^20.11.0 | Node 类型定义 |

### Vite 配置 (vite.config.ts)
```typescript
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_API_TARGET || 'http://localhost:8080'

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `@use "@/assets/styles/variables.scss" as *;`,
          api: 'modern-compiler'
        }
      }
    },
    server: {
      proxy: {
        '/careerforge': {
          target: apiTarget,
          changeOrigin: true
        }
      }
    }
  }
})
```

---

## 数据模型
### TypeScript 类型定义
#### 核心类型文件
| 文件 | 描述 |
|------|------|
| `types/index.ts` | 通用业务类型定义 |
| `types/auth.ts` | 认证类型定义（RegisterRequest、LoginRequest、LoginResponse 等） |
| `types/ai-chat.ts` | AI 聊天类型定义（ChatMessage、SectionChange、AIChatState 等） |
| `types/resume-optimize.ts` | 简历优化工作流类型定义 |
| `types/resume-tailor.ts` | 简历定制工作流类型定义 |
| `types/interview-center.ts` | 面试中心类型定义 |
| `types/interview-voice.ts` | 语音面试类型定义 |
| `types/job-position.ts` | 职位信息类型定义 |
| `types/marked.d.ts` | marked 库类型声明 |

#### 语音面试相关 (types/interview-voice.ts)
```typescript
// 语音模式
type VoiceMode = 'half_voice' | 'full_voice'

// 会话状态
type SessionState = 'interviewing' | 'frozen' | 'completed'

// 求助类型
type AssistType = 'give_hints' | 'explain_concept' | 'polish_answer' | 'free_question'

// 对话角色
type ConversationRole = 'interviewer' | 'candidate' | 'assistant'

// 语音设置
interface VoiceSettings {
  mode: VoiceMode
  sampleRate: 16000 | 24000
  interviewerVoice: string
  assistantVoice: string
  speechRate: number
  vadEnabled: boolean
  vadSilenceMs: number
}

// WebSocket 消息
interface WSMessage {
  type: 'transcript' | 'audio' | 'state' | 'error'
  data: TranscriptData | AudioData | StateData | ErrorData
}

// SSE 事件
interface AssistSSEEvent {
  type: 'text' | 'audio' | 'done' | 'error'
  data: TextEventData | AudioEventData | DoneEventData | ErrorEventData
}

// 录音回放信息
interface RecordingInfo {
  sessionId: string
  totalDurationMs: number
  mergedAudioUrl: string
  segments: RecordingSegment[]
  transcript: TranscriptEntry[]
}
```

---

## 状态管理
### Pinia Store (stores/index.ts)

#### 状态
```typescript
// 用户状态
user: User
isLoggedIn: boolean
isInitialized: boolean

// 简历相关
resumeList: Resume[]
currentResume: ResumeDetail
suggestions: ResumeSuggestion[]
primaryResume: PrimaryResumeVO | null

// 面试相关
interviews: Interview[]
questions: InterviewQuestions
currentInterview: InterviewDetail

// 统计数据
stats: Statistics

// 职位推荐
jobs: Job[]

// UI 状态
activeNav: string
sidebarCollapsed: boolean
```

#### 计算属性
```typescript
primaryResume: Resume | undefined    // 主简历
recentInterviews: Interview[]        // 最近面试
averageInterviewScore: number        // 平均面试分
```

#### 核心方法
```typescript
// 用户相关
checkUserExists(): Promise<UserStatusResponse>
initUser(file: File): Promise<void>

// 认证相关（使用 auth.ts API）
login(account: string, password: string): Promise<LoginResponse>
register(email: string, password: string, name: string): Promise<RegisterResponse>
logout(): Promise<void>

// 简历相关
fetchPrimaryResume(): Promise<void>
fetchResumeDetail(id: string): Promise<void>
updateResumeSection(resumeId: string, sectionId: string, content: Record<string, unknown>): Promise<void>
addResumeSection(resumeId: string, data: {...}): Promise<void>
deleteResumeSection(resumeId: string, sectionId: string): Promise<void>
addResumeSectionItem(resumeId: string, parentSectionId: string, content: Record<string, unknown>): Promise<void>
updateResumeSectionItem(resumeId: string, itemId: string, content: Record<string, unknown>): Promise<void>
deleteResumeSectionItem(resumeId: string, itemId: string): Promise<void>

// UI 相关
setActiveNav(nav: string): void
toggleSidebar(): void
```

---

## Composables
前端使用 **Composables（组合式函数）** 封装复用逻辑：

### useAIChat (composables/useAIChat.ts)
AI 聊天的核心组合式函数,使用**单例模式**管理全局状态:
```typescript
const {
  // 状态
  state,            // AIChatState 单例 reactive

  // 方法
  loadHistory,       // 从后端加载聊天历史
  sendMessage,       // 发送消息（SSE 流式）
  handleQuickCommand,// 快捷指令
  applySingleChange, // 应用单条变更
  ignoreChange,      // 忽略单条变更
  applyAllChanges,   // 批量应用所有 pending 变更
  handleImageSelect, // 选择图片（最多10张）
  handleImageRemove, // 移除图片
  toggleWindow,      // 切换聊天窗口
  closeWindow,       // 关闭聊天窗口
  startNewSession    // 开始新会话（清空历史）
} = useAIChat()
```
**特性**：
- 单例 reactive 状态管理
- SSE 事件解析（chunk/suggestion/resume_selected/complete/error）
- 内容分片追踪（segments，文字+操作卡片穿插渲染）
- 操作卡片状态管理（pending/applied/rejected/failed）
- 双模式支持（general 通用聊天 / resume 简历对话）
- `hideFloat` 控制：外部弹窗打开时隐藏悬浮球

### useInterviewVoice (composables/useInterviewVoice.ts)
AI 语音面试的核心组合式函数:
```typescript
const {
  // 状态
  settings,           // 语音设置
  sessionState,       // 会话状态（interviewing/frozen/completed）
  currentQuestion,    // 当前问题序号
  totalQuestions,     // 问题总数
  assistCount,        // 已求助次数
  assistLimit,        // 求助上限
  elapsedTime,        // 已用时间
  messages,           // 对话消息列表
  partialTranscript,  // 实时转录（非最终结果）
  error,              // 错误信息
  isRecording,        // 是否正在录音
  audioLevel,         // 音频电平
  isPlaying,          // 是否正在播放

  // 计算属性
  isInterviewing,     // 是否面试进行中
  isFrozen,           // 是否冻结状态
  isCompleted,       // 是否已完成
  assistRemaining,    // 剩余求助次数
  progress,           // 进度百分比

  // 方法
  init,               // 初始化（WebSocket + 录音器）
  startRecording,     // 开始录音
  stopRecording,      // 停止录音
  freeze,             // 冻结面试（进入求助模式）
  resumeInterview,    // 恢复面试
  endInterview,       // 结束面试
  dispose             // 清理资源
} = useInterviewVoice(sessionId)
```
**特性**：
- WebSocket 自动连接与重连（最多 5 次）
- 心跳检测（30 秒间隔）
- VAD 静音检测
- 实时转录显示
- 音频播放器集成
- 录音器集成
- 会话状态管理

### useStreamingAudio (composables/useStreamingAudio.ts)
流式音频播放器的组合式函数:
```typescript
const {
  // 状态
  isPlaying,
  playbackState,       // idle/playing/paused/loading
  currentTime,
  duration,
  volume,
  muted,

  // 方法
  initAudioContext,    // 初始化音频上下文
  playAudioChunk,      // 播放音频块（Base64 -> PCM）
  pause,
  resume,
  setVolume,
  toggleMute,
  dispose
} = useStreamingAudio()
```
**功能**：
- 流式音频播放（PCM 格式）
- 音频队列管理
- 音量控制
- 静音切换

### useStreamAssist (composables/useStreamAssist.ts)
SSE 流式求助的组合式函数（新增）:
```typescript
const {
  // 状态
  isRequesting,        // 是否正在请求
  textContent,         // 文本内容
  deltaText,           // 增量文本（用于实时显示）
  assistRemaining,     // 剩余求助次数
  totalDurationMs,     // 总时长（毫秒）
  error,               // 错误信息
  hasRemaining,        // 是否有剩余次数

  // 音频播放器状态
  isPlaying,
  playbackState,

  // 方法
  requestAssist,       // 请求流式求助
  stopAssist,          // 停止流式请求
  giveHints,           // 给出提示
  explainConcept,      // 解释概念
  polishAnswer,        // 帮我润色
  freeQuestion,        // 自由提问

  // 音频控制
  pause,
  resume,
  setVolume,
  toggleMute
} = useStreamAssist(sessionId)
```
**功能**：
- SSE 流式求助（text/audio/done/error 事件）
- 实时文本显示（增量/全量）
- 流式音频播放集成
- 剩余求助次数追踪
- 快捷求助方法封装

### useAudioRecorder (composables/useAudioRecorder.ts)
音频录制器的组合式函数:
```typescript
const {
  // 状态
  isRecording,
  recordingTime,
  audioLevel,          // 音频电平（用于可视化）

  // 方法
  init,                // 初始化录音器
  startRecording,      // 开始录音
  stopRecording,       // 停止录音
  dispose              // 清理资源
} = useAudioRecorder()
```
**功能**：
- VAD 静音检测
- 音频电平监控
- PCM 数据回调
- 录音时间追踪

### useResumeOptimize (composables/useResumeOptimize.ts)
简历优化工作流的组合式函数,封装 SSE 连接和状态管理:
```typescript
const {
  // 状态
  state,
  stageHistory,
  currentData,

  // 方法
  startOptimize,
  resumeOptimize,
  cancelOptimize,
  resetState
} = useResumeOptimize()
```
**功能**：
- SSE 连接管理
- 优化进度追踪
- 阶段数据解析
- 错误处理
- 断点续传

### useResumeTailor (composables/useResumeTailor.ts)
简历定制工作流的组合式函数,封装 SSE 连接和状态管理:
```typescript
const {
  state,
  stageHistory,
  currentData,
  startTailor,
  resumeTailor,
  cancelTailor,
  resetState
} = useResumeTailor()
```
**功能**：
- SSE 连接管理
- 定制进度追踪
- JD 分析、匹配、生成定制简历阶段
- 错误处理

### useInterviewPreparation (composables/useInterviewPreparation.ts)
面试准备工作流的组合式函数:
```typescript
const {
  state,              // 状态（isRunning、isCompleted、currentStage 等）
  startPreparation,   // 开始准备工作流
  resetState          // 重置状态
} = useInterviewPreparation()
```
**功能**：
- SSE 连接管理
- 准备事项生成进度追踪
- AI 生成的准备事项列表

### useReviewAnalysis (composables/useReviewAnalysis.ts)
复盘分析工作流的组合式函数:
```typescript
const {
  state,              // 状态（isRunning、isCompleted、currentStage 等）
  startAnalysis,      // 开始复盘分析
  resetState          // 重置状态
} = useReviewAnalysis()
```
**功能**：
- fetch + ReadableStream 流式读取
- 复盘分析进度追踪
- AI 生成的改进建议

### useSectionEdit (composables/useSectionEdit.ts)
简历区块编辑的组合式函数,封装区块的增删改逻辑:
```typescript
const {
  editingSection,
  isEditing,
  startEdit,
  saveEdit,
  cancelEdit
} = useSectionEdit()
```
**功能**：
- 区块编辑状态管理
- 表单数据验证
- 保存/取消操作

### useSectionHelper (composables/useSectionHelper.ts)
简历区块辅助工具函数:
```typescript
const {
  getSectionTypeLabel,
  getSectionIcon,
  formatSectionContent,
  validateSectionData
} = useSectionHelper()
```
**功能**：
- 区块类型标签映射
- 区块图标获取
- 内容格式化
- 数据校验

### useSectionDiff (composables/useSectionDiff.ts)
简历区块差异对比的组合式函数:
**功能**：
- 区块内容差异计算
- 高亮变更部分
- 对比视图渲染

### useStageEdit (composables/useStageEdit.ts)
阶段编辑的组合式函数:
**功能**：
- 阶段数据编辑状态管理
- 阶段数据更新

### useStageTimer (composables/useStageTimer.ts)
阶段计时器的组合式函数:
**功能**：
- 阶段计时
- 自动更新计时状态

### useConfirm (composables/useConfirm.ts)
确认弹窗的组合式函数:
**功能**：
- 确认/取消弹窗
- 异步确认结果
- 自定义弹窗内容

### useToast (composables/useToast.ts)
Toast 提示的组合式函数:
**功能**：
- 成功/错误/警告提示
- 自动消失
- 可配置显示时长

### useFormValidation (composables/useFormValidation.ts)
表单验证的组合式函数:
**功能**：
- 表单字段验证
- 错误信息收集
- 实时验证反馈

### useMarkdown (composables/useMarkdown.ts)
Markdown 渲染的组合式函数:
**功能**：
- Markdown 文本渲染
- 代码高亮

### usePageGuard (composables/usePageGuard.ts)
页面离开保护的组合式函数（全局单例）:
```typescript
const {
  // 方法
  registerGuard,     // 注册保护锁
  unregisterGuard,   // 移除保护锁

  // 状态
  isBlocked,         // 是否有任何保护锁激活
  activeKeys         // 当前激活的保护锁列表
} = usePageGuard()
```
**功能**：
- 全局保护锁注册表（模块级 reactive Set）
- 工作流运行/AI聊天流式/语音面试/弹窗打开时注册保护锁
- App.vue 绑定 beforeunload 事件，isBlocked 时弹出浏览器原生确认框

---

## 设计系统
### SCSS 变量 (assets/styles/variables.scss)

#### 色彩系统
```scss
// 背景色阶
$color-bg-deep: #0a0a0b;
$color-bg-primary: #111113;
$color-bg-secondary: #1a1a1d;
$color-bg-tertiary: #232328;
$color-bg-elevated: #2a2a30;

// 文字色阶
$color-text-primary: #f4f4f5;
$color-text-secondary: #a1a1aa;
$color-text-tertiary: #71717a;

// 强调色（琥珀金）
$color-accent: #d4a853;
$color-accent-light: #e8c47a;
$color-accent-dark: #b8923f;

// 功能色
$color-success: #34d399;
$color-warning: #fbbf24;
$color-error: #f87171;
$color-info: #60a5fa;
```

#### 字体
```scss
$font-display: 'Crimson Pro', Georgia, serif;
$font-body: 'Outfit', -apple-system, sans-serif;
```

#### 间距
```scss
$spacing-xs: 0.25rem;
$spacing-sm: 0.5rem;
$spacing-md: 1rem;
$spacing-lg: 1.5rem;
$spacing-xl: 2rem;
$spacing-2xl: 3rem;
```

#### 圆角
```scss
$radius-sm: 6px;
$radius-md: 10px;
$radius-lg: 16px;
$radius-xl: 24px;
$radius-full: 9999px;
```

---

## 组件结构
### 页面组件 (views/) - 12+ 个
| 组件 | 功能 |
|------|------|
| Onboarding.vue | 用户引导页,上传简历初始化 |
| Home.vue | 首页仪表盘,展示统计数据和快捷入口 |
| Resume.vue | 简历列表,支持筛选和排序 |
| ResumeDetail.vue | 简历详情编辑,支持模块化编辑和优化 |
| Interview.vue | 面试历史列表 |
| InterviewSession.vue | 实时面试界面,答题交互 |
| **InterviewRecording.vue** | **面试录音回放界面** |
| interview-center/Layout.vue | 面试中心布局 |
| interview-center/MockEntry.vue | 模拟面试入口 |
| interview-center/CreateInterview.vue | 创建面试 |
| interview-center/InterviewList.vue | 面试列表 |
| interview-center/InterviewDetail.vue | 面试详情 |
| interview-center/PositionDetail.vue | 职位详情 |
| Profile.vue | 个人信息设置 |

### 公共组件 (components/common/) - 5 个
| 组件 | 功能 |
|------|------|
| AppNavbar.vue | 顶部导航栏 |
| ConfirmModal.vue | 确认弹窗 |
| Toast.vue | Toast 提示 |
| ImagePreviewModal.vue | 图片预览弹窗 |
| AIIcon.vue | AI 图标组件 |

### 聊天组件 (components/chat/) - 10 个
| 组件 | 功能 |
|------|------|
| AIChatFloat.vue | 悬浮球入口（支持 hideFloat 控制隐藏） |
| AIChatWindow.vue | 聊天窗口主容器（全屏模式,含图片预览） |
| ChatHeader.vue | 聊天头部（模式切换、简历标识） |
| ChatMessageList.vue | 消息列表（虚拟滚动） |
| ChatMessageItem.vue | 单条消息（文字+操作卡片穿插渲染） |
| ChatInputArea.vue | 输入区域（文本+图片,最多10张） |
| QuickCommands.vue | 快捷指令面板（通用4个+简历8个） |
| ApplyChangesDialog.vue | 批量修改确认弹窗 |
| suggestion-cards/SectionChangeCard.vue | 单条区块变更卡片 |
| suggestion-cards/FieldDiffViewer.vue | 字段级 Diff 对比视图 |

### 语音面试组件 (components/interview/voice/) - 4 个
| 组件 | 功能 |
|------|------|
| VoiceControls.vue | 语音控制面板（录音按钮、状态指示） |
| TranscriptDisplay.vue | 转录文字显示（实时/最终结果） |
| AssistantPanel.vue | 助手面板（求助入口） |
| QuickAssistButtons.vue | 快捷求助按钮组（提示/概念/润色/自由提问） |

### 录音回放组件 (components/interview/recording/) - 2 个
| 组件 | 功能 |
|------|------|
| RecordingPlayer.vue | 录音播放器（合并音频播放、进度控制） |
| TranscriptViewer.vue | 文字记录查看器（片段列表、点击跳转） |

### 面试中心组件 (components/interview-center/) - 7 个
| 组件 | 功能 |
|------|------|
| JobPositionCard.vue | 职位卡片展示 |
| CreateInterviewDialog.vue | 创建面试弹窗 |
| EditInterviewDialog.vue | 编辑面试弹窗 |
| EditPositionDialog.vue | 编辑职位弹窗 |
| ReviewNoteDialog.vue | 复盘笔记弹窗 |
| AddPreparationDialog.vue | 添加准备事项弹窗 |
| PreparationProgressModal.vue | AI 生成准备事项进度弹窗 |

### 简历组件 (components/resume/) - 12 个
| 组件 | 功能 |
|------|------|
| EditSectionModal.vue | 编辑简历模块弹窗 |
| AddSectionModal.vue | 添加简历模块弹窗 |
| ResumeContentViewer.vue | 简历内容查看器 |
| ResumeComparison.vue | 简历优化前后对比 |
| OptimizeProgressModal.vue | 优化进度弹窗（SSE 实时展示,主容器组件） |
| TailorResumeModal.vue | 定制简历弹窗 |
| ResumeHeader.vue | 简历头部信息 |
| MetricsSection.vue | 指标展示区域 |
| SectionContent.vue | 区块内容渲染 |
| SectionList.vue | 区块列表管理 |
| SuggestionsBlock.vue | 建议区块 |
| ResumeSuggestionsGroup.vue | 建议分组卡片（按简历分组展示） |
| SuggestionCard.vue | 建议卡片 |

### 优化进度子组件 (components/resume/optimize/) - 7 个
| 组件 | 功能 |
|------|------|
| ModalHeader.vue | 弹窗头部（状态图标、标题、关闭按钮） |
| ProgressBar.vue | 进度条（百分比、消息展示） |
| StageList.vue | 阶段列表容器 |
| StageItem.vue | 单个阶段项（指示器、展开/收起） |
| DiagnoseStageContent.vue | 诊断阶段内容（评分、维度、问题、亮点） |
| SuggestionsStageContent.vue | 建议阶段内容（建议列表、快速改进项） |
| OptimizeStageContent.vue | 优化阶段内容（变更详情、对比按钮） |

### 简历区块组件 (components/resume/sections/) - 8 个
| 组件 | 功能 |
|------|------|
| BasicInfoSection.vue | 基本信息区块 |
| EducationSection.vue | 教育经历区块 |
| WorkSection.vue | 工作经历区块 |
| ProjectSection.vue | 项目经验区块 |
| SkillsSection.vue | 技能区块 |
| CertificateSection.vue | 证书荣誉区块 |
| OpenSourceSection.vue | 开源贡献区块 |
| CustomSection.vue | 自定义区块 |

### 表单组件 (components/resume/forms/) - 4 个
| 组件 | 功能 |
|------|------|
| BasicInfoForm.vue | 基本信息表单 |
| ExperienceForm.vue | 经历表单（工作/项目/教育） |
| SkillsForm.vue | 技能表单 |
| CustomItemForm.vue | 自定义区块表单 |

### 区块查看器组件 (components/resume/viewer/) - 11 个
| 组件 | 功能 |
|------|------|
| SectionHeader.vue | 区块头部（标题、编辑按钮） |
| SectionEditButton.vue | 区块编辑按钮 |
| BasicInfoViewer.vue | 基本信息查看器 |
| EducationViewer.vue | 教育经历查看器 |
| WorkViewer.vue | 工作经历查看器 |
| ProjectViewer.vue | 项目经验查看器 |
| SkillsViewer.vue | 技能查看器 |
| CertificateViewer.vue | 证书荣誉查看器 |
| OpenSourceViewer.vue | 开源贡献查看器 |
| CustomViewer.vue | 自定义区块查看器 |
| _shared.scss | 共享样式 |

### 定制简历组件 (components/resume/tailor/) - 5 个
| 组件 | 功能 |
|------|------|
| TailorForm.vue | 定制表单（职位信息输入） |
| TailorStageItem.vue | 定制阶段项 |
| AnalyzeJDStageContent.vue | JD 分析阶段内容 |
| MatchResumeStageContent.vue | 简历匹配阶段内容 |
| GenerateTailoredStageContent.vue | 生成定制简历阶段内容 |

---

## 工具函数
### stageHelpers (utils/stageHelpers.ts)
简历优化阶段的辅助工具函数：
- 严重性图标映射
- 阶段状态计算
- 格式化工具

### recording-helpers (utils/recording-helpers.ts)
录音回放的辅助工具函数：
- 时间格式化
- 片段跳转计算
- 音频 URL 构建

---

## Mock 数据
### 数据文件 (mock/data.ts)
项目使用 Mock 数据进行前端开发,包含：
- `currentUser` - 当前用户
- `resumes` - 简历列表
- `resumeDetail` - 简历详情
- `resumeSuggestions` - 简历建议
- `interviewHistory` - 面试历史
- `interviewQuestions` - 面试题库
- `interviewDetail` - 面试详情
- `statistics` - 统计数据
- `jobRecommendations` - 职位推荐

---

## 常见问题 (FAQ)

### Q: 如何使用 SCSS 变量？
A: 变量已通过 Vite 全局注入,可直接在组件中使用：
```scss
.my-component {
  color: $color-accent;
  padding: $spacing-md;
}
```

### Q: 如何添加新页面？
A:
1. 在 `views/` 创建 Vue 组件
2. 在 `router/index.ts` 添加路由配置
3. 在 `types/index.ts` 添加相关类型（如需要）
4. 在 `stores/index.ts` 添加状态管理（如需要）

### Q: 如何切换真实 API？
A:
1. 移除 `stores/index.ts` 中的 mock 数据导入
2. 使用 `api/` 目录中的 API 调用
3. 当前 Store 已集成真实 API 调用

### Q: 如何使用简历优化功能？
A:
```typescript
import { useResumeOptimize } from '@/composables/useResumeOptimize'

const { startOptimize, state, stageHistory } = useResumeOptimize()

// 开始优化
startOptimize(resumeId, 'quick', targetPosition)

// 监听进度
watch(() => state.currentStage, (stage) => {
  console.log('当前阶段:', stage)
})
```

### Q: 如何使用 AI 聊天功能？
A:
```typescript
import { useAIChat } from '@/composables/useAIChat'

const { state, sendMessage, applySingleChange, startNewSession } = useAIChat()
// state 是单例 reactive 对象,包含 messages、isStreaming、chatMode、hideFloat 等
// 支持通用聊天和简历对话双模式
// AI 自动通过 select_resume 工具选择简历
```

### Q: 如何使用 AI 语音面试功能？
A:
```typescript
import { useInterviewVoice } from '@/composables/useInterviewVoice'

const {
  // 状态
  sessionState, currentQuestion, totalQuestions, messages,
  assistRemaining, elapsedTime, isRecording, isPlaying,
  // 方法
  init, startRecording, stopRecording, freeze, resumeInterview, endInterview
} = useInterviewVoice(sessionId)

// 初始化
init()

// 开始录音
startRecording()

// 进入求助模式
freeze()

// 结束面试
endInterview()
```

### Q: 如何使用 SSE 流式求助？
A:
```typescript
import { useStreamAssist } from '@/composables/useStreamAssist'

const {
  // 状态
  isRequesting, textContent, assistRemaining, hasRemaining,
  // 方法
  giveHints, explainConcept, polishAnswer, freeQuestion
} = useStreamAssist(sessionId)

// 给出提示
giveHints()

// 解释概念
explainConcept()

// 帮我润色
polishAnswer(candidateDraft)

// 自由提问
freeQuestion(question)
```

### Q: 如何使用面试准备工作流？
A:
```typescript
import { useInterviewPreparation } from '@/composables/useInterviewPreparation'

const { state, startPreparation } = useInterviewPreparation()
startPreparation(interviewId)
// state 包含 isRunning、isCompleted、currentStage、preparationItems 等
```

### Q: 如何使用复盘分析工作流？
A:
```typescript
import { useReviewAnalysis } from '@/composables/useReviewAnalysis'

const { state, startAnalysis } = useReviewAnalysis()
startAnalysis(interviewId, sessionTranscript)
// state 包含 isRunning、isCompleted、currentStage、adviceList 等
```

---

## 相关文件清单

```
frontend/
├── package.json                 # NPM 配置
├── vite.config.ts               # Vite 配置
├── tsconfig.json                # TypeScript 配置
├── index.html                   # HTML 入口
├── .env.development             # 开发环境变量
├── public/
│   └── favicon.svg              # 网站图标
├── src/
│   ├── main.ts                  # 应用入口
│   ├── App.vue                  # 根组件
│   ├── router/
│   │   └── index.ts             # 路由配置
│   ├── stores/
│   │   └── index.ts             # Pinia Store
│   ├── types/
│   │   ├── index.ts             # 通用类型定义
│   │   ├── ai-chat.ts           # AI 聊天类型
│   │   ├── resume-optimize.ts   # 简历优化类型
│   │   ├── resume-tailor.ts     # 简历定制类型
│   │   ├── interview-center.ts  # 面试中心类型
│   │   ├── interview-voice.ts   # 语音面试类型
│   │   ├── job-position.ts      # 职位信息类型
│   │   └── marked.d.ts          # marked 类型声明
│   ├── api/
│   │   ├── user.ts              # 用户 API
│   │   ├── resume.ts            # 简历 API
│   │   ├── aiChat.ts            # AI 聊天 API
│   │   ├── interview-center.ts  # 面试中心 API
│   │   ├── interview-voice.ts   # 语音面试 API
│   │   └── job-position.ts      # 职位信息 API
│   ├── utils/
│   │   ├── stageHelpers.ts      # 阶段辅助工具函数
│   │   └── recording-helpers.ts # 录音回放辅助工具函数
│   ├── composables/             # 20 个 Composables
│   │   ├── useAIChat.ts         # AI 聊天
│   │   ├── useInterviewVoice.ts # AI 语音面试
│   │   ├── useStreamingAudio.ts # 流式音频播放
│   │   ├── useStreamAssist.ts   # SSE 流式求助
│   │   ├── useAudioRecorder.ts  # 音频录制器
│   │   ├── useResumeOptimize.ts # 简历优化
│   │   ├── useResumeTailor.ts   # 简历定制
│   │   ├── useInterviewPreparation.ts # 面试准备
│   │   ├── useReviewAnalysis.ts # 复盘分析
│   │   ├── useSectionEdit.ts    # 区块编辑
│   │   ├── useSectionHelper.ts  # 区块辅助工具
│   │   ├── useSectionDiff.ts    # 区块差异对比
│   │   ├── useStageEdit.ts      # 阶段编辑
│   │   ├── useStageTimer.ts     # 阶段计时器
│   │   ├── useMarkdown.ts       # Markdown 渲染
│   │   ├── useConfirm.ts        # 确认弹窗
│   │   ├── useToast.ts          # Toast 提示
│   │   ├── usePageGuard.ts      # 页面离开保护
│   │   └── useFormValidation.ts # 表单验证
│   ├── views/                   # 页面组件（12 个）
│   │   ├── Onboarding.vue
│   │   ├── Home.vue
│   │   ├── Resume.vue
│   │   ├── ResumeDetail.vue
│   │   ├── Interview.vue
│   │   ├── InterviewSession.vue
│   │   ├── InterviewRecording.vue
│   │   ├── Profile.vue
│   │   └── interview-center/
│   │       ├── Layout.vue
│   │       ├── MockEntry.vue
│   │       ├── CreateInterview.vue
│   │       ├── InterviewList.vue
│   │       ├── InterviewDetail.vue
│   │       └── PositionDetail.vue
│   ├── components/
│   │   ├── common/              # 公共组件（5 个）
│   │   │   ├── AppNavbar.vue
│   │   │   ├── ConfirmModal.vue
│   │   │   ├── Toast.vue
│   │   │   ├── ImagePreviewModal.vue
│   │   │   └── AIIcon.vue
│   │   ├── chat/                # 聊天组件（10 个）
│   │   │   ├── AIChatFloat.vue
│   │   │   ├── AIChatWindow.vue
│   │   │   ├── ChatHeader.vue
│   │   │   ├── ChatMessageList.vue
│   │   │   ├── ChatMessageItem.vue
│   │   │   ├── ChatInputArea.vue
│   │   │   ├── QuickCommands.vue
│   │   │   ├── ApplyChangesDialog.vue
│   │   │   └── suggestion-cards/
│   │   │       ├── SectionChangeCard.vue
│   │   │       └── FieldDiffViewer.vue
│   │   ├── interview/           # 面试组件（6 个）
│   │   │   ├── voice/            # 语音面试组件（4 个）
│   │   │   │   ├── VoiceControls.vue
│   │   │   │   ├── TranscriptDisplay.vue
│   │   │   │   ├── AssistantPanel.vue
│   │   │   │   └── QuickAssistButtons.vue
│   │   │   └── recording/        # 录音回放组件（2 个）
│   │   │       ├── RecordingPlayer.vue
│   │   │       └── TranscriptViewer.vue
│   │   ├── interview-center/    # 面试中心组件（7 个）
│   │   │   ├── JobPositionCard.vue
│   │   │   ├── CreateInterviewDialog.vue
│   │   │   ├── EditInterviewDialog.vue
│   │   │   ├── EditPositionDialog.vue
│   │   │   ├── ReviewNoteDialog.vue
│   │   │   ├── AddPreparationDialog.vue
│   │   │   └── PreparationProgressModal.vue
│   │   └── resume/
│   │       ├── EditSectionModal.vue
│   │       ├── AddSectionModal.vue
│   │       ├── ResumeContentViewer.vue
│   │       ├── ResumeComparison.vue
│   │       ├── OptimizeProgressModal.vue
│   │       ├── TailorResumeModal.vue
│   │       ├── ResumeHeader.vue
│   │       ├── MetricsSection.vue
│   │       ├── SectionContent.vue
│   │       ├── SectionList.vue
│   │       ├── SuggestionsBlock.vue
│   │       ├── ResumeSuggestionsGroup.vue
│   │       ├── SuggestionCard.vue
│   │       ├── optimize/           # 优化进度子组件（7 个）
│   │       │   ├── _shared.scss
│   │       │   ├── ModalHeader.vue
│   │       │   ├── ProgressBar.vue
│   │       │   ├── StageList.vue
│   │       │   ├── StageItem.vue
│   │       │   ├── DiagnoseStageContent.vue
│   │       │   ├── SuggestionsStageContent.vue
│   │       │   └── OptimizeStageContent.vue
│   │       ├── viewer/             # 区块查看器（11 个）
│   │       │   ├── _shared.scss
│   │       │   ├── SectionHeader.vue
│   │       │   ├── SectionEditButton.vue
│   │       │   ├── BasicInfoViewer.vue
│   │       │   ├── EducationViewer.vue
│   │       │   ├── WorkViewer.vue
│   │       │   ├── ProjectViewer.vue
│   │       │   ├── SkillsViewer.vue
│   │       │   ├── CertificateViewer.vue
│   │       │   ├── OpenSourceViewer.vue
│   │       │   └── CustomViewer.vue
│   │       ├── tailor/             # 定制简历组件（5 个）
│   │       │   ├── TailorForm.vue
│   │       │   ├── TailorStageItem.vue
│   │       │   ├── AnalyzeJDStageContent.vue
│   │       │   ├── MatchResumeStageContent.vue
│   │       │   └── GenerateTailoredStageContent.vue
│   │       ├── sections/           # 区块组件（8 个）
│   │       │   ├── BasicInfoSection.vue
│   │       │   ├── EducationSection.vue
│   │       │   ├── WorkSection.vue
│   │       │   ├── ProjectSection.vue
│   │       │   ├── SkillsSection.vue
│   │       │   ├── CertificateSection.vue
│   │       │   ├── OpenSourceSection.vue
│   │       │   └── CustomSection.vue
│   │       └── forms/              # 表单组件（4 个）
│   │           ├── BasicInfoForm.vue
│   │           ├── ExperienceForm.vue
│   │           ├── SkillsForm.vue
│   │           └── CustomItemForm.vue
│   ├── mock/
│   │   └── data.ts              # Mock 数据
│   └── assets/
│       └── styles/
│           ├── variables.scss   # 设计变量
│           └── global.scss      # 全局样式
└── dist/                        # 构建产物
```

---

## 测试与质量
**当前状态**：项目暂无前端测试文件
**建议补充**：
1. 组件测试：`src/__tests__/components/`
2. Store 测试：`src/__tests__/stores/`
3. Composable 测试：`src/__tests__/composables/`
4. E2E 测试：`e2e/`
