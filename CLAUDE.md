# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

# CareerForge - 智能求职助手

> **项目名称含义**：CareerForge = Land the job（拿下工作）

---

## 变更记录 (Changelog)

| 日期 | 版本 | 变更内容 |
|------|------|----------|
| 2026-04-21 | 2.9.0 | **AI 上下文全面扫描更新**：common/enums 新增 10 个枚举（11->21）；common 新增 JWT 认证基础设施；前端新增 Font Awesome 图标库、request.ts Axios 封装；前端 Components 93->101 |
| 2026-04-21 | 2.8.0 | **新增简历风格改写工作流**：后端新增 `resume/graph/rewrite/`；前端新增 useResumeRewrite composable、RewriteResumeModal/RewriteStageItem；**移除 job 模块**；Java 文件 314->324 |
| 2026-04-10 | 2.7.0 | **新增多用户登录注册系统**：后端新增 auth 模块；前端新增 auth.ts 类型、登录注册路由；SSE 从 Reactor 迁移至 Spring MVC SseEmitter；语音面试新增问题预生成与追问机制 |

---

## 项目愿景

CareerForge 是一款面向求职者的全流程智能助手工具，旨在帮助用户：
- 管理和优化求职简历
- AI 对话式简历优化（悬浮球交互，支持通用聊天和简历模式）
- **AI 语音模拟面试**（实时语音对话 + 求助系统 + 录音回放）
- 进行模拟面试训练
- 对面试表现进行深度复盘分析
- 获取个性化职位推荐
- 跟踪求职进度与能力提升

---

## 架构总览

本项目采用**前后端分离**架构：

```
+------------------+          HTTP/REST          +------------------+
|                  |  <----------------------->  |                  |
|    Frontend      |      /careerforge/*         |    Backend       |
|    Vue 3 + TS    |                             |   Spring Boot    |
|    Vite 5        |       WebSocket            |   MyBatis-Plus   |
|                  |  <-----------------------> |   SQLite         |
+------------------+      /ws/interview/voice   +------------------+
       |                                                |
       v                                                v
+------------------+                             +------------------+
|  Pinia Store     |                             |   SQLite DB      |
|  Composables     |                             |   careerforge.db |
+------------------+                             +------------------+
```

---

## 模块索引

| 模块 | 路径 | 语言/框架 | 职责 | 文档 |
|------|------|----------|------|------|
| **Backend** | `backend/` | Java 17 + Spring Boot 3.5.11 | 后端API服务 | [详情](./backend/CLAUDE.md) |
| **Frontend** | `frontend/` | TypeScript + Vue 3.4 | 前端SPA应用 | [详情](./frontend/CLAUDE.md) |

### 后端子模块

| 子模块 | 路径 | 职责 |
|--------|------|------|
| common | `backend/.../common/` | 基础实体、枚举（21个）、配置（9个）、统一响应、Schema构建、AI提示词、JWT认证、安全工具 |
| user | `backend/.../user/` | 用户信息管理 |
| **auth** | `backend/.../auth/` | **用户认证（登录注册、JWT Token、登出）** |
| resume | `backend/.../resume/` | 简历CRUD、AI优化、导出 |
| resume/graph/optimize | `backend/.../resume/graph/optimize/` | 简历优化工作流（StateGraph 状态机） |
| resume/graph/tailor | `backend/.../resume/graph/tailor/` | 简历定制工作流（StateGraph 状态机） |
| resume/graph/rewrite | `backend/.../resume/graph/rewrite/` | 简历风格改写工作流（参考简历风格改写） |
| interview | `backend/.../interview/` | 模拟面试会话、题库、答题流程 |
| **interview/voice** | `backend/.../interview/voice/` | **AI 语音面试（WebSocket + ASR/TTS + 求助系统）** |
| interview/graph/preparation | `backend/.../interview/graph/preparation/` | 面试准备工作流（AI 生成准备清单） |
| interview/graph/review | `backend/.../interview/graph/review/` | 复盘分析工作流（AI 分析面试表现） |
| statistics | `backend/.../statistics/` | 数据统计与可视化 |
| **chat** | `backend/.../chat/` | **AI 对话式简历优化（ReactAgent + 技能系统）** |
| **company** | `backend/.../company/` | **公司信息与调研** |
| **jobposition** | `backend/.../jobposition/` | **职位信息与 JD 分析** |
| **task** | `backend/.../task/` | **异步任务管理（音频转录、简历优化、复盘分析）** |

---

## 技术栈

### 后端
- **框架**：Spring Boot 3.5.11 + Java 17
- **ORM**：MyBatis-Plus 3.5.9
- **数据库**：SQLite（文件存储于 `backend/data/careerforge.db`）
- **AI 集成**：Spring AI OpenAI（支持 OpenAI 协议的模型）
- **工作流引擎**：Spring AI Alibaba Agent Framework（状态机 Graph）
- **Agent 框架**：ReactAgent（AI 聊天 Agent，含工具调用和技能系统）
- **语音服务**：阿里云智能语音交互（ASR）+ DashScope SDK 千问TTS
- **实时通信**：WebSocket（Jakarta WebSocket API）
- **文档处理**：Apache PDFBox 3.0.4（PDF）、Apache POI 5.3.0（Word）

### 前端
- **框架**：Vue 3.4 + TypeScript 5.4
- **构建工具**：Vite 5
- **状态管理**：Pinia 2.1
- **路由**：Vue Router 4.3
- **样式**：SCSS + 全局变量系统
- **工具库**：@vueuse/core、marked（Markdown 渲染）
- **图标库**：Font Awesome（@fortawesome/vue-fontawesome）

---

## 常用命令

### 后端
```bash
cd backend
mvn spring-boot:run          # 启动开发服务器（端口 8080）
mvn clean package            # 构建生产包
```

### 前端
```bash
cd frontend
npm run dev                  # 启动开发服务器（Vite 默认端口 5173）
npm run build                # 构建生产包（含类型检查）
```

### 环境变量
```bash
export OPENAI_API_KEY=your_api_key
export ALIYUN_API_KEY=your_aliyun_api_key
```

---

## 核心架构模式

> 各模块的详细组件列表、前端组件、数据流等请参考 [backend/CLAUDE.md](./backend/CLAUDE.md) 和 [frontend/CLAUDE.md](./frontend/CLAUDE.md)。

### AI 语音面试（Voice Interview）

基于 **WebSocket + 阿里云语音服务** 构建实时语音对话系统：
- 音频流：候选人音频 -> WebSocket -> ASR(阿里云) -> 文本 -> 面试官 Agent -> TTS合成 -> 音频推送
- 问题预生成：会话创建时批量生成 N 个问题并缓存
- 追问机制：回答不符合预期时动态生成追问
- 求助系统（SSE 流式）：提示/解释/润色/自由提问
- 录音回放：片段存储 -> 合并音频 -> 前端回放
- **详细组件**：[backend/CLAUDE.md](./backend/CLAUDE.md) | [frontend/CLAUDE.md](./frontend/CLAUDE.md)

### AI 聊天 Agent（ReactAgent）

基于 **ReactAgent** 构建，支持工具调用和技能系统：
- 双模式：通用聊天（chatMode=general）+ 简历对话（chatMode=resume）
- 简历操作工具（8个）：GetResume/GetSection/UpdateSection/AddSection/DeleteSection/CreateResume/GetResumeList/SelectResume
- 技能系统：resume-diagnosis / resume-optimizer / resume-suggestions
- SSE 事件：chunk / suggestion / resume_selected / complete / error
- 内容分片：segments 字段记录文字和操作卡片的穿插渲染顺序
- 服务重启恢复：从数据库加载最近 20 条历史消息注入 instruction
- **详细组件**：[backend/CLAUDE.md](./backend/CLAUDE.md) | [frontend/CLAUDE.md](./frontend/CLAUDE.md)

### 简历优化工作流（Resume Optimization）

```
START --> DiagnoseQuick --> GenerateSuggestions --> OptimizeSection --> END
```
三步完成优化（诊断 -> 建议 -> 优化），支持 SSE 实时推送，状态持久化通过 MemorySaver。

### 职位适配工作流（Resume Tailor）

```
START --> AnalyzeJD --> MatchResume --> GenerateTailored --> END
```

### 简历风格改写工作流（Resume Rewrite）

```
START --> AnalyzeStyle --> GenerateStyleDiff --> RewriteSection --> END
```
用户上传参考简历 -> AI 分析风格特征 -> 生成风格差异 -> 按参考风格改写各区块。

### 面试中心模块（Interview Center）

真实面试管理模块：创建面试 -> 面试准备 -> 进行面试 -> 复盘分析

**面试准备工作流**：`START --> CheckCompany --> [条件路由] --> CompanyResearch --> CheckJobPosition --> [条件路由] --> JDAnalysis --> GeneratePreparation --> END`

**复盘分析工作流**：`START --> AnalyzeTranscript --> AnalyzeInterview --> GenerateAdvice --> END`

- **详细组件**：[backend/CLAUDE.md](./backend/CLAUDE.md) | [frontend/CLAUDE.md](./frontend/CLAUDE.md)

### 区块类型系统（Section Type System）

| SectionType | schemaClass | 聚合类型 | 描述 |
|-------------|-------------|----------|------|
| BASIC_INFO | BasicInfo.class | 单对象 | 基本信息 |
| EDUCATION | EducationExperience.class | 数组 | 教育经历 |
| WORK | WorkExperience.class | 数组 | 工作经历 |
| PROJECT | ProjectExperience.class | 数组 | 项目经验 |
| SKILLS | Skill.class | 数组 | 专业技能 |
| CERTIFICATE | Certificate.class | 数组 | 证书荣誉 |
| OPEN_SOURCE | OpenSourceContribution.class | 数组 | 开源贡献 |
| CUSTOM | CustomSection.class | 数组 | 自定义区块 |
| RAW_TEXT | null | - | 原始文本（不参与Schema） |

### API 统一响应格式
```json
{ "code": 200, "message": "success", "data": <T>, "timestamp": 1708329600000 }
```

### 后端上下文路径
所有 API 请求前缀：`/careerforge`

### 数据库特性
- SQLite 文件数据库，主键策略：雪花算法（ASSIGN_ID）
- 逻辑删除字段：`deleted`（0=未删除，1=已删除）
- 自动填充：`createdAt`（插入）、`updatedAt`（插入+更新）

---

## 数据库表结构

| 表名 | 实体类 | 描述 |
|------|--------|------|
| t_user | User | 用户信息 |
| t_resume | Resume | 简历主表 |
| t_resume_section | ResumeSection | 简历模块（区块） |
| t_resume_suggestion | ResumeSuggestion | 简历优化建议 |
| t_interview | Interview | 面试记录 |
| t_interview_question | InterviewQuestion | 面试题库 |
| t_interview_session | InterviewSession | 面试会话 |
| t_conversation | Conversation | 面试对话 |
| t_chat_message | ChatMessage | AI 聊天消息 |
| t_assistant_conversation | AssistantConversation | 助手对话记录（语音面试求助） |
| t_interview_recording | InterviewRecording | 面试录音片段 |
| t_recording_index | RecordingIndex | 录音合并索引 |
| t_company | Company | 公司信息与调研 |
| t_job_position | JobPosition | 职位信息与 JD 分析 |
| t_interview_preparation | InterviewPreparation | 面试准备事项 |
| t_interview_ai_analysis | InterviewAiAnalysis | AI 面试分析结果 |
| t_interview_review_note | InterviewReviewNote | 面试复盘笔记 |
| t_async_task | AsyncTask | 异步任务 |

---

## API 清单

### 认证模块 `/auth`
| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /register | 用户注册 |
| POST | /login | 用户登录（返回 JWT Token） |
| POST | /logout | 用户登出 |

### 用户模块 `/user`
| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /status | 获取用户状态 |
| POST | /init | 初始化用户（上传简历） |
| GET | /profile | 获取当前用户信息 |
| PUT | /profile | 更新用户信息 |
| POST | /avatar | 上传头像 |

### AI 聊天模块 `/chat`
| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /stream | SSE 流式聊天 |
| GET | /history/{sessionId} | 获取聊天历史 |
| DELETE | /history/{sessionId} | 清空聊天历史 |
| PATCH | /messages/{messageId}/status | 更新消息操作状态 |
| POST | /apply | 批量应用简历修改 |

### 简历模块 `/resumes`
| 方法 | 路径 | 描述 |
|------|------|------|
| GET | / | 获取简历列表 |
| GET | /primary | 获取主简历 |
| POST | / | 创建空白简历 |
| POST | /upload | 上传简历文件 |
| POST | /parse | 解析简历文件 |
| GET | /{id} | 获取简历详情 |
| PUT | /{id} | 更新简历 |
| DELETE | /{id} | 删除简历 |
| PUT | /{id}/primary | 设置主简历 |
| POST | /{id}/derive | 派生岗位定制简历 |
| GET | /{id}/export | 导出简历PDF |
| PUT | /{id}/sections/{sectionId} | 更新简历模块 |
| POST | /{id}/sections | 新增简历模块 |
| DELETE | /{id}/sections/{sectionId} | 删除简历模块 |

### 简历工作流 `/resumes`
| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /{id}/optimize/stream | SSE流式简历优化 |
| POST | /{id}/rewrite/parse-reference | 上传参考简历解析 |
| GET | /{id}/rewrite/stream | SSE流式风格改写 |
| GET | /workflow/state | 获取工作流状态 |
| POST | /workflow/resume | 恢复工作流执行 |

### 面试模块 `/interviews`
| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /sessions | 开始面试会话 |
| POST | /sessions/{sessionId}/answers | 提交回答 |
| POST | /sessions/{sessionId}/finish | 结束面试 |
| GET | /history | 面试历史 |
| GET | /{id} | 面试详情 |

### 语音面试（WebSocket + REST）
| 方法 | 路径 | 描述 |
|------|------|------|
| WebSocket | `/ws/interview/voice/{sessionId}` | 实时语音对话 |
| GET | /sessions/{sessionId}/assist/remaining | 求助剩余次数 |
| GET | /sessions/{sessionId}/assist/stream | SSE 流式求助 |

### 录音回放 `/recordings/{sessionId}`
| 方法 | 路径 | 描述 |
|------|------|------|
| GET | / | 获取录音回放信息 |
| GET | /audio | 获取完整音频 |

### 面试中心 `/interview-center`
| 方法 | 路径 | 描述 |
|------|------|------|
| GET | / | 面试列表（分页） |
| GET | /{id} | 面试详情 |
| POST | / | 创建面试 |
| PUT | /{id} | 更新面试 |
| DELETE | /{id} | 删除面试 |
| GET | /{id}/preparation/stream | SSE 准备事项生成 |
| POST | /{id}/review-analysis/stream | SSE 复盘分析 |
| GET | /{id}/preparations | 获取准备事项 |
| POST | /{id}/preparations | 添加准备事项 |
| PATCH | /{id}/preparations/{prepId}/toggle | 切换完成状态 |
| DELETE | /{id}/preparations/{prepId} | 删除准备事项 |
| GET | /{id}/review-note | 获取复盘笔记 |
| PUT | /{id}/review-note | 保存复盘笔记 |

### 统计模块 `/statistics`
| 方法 | 路径 | 描述 |
|------|------|------|
| GET | / | 获取统计数据 |

### 异步任务 `/tasks`
| 方法 | 路径 | 描述 |
|------|------|------|
| GET | / | 任务列表 |
| DELETE | /completed | 清理已完成任务 |

---

## 编码规范

### Java 后端
1. 所有类添加 `@author Azir` 注释
2. 使用 `@RequiredArgsConstructor` + `private final` 进行构造注入
3. Service 接口继承 `IService<T>`
4. 避免在 Controller 中写业务逻辑（委托 Handler 处理）
5. 简单查询使用 MyBatis-Plus 条件构造器，复杂联查使用 XML

### TypeScript 前端
1. 类型定义集中在 `types/` 目录
2. 使用 Composition API + `<script setup>`
3. 状态管理使用 Pinia Store
4. Composable 使用单例模式
5. HTTP 请求使用 `utils/request.ts` 封装的 Axios 实例（自动携带 JWT Token）

---

## AI 使用指引

### 开发建议
1. 修改代码后同步更新 `CLAUDE.md` 文档
2. 新增 API 需在 `openapi.yaml` 中定义 Schema
3. 新增数据库表需更新 `schema.sql`
4. 新增简历区块类型需更新 `SectionType` 枚举和对应 DTO
5. 新增工作流节点：在对应 graph 子包下创建 Node 类（实现 AsyncNodeAction）-> 在 `*GraphConfig` 注册 -> 在 `*GraphConstants` 定义常量
6. 新增 AI Chat 工具：在 `chat/tools/` 创建工具类（`ToolUtils.createCallback`）-> 在 `ChatAgentConfig.createResumeTools()` 注册
7. 新增 SSE 事件：后端 `ChatEvent` 工厂方法 -> 前端 `types/ai-chat.ts` 类型 -> 前端 `useAIChat.ts` 处理分支
8. 新增面试中心工作流节点：在 `interview/graph/` 对应子包创建 Node -> `*GraphConstants` 常量 -> `*GraphConfig` 注册 -> `InterviewCenterHandler` SSE 方法
9. 新增语音面试功能：`interview/voice/` 下创建 Service/Handler/DTO -> `InterviewVoiceController` WebSocket 端点 -> 前端 `useInterviewVoice.ts` / `types/interview-voice.ts`
10. 新增异步任务：`task/` 下创建 Service/DTO -> `TaskController` REST API -> 前端 `api/task.ts` / `types/notification.ts`
11. 新增认证功能：`auth/` 下创建 Service/Controller/DTO -> JWT Token -> 前端 `api/auth.ts` / `types/auth.ts` / 路由守卫
12. 新增枚举：`common/enums/` 下创建枚举类实现 `BaseEnum` 接口，提供 `getCode()`/`getDescription()`/`fromCode()`

### 关键上下文文件
- **数据库结构**：`backend/src/main/resources/schema.sql`
- **区块类型枚举**：`backend/.../common/enums/SectionType.java`
- **枚举基类**：`backend/.../common/enums/BaseEnum.java`
- **Graph 公共常量**：`backend/.../resume/graph/BaseGraphConstants.java`
- **ChatAgent 配置**：`backend/.../chat/config/ChatAgentConfig.java`
- **Schema 注册表**：`backend/.../common/schema/SectionSchemaRegistry.java`
- **JWT 配置**：`backend/.../common/config/JwtProperties.java`
- **前端类型目录**：`frontend/src/types/`
- **前端 HTTP 封装**：`frontend/src/utils/request.ts`

---

## 覆盖率报告

> Java 324 | Controllers 17 | Handlers 19 | Graph节点 15+1 | Chat工具 8 | 枚举 21 | 数据库表 18
>
> Vue 101 | Views 14 | Composables 21 | Types 11 | API 10 | Stores 2

---
# currentDate
Todayʹs date is 2026/04/21.
