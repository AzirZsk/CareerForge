<!--=====================================================
  LandIt 面试复盘详情页面
  @author Azir
=====================================================-->

<template>
  <div class="review-detail-page">
    <div class="container">
      <!-- 返回导航 -->
      <nav class="back-nav animate-in" style="--delay: 0">
        <router-link to="/review" class="back-link">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="19" y1="12" x2="5" y2="12"></line>
            <polyline points="12 19 5 12 12 5"></polyline>
          </svg>
          返回复盘列表
        </router-link>
      </nav>

      <!-- 头部概览 -->
      <header class="review-header animate-in" style="--delay: 1">
        <div class="header-main">
          <div class="review-meta">
            <span class="review-type technical">技术面试</span>
            <span class="review-date">{{ store.currentInterview.date }}</span>
          </div>
          <h1 class="review-title">{{ store.currentInterview.position }}</h1>
          <p class="review-company">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
              <circle cx="12" cy="10" r="3"></circle>
            </svg>
            {{ store.currentInterview.company }}
          </p>
        </div>
        <div class="header-score">
          <div class="score-circle" :style="{ '--score': store.currentInterview.score }">
            <span class="score-value">{{ store.currentInterview.score }}</span>
            <span class="score-label">综合评分</span>
          </div>
        </div>
      </header>

      <!-- 能力维度 -->
      <section class="dimensions-section animate-in" style="--delay: 2">
        <h2 class="section-title">能力维度分析</h2>
        <div class="dimensions-grid">
          <div
            v-for="(dim, index) in store.currentReview.dimensions"
            :key="dim.name"
            class="dimension-card"
            :style="{ '--index': index }"
          >
            <div class="dim-header">
              <span class="dim-name">{{ dim.name }}</span>
              <span class="dim-score">{{ dim.score }}/{{ dim.maxScore }}</span>
            </div>
            <div class="dim-progress">
              <svg viewBox="0 0 100 10" class="dim-svg">
                <rect x="0" y="3" width="100" height="4" rx="2" fill="rgba(255,255,255,0.1)" />
                <rect
                  x="0"
                  y="3"
                  :width="(dim.score / dim.maxScore * 100)"
                  height="4"
                  rx="2"
                  fill="url(#goldGradient)"
                />
              </svg>
            </div>
            <p class="dim-feedback">{{ dim.feedback }}</p>
          </div>
        </div>
      </section>

      <!-- 对话回放 -->
      <section class="conversation-section animate-in" style="--delay: 3">
        <div class="section-header">
          <h2 class="section-title">对话回放</h2>
          <span class="conversation-count">{{ store.currentInterview.conversation.length }} 条对话</span>
        </div>
        <div class="conversation-list">
          <div
            v-for="(msg, index) in store.currentInterview.conversation"
            :key="msg.id"
            class="conversation-item"
            :class="msg.role"
            :style="{ '--index': index }"
          >
            <div class="conv-avatar">
              {{ msg.role === 'interviewer' ? 'AI' : '我' }}
            </div>
            <div class="conv-content">
              <div class="conv-header">
                <span class="conv-role">{{ msg.role === 'interviewer' ? '面试官' : '我的回答' }}</span>
                <span class="conv-time">{{ formatTime(msg.timestamp) }}</span>
              </div>
              <p class="conv-text">{{ msg.content }}</p>
              <div v-if="msg.score" class="conv-evaluation">
                <div class="eval-score">
                  <span class="eval-value">{{ msg.score }}</span>
                  <span class="eval-label">分</span>
                </div>
                <p class="eval-feedback">{{ msg.feedback }}</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 题目分析 -->
      <section class="questions-section animate-in" style="--delay: 4">
        <div class="section-header">
          <h2 class="section-title">题目分析</h2>
        </div>
        <div class="questions-list">
          <div
            v-for="(q, index) in store.currentReview.questionAnalysis"
            :key="index"
            class="question-analysis-card"
            :style="{ '--index': index }"
          >
            <div class="qa-header">
              <span class="qa-number">Q{{ index + 1 }}</span>
              <div class="qa-score" :class="getScoreClass(q.score)">
                {{ q.score }}分
              </div>
            </div>
            <h4 class="qa-question">{{ q.question }}</h4>
            <div class="qa-answer">
              <span class="qa-label">你的回答</span>
              <p class="qa-text">{{ q.yourAnswer }}</p>
            </div>
            <div class="qa-points">
              <div class="points-row covered">
                <span class="points-label">已覆盖要点</span>
                <div class="points-tags">
                  <span v-for="p in q.keyPointsCovered" :key="p" class="point-tag covered">{{ p }}</span>
                </div>
              </div>
              <div class="points-row missed">
                <span class="points-label">遗漏要点</span>
                <div class="points-tags">
                  <span v-for="p in q.keyPointsMissed" :key="p" class="point-tag missed">{{ p }}</span>
                </div>
              </div>
            </div>
            <div class="qa-suggestion">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"></path>
              </svg>
              <span>{{ q.suggestion }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 综合评估 -->
      <section class="summary-section animate-in" style="--delay: 5">
        <div class="summary-grid">
          <div class="summary-card strengths">
            <div class="summary-header">
              <span class="summary-icon">✨</span>
              <h3 class="summary-title">优势亮点</h3>
            </div>
            <ul class="summary-list">
              <li v-for="item in store.currentReview.analysis.strengths" :key="item">{{ item }}</li>
            </ul>
          </div>
          <div class="summary-card weaknesses">
            <div class="summary-header">
              <span class="summary-icon">🎯</span>
              <h3 class="summary-title">待改进</h3>
            </div>
            <ul class="summary-list">
              <li v-for="item in store.currentReview.analysis.weaknesses" :key="item">{{ item }}</li>
            </ul>
          </div>
        </div>
        <div class="overall-feedback">
          <h3 class="feedback-title">整体反馈</h3>
          <p class="feedback-text">{{ store.currentReview.analysis.overallFeedback }}</p>
        </div>
      </section>

      <!-- 操作按钮 -->
      <div class="action-bar animate-in" style="--delay: 6">
        <button class="action-btn primary" @click="practiceAgain">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="5 3 19 12 5 21 5 3"></polygon>
          </svg>
          再次练习
        </button>
        <button class="action-btn secondary" @click="exportReport">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
          导出报告
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores'

const store = useAppStore()
const router = useRouter()

function formatTime(timestamp: string): string {
  return new Date(timestamp).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function getScoreClass(score: number): string {
  if (score >= 90) return 'excellent'
  if (score >= 75) return 'good'
  if (score >= 60) return 'average'
  return 'poor'
}

function practiceAgain(): void {
  router.push('/interview')
  store.setActiveNav('interview')
}

function exportReport(): void {
  console.log('导出报告')
}
</script>

<style lang="scss" scoped>
.review-detail-page {
  padding: $spacing-2xl;
  max-width: 1000px;
  margin: 0 auto;
}

.container {
  display: flex;
  flex-direction: column;
  gap: $spacing-2xl;
}

.back-nav {
  margin-bottom: $spacing-sm;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: $spacing-sm;
  font-size: $text-sm;
  color: $color-text-tertiary;
  transition: color $transition-fast;
  &:hover {
    color: $color-accent;
  }
}

// 头部
.review-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: $spacing-2xl;
  background: $gradient-card;
  border-radius: $radius-xl;
  border: 1px solid rgba(255, 255, 255, 0.05);
  position: relative;
  overflow: hidden;
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(212, 168, 83, 0.3), transparent);
  }
}

.review-meta {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  margin-bottom: $spacing-md;
}

.review-type {
  padding: $spacing-xs $spacing-sm;
  font-size: $text-xs;
  font-weight: $weight-medium;
  border-radius: $radius-sm;
  &.technical {
    background: rgba(96, 165, 250, 0.15);
    color: #60a5fa;
  }
}

.review-date {
  font-size: $text-sm;
  color: $color-text-tertiary;
}

.review-title {
  font-family: $font-display;
  font-size: $text-3xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-sm;
}

.review-company {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  font-size: $text-base;
  color: $color-accent;
}

.score-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: conic-gradient(
    $color-accent calc(var(--score) * 3.6deg),
    rgba(255, 255, 255, 0.1) calc(var(--score) * 3.6deg)
  );
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  &::before {
    content: '';
    position: absolute;
    inset: 10px;
    background: $color-bg-tertiary;
    border-radius: 50%;
  }
  .score-value {
    position: relative;
    z-index: 1;
    font-family: $font-display;
    font-size: $text-4xl;
    font-weight: $weight-bold;
    color: $color-accent;
  }
  .score-label {
    position: relative;
    z-index: 1;
    font-size: $text-xs;
    color: $color-text-tertiary;
  }
}

// 维度分析
.section-title {
  font-family: $font-display;
  font-size: $text-xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-lg;
}

.dimensions-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: $spacing-md;
}

.dimension-card {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
  animation: slideUp 0.5s ease forwards;
  animation-delay: calc(var(--index) * 0.1s);
  opacity: 0;
}

.dim-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-sm;
}

.dim-name {
  font-size: $text-sm;
  color: $color-text-secondary;
}

.dim-score {
  font-size: $text-sm;
  font-weight: $weight-semibold;
  color: $color-accent;
}

.dim-progress {
  margin-bottom: $spacing-md;
}

.dim-svg {
  width: 100%;
  height: 10px;
}

.dim-feedback {
  font-size: $text-xs;
  color: $color-text-tertiary;
  line-height: $leading-relaxed;
}

// 对话回放
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-lg;
}

.conversation-count {
  font-size: $text-sm;
  color: $color-text-tertiary;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.conversation-item {
  display: flex;
  gap: $spacing-md;
  animation: slideUp 0.5s ease forwards;
  animation-delay: calc(var(--index) * 0.1s);
  opacity: 0;
  &.interviewer {
    .conv-avatar {
      background: $gradient-gold;
      color: $color-bg-deep;
    }
  }
  &.candidate {
    .conv-avatar {
      background: rgba(96, 165, 250, 0.2);
      color: #60a5fa;
    }
  }
}

.conv-avatar {
  width: 40px;
  height: 40px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $text-sm;
  font-weight: $weight-semibold;
  flex-shrink: 0;
}

.conv-content {
  flex: 1;
}

.conv-header {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  margin-bottom: $spacing-sm;
}

.conv-role {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.conv-time {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.conv-text {
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.03);
  border-radius: $radius-md;
  font-size: $text-sm;
  color: $color-text-secondary;
  line-height: $leading-relaxed;
}

.conv-evaluation {
  display: flex;
  align-items: flex-start;
  gap: $spacing-md;
  margin-top: $spacing-md;
  padding: $spacing-md;
  background: $color-success-bg;
  border-radius: $radius-md;
}

.eval-score {
  display: flex;
  align-items: baseline;
  gap: 2px;
  .eval-value {
    font-family: $font-display;
    font-size: $text-2xl;
    font-weight: $weight-bold;
    color: $color-success;
  }
  .eval-label {
    font-size: $text-xs;
    color: $color-success;
  }
}

.eval-feedback {
  flex: 1;
  font-size: $text-sm;
  color: $color-success;
}

// 题目分析
.questions-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.question-analysis-card {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-xl;
  border: 1px solid rgba(255, 255, 255, 0.05);
  animation: slideUp 0.5s ease forwards;
  animation-delay: calc(var(--index) * 0.1s);
  opacity: 0;
}

.qa-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-md;
}

.qa-number {
  font-family: $font-display;
  font-size: $text-sm;
  font-weight: $weight-semibold;
  color: $color-accent;
}

.qa-score {
  padding: $spacing-xs $spacing-md;
  font-size: $text-sm;
  font-weight: $weight-semibold;
  border-radius: $radius-sm;
  &.excellent {
    background: $color-success-bg;
    color: $color-success;
  }
  &.good {
    background: $color-accent-glow;
    color: $color-accent;
  }
  &.average {
    background: $color-warning-bg;
    color: $color-warning;
  }
  &.poor {
    background: $color-error-bg;
    color: $color-error;
  }
}

.qa-question {
  font-size: $text-base;
  font-weight: $weight-medium;
  color: $color-text-primary;
  margin-bottom: $spacing-lg;
}

.qa-answer {
  margin-bottom: $spacing-lg;
}

.qa-label {
  display: block;
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-bottom: $spacing-sm;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.qa-text {
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.03);
  border-radius: $radius-md;
  font-size: $text-sm;
  color: $color-text-secondary;
  line-height: $leading-relaxed;
}

.qa-points {
  margin-bottom: $spacing-lg;
}

.points-row {
  margin-bottom: $spacing-md;
  &:last-child {
    margin-bottom: 0;
  }
}

.points-label {
  display: block;
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-bottom: $spacing-sm;
}

.points-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
}

.point-tag {
  padding: $spacing-xs $spacing-sm;
  font-size: $text-xs;
  border-radius: $radius-sm;
  &.covered {
    background: $color-success-bg;
    color: $color-success;
  }
  &.missed {
    background: $color-error-bg;
    color: $color-error;
  }
}

.qa-suggestion {
  display: flex;
  align-items: flex-start;
  gap: $spacing-sm;
  padding: $spacing-md;
  background: rgba(212, 168, 83, 0.1);
  border-radius: $radius-md;
  border-left: 3px solid $color-accent;
  font-size: $text-sm;
  color: $color-text-secondary;
  svg {
    flex-shrink: 0;
    color: $color-accent;
    margin-top: 2px;
  }
}

// 综合评估
.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-lg;
  margin-bottom: $spacing-lg;
}

.summary-card {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-xl;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.summary-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-lg;
}

.summary-icon {
  font-size: $text-xl;
}

.summary-title {
  font-size: $text-base;
  font-weight: $weight-semibold;
  color: $color-text-primary;
}

.summary-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  li {
    position: relative;
    padding-left: $spacing-lg;
    font-size: $text-sm;
    color: $color-text-secondary;
    line-height: $leading-relaxed;
    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 8px;
      width: 6px;
      height: 6px;
      border-radius: 50%;
    }
  }
  .strengths & li::before {
    background: $color-success;
  }
  .weaknesses & li::before {
    background: $color-warning;
  }
}

.overall-feedback {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-xl;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.feedback-title {
  font-size: $text-base;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-md;
}

.feedback-text {
  font-size: $text-sm;
  color: $color-text-secondary;
  line-height: $leading-relaxed;
}

// 操作按钮
.action-bar {
  display: flex;
  justify-content: center;
  gap: $spacing-lg;
  padding-top: $spacing-lg;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md $spacing-2xl;
  font-size: $text-base;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  transition: all $transition-fast;
  &.primary {
    background: $gradient-gold;
    color: $color-bg-deep;
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(212, 168, 83, 0.3);
    }
  }
  &.secondary {
    background: rgba(255, 255, 255, 0.05);
    color: $color-text-secondary;
    border: 1px solid rgba(255, 255, 255, 0.1);
    &:hover {
      background: rgba(255, 255, 255, 0.1);
      color: $color-text-primary;
    }
  }
}

// 动画
.animate-in {
  animation: slideUp 0.6s ease forwards;
  animation-delay: calc(var(--delay) * 0.1s);
  opacity: 0;
}
</style>
