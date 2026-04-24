# AI 辅助部署（推荐）

> 将部署指令文件 URL 发送给你的 AI 编程工具，即可自动完成 CareerForge 的全流程部署。

适用于 **Claude Code、OpenClaw** 等 AI 工具。

## 前置条件

确保机器已安装以下工具：

| 工具 | 检查命令 | 安装方式 |
|------|----------|----------|
| Docker | `docker info` | `curl -fsSL https://get.docker.com \| sh` |
| Docker Compose | `docker compose version` | 随 Docker 一起安装 |
| Git | `git --version` | `sudo apt install -y git` |

确认端口 80 未被占用：

```bash
ss -tlnp | grep ':80 ' || echo "Port 80 available"
```

## 一键部署

将以下指令发送给你的 AI 工具：

```
请按照 https://github.com/AzirZsk/CareerForge/blob/main/deploy-guide.md 的指引，帮我部署 CareerForge 项目
```

AI 会按以下流程自动执行：

1. **克隆仓库** — 拉取最新代码
2. **配置环境变量** — 交互式收集 API Key 等配置
3. **构建并启动** — 自动执行 `docker compose up -d --build`
4. **健康检查** — 验证服务是否正常运行

## 环境变量

AI 会逐项询问以下配置：

| 变量 | 必填 | 说明 | 默认值 |
|------|------|------|--------|
| `OPENAI_API_KEY` | 是 | AI 模型 API Key | 无，必须提供 |
| `OPENAI_BASE_URL` | 否 | API 地址（支持第三方服务） | `https://api.openai.com` |
| `AI_MODEL` | 否 | AI 模型名称 | `gpt-4o` |
| `ALIYUN_API_KEY` | 否 | 阿里云 API Key（语音面试功能） | 无 |
| `JWT_SECRET` | 否 | JWT 认证密钥 | 自动生成 |

## 部署结果

成功后输出：

```
CareerForge 部署成功！

访问地址：
  前端应用：http://localhost
  API 文档：http://localhost/careerforge/swagger-ui.html

数据持久化目录：./backend/data
```

## 常用运维命令

```bash
# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f --tail=100 careerforge

# 重启服务
docker compose restart

# 停止服务（数据不丢失）
docker compose down

# 代码更新后重新部署
docker compose down && docker compose up -d --build

# 完全重建（无缓存）
docker compose build --no-cache && docker compose up -d
```
