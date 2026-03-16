# 技术栈

## 框架与核心库

- Vue 3.4+（Composition API + `<script setup>` 语法）
- TypeScript 5.4+（strict 模式）
- Vue Router 4（History 模式，懒加载路由）
- Pinia 2（状态管理，Setup Store 风格）
- VueUse 10（组合式工具库）

## 构建与工具链

- Vite 5（开发服务器 + 构建）
- vue-tsc（类型检查）
- SCSS（样式预处理，通过 Vite 全局注入 variables.scss）

## API 通信

- 原生 fetch（REST API 调用）
- EventSource（SSE 流式通信，用于简历优化和岗位定制）
- API 基础路径：`/landit`，开发环境通过 Vite proxy 转发到后端（默认 `http://localhost:8080`）
- 统一响应格式：`ApiResponse<T>` — `{ code, message, data, timestamp }`

## 样式系统

- SCSS + 设计令牌（Design Tokens）定义在 `src/assets/styles/variables.scss`
- 主题：深色科技风，主色调深炭黑，强调色琥珀金（#d4a853）
- 字体：Crimson Pro（展示）+ Outfit（正文）
- 组件使用 `<style lang="scss" scoped>`

## 常用命令

```bash
npm run dev          # 启动开发服务器
npm run build        # 类型检查 + 生产构建
npm run preview      # 预览生产构建
npm run type-check   # 仅运行类型检查
```
