[根目录](../CLAUDE.md) > **frontend**

---

# Frontend - 前端应用模块

## 变更记录 (Changelog)

| 日期 | 版本 | 变更内容 |
|------|------|----------|
| 2026-03-08 | 1.2.0 | 更新组件清单、补充 Composables 文档、完善简历区块组件 |
| 2026-03-03 | 1.1.0 | 添加简历优化相关类型、更新组件清单、完善 API 模块文档 |
| 2026-02-19 | 1.0.0 | 初始版本 |

---

## 模块职责

前端应用模块负责提供用户界面，包括：
- 首页数据展示
- 简历管理与编辑
- 简历优化进度展示
- 模拟面试进行
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

前端通过 HTTP 请求调用后端 API，基础路径：`http://localhost:8080/landit/`

### API 模块

| 模块 | 文件 | 描述 |
|------|------|------|
| user | `api/user.ts` | 用户状态、初始化、信息管理 |
| resume | `api/resume.ts` | 简历 CRUD、模块操作 |

### 页面路由

| 路径 | 组件 | 标题 | 描述 | 权限 |
|------|------|------|------|------|
| `/onboarding` | Onboarding.vue | 欢迎 | 用户引导页 | 公开 |
| `/` | Home.vue | 首页 | 数据概览与快捷入口 | 登录 |
| `/resume` | Resume.vue | 简历管理 | 简历列表 | 登录 |
| `/resume/:id` | ResumeDetail.vue | 简历详情 | 简历编辑与优化 | 登录 |
| `/interview` | Interview.vue | 面试演练 | 面试历史列表 | 登录 |
| `/interview/:id` | InterviewSession.vue | 面试进行中 | 实时面试界面 | 登录 |
| `/review` | Review.vue | 面试复盘 | 复盘列表 | 登录 |
| `/review/:id` | ReviewDetail.vue | 复盘详情 | 复盘分析查看 | 登录 |
| `/profile` | Profile.vue | 个人中心 | 用户信息设置 | 登录 |

### 路由守卫
- 检查用户是否已初始化
- 未初始化用户跳转到 `/onboarding`
- 非公开页面需要登录状态

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
        '/landit': {
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
| `types/index.ts` | 通用业务类型定义（445 行） |
| `types/resume-optimize.ts` | 简历优化工作流类型定义（295 行） |

#### 用户相关 (types/index.ts)
```typescript
interface User {
  id: string
  name: string
  gender: Gender | null
  avatar: string | null
  createdAt: string
}

interface UserStatusResponse {
  exists: boolean
  user?: {
    id: string
    name: string
    gender: string
    avatar: string | null
  }
}

interface UserInitResponse {
  name: string
  gender: Gender | null
}
```

#### 简历相关 (types/index.ts)
```typescript
type ResumeStatus = 'OPTIMIZED' | 'DRAFT'
type SectionType = 'BASIC_INFO' | 'EDUCATION' | 'WORK' | 'PROJECT' | 'SKILLS' | 'CERTIFICATE' | 'OPEN_SOURCE' | 'CUSTOM'

interface PrimaryResumeVO {
  id: string
  name: string
  targetPosition: string
  status: ResumeStatus
  score: number
  completeness: number
  analyzed: boolean
  createdAt: string
  updatedAt: string
}

interface ResumeDetail {
  id: string
  name: string
  targetPosition: string
  sections: ResumeSection[]
  overallScore: number
  contentScore: number
  structureScore: number
  matchingScore: number
  competitivenessScore: number
  analyzed: boolean
}

interface ResumeSection {
  id: string
  type: string
  title: string
  content: ResumeSectionContent | null
  items: ResumeSectionItem[] | null
  score: number
  suggestions: ResumeSuggestionItem[] | null
}
```

#### 简历优化相关 (types/resume-optimize.ts)
```typescript
// SSE 事件类型
type OptimizeEventType = 'start' | 'progress' | 'complete' | 'error'

// 优化阶段
type OptimizeStage =
  | 'start'
  | 'diagnose_quick'
  | 'diagnose_precise'
  | 'generate_suggestions'
  | 'optimize_section'
  | 'human_review'
  | 'end'

// SSE 进度事件
interface OptimizeProgressEvent {
  event: OptimizeEventType
  nodeId: OptimizeStage | null
  progress: number | null
  message: string
  threadId: string | null
  data: any
  timestamp: number
}

// 诊断数据
interface DiagnoseData {
  overallScore: number
  dimensionScores: DimensionScores
  suggestions?: SuggestionItem[]
  strengths: string[]
  weaknesses: string[] | WeaknessItem[]
  quickWins: string[]
  preciseAnalysis?: any
}

// 优化建议项
interface SuggestionItem {
  priority: 'high' | 'medium' | 'low'
  category: string
  position: string
  title: string
  current: string
  suggestion: string
  impact: string
}

// 内容优化数据
interface OptimizeSectionData {
  changes: ChangeItem[]
  improvementScore: number
  tips: string[]
  confidence: 'high' | 'medium' | 'low'
  needsReview: boolean
  changeCount: number
  beforeSection?: ResumeSection[]
  afterSection?: ResumeSection[]
}

// 优化状态
interface OptimizeState {
  isConnecting: boolean
  isOptimizing: boolean
  isCompleted: boolean
  hasError: boolean
  threadId: string | null
  resumeId: string | null
  targetPosition: string
  mode: OptimizeMode
  currentStage: OptimizeStage
  progress: number
  message: string
  errorMessage: string | null
  stageHistory: StageHistoryItem[]
}
```

#### 面试相关 (types/index.ts)
```typescript
type InterviewType = 'technical' | 'behavioral'
type InterviewStatus = 'completed' | 'in_progress'

interface Interview {
  id: string
  type: InterviewType
  position: string
  company: string
  date: string
  duration: number
  score: number
  status: InterviewStatus
  questions: number
  correctAnswers: number
}

interface InterviewDetail {
  id: string
  type: InterviewType
  position: string
  company: string
  date: string
  duration: number
  score: number
  conversation: Conversation[]
  analysis: InterviewAnalysis
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
currentReview: InterviewReview

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

### useResumeOptimize (composables/useResumeOptimize.ts)

简历优化工作流的组合式函数，封装 SSE 连接和状态管理：

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

**使用示例**：
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

### useSectionEdit (composables/useSectionEdit.ts)

简历区块编辑的组合式函数，封装区块的增删改逻辑：

```typescript
const {
  // 状态
  editingSection,
  isEditing,

  // 方法
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

简历区块辅助工具函数：

```typescript
const {
  // 方法
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

### 页面组件 (views/) - 9 个
| 组件 | 功能 |
|------|------|
| Onboarding.vue | 用户引导页，上传简历初始化 |
| Home.vue | 首页仪表盘，展示统计数据和快捷入口 |
| Resume.vue | 简历列表，支持筛选和排序 |
| ResumeDetail.vue | 简历详情编辑，支持模块化编辑和优化 |
| Interview.vue | 面试历史列表 |
| InterviewSession.vue | 实时面试界面，答题交互 |
| Review.vue | 复盘列表 |
| ReviewDetail.vue | 复盘详情，维度分析可视化 |
| Profile.vue | 个人信息设置 |

### 公共组件 (components/common/) - 1 个
| 组件 | 功能 |
|------|------|
| AppNavbar.vue | 顶部导航栏 |

### 简历组件 (components/resume/) - 8 个
| 组件 | 功能 |
|------|------|
| EditSectionModal.vue | 编辑简历模块弹窗 |
| ResumeContentViewer.vue | 简历内容查看器 |
| ResumeComparison.vue | 简历优化前后对比 |
| OptimizeProgressModal.vue | 优化进度弹窗（SSE 实时展示） |
| ResumeHeader.vue | 简历头部信息 |
| MetricsSection.vue | 指标展示区域 |
| SectionContent.vue | 区块内容渲染 |
| SectionList.vue | 区块列表管理 |

### 简历区块组件 (components/resume/sections/) - 7 个
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

### 表单组件 (components/resume/forms/) - 3 个
| 组件 | 功能 |
|------|------|
| BasicInfoForm.vue | 基本信息表单 |
| ExperienceForm.vue | 经历表单（工作/项目/教育） |
| SkillsForm.vue | 技能表单 |

---

## Mock 数据

### 数据文件 (mock/data.ts)
项目使用 Mock 数据进行前端开发，包含：
- `currentUser` - 当前用户
- `resumes` - 简历列表
- `resumeDetail` - 简历详情
- `resumeSuggestions` - 简历建议
- `interviewHistory` - 面试历史
- `interviewQuestions` - 面试题库
- `interviewDetail` - 面试详情
- `interviewReview` - 面试复盘
- `statistics` - 统计数据
- `jobRecommendations` - 职位推荐

---

## 常见问题 (FAQ)

### Q: 如何使用 SCSS 变量？
A: 变量已通过 Vite 全局注入，可直接在组件中使用：
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
│   │   ├── index.ts             # 通用类型定义（445 行）
│   │   └── resume-optimize.ts   # 简历优化类型（295 行）
│   ├── api/
│   │   ├── user.ts              # 用户 API
│   │   └── resume.ts            # 简历 API
│   ├── composables/
│   │   ├── useResumeOptimize.ts # 简历优化 Composable
│   │   ├── useSectionEdit.ts    # 区块编辑 Composable
│   │   └── useSectionHelper.ts  # 区块辅助工具
│   ├── views/                   # 页面组件（9 个）
│   │   ├── Onboarding.vue
│   │   ├── Home.vue
│   │   ├── Resume.vue
│   │   ├── ResumeDetail.vue
│   │   ├── Interview.vue
│   │   ├── InterviewSession.vue
│   │   ├── Review.vue
│   │   ├── ReviewDetail.vue
│   │   └── Profile.vue
│   ├── components/
│   │   ├── common/
│   │   │   └── AppNavbar.vue
│   │   └── resume/
│   │       ├── EditSectionModal.vue
│   │       ├── ResumeContentViewer.vue
│   │       ├── ResumeComparison.vue
│   │       ├── OptimizeProgressModal.vue
│   │       ├── ResumeHeader.vue
│   │       ├── MetricsSection.vue
│   │       ├── SectionContent.vue
│   │       ├── SectionList.vue
│   │       ├── sections/
│   │       │   ├── BasicInfoSection.vue
│   │       │   ├── EducationSection.vue
│   │       │   ├── WorkSection.vue
│   │       │   ├── ProjectSection.vue
│   │       │   ├── SkillsSection.vue
│   │       │   ├── CertificateSection.vue
│   │       │   ├── OpenSourceSection.vue
│   │       │   └── CustomSection.vue
│   │       └── forms/
│   │           ├── BasicInfoForm.vue
│   │           ├── ExperienceForm.vue
│   │           └── SkillsForm.vue
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

**当前状态**：项目暂无测试文件

**建议补充**：
1. 组件测试：`src/__tests__/components/`
2. Store 测试：`src/__tests__/stores/`
3. Composable 测试：`src/__tests__/composables/`
4. E2E 测试：`e2e/`
