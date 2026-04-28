---
layout: home

hero:
  name: CareerForge
  text: 智能求职助手
  tagline: AI 驱动的全流程求职工具 -- 从简历优化到模拟面试，一站式搞定
  actions:
    - theme: brand
      text: 快速开始
      link: /#quick-start
    - theme: alt
      text: GitHub
      link: https://github.com/AzirZsk/CareerForge

features:
  - icon: 📄
    title: 智能简历管理
    details: 上传解析、版本管理、区块式编辑、AI 优化建议、一键 PDF 导出
  - icon: 💬
    title: AI 对话式优化
    details: 悬浮球交互、双模式支持、操作卡片式修改建议、批量一键应用
  - icon: 🎙️
    title: AI 语音模拟面试
    details: 实时语音对话、问题预生成、智能追问、求助系统、录音回放
  - icon: 📋
    title: 面试中心
    details: 真实面试管理、AI 准备工作流（公司调研 + JD 分析）、AI 复盘分析
  - icon: 🔄
    title: 简历优化工作流
    details: 快速诊断 → 生成建议 → 内容优化，三步完成，实时进度推送
  - icon: 🎨
    title: 简历风格改写
    details: 上传参考简历，AI 分析写作风格并按相同风格改写你的简历
---
<div id="quick-start" class="home-section">
  <div class="home-section-title">
    <h2>快速开始</h2>
    <p>选择你喜欢的方式部署</p>
  </div>
  <div class="deploy-tabs">
    <button :class="['deploy-tab', activeTab === 'claude' && 'active']" @click="activeTab = 'claude'">AI 辅助部署</button>
    <button :class="['deploy-tab', activeTab === 'docker' && 'active']" @click="activeTab = 'docker'">Docker 部署</button>
  </div>

  <div v-if="activeTab === 'docker'" class="tab-content">
    <div class="steps-grid">
      <div class="step-card">
        <div class="step-number">1</div>
        <h3>克隆仓库</h3>
        <p style="color: #a1a1aa; font-size: 0.9rem; margin: 0 0 8px;">从 GitHub 拉取项目代码</p>
        <div class="code-block">
          <button class="copy-btn" @click="copyCode($event, 'git clone https://github.com/AzirZsk/CareerForge.git && cd CareerForge')">复制</button>
          <code>git clone https://github.com/AzirZsk/CareerForge.git && cd CareerForge</code>
        </div>
      </div>
      <div class="step-card">
        <div class="step-number">2</div>
        <h3>配置环境</h3>
        <p style="color: #a1a1aa; font-size: 0.9rem; margin: 0 0 8px;">设置 AI 模型的 API Key</p>
        <div class="code-block">
          <button class="copy-btn" @click="copyCode($event, 'export OPENAI_API_KEY=your_api_key')">复制</button>
          <code>export OPENAI_API_KEY=your_api_key</code>
        </div>
      </div>
      <div class="step-card">
        <div class="step-number">3</div>
        <h3>启动服务</h3>
        <p style="color: #a1a1aa; font-size: 0.9rem; margin: 0 0 8px;">Docker 一键启动（前端 + 后端）</p>
        <div class="code-block">
          <button class="copy-btn" @click="copyCode($event, 'docker-compose up -d')">复制</button>
          <code>docker-compose up -d</code>
        </div>
      </div>
    </div>
    <div style="text-align: center; margin-top: 40px;">
      <p style="color: #71717a; font-size: 0.9rem;">需要配置环境变量：<code style="color: #e8c47a;">OPENAI_API_KEY</code></p>
      <p style="color: #71717a; font-size: 0.85rem; margin-top: 4px;">可选：阿里云语音服务 <code style="color: #a1a1aa;">ALIYUN_API_KEY</code></p>
    </div>
  </div>

  <div v-if="activeTab === 'claude'" class="tab-content">
    <div class="claude-deploy-card">
      <div class="claude-deploy-icon">🤖</div>
      <h3>一句话部署</h3>
      <p style="color: #a1a1aa; font-size: 0.95rem; margin: 8px 0 24px;">把部署指令发给你的 AI 工具（Claude Code / OpenClaw 等），让它帮你搞定一切</p>
      <div class="code-block" style="max-width: 640px; margin: 0 auto; text-align: left;">
        <button class="copy-btn" @click="copyCode($event, '请按照 https://github.com/AzirZsk/CareerForge/blob/main/deploy-guide.md 的指引，帮我部署 CareerForge 项目')">复制</button>
        <code style="font-size: 0.85rem; line-height: 1.6;">请按照 https://github.com/AzirZsk/CareerForge/blob/main/deploy-guide.md 的指引，帮我部署 CareerForge 项目</code>
      </div>
      <div style="margin-top: 32px; text-align: left; max-width: 640px; margin-left: auto; margin-right: auto;">
        <p style="color: #a1a1aa; font-size: 0.9rem; margin-bottom: 12px;">AI 会自动完成：</p>
        <div style="display: flex; flex-direction: column; gap: 8px;">
          <div style="display: flex; align-items: center; gap: 8px;">
            <span style="color: #d4a853;">✓</span>
            <span style="color: #a1a1aa; font-size: 0.88rem;">检查 Docker、Git 等前置依赖</span>
          </div>
          <div style="display: flex; align-items: center; gap: 8px;">
            <span style="color: #d4a853;">✓</span>
            <span style="color: #a1a1aa; font-size: 0.88rem;">引导你配置 API Key 等环境变量</span>
          </div>
          <div style="display: flex; align-items: center; gap: 8px;">
            <span style="color: #d4a853;">✓</span>
            <span style="color: #a1a1aa; font-size: 0.88rem;">自动构建镜像并启动服务</span>
          </div>
          <div style="display: flex; align-items: center; gap: 8px;">
            <span style="color: #d4a853;">✓</span>
            <span style="color: #a1a1aa; font-size: 0.88rem;">健康检查确认部署成功</span>
          </div>
        </div>
      </div>
      <div style="margin-top: 28px; padding-top: 20px; border-top: 1px solid rgba(255,255,255,0.06);">
        <p style="color: #71717a; font-size: 0.85rem;">前提：已安装 AI 工具（如 <a href="https://claude.ai/code" target="_blank" style="color: #d4a853; text-decoration: none;">Claude Code</a>）和 Docker</p>
      </div>
    </div>
  </div>
</div>

<script setup>
import { ref } from 'vue'

const activeTab = ref('claude')

function copyCode(event, text) {
  const btn = event.currentTarget
  navigator.clipboard.writeText(text).then(() => {
    btn.textContent = '已复制'
    btn.classList.add('copied')
    setTimeout(() => {
      btn.textContent = '复制'
      btn.classList.remove('copied')
    }, 2000)
  })
}
</script>

<style>
.deploy-tabs {
  display: flex;
  justify-content: center;
  gap: 4px;
  margin-bottom: 32px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 10px;
  padding: 4px;
  width: fit-content;
  margin-left: auto;
  margin-right: auto;
}

.deploy-tab {
  padding: 10px 28px;
  border: none;
  background: transparent;
  color: #a1a1aa;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.25s ease;
  font-family: inherit;
}

.deploy-tab:hover {
  color: #f4f4f5;
  background: rgba(255, 255, 255, 0.06);
}

.deploy-tab.active {
  color: #1a1a1d;
  background: #d4a853;
  font-weight: 600;
}

.claude-deploy-card {
  text-align: center;
  padding: 40px 32px;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  max-width: 720px;
  margin: 0 auto;
}

.claude-deploy-icon {
  font-size: 2.5rem;
  margin-bottom: 8px;
}
</style>
