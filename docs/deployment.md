# 部署指南

## Docker 部署（推荐） {#docker}

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

### 快速开始

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

### 常用命令

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

---

## 环境变量 {#env}

通过 `.env` 文件或 `docker-compose.yml` 的 `environment` 配置：

| 变量 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| `OPENAI_API_KEY` | 是 | - | AI 模型 API Key（支持 OpenAI 协议的模型） |
| `OPENAI_BASE_URL` | 否 | `https://api.openai.com` | API 基础 URL（可替换为兼容接口） |
| `AI_MODEL` | 否 | `gpt-4o` | AI 模型名称 |
| `ALIYUN_ACCESS_KEY_ID` | 否 | - | 阿里云语音服务 AccessKey ID |
| `ALIYUN_ACCESS_KEY_SECRET` | 否 | - | 阿里云语音服务 AccessKey Secret |
| `ALIYUN_VOICE_APP_KEY` | 否 | - | 阿里云语音服务 AppKey |

::: tip
阿里云语音服务仅 AI 语音模拟面试功能需要，不使用该功能可不配置。
:::

---

## 传统部署 {#manual}

不使用 Docker，分别构建部署前后端。

### 环境要求

- Java 17+
- Node.js 18+
- Maven 3.8+

### 后端

```bash
cd backend

# 设置环境变量
export OPENAI_API_KEY=your_api_key

# 编译打包
mvn clean package

# 运行
java -jar target/careerforge-*.jar
```

后端默认运行在 `8080` 端口。

### 前端

```bash
cd frontend

# 安装依赖
npm install

# 构建生产包
npm run build

# 预览构建结果
npm run preview
```

构建产物在 `dist/` 目录，需要用 Nginx 等 Web 服务器托管，并配置反向代理到后端。

---

## Nginx 配置 {#nginx}

生产环境的 Nginx 配置参考：

```nginx
server {
    listen 80;
    server_name localhost;

    root /usr/share/nginx/html;
    index index.html;

    # 静态资源缓存
    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # API 反向代理
    location /careerforge/ {
        proxy_pass http://127.0.0.1:8080/careerforge/;
        proxy_set_header Host $host;
        proxy_connect_timeout 60s;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }

    # WebSocket 代理
    location /careerforge/ws/ {
        proxy_pass http://127.0.0.1:8080/careerforge/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 604800s;
    }

    # SPA 路由回退
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

**关键配置说明：**

| 配置项 | 说明 |
|--------|------|
| 静态资源缓存 | 带 hash 的 JS/CSS/图片设置 1 年缓存 |
| API 代理 | `/careerforge/` 路径代理到后端 8080 |
| WebSocket | `/careerforge/ws/` 路径启用 Upgrade 头，7 天长连接 |
| SPA 回退 | 所有未匹配路径返回 `index.html`，支持前端路由 |

---

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

---

## 故障排除 {#troubleshooting}

### 服务无法启动

```bash
# 查看容器日志
docker-compose logs -f

# 检查端口占用
lsof -i :80
```

常见原因：端口 80 被占用、环境变量未配置、内存不足。

### 前端页面 404

检查 Nginx 配置是否包含 SPA 回退规则：

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

### 后端 API 返回 500

```bash
# 查看后端日志
docker exec -it careerforge tail -f /var/log/supervisor/careerforge-stdout.log

# 检查 API Key 是否正确
docker exec -it careerforge env | grep OPENAI
```

### WebSocket 连接失败

确认 Nginx 配置了 WebSocket 代理头：

```nginx
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
```

检查 `proxy_read_timeout` 是否足够长（建议 604800s）。
