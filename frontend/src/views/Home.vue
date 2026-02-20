<!--=====================================================
  LandIt 首页 - 工作台
  @author Azir
=====================================================-->

<template>
  <div class="home-page">
    <div class="container">
      <!-- 欢迎区域 -->
      <section class="welcome-section animate-in" style="--delay: 0">
        <div class="welcome-content">
          <h1 class="welcome-title">
            你好，{{ store.user.name || '求职者' }}
            <span class="wave-emoji">👋</span>
          </h1>
          <p class="welcome-subtitle">
            今天是迈向理想工作的又一天，让我们继续努力吧！
          </p>
        </div>
        <div class="quick-actions">
          <button class="action-btn primary" @click="startInterview">
            <span class="action-icon">🎯</span>
            <span>开始面试</span>
          </button>
          <button class="action-btn secondary" @click="goToResume">
            <span class="action-icon">📄</span>
            <span>优化简历</span>
          </button>
        </div>
      </section>

      <!-- 统计概览 -->
      <section class="stats-section animate-in" style="--delay: 1">
        <div class="section-header">
          <h2 class="section-title">数据概览</h2>
          <span class="section-badge">本周</span>
        </div>
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon interviews">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
              </svg>
            </div>
            <div class="stat-content">
              <span class="stat-value">{{ store.stats.overview.totalInterviews }}</span>
              <span class="stat-label">模拟面试次数</span>
            </div>
            <div class="stat-trend up">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
                <polyline points="17 6 23 6 23 12"></polyline>
              </svg>
              +12%
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon score">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
              </svg>
            </div>
            <div class="stat-content">
              <span class="stat-value">{{ store.stats.overview.averageScore }}</span>
              <span class="stat-label">平均得分</span>
            </div>
            <div class="stat-trend up">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
                <polyline points="17 6 23 6 23 12"></polyline>
              </svg>
              +8%
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon improvement">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="20" x2="12" y2="10"></line>
                <line x1="18" y1="20" x2="18" y2="4"></line>
                <line x1="6" y1="20" x2="6" y2="16"></line>
              </svg>
            </div>
            <div class="stat-content">
              <span class="stat-value">{{ store.stats.overview.improvementRate }}%</span>
              <span class="stat-label">能力提升率</span>
            </div>
            <div class="stat-trend up">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
                <polyline points="17 6 23 6 23 12"></polyline>
              </svg>
              +23%
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon study">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <polyline points="12 6 12 12 16 14"></polyline>
              </svg>
            </div>
            <div class="stat-content">
              <span class="stat-value">{{ store.stats.overview.studyHours }}h</span>
              <span class="stat-label">累计学习时长</span>
            </div>
            <div class="stat-trend neutral">
              本周 +5h
            </div>
          </div>
        </div>
      </section>

      <!-- 主内容区域 -->
      <div class="main-grid">
        <!-- 进度图表 -->
        <section class="progress-section animate-in" style="--delay: 2">
          <div class="section-header">
            <h2 class="section-title">学习进度</h2>
            <div class="chart-tabs">
              <button
                v-for="tab in chartTabs"
                :key="tab.key"
                class="chart-tab"
                :class="{ active: activeChartTab === tab.key }"
                @click="activeChartTab = tab.key"
              >
                {{ tab.label }}
              </button>
            </div>
          </div>
          <div class="chart-container">
            <div class="chart-bars">
              <div
                v-for="(item, index) in chartData"
                :key="item.week"
                class="bar-group"
                :style="{ '--index': index }"
              >
                <div class="bar-wrapper">
                  <div class="bar" :style="{ height: item.score + '%' }">
                    <span class="bar-value">{{ item.score }}</span>
                  </div>
                </div>
                <span class="bar-label">{{ item.week }}</span>
              </div>
            </div>
          </div>
        </section>

        <!-- 技能雷达 -->
        <section class="skills-section animate-in" style="--delay: 3">
          <div class="section-header">
            <h2 class="section-title">技能评估</h2>
          </div>
          <div class="skills-list">
            <div
              v-for="(skill, index) in store.stats.skillRadar"
              :key="skill.skill"
              class="skill-item"
              :style="{ '--index': index }"
            >
              <div class="skill-info">
                <span class="skill-name">{{ skill.skill }}</span>
                <span class="skill-score">{{ skill.score }}</span>
              </div>
              <div class="skill-bar">
                <div class="skill-fill" :style="{ width: skill.score + '%' }"></div>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- 最近动态 & 推荐岗位 -->
      <div class="bottom-grid">
        <!-- 最近动态 -->
        <section class="activity-section animate-in" style="--delay: 4">
          <div class="section-header">
            <h2 class="section-title">最近动态</h2>
            <button class="view-all-btn">查看全部</button>
          </div>
          <div class="activity-list">
            <div
              v-for="(activity, index) in store.stats.recentActivity"
              :key="index"
              class="activity-item"
              :style="{ '--index': index }"
            >
              <div class="activity-icon" :class="activity.type">
                <span v-if="activity.type === 'interview'">🎯</span>
                <span v-else-if="activity.type === 'resume'">📄</span>
                <span v-else-if="activity.type === 'practice'">📝</span>
                <span v-else>📊</span>
              </div>
              <div class="activity-content">
                <p class="activity-text">{{ activity.content }}</p>
                <span class="activity-time">{{ activity.time }}</span>
              </div>
              <div v-if="activity.score" class="activity-score">
                {{ activity.score }}分
              </div>
            </div>
          </div>
        </section>

        <!-- 推荐岗位 -->
        <section class="jobs-section animate-in" style="--delay: 5">
          <div class="section-header">
            <h2 class="section-title">推荐岗位</h2>
            <button class="view-all-btn">更多职位</button>
          </div>
          <div class="jobs-list">
            <div
              v-for="job in store.jobs.slice(0, 3)"
              :key="job.id"
              class="job-card"
            >
              <div class="job-header">
                <div class="company-logo">
                  {{ job.company.charAt(0) }}
                </div>
                <div class="job-info">
                  <h4 class="job-position">{{ job.position }}</h4>
                  <span class="job-company">{{ job.company }}</span>
                </div>
                <div class="match-score">
                  <span class="score-value">{{ job.matchScore }}%</span>
                  <span class="score-label">匹配度</span>
                </div>
              </div>
              <div class="job-details">
                <span class="job-salary">{{ job.salary }}</span>
                <span class="job-location">{{ job.location }}</span>
              </div>
              <div class="job-tags">
                <span v-for="tag in job.tags" :key="tag" class="job-tag">{{ tag }}</span>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAppStore } from '@/stores'
import { useRouter } from 'vue-router'
import type { WeeklyProgress } from '@/types'

interface ChartTab {
  key: string
  label: string
}

const store = useAppStore()
const router = useRouter()

const activeChartTab = ref<string>('score')
const chartTabs: ChartTab[] = [
  { key: 'score', label: '得分趋势' },
  { key: 'count', label: '面试次数' }
]

const chartData = computed<WeeklyProgress[]>(() => store.stats.weeklyProgress)

function startInterview(): void {
  router.push('/interview')
  store.setActiveNav('interview')
}

function goToResume(): void {
  router.push('/resume')
  store.setActiveNav('resume')
}
</script>

<style lang="scss" scoped>
.home-page {
  padding: $spacing-2xl;
  max-width: 1400px;
  margin: 0 auto;
}

.container {
  display: flex;
  flex-direction: column;
  gap: $spacing-2xl;
}

// 欢迎区域
.welcome-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.welcome-title {
  font-family: $font-display;
  font-size: $text-4xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-sm;
}

.wave-emoji {
  display: inline-block;
  animation: wave 1.5s ease-in-out infinite;
  transform-origin: 70% 70%;
}

@keyframes wave {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(20deg); }
  75% { transform: rotate(-15deg); }
}

.welcome-subtitle {
  font-size: $text-base;
  color: $color-text-secondary;
}

.quick-actions {
  display: flex;
  gap: $spacing-md;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md $spacing-lg;
  border-radius: $radius-md;
  font-size: $text-sm;
  font-weight: $weight-medium;
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
    color: $color-text-primary;
    border: 1px solid rgba(255, 255, 255, 0.1);
    &:hover {
      background: rgba(255, 255, 255, 0.1);
      border-color: rgba(255, 255, 255, 0.2);
    }
  }
}

// 统计卡片
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $spacing-lg;
  margin-top: $spacing-lg;
}

.stat-card {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
  transition: all $transition-fast;
  &:hover {
    transform: translateY(-4px);
    border-color: rgba(212, 168, 83, 0.2);
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  }
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  &.interviews {
    background: rgba(96, 165, 250, 0.15);
    color: #60a5fa;
  }
  &.score {
    background: rgba(212, 168, 83, 0.15);
    color: $color-accent;
  }
  &.improvement {
    background: rgba(52, 211, 153, 0.15);
    color: #34d399;
  }
  &.study {
    background: rgba(167, 139, 250, 0.15);
    color: #a78bfa;
  }
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.stat-value {
  font-family: $font-display;
  font-size: $text-3xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
}

.stat-label {
  font-size: $text-sm;
  color: $color-text-tertiary;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $text-xs;
  font-weight: $weight-medium;
  &.up {
    color: $color-success;
  }
  &.down {
    color: $color-error;
  }
  &.neutral {
    color: $color-text-tertiary;
  }
}

// Section 通用
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

// 主内容区域
.main-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: $spacing-xl;
}

// 进度图表
.chart-container {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-xl;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.chart-tabs {
  display: flex;
  gap: $spacing-xs;
}

.chart-tab {
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

.chart-bars {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  height: 200px;
  padding-top: $spacing-lg;
}

.bar-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-sm;
  flex: 1;
  animation: slideUp 0.6s ease forwards;
  animation-delay: calc(var(--index) * 0.1s);
  opacity: 0;
}

.bar-wrapper {
  width: 40px;
  height: 160px;
  display: flex;
  align-items: flex-end;
}

.bar {
  width: 100%;
  background: $gradient-gold;
  border-radius: $radius-sm $radius-sm 0 0;
  position: relative;
  transition: height 0.6s ease;
  &:hover {
    filter: brightness(1.1);
  }
}

.bar-value {
  position: absolute;
  top: -24px;
  left: 50%;
  transform: translateX(-50%);
  font-size: $text-xs;
  font-weight: $weight-semibold;
  color: $color-accent;
}

.bar-label {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

// 技能列表
.skills-list {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.skill-item {
  animation: slideUp 0.5s ease forwards;
  animation-delay: calc(var(--index) * 0.08s);
  opacity: 0;
}

.skill-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: $spacing-xs;
}

.skill-name {
  font-size: $text-sm;
  color: $color-text-secondary;
}

.skill-score {
  font-size: $text-sm;
  font-weight: $weight-semibold;
  color: $color-accent;
}

.skill-bar {
  height: 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: $radius-full;
  overflow: hidden;
}

.skill-fill {
  height: 100%;
  background: $gradient-gold;
  border-radius: $radius-full;
  transition: width 0.8s ease;
}

// 底部区域
.bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: $spacing-xl;
}

// 活动列表
.activity-list {
  background: $gradient-card;
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
  overflow: hidden;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-lg;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
  transition: background $transition-fast;
  animation: slideUp 0.5s ease forwards;
  animation-delay: calc(var(--index) * 0.1s);
  opacity: 0;
  &:last-child {
    border-bottom: none;
  }
  &:hover {
    background: rgba(255, 255, 255, 0.02);
  }
}

.activity-icon {
  width: 40px;
  height: 40px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.05);
}

.activity-content {
  flex: 1;
}

.activity-text {
  font-size: $text-sm;
  color: $color-text-primary;
  margin-bottom: $spacing-xs;
}

.activity-time {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.activity-score {
  padding: $spacing-xs $spacing-sm;
  background: $color-success-bg;
  color: $color-success;
  font-size: $text-xs;
  font-weight: $weight-semibold;
  border-radius: $radius-sm;
}

// 岗位列表
.jobs-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.job-card {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all $transition-fast;
  &:hover {
    transform: translateX(4px);
    border-color: rgba(212, 168, 83, 0.2);
  }
}

.job-header {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  margin-bottom: $spacing-md;
}

.company-logo {
  width: 44px;
  height: 44px;
  border-radius: $radius-md;
  background: $gradient-gold;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: $font-display;
  font-weight: $weight-bold;
  color: $color-bg-deep;
}

.job-info {
  flex: 1;
}

.job-position {
  font-size: $text-base;
  font-weight: $weight-medium;
  color: $color-text-primary;
  margin-bottom: $spacing-xs;
}

.job-company {
  font-size: $text-sm;
  color: $color-text-tertiary;
}

.match-score {
  text-align: right;
}

.score-value {
  display: block;
  font-size: $text-lg;
  font-weight: $weight-semibold;
  color: $color-accent;
}

.score-label {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.job-details {
  display: flex;
  gap: $spacing-md;
  margin-bottom: $spacing-md;
}

.job-salary {
  font-size: $text-sm;
  font-weight: $weight-semibold;
  color: $color-success;
}

.job-location {
  font-size: $text-sm;
  color: $color-text-tertiary;
}

.job-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
}

.job-tag {
  padding: $spacing-xs $spacing-sm;
  background: rgba(255, 255, 255, 0.05);
  color: $color-text-secondary;
  font-size: $text-xs;
  border-radius: $radius-sm;
}

.view-all-btn {
  font-size: $text-sm;
  color: $color-accent;
  transition: color $transition-fast;
  &:hover {
    color: $color-accent-light;
  }
}

// 动画
.animate-in {
  animation: slideUp 0.6s ease forwards;
  animation-delay: calc(var(--delay) * 0.1s);
  opacity: 0;
}
</style>
