[根目录](../CLAUDE.md) > **backend**

---

# Backend - 后端服务模块

## 变更记录 (Changelog)

| 日期 | 版本 | 变更内容 |
|------|------|----------|
| 2026-04-21 | 2.6.0 | **新增简历风格改写工作流**：新增 `resume/graph/rewrite/` 子模块（7 个 Java 文件、3 个 Node）、ResumeRewriteGraphController/Handler；**移除 job 模块**；Java 文件 314→324 |
| 2026-04-13 | 2.5.0 | **文档同步更新**：新增 task 异步任务模块文档（10 个 Java 文件） |
| 2026-04-10 | 2.4.0 | **新增多用户登录注册系统**：新增 auth 模块（5 个 Java 文件）、AuthController；SSE 从 Reactor 迁移至 Spring MVC SseEmitter；语音面试新增问题预生成与追问机制；更新文件统计 |
| 2026-04-07 | 2.3.0 | **清理老版本复盘模块**：删除 `review/` 目录（13 个 Java 文件），复盘功能统一使用 `interview/graph/review/` 工作流 + `InterviewReviewNote` |
| 2026-04-02 | 2.2.0 | **新增 AI 语音面试模块**（37 个 Java 文件）：WebSocket + 阿里云 ASR/TTS + 求助系统 + 录音回放；新增 company/jobposition 模块（11+11 个文件）；新增面试中心相关表和语音面试相关表；新增 7 个 Controllers |
| 2026-03-30 | 2.1.0 | Graph 子包重组（optimize/tailor 分包）、新增 BaseGraphConstants 公共常量基类、新增 ResumeChangeApplier 工具类、测试文件增至 2 个 |
| 2026-03-30 | 2.0.0 | 新增 AI Chat 模块（30 个 Java 文件）：ReactAgent + 技能系统 + 8 个工具 + MemorySaver + SSE 流式输出 |
| 2026-03-18 | 1.5.0 | AI 上下文全面扫描更新：更新文件统计（110 个 Java 文件、8 个控制器、11 个服务、8 个 Handler） |
| 2026-03-18 | 1.4.0 | 定制简历限制条件改为"仅已优化简历" |
| 2026-03-08 | 1.3.0 | 更新工作流结构（简化为三节点）、更新文件清单、补充 Composables 说明 |
| 2026-03-06 | 1.2.0 | 从 DashScope Starter 迁移到 OpenAI Starter |
| 2026-03-03 | 1.1.0 | 更新工作流文档、补充 AI 配置详情、完善 API 清单 |
| 2026-02-19 | 1.0.0 | 初始版本 |

---

## 模块职责

后端服务模块负责提供所有业务 API 接口，包括：
- **用户认证**（登录注册、JWT Token、登出）
- 用户信息管理
- 简历管理、优化、导出
- 简历优化/定制/改写工作流（StateGraph 状态机）
- AI 对话式简历优化（ReactAgent + 技能系统）
- 面试会话与答题流程
- **AI 语音模拟面试**（WebSocket 实时对话 + 求助系统 + 录音回放 + 问题预生成）
- 面试复盘与分析
- 数据统计
- **异步任务管理**（音频转录、简历优化、复盘分析）
- **公司信息与调研**
- **职位信息与 JD 分析**

---

## 入口与启动

### 主入口
- **文件**：`src/main/java/com/careerforge/CareerforgeApplication.java`
- **端口**：8080
- **上下文路径**：`/careerforge`

### 启动命令
```bash
cd backend
mvn spring-boot:run
```

### 环境变量
```bash
# 必需：AI 功能 API Key
export OPENAI_API_KEY=your_api_key

# 可选：OpenAI API 基础 URL（默认 https://api.openai.com）
export OPENAI_BASE_URL=https://api.openai.com

# 可选：AI 模型选择（默认 gpt-4o）
export AI_MODEL=gpt-4o

# 必需：阿里云语音服务（用于语音面试）
export ALIYUN_API_KEY=your_aliyun_api_key
```

### 访问地址
- API 基础路径：`http://localhost:8080/careerforge/`
- Swagger UI：`http://localhost:8080/careerforge/swagger-ui.html`
- WebSocket 端点：`ws://localhost:8080/careerforge/ws/interview/voice/{sessionId}`

---

## 对外接口

### API 模块清单

| 模块 | 基础路径 | 控制器 | 描述 |
|------|---------|--------|------|
| user | `/user` | UserController | 用户信息管理 |
| resume | `/resumes` | ResumeController | 简历CRUD与优化 |
| resume-workflow | `/resumes` | ResumeOptimizeGraphController | 简历优化工作流 |
| resume-tailor | `/resumes` | TailorResumeController | 简历定制工作流 |
| **resume-rewrite** | `/resumes` | **ResumeRewriteGraphController** | **简历风格改写工作流** |
| resume-suggestion | `/resumes` | ResumeSuggestionController | 简历建议管理 |
| **chat** | `/chat` | **AIChatController** | **AI 对话式简历优化** |
| interview | `/interviews` | InterviewController | 面试会话管理 |
| **interview-center** | `/interview-center` | **InterviewCenterController** | **面试中心（真实面试管理）** |
| **interview-preparation** | `/interviews` | **InterviewPreparationController** | **面试准备事项** |
| **interview-review-note** | `/interviews` | **InterviewReviewNoteController** | **面试复盘笔记** |
| **interview-voice** | `/interviews` | **InterviewVoiceController (WebSocket)** | **AI 语音面试（WebSocket 端点）** |
| **assistant** | `/interviews` | **AssistantController** | **语音面试求助系统** |
| **recording** | `/recordings` | **RecordingController** | **录音回放管理** |
| statistics | `/statistics` | StatisticsController | 数据统计 |
| **job-position** | `/job-positions` | **JobPositionController** | **职位信息与 JD 分析** |
| **task** | `/tasks` | **TaskController** | **异步任务管理** |

### 统一响应格式
```java
ApiResponse<T> {
    Integer code;      // 状态码：200=成功，4xx=客户端错误，5xx=服务端错误
    String message;    // 提示信息
    T data;           // 业务数据
    Long timestamp;   // 时间戳
}
```

---

## 关键依赖与配置

### Maven 依赖 (pom.xml)
| 依赖 | 版本 | 用途 |
|------|------|------|
| spring-boot-starter-web | 3.5.11 | Web 框架 |
| spring-boot-starter-validation | 3.5.11 | 参数校验 |
| spring-boot-starter-websocket | 3.5.11 | WebSocket 支持 |
| mybatis-plus-spring-boot3-starter | 3.5.9 | ORM 框架 |
| mybatis-plus-jsqlparser | 3.5.9 | 分页插件 |
| sqlite-jdbc | 3.47.2.0 | SQLite 驱动 |
| lombok | - | 代码简化 |
| hutool-all | 5.8.34 | 工具库 |
| springdoc-openapi-starter-webmvc-ui | 2.8.4 | API 文档 |
| spring-ai-alibaba-agent-framework | 1.1.2.0 | 工作流引擎 + ReactAgent |
| spring-ai-starter-model-openai | 1.1.2 | AI 大模型（OpenAI 协议） |
| aliyun-sdk-nls | 2.2.1 | 阿里云智能语音交互 |
| dashscope-sdk-java | 2.22.14 | 阿里云 DashScope SDK（千问TTS实时语音合成） |
| pdfbox | 3.0.4 | PDF 处理 |
| poi-ooxml | 5.3.0 | Word 处理 |
| mapstruct | 1.6.3 | 对象映射 |

### 配置文件 (application.yml)
```yaml
server:
  port: 8080
  servlet:
    context-path: /careerforge

spring:
  datasource:
    driver-class-name: org.sqlite.JDBC
    url: jdbc:sqlite:./data/careerforge.db
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: ${AI_MODEL:gpt-4o}
          temperature: 0.7

# 阿里云语音服务配置
aliyun:
  voice:
    api-key: ${ALIYUN_API_KEY:}

mybatis-plus:
  global-config:
    db-config:
      id-type: assign_id           # 雪花算法主键
      logic-delete-field: deleted  # 逻辑删除字段
```

---

## 核心架构

### AI 语音面试（Voice Interview）

AI 语音面试模块基于 **WebSocket + 阿里云语音服务** 构建实时语音对话系统：

```
+----------------------------------------------------------------------------------+
|                         AI 语音面试系统                                            |
+----------------------------------------------------------------------------------+
|                                                                                  |
|   候选人音频 --> WebSocket --> ASR(阿里云) --> 文本 --> 面试官 Agent --> 回复       |
|        |                                                          |              |
|        +-- VAD 静音检测                                            +-- TTS 合成   |
|        |                                                          |              |
|        +-- PCM 16kHz                                              +-- 音频推送   |
|                                                                                  |
|   求助系统（SSE 流式）：                                                            |
|   快捷求助 --> StreamAssistService --> Assistant Agent --> SSE 流式返回             |
|   (提示/概念/润色/自由提问)                                                          |
|                                                                                  |
|   录音回放：                                                                        |
|   片段存储 --> RecordingService --> 合并音频 --> TranscriptViewer                    |
|                                                                                  |
+----------------------------------------------------------------------------------+
```

**语音模式支持：**
- **半语音模式**（half_voice）：候选人语音输入，AI 文字回复
- **全语音模式**（full_voice）：候选人语音输入，AI 语音回复（TTS）

**WebSocket 消息类型：**
- `transcript` - 转录结果（实时/最终）
- `audio` - 音频数据（Base64 编码）
- `state` - 会话状态更新
- `error` - 错误信息

**求助类型（AssistType）：**
- `GIVE_HINTS` - 提示思路
- `EXPLAIN_CONCEPT` - 解释概念
- `POLISH_ANSWER` - 润色答案
- `FREE_QUESTION` - 自由提问

**关键组件：**

| 组件 | 位置 | 职责 |
|------|------|------|
| `InterviewVoiceController` | `interview/voice/controller/` | WebSocket 端点，处理实时语音对话 |
| `InterviewVoiceGateway` | `interview/voice/gateway/` | 语音面试网关（会话管理、角色路由、状态机） |
| `InterviewerAgentHandler` | `interview/voice/handler/` | 面试官 Agent 处理器（处理候选人回答） |
| `AssistantAgentHandler` | `interview/voice/handler/` | 助手 Agent 处理器（快捷求助） |
| `QuestionPreGenerateService` | `interview/voice/service/` | 问题预生成服务（批量生成 + 追问） |
| `AliyunASRService` | `interview/voice/service/impl/` | 阿里云 ASR 语音识别服务 |
| `AliyunTTSService` | `interview/voice/service/impl/` | 千问TTS实时语音合成服务（DashScope SDK） |
| `StreamAssistService` | `interview/voice/service/` | 流式求助服务（SSE 流式返回） |
| `RecordingService` | `interview/voice/service/` | 录音存储与合并服务 |
| `VoiceSessionManager` | `interview/voice/service/` | WebSocket 会话管理器 |
| `VoiceRequest` | `interview/voice/dto/` | WebSocket 请求（audio/control） |
| `VoiceResponse` | `interview/voice/dto/` | WebSocket 响应（transcript/audio/state/error） |
| `AssistSSEEvent` | `interview/voice/dto/` | SSE 求助事件（text/audio/done/error） |
| `PreGeneratedQuestion` | `interview/voice/dto/` | 预生成问题 DTO |

**WebSocket 端点：**
- **路径**：`/ws/interview/voice/{sessionId}`
- **协议**：WebSocket（文本消息 JSON，二进制消息 PCM 音频）
- **心跳**：客户端发送 `{ "type": "ping" }`，服务端响应 `{ "type": "pong" }`

**前端数据流：**
- Composable：`useInterviewVoice.ts` -- WebSocket 连接、状态管理、音频录制/播放
- API：`api/interview-voice.ts` -- 录音回放、求助次数查询
- 类型：`types/interview-voice.ts` -- VoiceSettings、WSMessage、RecordingInfo 等

### AI 聊天 Agent（ReactAgent）

AI 聊天模块基于 ReactAgent 构建，支持双模式（通用聊天/简历对话）：

**配置类（ChatAgentConfig）：**
- `skillRegistry()` - 从 classpath:skills/ 加载技能
- `skillsAgentHook()` - 注册 read_skill 工具
- `chatMemorySaver()` - 内存存储对话记忆
- `chatAgent()` - 创建 ReactAgent（系统提示词 + 工具 + 技能 Hook）

**SSE 事件处理流程：**
1. 用户消息通过 `ChatStreamRequest`（FormData）接收
2. `AIChatService.chat()` 构建上下文（简历内容/通用提示词）
3. `chatAgent.stream()` 返回 Flux
4. 处理 `AGENT_MODEL_STREAMING`（文本片段）和 `AGENT_TOOL_FINISHED`（工具结果）
5. 工具结果路由：`select_resume` -> 简历选择事件，`update/add/delete_section` -> 修改建议事件
6. `AIChatHandler.streamChat()` 包装为 SseEmitter

**服务重启恢复：**
- `AIChatService.hasMemoryCheckpoint()` 检查 MemorySaver 是否有上下文
- 如果丢失（服务重启），从数据库加载最近 20 条历史，截断 >500 字符
- 将历史注入到 instruction 中恢复对话上下文

**关键 DTO：**

| DTO | 字段 | 说明 |
|-----|------|------|
| ChatStreamRequest | resumeId, sessionId, currentUserMessage, images | 流式请求（FormData） |
| ChatEvent | type, content, timestamp | SSE 事件（chunk/suggestion/complete/error/resume_selected） |
| ChatMessageVO | id, role, content, createdAt, actions, actionStatus, segments | 消息 VO |
| SectionChange | sectionId, sectionType, sectionTitle, changeType, beforeContent, afterContent, description | 区块变更明细 |
| ApplyChangesRequest | resumeId, changes | 批量应用请求 |

### 简历优化工作流 Graph

简历优化功能基于 Spring AI Alibaba Agent Framework 构建状态机工作流：

```
START --> DiagnoseQuick --> GenerateSuggestions --> OptimizeSection --> END
           (快速诊断)           (生成建议)              (内容优化)
```

**当前工作流为简化版本，仅包含三个核心节点：**

| 节点 | 类名 | 位置 | 职责 |
|------|------|------|------|
| diagnose_quick | DiagnoseResumeNode | `graph/optimize/` | 快速诊断简历问题（评分、建议） |
| generate_suggestions | GenerateSuggestionsNode | `graph/optimize/` | 基于诊断结果生成优化建议 |
| optimize_section | OptimizeSectionNode | `graph/optimize/` | 根据建议优化简历内容 |

**工作流配置文件：**

| 文件 | 位置 | 职责 |
|------|------|------|
| `BaseGraphConstants.java` | `graph/` | 公共常量基类（STATE_RESUME_CONTENT, STATE_MESSAGES 等） |
| `ResumeOptimizeGraphConfig.java` | `graph/optimize/` | 定义工作流节点、边、状态策略 |
| `ResumeOptimizeGraphService.java` | `graph/optimize/` | 执行、恢复、状态管理工作流 |
| `ResumeOptimizeGraphConstants.java` | `graph/optimize/` | 统一管理状态键、节点名称等常量 |

**工作流 API：**

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/{id}/optimize/stream` | SSE流式执行简历优化 |
| POST | `/{id}/optimize` | 同步执行简历优化 |
| GET | `/workflow/state` | 获取工作流状态 |
| POST | `/workflow/review` | 提交人工审核结果 |
| POST | `/workflow/resume` | 恢复工作流执行 |

### 简历定制工作流 Graph

简历定制功能基于 Spring AI Alibaba Agent Framework 构建状态机工作流：

```
START --> AnalyzeJD --> MatchResume --> GenerateTailored --> END
           (分析JD)     (匹配简历)       (生成定制简历)
```

**工作流节点：**

| 节点 | 类名 | 位置 | 职责 |
|------|------|------|------|
| analyze_jd | AnalyzeJDNode | `graph/tailor/` | 分析职位描述，提取必备技能、关键词等 |
| match_resume | MatchResumeNode | `graph/tailor/` | 匹配简历与 JD，计算匹配度 |
| generate_tailored | GenerateTailoredResumeNode | `graph/tailor/` | 根据匹配分析生成定制简历 |

**工作流配置文件：**

| 文件 | 位置 | 职责 |
|------|------|------|
| `TailorResumeGraphConfig.java` | `graph/tailor/` | 定义工作流节点、边、状态策略 |
| `TailorResumeGraphService.java` | `graph/tailor/` | 执行、恢复、状态管理工作流 |
| `TailorResumeGraphConstants.java` | `graph/tailor/` | 统一管理状态键、节点名称等常量 |

### 面试准备工作流 Graph

面试准备工作流基于 Spring AI Alibaba Agent Framework 构建状态机工作流：

```
START --> CheckCompany --> [条件路由] --> CompanyResearch --> CheckJobPosition --> [条件路由] --> JDAnalysis --> GeneratePreparation --> END
           (检查公司)       (需调研?)       (公司调研)        (检查职位)           (需分析?)      (JD分析)     (生成准备事项)
```

**工作流节点：**

| 节点 | 类名 | 位置 | 职责 |
|------|------|------|------|
| check_company | CheckCompanyNode | `interview/graph/preparation/` | 检查公司是否存在 |
| company_research | CompanyResearchNode | `interview/graph/preparation/` | 公司调研（AI 生成） |
| check_job_position | CheckJobPositionNode | `interview/graph/preparation/` | 检查职位是否存在 |
| jd_analysis | JDAnalysisNode | `interview/graph/preparation/` | JD 分析（提取关键信息） |
| generate_preparation | GeneratePreparationNode | `interview/graph/preparation/` | 生成准备事项 |

### 复盘分析工作流 Graph

复盘分析工作流基于 Spring AI Alibaba Agent Framework 构建状态机工作流：

```
START --> CollectData --> AnalyzeInterview --> GenerateAdvice --> END
           (收集数据)      (AI分析表现)         (生成改进建议)
```

**工作流节点：**

| 节点 | 类名 | 位置 | 职责 |
|------|------|------|------|
| collect_data | CollectInterviewDataNode | `interview/graph/review/` | 收集面试相关数据 |
| analyze_interview | AnalyzeInterviewNode | `interview/graph/review/` | AI 分析面试表现 |
| generate_advice | GenerateAdviceNode | `interview/graph/review/` | 生成改进建议 |

### 区块类型系统

简历模块采用区块类型系统实现动态简历结构解析：

| 组件 | 路径 | 职责 |
|------|------|------|
| SectionType | `common/enums/` | 定义 9 种区块类型 |
| SectionSchemaRegistry | `common/schema/` | 动态构建简历 JSON Schema |
| GraphSchemaRegistry | `common/schema/` | 工作流节点 Schema 构建 |
| JsonSchemaBuilder | `common/util/` | Schema 构建工具 |
| SchemaGenerator | `common/util/` | 从 Class 生成 Schema |
| AIPromptProperties | `common/config/` | AI 提示词配置 |

**区块类型定义：**

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
| RAW_TEXT | null | - | 原始文本 |

### AI 配置

AI 功能通过 `AIPromptProperties` 配置类管理提示词：

**配置路径**：`careerforge.ai.prompt`

**提示词类别**：
- `resume.parse` - 简历解析提示词
- `chat.advisorConfig` - AI 聊天系统提示词（简历模式）
- `chat.generalConfig` - AI 聊天系统提示词（通用模式）
- `graph.diagnoseQuickConfig` - 工作流快速诊断提示词（拆分版：systemPrompt + userPromptTemplate）
- `graph.generateSuggestionsConfig` - 生成建议提示词（拆分版）
- `graph.optimizeSectionConfig` - 内容优化提示词（拆分版）

**提示词拆分优化**：
为了支持前缀缓存（Prefix Caching），提示词被拆分为：
- `systemPrompt`：固定部分（角色定义、任务说明、评估维度、输出格式）
- `userPromptTemplate`：动态部分（目标岗位、简历内容等，使用 `{variableName}` 占位符）

---

## 数据模型

### 数据库表结构

| 表名 | 实体类 | 描述 |
|------|--------|------|
| t_user | User | 用户信息 |
| t_resume | Resume | 简历主表（含评分字段） |
| t_resume_section | ResumeSection | 简历模块（区块） |
| t_resume_suggestion | ResumeSuggestion | 简历优化建议 |
| t_interview | Interview | 面试记录 |
| t_interview_question | InterviewQuestion | 面试题库 |
| t_interview_session | InterviewSession | 面试会话 |
| t_conversation | Conversation | 面试对话 |
| t_chat_message | ChatMessage | AI 聊天消息（含 actions、action_status、segments 字段） |
| **t_company** | **Company** | **公司信息与调研** |
| **t_job_position** | **JobPosition** | **职位信息与 JD 分析** |
| **t_interview_preparation** | **InterviewPreparation** | **面试准备事项** |
| **t_interview_review_note** | **InterviewReviewNote** | **面试复盘笔记** |
| **t_assistant_conversation** | **AssistantConversation** | **语音面试求助记录** |
| **t_interview_recording** | **InterviewRecording** | **面试录音片段** |
| **t_recording_index** | **RecordingIndex** | **录音索引与合并信息** |

### 基础实体 (BaseEntity)
```java
public abstract class BaseEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;                    // 雪花ID

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;    // 创建时间（自动填充）

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;    // 更新时间（自动填充）

    @TableLogic
    private Integer deleted;            // 逻辑删除（0=未删，1=已删）
}
```

### 枚举定义

| 枚举 | 值 | 描述 |
|------|-----|------|
| SectionType | BASIC_INFO, EDUCATION, WORK, PROJECT, SKILLS, CERTIFICATE, OPEN_SOURCE, CUSTOM, RAW_TEXT | 简历区块类型 |
| Gender | MALE, FEMALE | 性别 |
| ResumeStatus | OPTIMIZED, DRAFT | 简历状态 |
| ResumeType | PRIMARY, DERIVED | 简历类型 |
| InterviewType | TECHNICAL, BEHAVIORAL | 面试类型 |
| InterviewStatus | IN_PROGRESS, COMPLETED | 面试状态 |
| QuestionDifficulty | EASY, MEDIUM, HARD | 题目难度 |
| ConversationRole | INTERVIEWER, CANDIDATE | 对话角色 |
| SuggestionType | CRITICAL, IMPROVEMENT, ENHANCEMENT | 建议类型 |
| ActivityType | INTERVIEW, RESUME, PRACTICE, REVIEW | 活动类型 |
| ChangeType | MANUAL, AI_OPTIMIZE, DERIVE, ROLLBACK | 变更类型 |

---

## 子模块详情

### common - 公共模块
- **路径**：`src/main/java/com/careerforge/common/`
- **文件统计**：33 个 Java 文件
- **组件**：
  - `annotation/` - @SchemaField（标记 DTO 字段的 Schema 元数据）
  - `config/` - MyBatisPlusConfig, SqliteConfig, AIConfig, AIPromptProperties, JacksonConfig
  - `entity/` - BaseEntity
  - `enums/` - SectionType, Gender 等 11 个枚举类
  - `exception/` - BusinessException, GlobalExceptionHandler
  - `handler/` - MyMetaObjectHandler（自动填充）
  - `response/` - ApiResponse, PageResponse
  - `schema/` - SectionSchemaRegistry, GraphSchemaRegistry
  - `service/` - AIService, FileToImageService, SearchService
  - `util/` - JsonSchemaBuilder, SchemaGenerator, ChatClientHelper, JsonParseHelper

### auth - 认证模块（新增）
- **路径**：`src/main/java/com/careerforge/auth/`
- **文件统计**：5 个 Java 文件
- **控制器**：AuthController（注册、登录、登出）
- **服务**：AuthService
- **DTO**：RegisterRequest, LoginRequest, RegisterResponse, LoginResponse
- **核心功能**：
  - 用户注册（邮箱+密码）
  - 用户登录（邮箱/账号 + 密码，返回 JWT Token）
  - 用户登出（清除服务端会话）
  - Token 验证与身份校验

### user - 用户模块
- **路径**：`src/main/java/com/careerforge/user/`
- **文件统计**：11 个 Java 文件
- **实体**：User（name, gender, avatar）
- **API**：
  - `GET /user/status` - 获取用户状态
  - `POST /user/init` - 初始化用户（上传简历）
  - `GET /user/profile` - 获取用户信息
  - `PUT /user/profile` - 更新用户信息
  - `POST /user/avatar` - 上传头像
  - `POST /user/parse` - 解析简历文件为图片列表

### resume - 简历模块
- **路径**：`src/main/java/com/careerforge/resume/`
- **文件统计**：71 个 Java 文件（含 graph 子模块）
- **实体**：Resume, ResumeSection, ResumeSuggestion, ResumeVersion
- **控制器**：ResumeController, ResumeOptimizeGraphController, TailorResumeController, **ResumeRewriteGraphController**, ResumeSuggestionController
- **Handler**：ResumeHandler, ResumeOptimizeGraphHandler, TailorResumeGraphHandler, **ResumeRewriteGraphHandler**, ResumeSuggestionHandler
- **工具类**：ChangeFieldTranslator, ResumeChangeApplier, ResumeSectionShortener, TailoredResumeToSectionConverter, GraphSseHelper
- **核心功能**：
  - 简历 CRUD
  - 版本管理与回滚
  - 简历派生（岗位定制）
  - AI 优化工作流（SSE 流式）
  - PDF 导出
  - 模块级 CRUD

### resume/graph - 简历工作流
- **路径**：`src/main/java/com/careerforge/resume/graph/`
- **文件统计**：20 个 Java 文件
- **公共**：BaseGraphConstants（常量基类）
- **optimize 子包**：DiagnoseResumeNode, GenerateSuggestionsNode, OptimizeSectionNode, ResumeOptimizeGraphConfig, ResumeOptimizeGraphConstants, ResumeOptimizeGraphService
- **tailor 子包**：AnalyzeJDNode, MatchResumeNode, GenerateTailoredResumeNode, TailorResumeGraphConfig, TailorResumeGraphConstants, TailorResumeGraphService
- **rewrite 子包**：AnalyzeStyleNode, GenerateStyleDiffNode, RewriteSectionNode, RewriteGraphConfig, RewriteGraphConstants, RewriteGraphService, StyleAnalysisResponse

### chat - AI 聊天模块
- **路径**：`src/main/java/com/careerforge/chat/`
- **文件统计**：30 个 Java 文件
- **config/** - ChatAgentConfig（ReactAgent 创建、技能注册、工具注入）
- **controller/** - AIChatController（SSE 流式聊天、历史管理、修改应用）
- **handler/** - AIChatHandler（SSE 事件管理，5分钟超时）
- **service/** - AIChatService（Agent 执行、上下文构建、重启恢复）, ChatMessageService（消息 CRUD）
- **entity/** - ChatMessage（含 actions, actionStatus, segments 字段）
- **dto/** - ChatStreamRequest, ChatEvent, ChatMessageVO, SectionChange, ApplyChangesRequest
- **dto/tool/** - ToolResponse, ToolErrorResponse, SectionSuggestionResponse, GetResumeResponse, GetSectionResponse, CreateResumeResponse, ResumeBriefVO, SelectResumeResponse
- **mapper/** - ChatMessageMapper
- **tools/** - ToolUtils, GetResumeTool, GetSectionTool, UpdateSectionTool, AddSectionTool, DeleteSectionTool, CreateResumeTool, GetResumeListTool, SelectResumeTool

### interview - 面试模块
- **路径**：`src/main/java/com/careerforge/interview/`
- **文件统计**：20 个 Java 文件（不含 voice 子模块）
- **实体**：Interview, InterviewQuestion, InterviewSession, Conversation
- **核心功能**：
  - 面试会话管理
  - 答题与评分
  - 提示系统
  - 题库管理

### interview/voice - AI 语音面试模块（新增）
- **路径**：`src/main/java/com/careerforge/interview/voice/`
- **文件统计**：37 个 Java 文件
- **config/** - AliyunVoiceAutoConfiguration（阿里云语音服务自动配置）
- **controller/** - InterviewVoiceController（WebSocket 端点）, AssistantController（求助系统）, RecordingController（录音回放）
- **gateway/** - InterviewVoiceGateway（网关实现，会话管理、角色路由、状态机）
- **handler/** - InterviewerAgentHandler（面试官 Agent）, AssistantAgentHandler（助手 Agent）, impl/（实现类）
- **service/** - ASRService, TTSService, StreamAssistService, RecordingService, VoiceSessionManager, VoiceServiceFactory, impl/AliyunASRService, impl/AliyunTTSService, impl/StreamAssistServiceImpl, impl/RecordingMergeServiceImpl
- **entity/** - InterviewRecording（录音片段）, AssistantConversation（求助对话）, RecordingIndex（录音索引）
- **dto/** - VoiceRequest, VoiceResponse, ASRConfig, ASRResult, TTSConfig, TTSChunk, AssistSSEEvent, RecordingInfo, RecordingSegment
- **mapper/** - InterviewRecordingMapper, AssistantConversationMapper, RecordingIndexMapper
- **util/** - WavHeaderUtils（WAV 文件头工具）

### interview/graph - 面试工作流
- **路径**：`src/main/java/com/careerforge/interview/graph/`
- **preparation 子包**：CheckCompanyNode, CompanyResearchNode, CheckJobPositionNode, JDAnalysisNode, GeneratePreparationNode, InterviewPreparationGraphConfig, InterviewPreparationGraphConstants, InterviewPreparationGraphService
- **review 子包**：CollectInterviewDataNode, AnalyzeInterviewNode, GenerateAdviceNode, ReviewAnalysisGraphConfig, ReviewAnalysisGraphConstants, ReviewAnalysisGraphService

### company - 公司模块（新增）
- **路径**：`src/main/java/com/careerforge/company/`
- **文件统计**：6 个 Java 文件
- **实体**：Company（name, research, researchUpdatedAt）
- **DTO**：CompanyVO, CompanyResearchResult
- **服务**：CompanyService
- **Handler**：CompanyHandler
- **Mapper**：CompanyMapper

### jobposition - 职位模块（新增）
- **路径**：`src/main/java/com/careerforge/jobposition/`
- **文件统计**：11 个 Java 文件
- **实体**：JobPosition（companyId, title, jdContent, jdAnalysis）
- **DTO**：JobPositionVO, JobPositionListItemVO, JobPositionDetailVO, CreateJobPositionRequest, UpdateJobPositionRequest, JDAnalysisResult
- **服务**：JobPositionService
- **Handler**：JobPositionHandler
- **Controller**：JobPositionController
- **Mapper**：JobPositionMapper

### statistics - 统计模块
- **路径**：`src/main/java/com/careerforge/statistics/`
- **文件统计**：4 个 Java 文件
- **DTO**：StatisticsVO
- **核心功能**：
  - 总览数据（面试次数、平均分、提升率）
  - 周进度追踪
  - 技能雷达图
  - 最近活动

### task - 异步任务模块
- **路径**：`src/main/java/com/careerforge/task/`
- **文件统计**：10 个 Java 文件
- **实体**：AsyncTask（表 t_async_task）
- **枚举**：TaskStatus（PENDING/RUNNING/SUCCESS/FAILED）、TaskType（AUDIO_TRANSCRIBE/RESUME_OPTIMIZE/REVIEW_ANALYSIS）
- **核心功能**：
  - 异步任务创建、查询、删除
  - 音频转录任务（AudioTranscribeTaskService）
  - 任务状态管理（pending -> running -> success/failed）
- **Controller**：TaskController（REST API `/tasks`）
- **Service**：AsyncTaskService（CRUD）、AudioTranscribeTaskService（音频转录）

---

## 常见问题 (FAQ)

### Q: 如何查看 SQL 日志？
A: `application.yml` 中取消注释 `log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl`

### Q: 数据库文件在哪里？
A: `backend/data/careerforge.db`（相对于 backend 目录）

### Q: 如何配置 AI API Key？
A: 设置环境变量 `OPENAI_API_KEY`，或在 `application.yml` 中配置 `spring.ai.openai.api-key`

### Q: 如何配置阿里云语音服务？
A: 设置环境变量 `ALIYUN_API_KEY`，或在 `application.yml` 中配置 `careerforge.voice.aliyun.api-key`

### Q: 如何添加新的简历区块类型？
A:
1. 在 `SectionType` 枚举中添加新类型
2. 在 `ResumeStructuredData` 中创建对应的内部类 DTO
3. 使用 `@SchemaField` 注解标记字段
4. `SectionSchemaRegistry` 会自动包含新类型

### Q: 如何添加新的工作流节点？
A:
1. 在 `resume/graph/optimize/` 或 `resume/graph/tailor/` 下创建 Node 类，实现 `AsyncNodeAction` 接口
2. 在对应 `*GraphConstants` 中定义节点名称常量
3. 在对应 `*GraphConfig` 中使用 `addNode()` 注册节点
4. 在对应 `keyStrategyFactory` 中添加节点需要的状态策略
5. 使用 `addEdge()` 或 `addConditionalEdges()` 连接节点

### Q: 如何添加新的 AI Chat 工具？
A:
1. 在 `chat/tools/` 下创建工具类，使用 `ToolUtils.createCallback()` 包装
2. 在 `ChatAgentConfig.createResumeTools()` 中注册到工具列表
3. 如果需要生成前端事件，在 `AIChatService.buildSuggestionEvents()` 中添加路由逻辑

### Q: 如何添加新的语音面试功能？
A:
1. 在 `interview/voice/service/` 下创建新的 Service 类
2. 如果需要新的 WebSocket 消息类型，在 `VoiceRequest` 和 `VoiceResponse` 中添加
3. 在 `InterviewVoiceGateway` 中添加对应的消息处理逻辑
4. 前端在 `useInterviewVoice.ts` 中添加对应的状态和方法

### Q: 如何添加新的业务模块？
A:
1. 在 `com.careerforge` 下创建模块包
2. 按分层创建 controller/service/mapper/entity/dto/handler 子包
3. 在 `schema.sql` 中添加对应表结构
4. 在 `openapi.yaml` 中定义 API

---

## 相关文件清单

```
backend/
├── pom.xml                              # Maven 配置
├── docs/
│   ├── openapi.yaml                     # API 定义文档
│   ├── api-document.md                  # API 文档
│   └── data-models.md                   # 数据模型文档
├── data/
│   └── careerforge.db                        # SQLite 数据库文件
├── src/main/
│   ├── java/com/careerforge/
│   │   ├── CareerforgeApplication.java       # 应用入口
│   │   ├── common/                      # 公共模块（33 个文件）
│   │   │   ├── annotation/              # @SchemaField 注解
│   │   │   ├── config/                  # 配置类（AI、MyBatis、SQLite、Jackson）
│   │   │   ├── constant/                # 常量
│   │   │   ├── entity/                  # 基础实体
│   │   │   ├── enums/                   # 枚举定义（11 个）
│   │   │   ├── exception/               # 异常处理
│   │   │   ├── handler/                 # MyBatis 处理器
│   │   │   ├── response/                # 统一响应
│   │   │   ├── schema/                  # Schema 注册表
│   │   │   ├── service/                 # 公共服务（AI、文件、搜索）
│   │   │   └── util/                    # 工具类
│   │   ├── user/                        # 用户模块（11 个文件）
│   │   ├── resume/                      # 简历模块（71 个文件）
│   │   │   ├── controller/              # 控制器（5 个）
│   │   │   ├── convertor/               # 转换器
│   │   │   ├── dto/                     # 数据传输对象
│   │   │   ├── entity/                  # 实体类
│   │   │   ├── graph/                   # 工作流（20 个文件）
│   │   │   │   ├── BaseGraphConstants.java    # 公共常量基类
│   │   │   │   ├── optimize/            # 优化工作流子包
│   │   │   │   ├── tailor/              # 定制工作流子包
│   │   │   │   └── rewrite/             # 改写工作流子包
│   │   │   ├── handler/                 # 业务处理器（5 个）
│   │   │   ├── mapper/                  # 数据访问
│   │   │   ├── service/                 # 服务层
│   │   │   └── util/                    # 工具类（5 个）
│   │   ├── chat/                        # AI 聊天模块（30 个文件）
│   │   │   ├── config/                  # Agent 配置
│   │   │   ├── controller/              # 控制器
│   │   │   ├── dto/                     # DTO（含 tool 子包）
│   │   │   ├── entity/                  # 实体
│   │   │   ├── handler/                 # Handler
│   │   │   ├── mapper/                  # Mapper
│   │   │   ├── service/                 # 服务层
│   │   │   └── tools/                   # 工具（9 个）
│   │   ├── interview/                   # 面试模块（20 个文件）
│   │   │   ├── controller/              # 控制器
│   │   │   ├── dto/                     # DTO
│   │   │   ├── entity/                  # 实体
│   │   │   ├── graph/                   # 工作流
│   │   │   │   ├── preparation/         # 准备工作流子包
│   │   │   │   └── review/              # 复盘工作流子包
│   │   │   ├── handler/                 # Handler
│   │   │   ├── mapper/                  # Mapper
│   │   │   ├── service/                 # 服务层
│   │   │   └── voice/                   # 语音面试子模块（37 个文件）
│   │   │       ├── config/              # 阿里云语音配置
│   │   │       ├── controller/          # WebSocket 控制器
│   │   │       ├── dto/                 # DTO
│   │   │       ├── entity/              # 实体
│   │   │       ├── gateway/             # 网关
│   │   │       ├── handler/             # Agent Handler
│   │   │       ├── mapper/              # Mapper
│   │   │       ├── service/             # 服务层
│   │   │       └── util/                # 工具类
│   │   ├── company/                     # 公司模块（6 个文件）
│   │   ├── jobposition/                 # 职位模块（11 个文件）
│   │   ├── task/                        # 异步任务模块（10 个文件）
│   │   ├── statistics/                  # 统计模块（4 个文件）
│   └── resources/
│       ├── application.yml              # 应用配置
│       ├── logback-spring.xml           # 日志配置
│       ├── schema.sql                   # 数据库结构
│       └── skills/                      # AI 技能定义
│           ├── resume-diagnosis/
│           ├── resume-optimizer/
│           └── resume-suggestions/
└── src/test/                            # 测试目录（2 个测试文件）
    └── java/com/careerforge/resume/util/
        ├── ChangeFieldTranslatorTest.java
        └── ResumeChangeApplierTest.java
```

---

## 测试与质量

**当前状态**：项目有 2 个测试文件
- `ChangeFieldTranslatorTest.java` - 字段翻译工具测试
- `ResumeChangeApplierTest.java` - 简历修改应用工具测试

**建议补充**：
1. 单元测试：Service 层业务逻辑测试
2. 集成测试：Controller 层 API 测试
3. 工作流测试：Graph 节点测试
4. Schema 测试：JSON Schema 构建测试
5. 语音面试测试：WebSocket 连接测试、语音服务测试
