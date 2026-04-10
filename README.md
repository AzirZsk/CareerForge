# LandIt - 智能求职助手

<div align="center">

**Land the job with AI-powered assistance**

![Version](https://img.shields.io/badge/version-2.7.0-blue)
![License](https://img.shields.io/badge/license-AGPL--3.0-green)

[功能特性](#功能特性) • [快速开始](#快速开始) • [技术架构](#技术架构) • [核心模块](#核心模块详解)

</div>

---

## 项目简介

LandIt 是一款面向求职者的全流程智能助手工具，通过 AI 技术帮助用户：

- 📄 **智能简历管理** - 上传解析、版本管理、AI 优化、PDF 导出
- 💬 **AI 对话式优化** - 悬浮球交互，支持通用聊天和简历模式
- 🎙️ **AI 语音模拟面试** - 实时语音对话、静音检测、求助系统、录音回放
- 📋 **面试中心** - 真实面试管理、AI 生成准备清单、AI 复盘分析
- 👥 **多用户系统** - 注册登录、JWT 认证、用户数据隔离

> **项目名称含义**：LandIt = Land the job（拿下工作）

---

## 功能特性

### 📄 简历管理
- 支持上传 PDF/Word 简历，自动解析为结构化数据
- 简历版本管理，支持回滚历史版本
- 区块式编辑（基本信息、教育经历、工作经历、项目经验等）
- 一键导出 PDF 简历

### 💬 AI 对话式优化
- 悬浮球交互，随时唤醒 AI 助手
- **双模式支持**：
  - 通用聊天模式：求职咨询、职业建议
  - 简历对话模式：AI 自动分析简历，提供优化建议
- 操作卡片式修改建议，支持批量应用
- 文字和操作卡片穿插渲染，阅读体验流畅

### 🎙️ AI 语音模拟面试
- **实时语音对话**：基于 WebSocket 的低延迟语音交互
- **阿里云语音服务**：ASR 实时转文字 + TTS 语音合成
- **问题预生成**：会话开始前批量生成面试问题，减少等待时间
- **追问机制**：回答不符合预期时，AI 实时生成追问
- **求助系统**：快捷求助（提示/解释/润色/自由提问），SSE 流式返回
- **录音回放**：完整保存面试过程，支持音频回放和文字记录查看

### 📋 面试中心
- **真实面试管理**：记录公司、职位、面试时间等信息
- **AI 准备工作流**：
  - 公司调研（自动收集公司信息）
  - JD 分析（提取关键技能、要求）
  - 生成准备事项清单
- **AI 复盘工作流**：
  - 分析面试对话内容
  - AI 评估表现（优势、不足）
  - 生成改进建议
- 手动添加准备事项和复盘笔记

### 👥 多用户系统
- 用户注册/登录
- JWT Token 认证
- 用户数据隔离（简历、面试记录、聊天记录等）

---

## 技术架构

### 整体架构

```
┌─────────────────┐          HTTP/REST          ┌─────────────────┐
│                 │  <──────────────────────>  │                 │
│   Frontend      │       /landit/*            │    Backend      │
│   Vue 3 + TS    │                             │   Spring Boot   │
│   Vite 5        │       WebSocket            │   MyBatis-Plus  │
│                 │  <──────────────────────>  │   SQLite        │
└─────────────────┘      /ws/interview/voice   └─────────────────┘
        │                                              │
        v                                              v
┌─────────────────┐                             ┌─────────────────┐
│  Pinia Store    │                             │   SQLite DB     │
│  Composables    │                             │   landit.db     │
└─────────────────┘                             └─────────────────┘
```

### 技术栈

#### 后端
- **框架**：Spring Boot 3.5.11 + Java 17
- **ORM**：MyBatis-Plus 3.5.9
- **数据库**：SQLite
- **AI 集成**：Spring AI OpenAI（支持 OpenAI 协议的模型）
- **工作流引擎**：Spring AI Alibaba Agent Framework（状态机 Graph）
- **Agent 框架**：ReactAgent（AI 聊天 Agent，含工具调用和技能系统）
- **语音服务**：阿里云智能语音交互（ASR + TTS）
- **实时通信**：WebSocket（Jakarta WebSocket API）+ SSE
- **文档处理**：Apache PDFBox 3.0.4（PDF）、Apache POI 5.3.0（Word）
- **对象映射**：MapStruct 1.6.3
- **工具库**：Lombok、Hutool 5.8.34
- **API 文档**：SpringDoc OpenAPI

#### 前端
- **框架**：Vue 3.4 + TypeScript 5.4
- **构建工具**：Vite 5
- **状态管理**：Pinia 2.1
- **路由**：Vue Router 4.3
- **样式**：SCSS + 全局变量系统
- **工具库**：@vueuse/core、marked（Markdown 渲染）

---

## 快速开始

### 环境要求

- Java 17+
- Node.js 18+
- Maven 3.8+

### 后端启动

```bash
cd backend

# 配置环境变量（必需）
export OPENAI_API_KEY=your_api_key
export ALIYUN_ACCESS_KEY_ID=your_access_key_id
export ALIYUN_ACCESS_KEY_SECRET=your_access_key_secret

# 可选环境变量
export OPENAI_BASE_URL=https://api.openai.com
export AI_MODEL=gpt-4o

# 启动开发服务器
mvn spring-boot:run
```

后端服务默认运行在 `http://localhost:8080`

### 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端服务默认运行在 `http://localhost:5173`

### 访问地址

- 前端应用：http://localhost:5173
- 后端 API：http://localhost:8080/landit
- API 文档：http://localhost:8080/landit/swagger-ui.html

### 首次使用

1. 注册新用户账号
2. 登录系统
3. 上传简历或创建空白简历
4. 开始使用 AI 优化、语音面试等功能

---

## 核心模块详解

### AI 语音面试

AI 语音面试模块基于 **WebSocket + 阿里云语音服务** 构建实时语音对话系统。

**核心流程：**

```
候选人音频 → WebSocket → ASR(阿里云) → 文本 → 面试官 Agent → 回复
     │                                                            │
     ├─ VAD 静音检测                                               ├─ TTS 合成
     │                                                            │
     └─ PCM 16kHz                                                 └─ 音频推送
```

**问题预生成机制：**
- 会话创建时批量生成 N 个问题
- JD + 简历 → LLM → 问题列表 JSON
- 缓存在内存和数据库中

**求助系统：**
- 快捷求助：给出提示、解释概念、润色回答、自由提问
- SSE 流式返回文本和音频
- 实时播放 AI 回复

**录音回放：**
- 片段存储（每个音频流保存为一个片段）
- 自动合并音频片段
- 文字记录同步保存

### AI 聊天 Agent

AI 聊天模块基于 **ReactAgent** 构建，支持工具调用和技能系统。

**双模式支持：**
- **通用聊天模式**：不绑定简历，支持求职咨询、简历创建等
- **简历对话模式**：AI 通过 `select_resume` 工具自动选择简历，上下文注入简历内容

**8 个简历操作工具：**
- GetResume / GetSection / UpdateSection
- AddSection / DeleteSection / CreateResume
- GetResumeList / SelectResume

**技能系统：**
- resume-diagnosis（简历诊断）
- resume-optimizer（简历优化）
- resume-suggestions（简历建议）

**内容分片机制：**
AI 回复中文字和操作卡片按穿插顺序渲染，支持流畅的阅读体验。

### 面试中心

面试中心是**真实面试管理**模块，用于管理实际面试全流程。

**准备工作流：**
```
START → 检查公司 → [需调研?] → 公司调研 → 检查职位 → [需分析?] → JD分析 → 生成准备事项 → END
```

**复盘工作流：**
```
START → 分析面试对话 → AI分析表现 → 生成改进建议 → END
       进度: 0% → 30%      进度: 30% → 70%    进度: 70% → 100%
```

### 简历优化工作流 Graph

基于 **Spring AI Alibaba Agent Framework** 构建状态机工作流。

```
START → 快速诊断 → 生成建议 → 内容优化 → END
```

**特性：**
- 三步完成优化（诊断 → 建议 → 优化）
- SSE 实时推送节点输出
- 状态持久化（MemorySaver，生产环境建议替换为 Redis）

---

## 数据库设计

### 核心表结构

| 表名 | 描述 |
|------|------|
| t_user | 用户信息 |
| t_resume | 简历主表 |
| t_resume_section | 简历模块（区块） |
| t_resume_suggestion | 简历优化建议 |
| t_interview | 面试记录 |
| t_interview_session | 面试会话（含语音模式） |
| t_conversation | 面试对话 |
| t_chat_message | AI 聊天消息 |
| t_assistant_conversation | 助手对话记录（语音面试求助） |
| t_interview_recording | 面试录音片段 |
| t_recording_index | 录音合并索引 |
| t_company | 公司信息与调研 |
| t_job_position | 职位信息与 JD 分析 |
| t_interview_preparation | 面试准备事项 |
| t_interview_review_note | 面试复盘笔记 |
| t_async_task | 异步任务（音频转录、简历优化、复盘分析） |

### 数据库特性

- **文件数据库**：SQLite，无需额外服务
- **主键策略**：雪花算法（ASSIGN_ID）
- **逻辑删除**：`deleted` 字段（0=未删除，1=已删除）
- **自动填充**：`createdAt`（插入）、`updatedAt`（插入+更新）

---

## 部署说明

### 生产环境配置

#### 后端

```bash
# 构建生产包
cd backend
mvn clean package

# 运行
java -jar target/landit-backend.jar
```

**环境变量清单：**

| 变量名 | 必需 | 说明 |
|--------|------|------|
| OPENAI_API_KEY | ✅ | AI 模型 API Key |
| ALIYUN_ACCESS_KEY_ID | ✅ | 阿里云 Access Key ID |
| ALIYUN_ACCESS_KEY_SECRET | ✅ | 阿里云 Access Key Secret |
| OPENAI_BASE_URL | ❌ | OpenAI API 基础 URL（默认 https://api.openai.com） |
| AI_MODEL | ❌ | AI 模型选择（默认 gpt-4o） |

#### 前端

```bash
# 构建生产包
cd frontend
npm run build

# 预览构建结果
npm run preview
```

构建产物位于 `frontend/dist/` 目录，可部署到 Nginx 等静态服务器。

### API 文档

访问 `http://your-domain/landit/swagger-ui.html` 查看 API 文档。

---

## 开源许可

本项目采用 **GNU Affero General Public License v3.0 (AGPL-3.0)** 许可证。

### 许可证要点

- ✅ 你可以自由使用、修改和分发本软件
- ✅ 你可以将本软件用于商业用途
- ⚠️ **如果你对本软件进行修改并通过网络提供服务（SaaS），必须公开你的修改源代码**
- ⚠️ **如果你将本软件集成到商业产品中，该产品必须同样采用 AGPL-3.0 许可证**

### 为什么选择 AGPL-3.0？

我们希望：
1. 促进开源社区的发展
2. 防止大公司将本软件用于商业服务而不回馈社区
3. 确保所有改进和优化都能回馈到开源社区

> 如果你的公司希望使用本软件但不希望开源修改，请联系我们讨论商业授权。

---

<div align="center">

**Made with ❤️ by job seekers, for job seekers**

</div>
