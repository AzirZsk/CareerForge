[根目录](../CLAUDE.md) > **frontend**

---

# Frontend - 前端应用模块

## 模块职责

前端应用模块负责提供用户界面，包括：
- 首页数据展示
- 简历管理与编辑
- 模拟面试进行
- 面试复盘查看
- 个人中心设置

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

### 访问地址
- 开发环境：`http://localhost:5173`
- 生产预览：`npm run preview` 后访问 `http://localhost:4173`

---

## 对外接口

前端通过 HTTP 请求调用后端 API，基础路径：`http://localhost:8080/landit/`

### 页面路由

| 路径 | 组件 | 标题 | 描述 |
|------|------|------|------|
| `/` | Home.vue | 首页 | 数据概览与快捷入口 |
| `/resume` | Resume.vue | 简历管理 | 简历列表 |
| `/resume/:id` | ResumeDetail.vue | 简历详情 | 简历编辑与优化 |
| `/interview` | Interview.vue | 面试演练 | 面试历史列表 |
| `/interview/:id` | InterviewSession.vue | 面试进行中 | 实时面试界面 |
| `/review` | Review.vue | 面试复盘 | 复盘列表 |
| `/review/:id` | ReviewDetail.vue | 复盘详情 | 复盘分析查看 |
| `/profile` | Profile.vue | 个人中心 | 用户信息设置 |

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
export default defineConfig({
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
  }
})
```

---

## 数据模型

### TypeScript 类型定义 (types/index.ts)

#### 用户相关
```typescript
interface User {
  id: string
  name: string
  avatar: string | null
  email: string
  phone: string
  targetPosition: string
  targetSalary: string
  experience: string
  education: string
  skills: string[]
  createdAt: string
}
```

#### 简历相关
```typescript
type ResumeStatus = 'optimized' | 'draft'

interface Resume {
  id: string
  name: string
  targetPosition: string
  updatedAt: string
  status: ResumeStatus
  score: number
  completeness: number
  isPrimary: boolean
}

interface ResumeDetail {
  id: string
  name: string
  targetPosition: string
  sections: ResumeSection[]
  overallScore: number
  keywordMatch: number
  formatScore: number
  contentScore: number
}
```

#### 面试相关
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

#### 复盘相关
```typescript
interface InterviewReview {
  id: string
  interviewId: string
  overallScore: number
  analysis: InterviewAnalysis
  dimensions: ReviewDimension[]
  questionAnalysis: QuestionAnalysis[]
  improvementPlan: ImprovementPlan[]
}
```

#### 统计相关
```typescript
interface Statistics {
  overview: StatisticsOverview
  weeklyProgress: WeeklyProgress[]
  skillRadar: SkillRadar[]
  recentActivity: RecentActivity[]
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

// 简历相关
resumeList: Resume[]
currentResume: ResumeDetail
suggestions: ResumeSuggestion[]

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

#### 方法
```typescript
setActiveNav(nav: string): void      // 设置当前导航
toggleSidebar(): void                // 切换侧边栏
updateUserInfo(info: UserUpdateInfo): void  // 更新用户信息
setPrimaryResume(resumeId: string): void    // 设置主简历
addInterview(interview: Interview): void    // 添加面试记录
```

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

### 页面组件 (views/)
| 组件 | 功能 |
|------|------|
| Home.vue | 首页仪表盘，展示统计数据和快捷入口 |
| Resume.vue | 简历列表，支持筛选和排序 |
| ResumeDetail.vue | 简历详情编辑，支持模块化编辑和优化 |
| Interview.vue | 面试历史列表 |
| InterviewSession.vue | 实时面试界面，答题交互 |
| Review.vue | 复盘列表 |
| ReviewDetail.vue | 复盘详情，维度分析可视化 |
| Profile.vue | 个人信息设置 |

### 公共组件 (components/common/)
| 组件 | 功能 |
|------|------|
| AppNavbar.vue | 顶部导航栏 |

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
2. 使用 axios 或 fetch 调用后端 API
3. 建议创建 `api/` 目录统一管理 API 调用

---

## 相关文件清单

```
frontend/
├── package.json                 # NPM 配置
├── vite.config.ts               # Vite 配置
├── tsconfig.json                # TypeScript 配置
├── index.html                   # HTML 入口
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
│   │   └── index.ts             # 类型定义
│   ├── views/                   # 页面组件
│   │   ├── Home.vue
│   │   ├── Resume.vue
│   │   ├── ResumeDetail.vue
│   │   ├── Interview.vue
│   │   ├── InterviewSession.vue
│   │   ├── Review.vue
│   │   ├── ReviewDetail.vue
│   │   └── Profile.vue
│   ├── components/
│   │   └── common/
│   │       └── AppNavbar.vue    # 导航组件
│   ├── mock/
│   │   └── data.ts              # Mock 数据
│   └── assets/
│       └── styles/
│           ├── variables.scss   # 设计变量
│           └── global.scss      # 全局样式
└── dist/                        # 构建产物
```
