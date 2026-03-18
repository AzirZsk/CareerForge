[根目录](../CLAUDE.md) > **backend**

---

# Backend - 后端服务模块

## 变更记录 (Changelog)

| 日期 | 版本 | 变更内容 |
|------|------|----------|
| 2026-03-18 | 1.5.0 | AI 上下文全面扫描更新：更新文件统计（110 个 Java 文件、8 个控制器、11 个服务、8 个 Handler） |
| 2026-03-18 | 1.4.0 | 定制简历限制条件改为"仅已优化简历" |
| 2026-03-08 | 1.3.0 | 更新工作流结构（简化为三节点）、更新文件清单、补充 Composables 说明 |
| 2026-03-06 | 1.2.0 | 从 DashScope Starter 迁移到 OpenAI Starter |
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
export OPENAI_API_KEY=your_api_key

# 可选：OpenAI API 基础 URL（默认 https://api.openai.com）
export OPENAI_BASE_URL=https://api.openai.com

# 可选：AI 模型选择（默认 gpt-4o）
export AI_MODEL=gpt-4o
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
| resume-tailor | `/resumes` | TailorResumeController | 简历定制工作流 |
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
| mybatis-plus-jsqlparser | 3.5.9 | 分页插件 |
| sqlite-jdbc | 3.47.2.0 | SQLite 驱动 |
| lombok | - | 代码简化 |
| hutool-all | 5.8.34 | 工具库 |
| springdoc-openapi-starter-webmvc-ui | 2.8.4 | API 文档 |
| spring-ai-alibaba-agent-framework | 1.1.2.0 | 工作流引擎 |
| spring-ai-starter-model-openai | 1.1.2 | AI 大模型（OpenAI 协议） |
| pdfbox | 3.0.4 | PDF 处理 |
| poi-ooxml | 5.3.0 | Word 处理 |
| mapstruct | 1.6.3 | 对象映射 |

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
    openai:
      api-key: ${OPENAI_API_KEY:}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: ${AI_MODEL:gpt-4o}
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
START --> DiagnoseQuick --> GenerateSuggestions --> OptimizeSection --> END
           (快速诊断)           (生成建议)              (内容优化)
```

**当前工作流为简化版本，仅包含三个核心节点：**

| 节点 | 类名 | 职责 |
|------|------|------|
| diagnose_quick | DiagnoseResumeNode | 快速诊断简历问题（评分、建议） |
| generate_suggestions | GenerateSuggestionsNode | 基于诊断结果生成优化建议 |
| optimize_section | OptimizeSectionNode | 根据建议优化简历内容 |

**工作流配置文件：**

| 文件 | 职责 |
|------|------|
| `ResumeOptimizeGraphConfig.java` | 定义工作流节点、边、状态策略 |
| `ResumeOptimizeGraphService.java` | 执行、恢复、状态管理工作流 |
| `ResumeOptimizeGraphConstants.java` | 统一管理状态键、节点名称等常量 |

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

| 节点 | 类名 | 职责 |
|------|------|------|
| analyze_jd | AnalyzeJDNode | 分析职位描述，提取必备技能、关键词等 |
| match_resume | MatchResumeNode | 匹配简历与 JD，计算匹配度 |
| generate_tailored | GenerateTailoredResumeNode | 根据匹配分析生成定制简历 |

**工作流配置文件：**

| 文件 | 职责 |
|------|------|
| `TailorResumeGraphConfig.java` | 定义工作流节点、边、状态策略 |
| `TailorResumeGraphService.java` | 执行、恢复、状态管理工作流 |
| `TailorResumeGraphConstants.java` | 统一管理状态键、节点名称等常量 |

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

**配置路径**：`landit.ai.prompt`

**提示词类别**：
- `resume.parse` - 简历解析提示词
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
| t_resume_version | ResumeVersion | 简历历史版本（快照） |
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
- **路径**：`src/main/java/com/landit/common/`
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

### user - 用户模块
- **路径**：`src/main/java/com/landit/user/`
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
- **路径**：`src/main/java/com/landit/resume/`
- **文件统计**：39 个 Java 文件
- **实体**：Resume, ResumeSection, ResumeSuggestion, ResumeVersion
- **控制器**：ResumeController, ResumeOptimizeGraphController, TailorResumeController
- **Handler**：ResumeHandler, ResumeOptimizeGraphHandler, TailorResumeGraphHandler
- **核心功能**：
  - 简历 CRUD
  - 版本管理与回滚
  - 简历派生（岗位定制）
  - AI 优化工作流（SSE 流式）
  - PDF 导出
  - 模块级 CRUD

### resume/graph - 简历优化/定制工作流
- **路径**：`src/main/java/com/landit/resume/graph/`
- **文件统计**：13 个 Java 文件
- **优化节点**：DiagnoseResumeNode, GenerateSuggestionsNode, OptimizeSectionNode
- **定制节点**：AnalyzeJDNode, MatchResumeNode, GenerateTailoredResumeNode
- **配置**：ResumeOptimizeGraphConfig, ResumeOptimizeGraphConstants, TailorResumeGraphConfig, TailorResumeGraphConstants
- **服务**：ResumeOptimizeGraphService, TailorResumeGraphService
- **Handler**：ResumeOptimizeGraphHandler, TailorResumeGraphHandler

### interview - 面试模块
- **路径**：`src/main/java/com/landit/interview/`
- **文件统计**：20 个 Java 文件
- **实体**：Interview, InterviewQuestion, InterviewSession, Conversation
- **核心功能**：
  - 面试会话管理
  - 答题与评分
  - 提示系统
  - 题库管理

### review - 复盘模块
- **路径**：`src/main/java/com/landit/review/`
- **文件统计**：13 个 Java 文件
- **实体**：InterviewReview, ReviewDimension, QuestionAnalysis, ImprovementPlan
- **核心功能**：
  - 多维度评分
  - 问题分析
  - 改进计划生成
  - 报告导出

### statistics - 统计模块
- **路径**：`src/main/java/com/landit/statistics/`
- **文件统计**：4 个 Java 文件
- **DTO**：StatisticsVO
- **核心功能**：
  - 总览数据（面试次数、平均分、提升率）
  - 周进度追踪
  - 技能雷达图
  - 最近活动

### job - 职位推荐模块
- **路径**：`src/main/java/com/landit/job/`
- **文件统计**：4 个 Java 文件
- **实体**：Job
- **核心功能**：
  - 职位推荐
  - 匹配度计算

---

## 常见问题 (FAQ)

### Q: 如何查看 SQL 日志？
A: `application.yml` 中取消注释 `log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl`

### Q: 数据库文件在哪里？
A: `backend/data/landit.db`（相对于 backend 目录）

### Q: 如何配置 AI API Key？
A: 设置环境变量 `OPENAI_API_KEY`，或在 `application.yml` 中配置 `spring.ai.openai.api-key`

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
│   │   ├── common/                      # 公共模块（33 个文件）
│   │   │   ├── annotation/              # @SchemaField 注解
│   │   │   ├── config/                  # 配置类（AI、MyBatis、SQLite、Jackson）
│   │   │   ├── entity/                  # 基础实体
│   │   │   ├── enums/                   # 枚举定义（11 个）
│   │   │   ├── exception/               # 异常处理
│   │   │   ├── handler/                 # MyBatis 处理器
│   │   │   ├── response/                # 统一响应
│   │   │   ├── schema/                  # Schema 注册表
│   │   │   ├── service/                 # 公共服务（AI、文件、搜索）
│   │   │   └── util/                    # 工具类
│   │   ├── user/                        # 用户模块（11 个文件）
│   │   ├── resume/                      # 简历模块（39 个文件）
│   │   │   ├── controller/              # 控制器
│   │   │   ├── convertor/               # 转换器
│   │   │   ├── dto/                     # 数据传输对象
│   │   │   ├── entity/                  # 实体类
│   │   │   ├── graph/                   # 工作流节点（13 个文件）
│   │   │   ├── handler/                 # 业务处理器
│   │   │   ├── mapper/                  # 数据访问
│   │   │   ├── service/                 # 服务层
│   │   │   └── util/                    # 工具类
│   │   ├── interview/                   # 面试模块（20 个文件）
│   │   ├── review/                      # 复盘模块（13 个文件）
│   │   ├── statistics/                  # 统计模块（4 个文件）
│   │   └── job/                         # 职位模块（4 个文件）
│   └── resources/
│       ├── application.yml              # 应用配置
│       ├── logback-spring.xml           # 日志配置
│       └── schema.sql                   # 数据库结构
└── src/test/                            # 测试目录（1 个测试文件）
```

---

## 测试与质量

**当前状态**：项目有 1 个测试文件

**建议补充**：
1. 单元测试：Service 层业务逻辑测试
2. 集成测试：Controller 层 API 测试
3. 工作流测试：Graph 节点测试
4. Schema 测试：JSON Schema 构建测试
