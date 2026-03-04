[根目录](../CLAUDE.md) > **backend**

---

# Backend - 后端服务模块

## 变更记录 (Changelog)

| 日期 | 版本 | 变更内容 |
|------|------|----------|
| 2026-03-03 | 1.1.0 | 更新工作流文档、补充 AI 配置详情、完善 API 清单 |
| 2026-02-19 | 1.0.0 | 初始版本 |

---

## 模块职责

后端服务模块负责提供所有业务 API 接口，包括：
- 用户信息管理
- 简历管理、优化、导出
- 面试会话与答题流程
- 面试复盘与分析
- 数据统计
- 职位推荐

---

## 入口与启动

### 主入口
- **文件**：`src/main/java/com/landit/LanditApplication.java`
- **端口**：8080
- **上下文路径**：`/landit`

### 启动命令
```bash
cd backend
mvn spring-boot:run
```

### 环境变量
```bash
# 必需：AI 功能 API Key
export DASHSCOPE_API_KEY=your_api_key

# 可选：AI 模型选择（默认 qwen3.5-plus）
export AI_MODEL=qwen3.5-plus
```

### 访问地址
- API 基础路径：`http://localhost:8080/landit/`
- Swagger UI：`http://localhost:8080/landit/swagger-ui.html`

---

## 对外接口

### API 模块清单

| 模块 | 基础路径 | 控制器 | 描述 |
|------|---------|--------|------|
| user | `/user` | UserController | 用户信息管理 |
| resume | `/resumes` | ResumeController | 简历CRUD与优化 |
| resume-workflow | `/resumes` | ResumeOptimizeGraphController | 简历优化工作流 |
| interview | `/interviews` | InterviewController | 面试会话管理 |
| review | `/reviews` | ReviewController | 面试复盘 |
| statistics | `/statistics` | StatisticsController | 数据统计 |
| job | `/jobs` | JobController | 职位推荐 |

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
| mybatis-plus-spring-boot3-starter | 3.5.9 | ORM 框架 |
| sqlite-jdbc | 3.47.2.0 | SQLite 驱动 |
| lombok | - | 代码简化 |
| hutool-all | 5.8.34 | 工具库 |
| springdoc-openapi-starter-webmvc-ui | 2.8.4 | API 文档 |
| spring-ai-alibaba-agent-framework | 1.1.2.0 | 工作流引擎 |
| spring-ai-alibaba-starter-dashscope | 1.1.2.1 | AI 大模型 |
| pdfbox | 3.0.4 | PDF 处理 |
| poi-ooxml | 5.3.0 | Word 处理 |

### 配置文件 (application.yml)
```yaml
server:
  port: 8080
  servlet:
    context-path: /landit

spring:
  datasource:
    driver-class-name: org.sqlite.JDBC
    url: jdbc:sqlite:./data/landit.db
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:}
      chat:
        options:
          model: ${AI_MODEL:qwen3.5-plus}
          temperature: 0.7

mybatis-plus:
  global-config:
    db-config:
      id-type: assign_id           # 雪花算法主键
      logic-delete-field: deleted  # 逻辑删除字段
```

---

## 核心架构

### 简历优化工作流 Graph

简历优化功能基于 Spring AI Alibaba Agent Framework 构建状态机工作流：

```
START --> DiagnoseQuick --+--[MODE_PRECISE]--> DiagnosePrecise --+
                           |                                      |
                           +--[MODE_QUICK]----------------------->|
                                                                  v
                                                      GenerateSuggestions
                                                                          |
                                                                          v
                                                                  OptimizeSection
                                                                          |
                                                               [interruptBefore]
                                                                          v
                                                                    HumanReview
                                                                          |
                                                                          v
                                                                    SaveVersion --> END
```

**工作流组件：**

| 文件 | 职责 |
|------|------|
| `ResumeOptimizeGraphConfig.java` | 定义工作流节点、边、状态策略 |
| `ResumeOptimizeGraphService.java` | 执行、恢复、状态管理工作流 |
| `ResumeOptimizeGraphConstants.java` | 统一管理状态键、节点名称等常量 |
| `DiagnoseResumeNode.java` | 快速诊断简历问题 |
| `DiagnosePreciseResumeNode.java` | 精准诊断（结合搜索结果） |
| `GenerateSuggestionsNode.java` | 生成优化建议 |
| `OptimizeSectionNode.java` | 优化具体模块内容 |
| `HumanReviewNode.java` | 人工审核中断点 |
| `SaveVersionNode.java` | 保存优化后版本 |

**工作流 API：**

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/{id}/optimize/stream` | SSE流式执行简历优化 |
| POST | `/{id}/optimize` | 同步执行简历优化 |
| GET | `/workflow/state` | 获取工作流状态 |
| POST | `/workflow/review` | 提交人工审核结果 |
| POST | `/workflow/resume` | 恢复工作流执行 |

### 区块类型系统

简历模块采用区块类型系统实现动态简历结构解析：

| 组件 | 路径 | 职责 |
|------|------|------|
| SectionType | `common/enums/` | 定义 8 种区块类型 |
| SectionSchemaRegistry | `common/schema/` | 动态构建简历 JSON Schema |
| GraphSchemaRegistry | `common/schema/` | 工作流节点 Schema 构建 |
| JsonSchemaBuilder | `common/util/` | Schema 构建工具 |
| SchemaGenerator | `common/util/` | 从 Class 生成 Schema |
| AIPromptProperties | `common/config/` | AI 提示词配置 |

### AI 配置

AI 功能通过 `AIPromptProperties` 配置类管理提示词：

**配置路径**：`landit.ai.prompt`

**提示词类别**：
- `resume.parse` - 简历解析提示词
- `resume.diagnose` - 快速诊断提示词
- `resume.diagnosePrecise` - 精准诊断提示词
- `resume.optimizeSection` - 模块优化提示词
- `graph.diagnoseQuick` - 工作流快速诊断提示词
- `graph.diagnosePrecise` - 工作流精准诊断提示词
- `graph.generateSuggestions` - 生成建议提示词
- `graph.optimizeSection` - 内容优化提示词

---

## 数据模型

### 数据库表结构

| 表名 | 实体类 | 描述 |
|------|--------|------|
| t_user | User | 用户信息 |
| t_resume | Resume | 简历主表 |
| t_resume_version | - | 简历历史版本（快照） |
| t_resume_section | ResumeSection | 简历模块（区块） |
| t_resume_suggestion | ResumeSuggestion | 简历优化建议 |
| t_interview | Interview | 面试记录 |
| t_interview_question | InterviewQuestion | 面试题库 |
| t_interview_session | InterviewSession | 面试会话 |
| t_conversation | Conversation | 面试对话 |
| t_interview_review | InterviewReview | 面试复盘 |
| t_review_dimension | ReviewDimension | 复盘维度 |
| t_question_analysis | QuestionAnalysis | 问题分析 |
| t_improvement_plan | ImprovementPlan | 改进计划 |
| t_job | Job | 职位推荐 |

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
| SectionType | BASIC_INFO, EDUCATION, WORK, PROJECT, SKILLS, CERTIFICATE, OPEN_SOURCE, RAW_TEXT | 简历区块类型 |
| Gender | MALE, FEMALE, UNKNOWN | 性别 |
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
- **路径**：`src/main/java/com/landit/common/`
- **组件**：
  - `annotation/` - @SchemaField（标记 DTO 字段的 Schema 元数据）
  - `config/` - MyBatisPlusConfig, SqliteConfig, AIConfig, AIPromptProperties
  - `entity/` - BaseEntity
  - `enums/` - SectionType, Gender 等 11 个枚举类
  - `exception/` - BusinessException, GlobalExceptionHandler
  - `handler/` - MyMetaObjectHandler（自动填充）
  - `response/` - ApiResponse, PageResponse
  - `schema/` - SectionSchemaRegistry, GraphSchemaRegistry
  - `service/` - AIService, FileToImageService, SearchService
  - `util/` - JsonSchemaBuilder, SchemaGenerator, ChatClientHelper, JsonParseHelper

### user - 用户模块
- **路径**：`src/main/java/com/landit/user/`
- **实体**：User（name, gender, avatar）
- **API**：
  - `GET /user/status` - 获取用户状态
  - `POST /user/init` - 初始化用户
  - `GET /user/profile` - 获取用户信息
  - `PUT /user/profile` - 更新用户信息
  - `POST /user/avatar` - 上传头像

### resume - 简历模块
- **路径**：`src/main/java/com/landit/resume/`
- **实体**：Resume, ResumeSection, ResumeSuggestion, ResumeVersion
- **控制器**：ResumeController, ResumeOptimizeGraphController
- **Handler**：ResumeHandler, ResumeOptimizeGraphHandler
- **核心功能**：
  - 简历 CRUD
  - 版本管理与回滚
  - 简历派生（岗位定制）
  - AI 优化工作流
  - PDF 导出
  - 模块级 CRUD

### resume/graph - 简历优化工作流
- **路径**：`src/main/java/com/landit/resume/graph/`
- **节点**：DiagnoseResumeNode, DiagnosePreciseResumeNode, GenerateSuggestionsNode, OptimizeSectionNode, HumanReviewNode, SaveVersionNode
- **配置**：ResumeOptimizeGraphConfig, ResumeOptimizeGraphConstants
- **服务**：ResumeOptimizeGraphService

### interview - 面试模块
- **路径**：`src/main/java/com/landit/interview/`
- **实体**：Interview, InterviewQuestion, InterviewSession, Conversation
- **核心功能**：
  - 面试会话管理
  - 答题与评分
  - 提示系统
  - 题库管理

### review - 复盘模块
- **路径**：`src/main/java/com/landit/review/`
- **实体**：InterviewReview, ReviewDimension, QuestionAnalysis, ImprovementPlan
- **核心功能**：
  - 多维度评分
  - 问题分析
  - 改进计划生成
  - 报告导出

### statistics - 统计模块
- **路径**：`src/main/java/com/landit/statistics/`
- **DTO**：StatisticsVO
- **核心功能**：
  - 总览数据（面试次数、平均分、提升率）
  - 周进度追踪
  - 技能雷达图
  - 最近活动

### job - 职位推荐模块
- **路径**：`src/main/java/com/landit/job/`
- **实体**：Job
- **核心功能**：
  - 职位推荐
  - 匹配度计算

---

## 常见问题 (FAQ)

### Q: 如何查看 SQL 日志？
A: `application.yml` 中已配置 `log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl`

### Q: 数据库文件在哪里？
A: `backend/data/landit.db`（相对于 backend 目录）

### Q: 如何配置 AI API Key？
A: 设置环境变量 `DASHSCOPE_API_KEY`，或在 `application.yml` 中配置 `spring.ai.dashscope.api-key`

### Q: 如何添加新的简历区块类型？
A:
1. 在 `SectionType` 枚举中添加新类型
2. 在 `ResumeStructuredData` 中创建对应的内部类 DTO
3. 使用 `@SchemaField` 注解标记字段
4. `SectionSchemaRegistry` 会自动包含新类型

### Q: 如何添加新的工作流节点？
A:
1. 在 `resume/graph/` 下创建 Node 类，实现 `AsyncNodeAction` 接口
2. 在 `ResumeOptimizeGraphConstants` 中定义节点名称常量
3. 在 `ResumeOptimizeGraphConfig` 中使用 `addNode()` 注册节点
4. 在 `resumeOptimizeKeyStrategyFactory` 中添加节点需要的状态策略
5. 使用 `addEdge()` 或 `addConditionalEdges()` 连接节点

### Q: 如何添加新的业务模块？
A:
1. 在 `com.landit` 下创建模块包
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
│   └── landit.db                        # SQLite 数据库文件
├── src/main/
│   ├── java/com/landit/
│   │   ├── LanditApplication.java       # 应用入口
│   │   ├── common/                      # 公共模块
│   │   │   ├── annotation/              # @SchemaField 注解
│   │   │   ├── config/                  # 配置类（AI、MyBatis、SQLite）
│   │   │   ├── entity/                  # 基础实体
│   │   │   ├── enums/                   # 枚举定义
│   │   │   ├── exception/               # 异常处理
│   │   │   ├── handler/                 # MyBatis 处理器
│   │   │   ├── response/                # 统一响应
│   │   │   ├── schema/                  # Schema 注册表
│   │   │   ├── service/                 # 公共服务（AI、文件、搜索）
│   │   │   └── util/                    # 工具类
│   │   ├── user/                        # 用户模块
│   │   ├── resume/                      # 简历模块
│   │   │   ├── controller/              # 控制器
│   │   │   ├── convertor/               # 转换器
│   │   │   ├── dto/                     # 数据传输对象
│   │   │   ├── entity/                  # 实体类
│   │   │   ├── graph/                   # 工作流节点
│   │   │   ├── handler/                 # 业务处理器
│   │   │   ├── mapper/                  # 数据访问
│   │   │   └── service/                 # 服务层
│   │   ├── interview/                   # 面试模块
│   │   ├── review/                      # 复盘模块
│   │   ├── statistics/                  # 统计模块
│   │   └── job/                         # 职位模块
│   └── resources/
│       ├── application.yml              # 应用配置
│       ├── logback-spring.xml           # 日志配置
│       └── schema.sql                   # 数据库结构
└── src/test/                            # 测试目录（待补充）
```

---

## 测试与质量

**当前状态**：项目暂无测试文件

**建议补充**：
1. 单元测试：Service 层业务逻辑测试
2. 集成测试：Controller 层 API 测试
3. 工作流测试：Graph 节点测试
4. Schema 测试：JSON Schema 构建测试
