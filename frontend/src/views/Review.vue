<!--=====================================================
  LandIt 面试复盘页面
  @author Azir
=====================================================-->

<template>
  <div class="review-page">
    <div class="container">
      <!-- 页面标题 -->
      <header
        class="page-header animate-in"
        style="--delay: 0"
      >
        <div class="header-content">
          <h1 class="page-title">
            面试复盘
          </h1>
          <p class="page-desc">
            回顾面试表现，发现提升空间，持续精进
          </p>
        </div>
      </header>

      <!-- 统计概览 -->
      <section
        class="overview-section animate-in"
        style="--delay: 1"
      >
        <div class="overview-grid">
          <div class="overview-card main">
            <div class="overview-content">
              <h3 class="overview-title">
                平均得分
              </h3>
              <div class="overview-value">
                <span class="value-number">{{ store.averageInterviewScore }}</span>
                <span class="value-unit">分</span>
              </div>
              <p class="overview-trend up">
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <polyline points="23 6 13.5 15.5 8.5 10.5 1 18" />
                  <polyline points="17 6 23 6 23 12" />
                </svg>
                较上周提升 8%
              </p>
            </div>
            <div class="overview-chart">
              <div class="mini-chart">
                <div
                  v-for="(bar, i) in miniChartData"
                  :key="i"
                  class="mini-bar"
                  :style="{ height: bar + '%' }"
                />
              </div>
            </div>
          </div>
          <div class="overview-card">
            <div class="stat-icon total">
              <svg
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
              </svg>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ store.interviews.length }}</span>
              <span class="stat-label">累计面试</span>
            </div>
          </div>
          <div class="overview-card">
            <div class="stat-icon time">
              <svg
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <circle
                  cx="12"
                  cy="12"
                  r="10"
                />
                <polyline points="12 6 12 12 16 14" />
              </svg>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ totalDuration }}</span>
              <span class="stat-label">总时长(分钟)</span>
            </div>
          </div>
          <div class="overview-card">
            <div class="stat-icon improve">
              <svg
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                <polyline points="22 4 12 14.01 9 11.01" />
              </svg>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ improvementRate }}%</span>
              <span class="stat-label">提升率</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 最新复盘 -->
      <section
        class="latest-review-section animate-in"
        style="--delay: 2"
      >
        <div class="section-header">
          <h2 class="section-title">
            最新复盘
          </h2>
          <span class="section-badge">最近一次面试</span>
        </div>
        <div class="review-card">
          <div class="review-header">
            <div class="review-meta">
              <span class="review-type technical">技术面试</span>
              <h3 class="review-position">
                {{ latestInterview?.position }}
              </h3>
              <span class="review-company">{{ latestInterview?.company }}</span>
            </div>
            <div class="review-score">
              <div
                class="score-circle"
                :style="{ '--score': latestInterview?.score || 0 }"
              >
                <span class="score-value">{{ latestInterview?.score }}</span>
              </div>
            </div>
          </div>
          <div class="review-dimensions">
            <div
              v-for="dim in store.currentReview.dimensions"
              :key="dim.name"
              class="dimension-item"
            >
              <div class="dimension-header">
                <span class="dimension-name">{{ dim.name }}</span>
                <span class="dimension-score">{{ dim.score }}/{{ dim.maxScore }}</span>
              </div>
              <div class="dimension-bar">
                <div
                  class="dimension-fill"
                  :style="{ width: (dim.score / dim.maxScore * 100) + '%' }"
                />
              </div>
              <p class="dimension-feedback">
                {{ dim.feedback }}
              </p>
            </div>
          </div>
          <div class="review-summary">
            <div class="summary-section">
              <h4 class="summary-title">
                <span class="title-icon">✨</span>
                优势亮点
              </h4>
              <ul class="summary-list strengths">
                <li
                  v-for="item in store.currentReview.analysis.strengths"
                  :key="item"
                >
                  {{ item }}
                </li>
              </ul>
            </div>
            <div class="summary-section">
              <h4 class="summary-title">
                <span class="title-icon">🎯</span>
                改进方向
              </h4>
              <ul class="summary-list weaknesses">
                <li
                  v-for="item in store.currentReview.analysis.weaknesses"
                  :key="item"
                >
                  {{ item }}
                </li>
              </ul>
            </div>
          </div>
          <div class="review-actions">
            <button
              class="action-btn primary"
              @click="viewFullReview"
            >
              查看完整复盘
            </button>
            <button
              class="action-btn secondary"
              @click="practiceAgain"
            >
              再次练习
            </button>
          </div>
        </div>
      </section>

      <!-- 历史记录列表 -->
      <section
        class="history-section animate-in"
        style="--delay: 3"
      >
        <div class="section-header">
          <h2 class="section-title">
            全部记录
          </h2>
          <div class="filter-options">
            <button
              v-for="filter in filters"
              :key="filter.key"
              class="filter-btn"
              :class="{ active: activeFilter === filter.key }"
              @click="activeFilter = filter.key"
            >
              {{ filter.label }}
            </button>
          </div>
        </div>
        <div class="history-table">
          <div class="table-header">
            <div class="col-date">
              日期
            </div>
            <div class="col-type">
              类型
            </div>
            <div class="col-position">
              岗位
            </div>
            <div class="col-company">
              公司
            </div>
            <div class="col-duration">
              时长
            </div>
            <div class="col-score">
              得分
            </div>
            <div class="col-action">
              操作
            </div>
          </div>
          <div class="table-body">
            <div
              v-for="(record, index) in filteredHistory"
              :key="record.id"
              class="table-row"
              :style="{ '--index': index }"
            >
              <div class="col-date">
                {{ record.date }}
              </div>
              <div class="col-type">
                <span
                  class="type-tag"
                  :class="record.type"
                >
                  {{ record.type === 'technical' ? '技术' : '行为' }}
                </span>
              </div>
              <div class="col-position">
                {{ record.position }}
              </div>
              <div class="col-company">
                {{ record.company }}
              </div>
              <div class="col-duration">
                {{ record.duration }}分钟
              </div>
              <div class="col-score">
                <span
                  class="score-badge"
                  :class="getScoreClass(record.score)"
                >
                  {{ record.score }}
                </span>
              </div>
              <div class="col-action">
                <button
                  class="table-btn"
                  @click="viewDetail(record.id)"
                >
                  <svg
                    width="16"
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                    <circle
                      cx="12"
                      cy="12"
                      r="3"
                    />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 改进计划 -->
      <section
        class="improvement-section animate-in"
        style="--delay: 4"
      >
        <div class="section-header">
          <h2 class="section-title">
            改进计划
          </h2>
          <button class="edit-plan-btn">
            编辑计划
          </button>
        </div>
        <div class="improvement-grid">
          <div
            v-for="(plan, index) in store.currentReview.improvementPlan"
            :key="plan.category"
            class="improvement-card"
            :style="{ '--index': index }"
          >
            <div class="plan-header">
              <span class="plan-icon">{{ plan.category === '技术深化' ? '🔬' : '💬' }}</span>
              <h4 class="plan-title">
                {{ plan.category }}
              </h4>
            </div>
            <ul class="plan-items">
              <li
                v-for="item in plan.items"
                :key="item"
              >
                <span class="check-box" />
                {{ item }}
              </li>
            </ul>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAppStore } from '@/stores'
import { useRouter } from 'vue-router'
import type { Interview, InterviewType } from '@/types'

interface FilterItem {
  key: string
  label: string
}

const store = useAppStore()
const router = useRouter()

const activeFilter = ref<string>('all')
const filters: FilterItem[] = [
  { key: 'all', label: '全部' },
  { key: 'technical', label: '技术面试' },
  { key: 'behavioral', label: '行为面试' }
]

const miniChartData: number[] = [40, 60, 45, 70, 55, 80, 75, 90]

const latestInterview = computed<Interview | undefined>(() => store.interviews[0])

const totalDuration = computed<number>(() => {
  return store.interviews.reduce((sum: number, i: Interview) => sum + i.duration, 0)
})

const improvementRate = computed<number>(() => store.stats.overview.improvementRate)

const filteredHistory = computed<Interview[]>(() => {
  if (activeFilter.value === 'all') {
    return store.interviews
  }
  return store.interviews.filter((i: Interview) => i.type === activeFilter.value as InterviewType)
})

function getScoreClass(score: number): string {
  if (score >= 90) return 'excellent'
  if (score >= 75) return 'good'
  if (score >= 60) return 'average'
  return 'poor'
}

function viewFullReview(): void {
  router.push(`/review/${latestInterview.value?.id}`)
}

function practiceAgain(): void {
  router.push('/interview')
  store.setActiveNav('interview')
}

function viewDetail(id: string): void {
  router.push(`/review/${id}`)
}
</script>

<style lang="scss" scoped>
.review-page {
  padding: $spacing-2xl;
  max-width: 1400px;
  margin: 0 auto;
}

.container {
  display: flex;
  flex-direction: column;
  gap: $spacing-2xl;
}

// 页面标题
.page-header {
  margin-bottom: $spacing-md;
}

.page-title {
  font-family: $font-display;
  font-size: $text-4xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-xs;
}

.page-desc {
  font-size: $text-base;
  color: $color-text-secondary;
}

// 统计概览
.overview-grid {
  display: grid;
  grid-template-columns: 2fr repeat(3, 1fr);
  gap: $spacing-lg;
}

.overview-card {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all $transition-fast;
  &:hover {
    border-color: rgba(212, 168, 83, 0.15);
  }
  &.main {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.overview-content {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.overview-title {
  font-size: $text-sm;
  color: $color-text-tertiary;
  font-weight: $weight-medium;
}

.overview-value {
  display: flex;
  align-items: baseline;
  gap: $spacing-xs;
  .value-number {
    font-family: $font-display;
    font-size: $text-5xl;
    font-weight: $weight-bold;
    color: $color-accent;
  }
  .value-unit {
    font-size: $text-lg;
    color: $color-text-secondary;
  }
}

.overview-trend {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $text-sm;
  &.up {
    color: $color-success;
  }
  &.down {
    color: $color-error;
  }
}

.mini-chart {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  height: 60px;
}

.mini-bar {
  width: 8px;
  background: $gradient-gold;
  border-radius: $radius-sm;
  opacity: 0.6;
  transition: all $transition-fast;
  &:hover {
    opacity: 1;
  }
}

.overview-card:not(.main) {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  &.total {
    background: rgba(96, 165, 250, 0.15);
    color: #60a5fa;
  }
  &.time {
    background: rgba(52, 211, 153, 0.15);
    color: #34d399;
  }
  &.improve {
    background: rgba(212, 168, 83, 0.15);
    color: $color-accent;
  }
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.stat-value {
  font-family: $font-display;
  font-size: $text-2xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
}

.stat-label {
  font-size: $text-sm;
  color: $color-text-tertiary;
}

// 最新复盘
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-lg;
}

.section-title {
  font-family: $font-display;
  font-size: $text-xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
}

.section-badge {
  padding: $spacing-xs $spacing-sm;
  background: $color-accent-glow;
  color: $color-accent;
  font-size: $text-xs;
  font-weight: $weight-medium;
  border-radius: $radius-sm;
}

.review-card {
  background: $gradient-card;
  border-radius: $radius-xl;
  padding: $spacing-xl;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: $spacing-xl;
  padding-bottom: $spacing-lg;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.review-type {
  padding: $spacing-xs $spacing-sm;
  font-size: $text-xs;
  font-weight: $weight-medium;
  border-radius: $radius-sm;
  margin-bottom: $spacing-sm;
  display: inline-block;
  &.technical {
    background: rgba(96, 165, 250, 0.15);
    color: #60a5fa;
  }
  &.behavioral {
    background: rgba(167, 139, 250, 0.15);
    color: #a78bfa;
  }
}

.review-position {
  font-family: $font-display;
  font-size: $text-2xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-xs;
}

.review-company {
  font-size: $text-sm;
  color: $color-accent;
}

.score-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: conic-gradient(
    $color-accent calc(var(--score) * 3.6deg),
    rgba(255, 255, 255, 0.1) calc(var(--score) * 3.6deg)
  );
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  &::before {
    content: '';
    position: absolute;
    inset: 8px;
    background: $color-bg-tertiary;
    border-radius: 50%;
  }
  .score-value {
    position: relative;
    z-index: 1;
    font-family: $font-display;
    font-size: $text-2xl;
    font-weight: $weight-bold;
    color: $color-accent;
  }
}

.review-dimensions {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: $spacing-lg;
  margin-bottom: $spacing-xl;
}

.dimension-item {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.dimension-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dimension-name {
  font-size: $text-sm;
  color: $color-text-secondary;
}

.dimension-score {
  font-size: $text-sm;
  font-weight: $weight-semibold;
  color: $color-accent;
}

.dimension-bar {
  height: 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: $radius-full;
  overflow: hidden;
}

.dimension-fill {
  height: 100%;
  background: $gradient-gold;
  border-radius: $radius-full;
  transition: width 0.8s ease;
}

.dimension-feedback {
  font-size: $text-xs;
  color: $color-text-tertiary;
  line-height: $leading-relaxed;
}

.review-summary {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-xl;
  margin-bottom: $spacing-xl;
}

.summary-section {
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-lg;
  padding: $spacing-lg;
}

.summary-title {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  font-size: $text-base;
  font-weight: $weight-medium;
  color: $color-text-primary;
  margin-bottom: $spacing-md;
}

.summary-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  li {
    display: flex;
    align-items: flex-start;
    gap: $spacing-sm;
    font-size: $text-sm;
    color: $color-text-secondary;
    &::before {
      content: '•';
      color: $color-accent;
    }
  }
  &.strengths li::before {
    color: $color-success;
  }
  &.weaknesses li::before {
    color: $color-warning;
  }
}

.review-actions {
  display: flex;
  gap: $spacing-md;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-lg;
  font-size: $text-sm;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  transition: all $transition-fast;
  &.primary {
    background: $gradient-gold;
    color: $color-bg-deep;
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 16px rgba(212, 168, 83, 0.3);
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

// 历史表格
.filter-options {
  display: flex;
  gap: $spacing-xs;
}

.filter-btn {
  padding: $spacing-xs $spacing-md;
  font-size: $text-sm;
  color: $color-text-tertiary;
  border-radius: $radius-sm;
  transition: all $transition-fast;
  &.active {
    background: $color-accent-glow;
    color: $color-accent;
  }
  &:hover:not(.active) {
    color: $color-text-secondary;
  }
}

.history-table {
  background: $gradient-card;
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
  overflow: hidden;
}

.table-header {
  display: grid;
  grid-template-columns: 100px 80px 1fr 120px 80px 80px 60px;
  gap: $spacing-md;
  padding: $spacing-md $spacing-lg;
  background: rgba(255, 255, 255, 0.02);
  font-size: $text-xs;
  color: $color-text-tertiary;
  font-weight: $weight-medium;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.table-body {
  max-height: 400px;
  overflow-y: auto;
}

.table-row {
  display: grid;
  grid-template-columns: 100px 80px 1fr 120px 80px 80px 60px;
  gap: $spacing-md;
  padding: $spacing-md $spacing-lg;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
  font-size: $text-sm;
  color: $color-text-primary;
  transition: background $transition-fast;
  animation: slideUp 0.4s ease forwards;
  animation-delay: calc(var(--index) * 0.05s);
  opacity: 0;
  &:hover {
    background: rgba(255, 255, 255, 0.02);
  }
}

.type-tag {
  padding: $spacing-xs $spacing-sm;
  font-size: $text-xs;
  border-radius: $radius-sm;
  &.technical {
    background: rgba(96, 165, 250, 0.15);
    color: #60a5fa;
  }
  &.behavioral {
    background: rgba(167, 139, 250, 0.15);
    color: #a78bfa;
  }
}

.score-badge {
  padding: $spacing-xs $spacing-sm;
  font-size: $text-xs;
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

.table-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $color-text-tertiary;
  border-radius: $radius-sm;
  transition: all $transition-fast;
  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: $color-accent;
  }
}

// 改进计划
.edit-plan-btn {
  font-size: $text-sm;
  color: $color-accent;
  transition: color $transition-fast;
  &:hover {
    color: $color-accent-light;
  }
}

.improvement-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-lg;
}

.improvement-card {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
  animation: slideUp 0.5s ease forwards;
  animation-delay: calc(var(--index) * 0.1s);
  opacity: 0;
}

.plan-header {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  margin-bottom: $spacing-lg;
}

.plan-icon {
  font-size: $text-2xl;
}

.plan-title {
  font-size: $text-lg;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.plan-items {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
  li {
    display: flex;
    align-items: center;
    gap: $spacing-md;
    font-size: $text-sm;
    color: $color-text-secondary;
  }
}

.check-box {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: $radius-sm;
  transition: all $transition-fast;
  cursor: pointer;
  &:hover {
    border-color: $color-accent;
  }
}

// 动画
.animate-in {
  animation: slideUp 0.6s ease forwards;
  animation-delay: calc(var(--delay) * 0.1s);
  opacity: 0;
}
</style>
