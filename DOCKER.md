# CareerForge Docker 部署指南

## 架构说明

本项目采用**单镜像双服务**架构，使用 Supervisor 管理两个进程：

```
┌─────────────────────────────────────────────────────┐
│                   Docker 容器                        │
│                                                      │
│  ┌──────────────┐         ┌──────────────┐         │
│  │    Nginx     │         │ Spring Boot  │         │
│  │   :80 (前端)  │◄──────►│  :8080 (后端) │         │
│  │              │  127.0.0.1              │         │
│  │  静态文件     │         │   API 服务    │         │
│  │  反向代理     │         │   WebSocket  │         │
│  └──────────────┘         └──────────────┘         │
│                                                      │
│            Supervisor（进程管理）                    │
└─────────────────────────────────────────────────────┘
```

### 端口映射

| 容器内端口 | 说明 | 外部访问 |
|-----------|------|---------|
| 80 | Nginx（前端 + 反向代理） | `http://localhost:80` |
| 8080 | Spring Boot（仅内部通信） | ❌ 不暴露 |

### 请求路由

| 路径 | 处理方式 |
|------|---------|
| `/`、`/resume`、`/interview` 等 | Nginx → 静态文件（SPA 路由） |
| `/careerforge/**` | Nginx → Spring Boot（API 代理） |
| `/careerforge/ws/**` | Nginx → Spring Boot（WebSocket） |

---

## 快速开始

### 1. 配置环境变量

```bash
cp .env.example .env
vim .env
```

必填项：
- `OPENAI_API_KEY` - OpenAI API Key
- `ALIYUN_ACCESS_KEY_ID` - 阿里云 Access Key ID
- `ALIYUN_ACCESS_KEY_SECRET` - 阿里云 Access Key Secret

### 2. 启动服务

```bash
# 构建并启动（首次运行）
docker-compose up -d

# 查看日志
docker-compose logs -f

# 查看服务状态
docker-compose ps
```

### 3. 访问应用

```
前端：http://localhost
Swagger：http://localhost/careerforge/swagger-ui.html
```

---

## 常用命令

```bash
# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 重新构建（代码变更后）
docker-compose build --no-cache
docker-compose up -d

# 查看后端日志
docker-compose exec careerforge tail -f /var/log/supervisor/backend.out.log

# 查看前端日志
docker-compose exec careerforge tail -f /var/log/supervisor/nginx.out.log

# 进入容器
docker-compose exec careerforge sh
```

---

## 镜像结构

### 构建阶段

1. **frontend-builder** - Node 构建前端，产出 `dist/`
2. **backend-builder** - Maven 构建后端，产出 `*.jar`
3. **final** - 合并镜像，包含：
   - Nginx（前端静态文件）
   - JRE（后端运行环境）
   - Supervisor（进程管理）

### 最终镜像大小

约 **350MB**（Nginx + JRE + 应用代码）

---

## 数据持久化

| 容器内路径 | 说明 | 宿主机映射 |
|-----------|------|-----------|
| `/app/data` | SQLite 数据库 | `./backend/data` |

---

## 故障排查

### 服务无法启动

```bash
# 查看详细日志
docker-compose logs -f

# 检查容器状态
docker-compose ps

# 查看 Supervisor 进程状态
docker-compose exec careerforge supervisorctl status
```

### 前端 404

检查 Nginx 配置：
```bash
docker-compose exec careerforge cat /etc/nginx/http.d/default.conf
```

### 后端 API 500

查看后端日志：
```bash
docker-compose exec careerforge cat /var/log/supervisor/backend.out.log
```

### WebSocket 连接失败

检查 Nginx WebSocket 配置是否正确（见 `docker/nginx.conf`）

---

## 生产环境建议

1. **使用外部数据库**：SQLite 仅适合开发/小规模部署
2. **设置资源限制**：在 `docker-compose.yml` 中添加 `deploy.resources`
3. **配置 HTTPS**：使用 Nginx 反向代理或 Let's Encrypt
4. **日志管理**：挂载日志目录，使用日志收集工具
5. **健康检查**：已内置健康检查，可配置告警
