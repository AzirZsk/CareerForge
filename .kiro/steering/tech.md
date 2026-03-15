# 技术栈

## 后端

- Java 17 + Spring Boot 3.5.11
- ORM：MyBatis-Plus 3.5.9（分页插件 jsqlparser）
- 数据库：SQLite（文件 `backend/data/landit.db`）
- AI：Spring AI OpenAI 1.1.2 + Spring AI Alibaba Agent Framework 1.1.2.0（状态机工作流）
- 文档处理：Apache PDFBox 3.0.4（PDF）、Apache POI 5.3.0（Word）
- 对象映射：MapStruct 1.6.3
- 工具库：Lombok、Hutool 5.8.34
- API 文档：SpringDoc OpenAPI 2.8.4（Swagger UI：`/landit/swagger-ui.html`）
- 构建工具：Maven

## 前端

- Vue 3.4 + TypeScript 5.4
- 构建工具：Vite 5
- 状态管理：Pinia 2.1
- 路由：Vue Router 4.3
- 样式：SCSS + 全局变量系统（深色主题，琥珀金强调色）
- 工具库：@vueuse/core 10.9

## 常用命令

### 后端
```bash
cd backend
mvn spring-boot:run          # 启动开发服务器（端口 8080，上下文路径 /landit）
mvn clean package            # 构建生产包
mvn clean compile            # 仅编译
```

### 前端
```bash
cd frontend
npm run dev                  # 启动开发服务器（端口 5173）
npm run build                # 构建生产包（含类型检查）
npm run type-check           # TypeScript 类型检查
npm run preview              # 预览构建结果
```

## 环境变量

- `OPENAI_API_KEY`（必需）：AI 功能 API Key
- `OPENAI_BASE_URL`（可选）：OpenAI API 基础 URL，默认 `https://api.openai.com`
- `AI_MODEL`（可选）：AI 模型，默认 `gpt-4o`
- `VITE_API_TARGET`（前端）：后端地址，默认 `http://localhost:8080`

## 测试

项目当前无测试文件。后端测试依赖已配置（spring-boot-starter-test）。
