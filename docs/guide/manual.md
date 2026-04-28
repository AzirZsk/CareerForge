# 传统部署

不使用 Docker，分别构建部署前后端。

## 环境要求

- Java 17+
- Node.js 18+
- Maven 3.8+

## 后端

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

## 前端

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
