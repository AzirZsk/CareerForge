# CareerForge 部署指南（Claude Code 专用）

> 本文件是面向 Claude Code 的结构化部署指令。当用户将此文件的 URL 发送给 Claude Code 时，Claude 应按照以下步骤完成 CareerForge 的自动部署。

## 项目简介

CareerForge 是一款智能求职助手，提供简历管理、AI 对话式简历优化、AI 语音模拟面试、模拟面试训练、面试复盘分析等功能。

技术栈：Vue 3 + Spring Boot 3 + SQLite，使用 Docker 单容器部署（Nginx + Spring Boot via Supervisor）。

---

## 部署流程

严格按以下阶段顺序执行。每个阶段都有验证条件，验证通过后才进入下一阶段。

### 阶段 1：前置条件检查

在执行任何操作前，先检查用户的机器是否具备部署条件。

**检查 Docker：**

```bash
docker info >/dev/null 2>&1 && echo "Docker OK" || echo "Docker NOT FOUND"
```

如果 Docker 未安装或未启动：
- 提示用户安装 Docker：`curl -fsSL https://get.docker.com | sh`
- 启动 Docker 服务：`sudo systemctl start docker && sudo systemctl enable docker`
- 等待用户确认后重新检查

**检查 Docker Compose：**

```bash
docker compose version >/dev/null 2>&1 && echo "Compose V2 OK" || (docker-compose version >/dev/null 2>&1 && echo "Compose V1 OK" || echo "Compose NOT FOUND")
```

如果两者都不可用：
- Docker Compose V2 通常随 Docker 一起安装，提示用户更新 Docker
- 如果是 V1，后续命令中用 `docker-compose` 替代 `docker compose`

**检查 git：**

```bash
git --version >/dev/null 2>&1 && echo "Git OK" || echo "Git NOT FOUND"
```

如果 git 未安装：`sudo apt-get install -y git`（Ubuntu/Debian）或 `sudo yum install -y git`（CentOS/RHEL）

**检查端口占用：**

```bash
ss -tlnp | grep ':80 ' || echo "Port 80 available"
```

如果端口 80 被占用，告知用户并建议：停止占用端口的服务，或修改 `docker-compose.yml` 中的端口映射（如改为 `8080:80`）。

---

### 阶段 2：克隆仓库

如果用户已经在项目目录中，跳过此步骤。否则：

```bash
cd ~
git clone {{REPO_URL}} careerforge
cd careerforge
```

如果用户提供了其他目录偏好，使用用户指定的路径。

---

### 阶段 3：配置环境变量

这是最关键的一步，需要与用户交互获取配置信息。

**步骤 3.1：创建 .env 文件**

```bash
test -f .env && echo "EXISTS" || cp .env.example .env
```

**步骤 3.2：收集用户配置**

逐项询问用户以下配置。不要自行编造任何值。

| 变量 | 必填 | 说明 | 默认值 |
|------|------|------|--------|
| OPENAI_API_KEY | 是 | OpenAI API Key | 无，必须用户提供 |
| OPENAI_BASE_URL | 否 | API 地址（支持 OpenAI 协议的第三方服务） | https://api.openai.com |
| AI_MODEL | 否 | AI 模型名称 | gpt-4o |
| ALIYUN_API_KEY | 否 | 阿里云 API Key（语音面试功能需要） | 无 |
| JWT_SECRET | 否 | JWT 认证密钥 | 自动生成 |

询问用户的格式示例：

> 请提供你的 OpenAI API Key（必填，用于 AI 功能）：

> 是否使用第三方 API 服务？如果有，请提供 API 地址（默认使用 OpenAI 官方）：

> 你使用的 AI 模型名称是什么？（默认 gpt-4o）：

> 是否需要语音面试功能？如果需要，请提供阿里云 API Key（可选）：

**步骤 3.3：生成 JWT_SECRET**

如果用户没有提供 JWT_SECRET，自动生成一个随机密钥：

```bash
openssl rand -hex 32
```

**步骤 3.4：写入 .env 文件**

将收集到的值写入 `.env` 文件。格式：

```bash
cat > .env << 'ENVEOF'
OPENAI_API_KEY=用户提供的值
OPENAI_BASE_URL=用户提供的值或默认值
AI_MODEL=用户提供的值或默认值
ALIYUN_API_KEY=用户提供的值或留空
JWT_SECRET=自动生成或用户提供的值
ENVEOF
```

**步骤 3.5：验证 .env 文件**

确认必填项已填写：

```bash
grep -E '^OPENAI_API_KEY=' .env | grep -v 'sk-your-' >/dev/null && echo "API Key OK" || echo "API Key MISSING"
```

如果 OPENAI_API_KEY 未填写或仍为占位符，拒绝继续部署，要求用户提供真实的 Key。

---

### 阶段 4：数据目录准备

```bash
mkdir -p backend/data
```

此目录用于持久化 SQLite 数据库，Docker 通过 volume 映射 `./backend/data:/app/data`。

---

### 阶段 5：构建并启动

```bash
docker compose up -d --build
```

**说明：**
- 首次构建包含三阶段：前端 npm build + 后端 maven package + 最终镜像组装
- 构建时间约 3-8 分钟，取决于网络和机器性能
- Dockerfile 已配置国内镜像源（npm 用淘宝镜像，Maven 用阿里云镜像），国内环境也能快速构建

**监控构建进度：**

```bash
docker compose logs -f --tail=20 careerforge
```

---

### 阶段 6：健康检查

后端 Spring Boot 启动较慢，需要耐心等待。使用循环检测：

```bash
for i in $(seq 1 18); do
  STATUS=$(docker inspect --format='{{.State.Health.Status}}' careerforge 2>/dev/null || echo "not-found")
  echo "[$i/18] Status: $STATUS"
  if [ "$STATUS" = "healthy" ]; then
    echo "Service is healthy!"
    break
  fi
  if [ "$i" -eq 18 ]; then
    echo "Timeout: service did not become healthy within 90 seconds"
  fi
  sleep 5
done
```

**HTTP 探测验证：**

```bash
# 前端页面
curl -s -o /dev/null -w "Frontend: HTTP %{http_code}\n" http://localhost:80/
# 后端 API（Swagger）
curl -s -o /dev/null -w "Backend API: HTTP %{http_code}\n" http://localhost:80/careerforge/swagger-ui.html
```

两个请求都返回 200 或 302 即为成功。

---

### 阶段 7：部署结果

**成功时输出：**

```
CareerForge 部署成功！

访问地址：
  前端应用：http://localhost
  API 文档：http://localhost/careerforge/swagger-ui.html

数据持久化目录：./backend/data
```

**失败时排查：**

如果健康检查超时或 HTTP 探测失败：

```bash
# 查看容器状态
docker compose ps

# 查看 Supervisor 内部进程状态
docker compose exec careerforge supervisorctl status

# 查看后端日志（最常出问题的地方）
docker compose exec careerforge tail -n 50 /var/log/supervisor/backend.out.log

# 查看后端错误日志
docker compose exec careerforge tail -n 30 /var/log/supervisor/backend.err.log

# 查看 Nginx 日志
docker compose exec careerforge tail -n 30 /var/log/supervisor/nginx.err.log
```

根据日志内容分析失败原因，参考阶段 8 的故障排查表。

---

### 阶段 8：故障排查

| 问题 | 可能原因 | 解决方案 |
|------|----------|----------|
| 容器启动后立即退出 | 端口 80 被占用或配置错误 | 检查 `docker compose logs` 找到具体错误 |
| 前端 404 | 前端构建失败 | 检查构建日志，确认 `frontend/dist/` 目录存在 |
| 后端 API 500 | 数据库初始化失败或环境变量未传递 | 检查 `.env` 文件和后端日志 |
| WebSocket 连接失败 | Nginx 代理配置问题 | 确认 `docker/nginx.conf` 中 WebSocket 配置正确 |
| 健康检查一直 unhealthy | 后端启动慢或 JVM 内存不足 | 等待更长时间，或调整 `JAVA_OPTS` 增加内存 |
| npm install 失败 | 网络问题 | Dockerfile 已配置淘宝镜像，确认网络通畅 |
| Maven 依赖下载失败 | 网络问题 | Dockerfile 已配置阿里云镜像，确认网络通畅 |

**查看详细部署文档：** 项目根目录下的 `DOCKER.md` 包含更完整的部署指南和架构说明。

---

## 常用运维命令

部署完成后，用户日常可能用到的命令：

```bash
# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f --tail=100 careerforge

# 查看后端日志
docker compose exec careerforge tail -f /var/log/supervisor/backend.out.log

# 重启服务
docker compose restart

# 停止服务（数据不丢失）
docker compose down

# 代码更新后重新部署
docker compose down
docker compose up -d --build

# 完全重建（无缓存）
docker compose build --no-cache
docker compose up -d
```
