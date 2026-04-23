# Docker 部署

项目采用**单镜像双服务**架构，Nginx + Spring Boot 由 Supervisor 统一管理：

```
┌─────────────────────────────────────┐
│           Docker Container          │
│                                     │
│  ┌─────────┐     ┌──────────────┐  │
│  │  Nginx  │────→│ Spring Boot  │  │
│  │  :80    │     │    :8080     │  │
│  └─────────┘     └──────────────┘  │
│       ↓                 ↓           │
│  静态资源/API代理    后端服务        │
└─────────────────────────────────────┘
        ↓
    宿主机 :80
```

**镜像构建：** 三阶段 Dockerfile（前端构建 → 后端构建 → 最终镜像约 350MB）

## 快速开始

```bash
# 1. 克隆仓库
git clone https://github.com/AzirZsk/Landit.git
cd Landit

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env 填入你的 API Key

# 3. 启动服务
docker-compose up -d
```

启动后访问 `http://localhost` 即可。

## 环境变量

通过 `.env` 文件或 `docker-compose.yml` 的 `environment` 配置：

| 变量 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| `OPENAI_API_KEY` | 是 | - | AI 模型 API Key（支持 OpenAI 协议的模型） |
| `OPENAI_BASE_URL` | 否 | `https://api.openai.com` | API 基础 URL（可替换为兼容接口） |
| `AI_MODEL` | 否 | `gpt-4o` | AI 模型名称 |
| `ALIYUN_API_KEY` | 否 | - | 阿里云 DashScope API Key（语音服务） |

::: tip
阿里云语音服务仅 AI 语音模拟面试功能需要，不使用该功能可不配置。
:::

## 常用命令

```bash
# 启动
docker-compose up -d

# 停止
docker-compose down

# 重启
docker-compose restart

# 重新构建并启动
docker-compose up -d --build

# 查看日志
docker-compose logs -f

# 进入容器
docker exec -it careerforge sh
```

## 数据持久化

项目使用 SQLite 文件数据库，数据存储在 `backend/data/careerforge.db`。

Docker 部署时通过卷挂载持久化：

```yaml
volumes:
  - ./backend/data:/app/data   # SQLite 数据库文件
```

::: warning
**数据备份**：定期备份 `backend/data/careerforge.db` 文件即可。SQLite 不支持并发写入，生产环境建议迁移至 PostgreSQL 或 MySQL。
:::
