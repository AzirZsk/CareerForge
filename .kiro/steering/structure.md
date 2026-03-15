# 项目结构

前后端分离架构，前端通过 Vite 代理 `/landit` 路径到后端。

```
├── backend/                          # Java Spring Boot 后端
│   ├── pom.xml
│   ├── data/landit.db                # SQLite 数据库文件
│   ├── docs/                         # API 文档、数据模型文档、openapi.yaml
│   └── src/main/java/com/landit/
│       ├── LanditApplication.java    # 应用入口
│       ├── common/                   # 公共模块：配置、枚举、异常、响应、Schema、工具类
│       ├── user/                     # 用户模块
│       ├── resume/                   # 简历模块（含 graph/ 工作流子模块）
│       ├── interview/                # 面试模块
│       ├── review/                   # 复盘模块
│       ├── statistics/               # 统计模块
│       └── job/                      # 职位推荐模块
│
└── frontend/                         # Vue 3 + TypeScript 前端
    ├── package.json
    ├── vite.config.ts
    └── src/
        ├── main.ts                   # 应用入口
        ├── router/index.ts           # 路由配置
        ├── stores/index.ts           # Pinia 状态管理
        ├── types/                    # TypeScript 类型定义
        ├── api/                      # 后端 API 调用
        ├── composables/              # 组合式函数（useResumeOptimize 等）
        ├── views/                    # 页面组件（9 个）
        ├── components/               # 公共组件、简历组件、表单组件
        ├── mock/data.ts              # Mock 数据
        └── assets/styles/            # SCSS 变量和全局样式
```

## 后端分层约定

每个业务模块遵循统一分层：
- `controller/` — REST 控制器（不写业务逻辑）
- `handler/` — 业务编排层（协调 service 调用）
- `service/` — 服务层（接口继承 `IService<T>`）
- `mapper/` — MyBatis-Plus 数据访问层
- `entity/` — 数据库实体（继承 `BaseEntity`）
- `dto/` — 数据传输对象
- `convertor/` — MapStruct 对象转换器

## 编码规范

### Java
- 类注释添加 `@author Azir`
- 使用 `@RequiredArgsConstructor` + `private final` 构造注入
- 主键策略：雪花算法（ASSIGN_ID）
- 逻辑删除字段：`deleted`（0=未删除，1=已删除）
- 自动填充：`createdAt`（插入）、`updatedAt`（插入+更新）
- 简单查询用 MyBatis-Plus 条件构造器，复杂联查用 XML

### TypeScript / Vue
- 使用 Composition API + `<script setup>`
- 类型定义集中在 `types/` 目录
- 状态管理使用 Pinia Store
- SCSS 变量通过 Vite 全局注入，组件中直接使用

## API 约定

- 所有 API 前缀：`/landit`
- 统一响应格式：`{ code, message, data, timestamp }`
- API 定义文档：`backend/docs/openapi.yaml`
- 数据库结构：`backend/src/main/resources/schema.sql`
