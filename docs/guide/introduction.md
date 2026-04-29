# 项目介绍

## CareerForge 是什么

CareerForge 是一款 **AI 驱动的全流程智能求职助手**，帮助你从简历优化到模拟面试，一站式搞定求职准备。

![CareerForge 界面展示](/images/home.png)

## 核心特性

- **智能简历管理** — 上传 PDF/Word 自动解析，区块式编辑，多版本管理，一键 PDF 导出
- **AI 对话式优化** — 悬浮球随时唤起 AI，以对话方式优化简历，操作卡片式 Diff 对比
- **AI 语音模拟面试** — 实时语音对话，智能出题和追问，求助系统，全程录音回放
- **面试中心** — 真实面试管理，AI 生成准备事项（公司调研 + JD 分析），AI 复盘分析
- **简历优化工作流** — 快速诊断 → 生成建议 → 内容优化，三步完成
- **简历风格改写** — 上传参考简历，AI 分析风格后按相同风格改写

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue 3.4 + TypeScript 5.4 + Vite 5 + Pinia |
| 后端 | Spring Boot 3.5 + Java 17 + MyBatis-Plus |
| 数据库 | SQLite（零配置，开箱即用） |
| AI | Spring AI OpenAI（支持 OpenAI 协议的模型） |
| 语音 | 阿里云智能语音交互（ASR + TTS） |
| 实时通信 | WebSocket（语音面试） + SSE（流式 AI 响应） |

## 适用人群

- 正在求职、需要优化简历的求职者
- 想通过模拟面试提升面试表现的同学
- 希望用 AI 工具提升求职效率的任何人

## 开源协议

基于 [AGPL-3.0](https://github.com/AzirZsk/CareerForge/blob/main/LICENSE) 许可证开源。
