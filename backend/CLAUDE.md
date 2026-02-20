[根目录](../CLAUDE.md) > **backend**

---

# Backend - 后端服务模块

## 变更记录 (Changelog)

| 时间 | 变更内容 |
|------|---------|
| 2026-02-19 21:53:42 | 初始化模块文档 |

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

### 访问地址
- API 基础路径：`http://localhost:8080/landit/api/`
- Swagger UI：`http://localhost:8080/landit/swagger-ui.html`

---

## 对外接口

### API 模块清单

| 模块 | 基础路径 | 控制器 | 描述 |
|------|---------|--------|------|
| user | `/api/user` | UserController | 用户信息管理 |
| resume | `/api/resumes` | ResumeController | 简历CRUD与优化 |
| interview | `/api/interviews` | InterviewController | 面试会话管理 |
| review | `/api/reviews` | ReviewController | 面试复盘 |
| statistics | `/api/statistics` | StatisticsController | 数据统计 |
| job | `/api/jobs` | JobController | 职位推荐 |

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

mybatis-plus:
  global-config:
    db-config:
      id-type: assign_id           # 雪花算法主键
      logic-delete-field: deleted  # 逻辑删除字段
```

---

## 数据模型

### 数据库表结构

| 表名 | 实体类 | 描述 |
|------|--------|------|
| t_user | User | 用户信息 |
| t_resume | Resume | 简历 |
| t_resume_section | ResumeSection | 简历模块 |
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
| ResumeStatus | OPTIMIZED, DRAFT | 简历状态 |
| InterviewType | TECHNICAL, BEHAVIORAL | 面试类型 |
| InterviewStatus | IN_PROGRESS, COMPLETED | 面试状态 |
| QuestionDifficulty | EASY, MEDIUM, HARD | 题目难度 |
| ConversationRole | INTERVIEWER, CANDIDATE | 对话角色 |
| SuggestionType | CRITICAL, IMPROVEMENT, ENHANCEMENT | 建议类型 |
| ActivityType | INTERVIEW, RESUME, PRACTICE, REVIEW | 活动类型 |

---

## 子模块详情

### common - 公共模块
- **路径**：`src/main/java/com/landit/common/`
- **组件**：
  - `config/` - MyBatisPlusConfig, SqliteConfig
  - `entity/` - BaseEntity
  - `enums/` - 7个枚举类
  - `handler/` - MyMetaObjectHandler（自动填充）
  - `response/` - ApiResponse, PageResponse

### user - 用户模块
- **路径**：`src/main/java/com/landit/user/`
- **实体**：User（name, avatar, email, phone, skills...）
- **API**：
  - `GET /api/user/profile` - 获取用户信息
  - `PUT /api/user/profile` - 更新用户信息
  - `POST /api/user/avatar` - 上传头像

### resume - 简历模块
- **路径**：`src/main/java/com/landit/resume/`
- **实体**：Resume, ResumeSection, ResumeSuggestion
- **核心功能**：
  - 简历 CRUD
  - AI 优化建议生成
  - PDF 导出
  - 主简历设置

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

## 测试与质量

**当前状态**：未发现测试代码

**测试目录建议**：
- 单元测试：`src/test/java/com/landit/*/service/`
- 集成测试：`src/test/java/com/landit/*/controller/`

---

## 常见问题 (FAQ)

### Q: 如何查看 SQL 日志？
A: `application.yml` 中已配置 `log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl`

### Q: 数据库文件在哪里？
A: `backend/data/landit.db`（相对于 backend 目录）

### Q: 如何添加新的业务模块？
A:
1. 在 `com.landit` 下创建模块包
2. 按分层创建 controller/service/mapper/entity/dto 子包
3. 在 `schema.sql` 中添加对应表结构
4. 在 `openapi.yaml` 中定义 API

---

## 相关文件清单

```
backend/
├── pom.xml                              # Maven 配置
├── docs/
│   └── openapi.yaml                     # API 定义文档
├── data/
│   └── landit.db                        # SQLite 数据库文件
├── src/main/
│   ├── java/com/landit/
│   │   ├── LanditApplication.java       # 应用入口
│   │   ├── common/                      # 公共模块
│   │   ├── user/                        # 用户模块
│   │   ├── resume/                      # 简历模块
│   │   ├── interview/                   # 面试模块
│   │   ├── review/                      # 复盘模块
│   │   ├── statistics/                  # 统计模块
│   │   └── job/                         # 职位模块
│   └── resources/
│       ├── application.yml              # 应用配置
│       └── schema.sql                   # 数据库结构
└── src/test/                            # 测试目录（待补充）
```

---
*最后更新：2026-02-19 21:53:42*
