# 项目结构

```
src/
├── api/                  # API 请求层（按业务模块拆分，如 resume.ts、user.ts）
├── assets/styles/        # 全局样式和 SCSS 变量
├── components/
│   ├── common/           # 通用组件（导航栏、确认弹窗等）
│   └── resume/           # 简历相关组件
│       ├── forms/        # 表单组件（按模块类型：BasicInfoForm、ExperienceForm 等）
│       └── sections/     # 简历区块展示组件（按模块类型对应）
├── composables/          # 组合式函数（useXxx 命名，封装可复用业务逻辑）
├── mock/                 # Mock 数据
├── router/               # 路由配置（懒加载）
├── stores/               # Pinia Store（单一 useAppStore）
├── types/                # TypeScript 类型定义（按业务域拆分）
└── views/                # 页面级组件（对应路由）
```

## 代码约定

- Vue 组件使用 `<script setup lang="ts">` + `<style lang="scss" scoped>`
- 文件头部统一注释块格式：`// ===... \n// 描述 \n// @author Azir \n// ===...`
- 组合式函数放在 `composables/`，以 `use` 前缀命名
- 类型定义集中在 `src/types/`，按业务域拆分文件
- API 层按模块拆分，函数直接导出（非 class），统一处理 `ApiResponse` 解包和错误抛出
- Store 使用 Pinia Setup Store 风格（`defineStore('name', () => { ... })`）
- 路径别名：`@` → `src/`
