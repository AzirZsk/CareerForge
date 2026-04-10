# =====================================================
# LandIt 全栈 Dockerfile
# 单镜像双服务：Nginx（前端） + Spring Boot（后端）
# @author Azir
# =====================================================

# 阶段 1: 构建前端
FROM node:20-alpine AS frontend-builder

WORKDIR /frontend

# 复制依赖配置
COPY frontend/package.json frontend/package-lock.json* ./
# 安装所有依赖（构建需要 devDependencies）
# 使用淘宝 npm 镜像加速
RUN npm ci --registry=https://registry.npmmirror.com && npm cache clean --force

# 复制源码并构建
COPY frontend/ ./
RUN npm run build

# 阶段 2: 构建后端
FROM maven:3.9.9-eclipse-temurin-17-alpine AS backend-builder

WORKDIR /build

# 配置 Maven 使用阿里云镜像加速
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?>' > /root/.m2/settings.xml && \
    echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"' >> /root/.m2/settings.xml && \
    echo '    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"' >> /root/.m2/settings.xml && \
    echo '    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">' >> /root/.m2/settings.xml && \
    echo '  <mirrors>' >> /root/.m2/settings.xml && \
    echo '    <mirror>' >> /root/.m2/settings.xml && \
    echo '      <id>aliyunmaven</id>' >> /root/.m2/settings.xml && \
    echo '      <mirrorOf>*</mirrorOf>' >> /root/.m2/settings.xml && \
    echo '      <name>阿里云公共仓库</name>' >> /root/.m2/settings.xml && \
    echo '      <url>https://maven.aliyun.com/repository/public</url>' >> /root/.m2/settings.xml && \
    echo '    </mirror>' >> /root/.m2/settings.xml && \
    echo '  </mirrors>' >> /root/.m2/settings.xml && \
    echo '</settings>' >> /root/.m2/settings.xml

# 复制 pom.xml 并下载依赖
COPY backend/pom.xml ./
RUN mvn dependency:go-offline -B

# 复制源码并构建
COPY backend/src ./src
RUN mvn clean package -DskipTests -B

# 阶段 3: 合并镜像（Nginx + JRE）
FROM eclipse-temurin:17-jre-alpine

# 安装 Nginx 和其他依赖
# 使用阿里云 Alpine 镜像源加速
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories && \
    apk add --no-cache nginx sqlite-libs tzdata supervisor && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 创建应用用户
RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

# 从前端构建阶段复制静态文件到 Nginx 目录
COPY --from=frontend-builder /frontend/dist /usr/share/nginx/html

# 从后端构建阶段复制 jar 包
COPY --from=backend-builder /build/target/*.jar /app/app.jar

# 复制 Nginx 配置
COPY docker/nginx.conf /etc/nginx/http.d/default.conf

# 复制 Supervisor 配置
COPY docker/supervisord.conf /etc/supervisord.conf

# 创建数据目录并设置权限
RUN mkdir -p /app/data /var/log/supervisor /var/log/nginx /var/run && \
    chown -R app:app /app /usr/share/nginx/html /var/log/supervisor /var/log/nginx /var/run && \
    # 修改 Nginx 运行用户为 app
    sed -i 's/user nginx;/user app;/g' /etc/nginx/nginx.conf && \
    # 修改 Nginx pid 文件权限
    sed -i 's/\/run\/nginx.pid/\/var\/run\/nginx.pid/g' /etc/nginx/nginx.conf

# 切换到非 root 用户
USER app

# 暴露端口
EXPOSE 80 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:80/ || exit 1

# 使用 Supervisor 管理双服务
ENTRYPOINT ["supervisord", "-c", "/etc/supervisord.conf", "-n"]
