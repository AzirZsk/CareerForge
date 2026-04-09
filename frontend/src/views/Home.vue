<!--=====================================================
  LandIt 首页 - 工作台
  @author Azir
=====================================================-->

<template>
  <div class="home-page">
    <div class="container">
      <!-- 欢迎区域 -->
      <section
        class="welcome-section animate-in"
        style="--delay: 0"
      >
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
          <button
            class="action-btn primary"
            @click="startInterview"
          >
            <font-awesome-icon
              icon="fa-solid fa-bullseye"
              class="action-icon"
            />
            <span>开始面试</span>
          </button>
          <button
            class="action-btn secondary"
            @click="goToResume"
          >
            <font-awesome-icon
              icon="fa-solid fa-file-lines"
              class="action-icon"
            />
            <span>优化简历</span>
          </button>
        </div>
      </section>

      <!-- 统计概览 -->
      <section
        class="stats-section animate-in"
        style="--delay: 1"
      >
        <div class="section-header">
          <h2 class="section-title">
            数据概览
          </h2>
          <span class="section-badge">本周</span>
        </div>
        <div class="stats-grid">
          <div
            class="stat-card clickable"
            @click="goToInterviewCenter"
          >
            <div class="stat-icon interviews">
              <font-awesome-icon icon="fa-solid fa-comments" />
            </div>
            <div class="stat-content">
              <span class="stat-value">{{ store.stats.overview.realInterviews }}</span>
              <span class="stat-label">真实面试</span>
            </div>
          </div>

          <div
            class="stat-card clickable"
            @click="goToMockInterview"
          >
            <div class="stat-icon mock">
              <font-awesome-icon icon="fa-solid fa-star" />
            </div>
            <div class="stat-content">
              <span class="stat-value">{{ store.stats.overview.mockInterviews }}</span>
              <span class="stat-label">模拟面试</span>
            </div>
          </div>

          <div
            class="stat-card clickable"
            @click="goToResume"
          >
            <div class="stat-icon resume">
              <font-awesome-icon icon="fa-solid fa-file-alt" />
            </div>
            <div class="stat-content">
              <span class="stat-value">{{ store.stats.overview.resumeCount }}</span>
              <span class="stat-label">简历数量</span>
            </div>
          </div>

          <div
            class="stat-card clickable"
            @click="goToInterviewCenter"
          >
            <div class="stat-icon preparation">
              <font-awesome-icon icon="fa-solid fa-circle-check" />
            </div>
            <div class="stat-content">
              <span class="stat-value">{{ store.stats.overview.preparationCompletionRate }}%</span>
              <span class="stat-label">准备完成率</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 主内容区域 -->
      <div class="main-grid">
        <!-- 进度图表 -->
        <section
          class="progress-section animate-in"
          style="--delay: 2"
        >
          <div class="section-header">
            <h2 class="section-title">
              近期面试
            </h2>
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
                  <div
                    class="bar"
                    :style="{ height: getBarHeight(item) + '%' }"
                  >
                    <span class="bar-value">{{ getBarValue(item) }}</span>
                  </div>
                </div>
                <span class="bar-label">{{ item.week }}</span>
              </div>
            </div>
          </div>
        </section>

        <!-- 最近动态 -->
        <section
          class="activity-section animate-in"
          style="--delay: 3"
        >
          <div class="section-header">
            <h2 class="section-title">
              最近动态
            </h2>
          </div>
          <div class="activity-list">
            <div
              v-for="(activity, index) in store.stats.recentActivity"
              :key="index"
              class="activity-item clickable"
              :style="{ '--index': index }"
              @click="goToActivity(activity)"
            >
              <div
                class="activity-icon"
                :class="activity.type"
              >
                <font-awesome-icon :icon="getActivityIcon(activity.type)" />
              </div>
              <div class="activity-content">
                <p class="activity-text">
                  {{ activity.content }}
                </p>
                <span class="activity-time">{{ activity.time }}</span>
              </div>
              <div
                v-if="activity.score"
                class="activity-score"
              >
                {{ activity.score }}分
              </div>
            </div>
          </div>
        </section>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAppStore } from '@/stores'
import { useRouter } from 'vue-router'
import type { WeeklyProgress, RecentActivity } from '@/types'

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

// 加载统计数据
onMounted(async () => {
  await store.fetchStatistics()
})

function startInterview(): void {
  router.push('/interview')
  store.setActiveNav('interview')
}

function goToResume(): void {
  router.push('/resume')
  store.setActiveNav('resume')
}

function goToInterviewCenter(): void {
  router.push('/interview-center')
  store.setActiveNav('interview-center')
}

function goToMockInterview(): void {
  router.push('/interview')
  store.setActiveNav('interview')
}

// 点击最近动态跳转到对应详情页
function goToActivity(activity: RecentActivity): void {
  if (!activity.relatedId) return
  switch (activity.type) {
    case 'resume':
      router.push(`/resume/${activity.relatedId}`)
      store.setActiveNav('resume')
      break
    case 'interview':
    case 'practice':
    case 'review':
      router.push(`/interview-center/${activity.relatedId}`)
      store.setActiveNav('interview-center')
      break
  }
}

// 获取动态图标
function getActivityIcon(type: string): string {
  const iconMap: Record<string, string> = {
    interview: 'fa-solid fa-briefcase',
    resume: 'fa-solid fa-file-pen',
    practice: 'fa-solid fa-pencil',
    default: 'fa-solid fa-chart-line'
  }
  return iconMap[type] || iconMap.default
}

// 根据当前 tab 获取柱状图高度
function getBarHeight(item: WeeklyProgress): number {
  if (activeChartTab.value === 'score') {
    // 分数范围 0-100，直接作为百分比
    return Math.max(item.score, 5)
  } else {
    // 次数范围 0-10，放大到百分比（最大 10 次 = 100%）
    return Math.max(item.interviews * 10, 5)
  }
}

// 根据当前 tab 获取柱状图显示值
function getBarValue(item: WeeklyProgress): string | number {
  if (activeChartTab.value === 'score') {
    return item.score
  } else {
    return item.interviews
  }
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
  .action-icon {
    font-size: 18px; // FA 图标大小
  }
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
  &.clickable {
    cursor: pointer;
    &:active {
      transform: translateY(-2px);
    }
  }
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px; // FA 图标大小
  &.interviews {
    background: rgba(96, 165, 250, 0.15);
    color: #60a5fa;
  }
  &.mock {
    background: rgba(251, 191, 36, 0.15);
    color: #fbbf24;
  }
  &.resume {
    background: rgba(52, 211, 153, 0.15);
    color: #34d399;
  }
  &.preparation {
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
  grid-template-columns: 1fr 1fr;
  gap: $spacing-xl;
  align-items: stretch;
}

.progress-section,
.activity-section {
  display: flex;
  flex-direction: column;
}

// 进度图表
.chart-container {
  flex: 1;
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-xl;
  border: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  min-height: 320px;
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

// 活动列表
.activity-list {
  flex: 1;
  background: $gradient-card;
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
  overflow: hidden;
  display: flex;
  flex-direction: column;
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
  &.clickable {
    cursor: pointer;
    &:hover {
      background: rgba(255, 255, 255, 0.05);
    }
    &:active {
      background: rgba(255, 255, 255, 0.08);
    }
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
  font-size: 16px; // FA 图标大小
  &.interview {
    background: rgba(96, 165, 250, 0.15);
    color: #60a5fa;
  }
  &.resume {
    background: rgba(52, 211, 153, 0.15);
    color: #34d399;
  }
  &.practice {
    background: rgba(251, 191, 36, 0.15);
    color: #fbbf24;
  }
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

// 动画
.animate-in {
  animation: slideUp 0.6s ease forwards;
  animation-delay: calc(var(--delay) * 0.1s);
  opacity: 0;
}
</style>
