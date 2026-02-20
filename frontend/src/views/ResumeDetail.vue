<!--=====================================================
  LandIt 简历详情页面
  @author Azir
=====================================================-->

<template>
  <div class="resume-detail-page">
    <div class="container">
      <!-- 返回导航 -->
      <nav class="back-nav animate-in" style="--delay: 0">
        <router-link to="/resume" class="back-link">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="19" y1="12" x2="5" y2="12"></line>
            <polyline points="12 19 5 12 12 5"></polyline>
          </svg>
          返回简历列表
        </router-link>
      </nav>

      <!-- 简历头部 -->
      <header class="resume-header animate-in" style="--delay: 1">
        <div class="header-left">
          <h1 class="resume-title">{{ store.currentResume.name }}</h1>
          <p class="resume-target">目标岗位：{{ store.currentResume.targetPosition }}</p>
        </div>
        <div class="header-right">
          <div class="score-overview">
            <div class="score-main">
              <div class="score-ring" :style="{ '--score': store.currentResume.overallScore }">
                <span>{{ store.currentResume.overallScore }}</span>
              </div>
              <div class="score-labels">
                <span class="score-title">综合评分</span>
                <span class="score-detail">关键词匹配 {{ store.currentResume.keywordMatch }}%</span>
              </div>
            </div>
          </div>
          <div class="header-actions">
            <button class="action-btn primary" @click="optimizeResume">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
              </svg>
              AI一键优化
            </button>
            <button class="action-btn secondary">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                <polyline points="7 10 12 15 17 10"></polyline>
                <line x1="12" y1="15" x2="12" y2="3"></line>
              </svg>
              导出PDF
            </button>
          </div>
        </div>
      </header>

      <!-- 评分指标 -->
      <section class="metrics-section animate-in" style="--delay: 2">
        <div class="metric-card">
          <div class="metric-header">
            <span class="metric-icon keyword">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
            </span>
            <span class="metric-title">关键词匹配</span>
          </div>
          <div class="metric-value">{{ store.currentResume.keywordMatch }}</div>
          <div class="metric-bar">
            <div class="metric-fill" :style="{ width: store.currentResume.keywordMatch + '%' }"></div>
          </div>
        </div>
        <div class="metric-card">
          <div class="metric-header">
            <span class="metric-icon format">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                <line x1="3" y1="9" x2="21" y2="9"></line>
                <line x1="9" y1="21" x2="9" y2="9"></line>
              </svg>
            </span>
            <span class="metric-title">格式规范</span>
          </div>
          <div class="metric-value">{{ store.currentResume.formatScore }}</div>
          <div class="metric-bar">
            <div class="metric-fill" :style="{ width: store.currentResume.formatScore + '%' }"></div>
          </div>
        </div>
        <div class="metric-card">
          <div class="metric-header">
            <span class="metric-icon content">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
              </svg>
            </span>
            <span class="metric-title">内容质量</span>
          </div>
          <div class="metric-value">{{ store.currentResume.contentScore }}</div>
          <div class="metric-bar">
            <div class="metric-fill" :style="{ width: store.currentResume.contentScore + '%' }"></div>
          </div>
        </div>
      </section>

      <!-- 简历内容 -->
      <div class="content-grid">
        <section class="sections-panel animate-in" style="--delay: 3">
          <div class="panel-header">
            <h2 class="panel-title">简历模块</h2>
            <button class="add-section-btn">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
              添加模块
            </button>
          </div>
          <div class="sections-list">
            <div
              v-for="(section, index) in store.currentResume.sections"
              :key="section.id"
              class="section-card"
              :class="{ active: activeSection === section.id }"
              :style="{ '--index': index }"
              @click="activeSection = section.id"
            >
              <div class="section-header">
                <div class="section-info">
                  <span class="section-icon">{{ getSectionIcon(section.type) }}</span>
                  <span class="section-name">{{ section.title }}</span>
                </div>
                <div class="section-score" :class="getScoreClass(section.score)">
                  {{ section.score }}
                </div>
              </div>
              <p class="section-preview">{{ getSectionPreview(section) }}</p>
              <div v-if="section.suggestions.length > 0" class="section-hint">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="12" y1="16" x2="12" y2="12"></line>
                  <line x1="12" y1="8" x2="12.01" y2="8"></line>
                </svg>
                {{ section.suggestions.length }} 条优化建议
              </div>
            </div>
          </div>
        </section>

        <section class="detail-panel animate-in" style="--delay: 4">
          <div class="panel-header">
            <h2 class="panel-title">{{ currentSectionDetail?.title }}</h2>
            <div class="panel-actions">
              <button class="panel-btn">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                </svg>
                编辑
              </button>
            </div>
          </div>
          <div class="detail-content">
            <div class="content-block" v-if="currentSectionDetail?.type === 'basic' && basicContent">
              <div class="info-row" v-for="(value, key) in basicContent" :key="key">
                <span class="info-label">{{ getFieldLabel(key) }}</span>
                <span class="info-value">{{ value }}</span>
              </div>
            </div>
            <div class="content-block" v-else-if="currentSectionDetail?.type === 'experience'">
              <div v-for="(item, idx) in experienceContent" :key="idx" class="experience-item">
                <div class="exp-header">
                  <h4 class="exp-title">{{ item.company }}</h4>
                  <span class="exp-period">{{ item.period }}</span>
                </div>
                <p class="exp-position" v-if="item.position">{{ item.position }}</p>
                <p class="exp-desc">{{ item.description }}</p>
              </div>
            </div>
            <div class="content-block" v-else-if="currentSectionDetail?.type === 'project'">
              <div v-for="(item, idx) in projectContent" :key="idx" class="experience-item">
                <div class="exp-header">
                  <h4 class="exp-title">{{ item.name }}</h4>
                  <span class="exp-period">{{ item.period }}</span>
                </div>
                <p class="exp-desc">{{ item.description }}</p>
                <div v-if="item.achievements && item.achievements.length > 0" class="exp-achievements">
                  <span v-for="a in item.achievements" :key="a" class="achievement-tag">{{ a }}</span>
                </div>
              </div>
            </div>
            <div class="content-block" v-else-if="currentSectionDetail?.type === 'skill'">
              <ul class="skill-list">
                <li v-for="(skill, idx) in skillContent" :key="idx">{{ skill }}</li>
              </ul>
            </div>
          </div>
          <div v-if="hasSuggestions" class="suggestions-block">
            <h4 class="suggestions-title">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"></path>
              </svg>
              优化建议
            </h4>
            <div v-for="sug in sectionSuggestions" :key="sug.content" class="suggestion-item" :class="sug.type">
              <span class="sug-icon">{{ sug.type === 'critical' ? '⚠️' : sug.type === 'improvement' ? '💡' : '✨' }}</span>
              <p class="sug-text">{{ sug.content }}</p>
              <button class="apply-sug-btn">应用</button>
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
import type { ResumeSection, ResumeSuggestionItem } from '@/types'

interface BasicContent {
  name: string
  phone: string
  email: string
  location: string
  age: number
  workYears: number
}

interface ExperienceItem {
  company: string
  position: string
  period: string
  description: string
}

interface ProjectItem {
  name: string
  role: string
  period: string
  description: string
  achievements: string[]
}

const store = useAppStore()
const activeSection = ref<string>('section_001')

const currentSectionDetail = computed<ResumeSection | undefined>(() => {
  return store.currentResume.sections.find((s: ResumeSection) => s.id === activeSection.value)
})

const basicContent = computed<BasicContent | null>(() => {
  if (currentSectionDetail.value?.type === 'basic') {
    return currentSectionDetail.value.content as unknown as BasicContent
  }
  return null
})

const experienceContent = computed<ExperienceItem[]>(() => {
  if (currentSectionDetail.value?.type === 'experience') {
    return currentSectionDetail.value.content as ExperienceItem[]
  }
  return []
})

const projectContent = computed<ProjectItem[]>(() => {
  if (currentSectionDetail.value?.type === 'project') {
    return currentSectionDetail.value.content as ProjectItem[]
  }
  return []
})

const skillContent = computed<string[]>(() => {
  if (currentSectionDetail.value?.type === 'skill') {
    return currentSectionDetail.value.content as string[]
  }
  return []
})

const sectionSuggestions = computed<ResumeSuggestionItem[]>(() => {
  return currentSectionDetail.value?.suggestions ?? []
})

const hasSuggestions = computed<boolean>(() => {
  return sectionSuggestions.value.length > 0
})

const sectionIcons: Record<string, string> = {
  basic: '👤',
  experience: '💼',
  project: '🎯',
  skill: '⚡',
  education: '🎓'
}

function getSectionIcon(type: string): string {
  return sectionIcons[type] || '📄'
}

function getSectionPreview(section: ResumeSection): string {
  if (section.type === 'basic') {
    const content = section.content as Record<string, unknown>
    return `${content.name as string} · ${content.workYears as number}年经验`
  }
  if (section.type === 'experience' || section.type === 'project') {
    return `${(section.content as unknown[]).length} 条记录`
  }
  if (section.type === 'skill') {
    return `${(section.content as string[]).length} 项技能`
  }
  return ''
}

function getScoreClass(score: number): string {
  if (score >= 90) return 'excellent'
  if (score >= 75) return 'good'
  return 'average'
}

function getFieldLabel(key: string): string {
  const labels: Record<string, string> = {
    name: '姓名',
    phone: '电话',
    email: '邮箱',
    location: '所在地',
    age: '年龄',
    workYears: '工作年限'
  }
  return labels[key] || key
}

function optimizeResume(): void {
  console.log('AI优化简历')
}
</script>

<style lang="scss" scoped>
.resume-detail-page {
  padding: $spacing-2xl;
  max-width: 1400px;
  margin: 0 auto;
}

.container {
  display: flex;
  flex-direction: column;
  gap: $spacing-xl;
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
.resume-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: $spacing-xl;
  background: $gradient-card;
  border-radius: $radius-xl;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.resume-title {
  font-family: $font-display;
  font-size: $text-3xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-xs;
}

.resume-target {
  font-size: $text-base;
  color: $color-text-secondary;
}

.header-right {
  display: flex;
  align-items: center;
  gap: $spacing-2xl;
}

.score-main {
  display: flex;
  align-items: center;
  gap: $spacing-lg;
}

.score-ring {
  width: 72px;
  height: 72px;
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
    inset: 6px;
    background: $color-bg-tertiary;
    border-radius: 50%;
  }
  span {
    position: relative;
    z-index: 1;
    font-family: $font-display;
    font-size: $text-2xl;
    font-weight: $weight-bold;
    color: $color-accent;
  }
}

.score-labels {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.score-title {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.score-detail {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.header-actions {
  display: flex;
  gap: $spacing-md;
}

.action-btn {
  display: flex;
  align-items: center;
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
    }
  }
}

// 指标区域
.metrics-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $spacing-lg;
}

.metric-card {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.metric-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
}

.metric-icon {
  width: 32px;
  height: 32px;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  &.keyword {
    background: rgba(96, 165, 250, 0.15);
    color: #60a5fa;
  }
  &.format {
    background: rgba(52, 211, 153, 0.15);
    color: #34d399;
  }
  &.content {
    background: rgba(212, 168, 83, 0.15);
    color: $color-accent;
  }
}

.metric-title {
  font-size: $text-sm;
  color: $color-text-tertiary;
}

.metric-value {
  font-family: $font-display;
  font-size: $text-3xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-md;
}

.metric-bar {
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: $radius-full;
  overflow: hidden;
}

.metric-fill {
  height: 100%;
  background: $gradient-gold;
  border-radius: $radius-full;
  transition: width 0.8s ease;
}

// 内容区域
.content-grid {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: $spacing-xl;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-lg;
}

.panel-title {
  font-family: $font-display;
  font-size: $text-xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
}

.add-section-btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $text-sm;
  color: $color-accent;
  transition: color $transition-fast;
  &:hover {
    color: $color-accent-light;
  }
}

.sections-panel {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.sections-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.section-card {
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all $transition-fast;
  animation: slideUp 0.4s ease forwards;
  animation-delay: calc(var(--index) * 0.1s);
  opacity: 0;
  &:hover {
    background: rgba(255, 255, 255, 0.04);
  }
  &.active {
    background: $color-accent-glow;
    border-color: rgba(212, 168, 83, 0.3);
  }
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xs;
}

.section-info {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.section-icon {
  font-size: $text-lg;
}

.section-name {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.section-score {
  font-size: $text-sm;
  font-weight: $weight-semibold;
  padding: $spacing-xs $spacing-sm;
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
}

.section-preview {
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-bottom: $spacing-sm;
}

.section-hint {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $text-xs;
  color: $color-warning;
}

// 详情面板
.detail-panel {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.panel-btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-xs $spacing-md;
  font-size: $text-sm;
  color: $color-text-secondary;
  background: rgba(255, 255, 255, 0.05);
  border-radius: $radius-sm;
  transition: all $transition-fast;
  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: $color-text-primary;
  }
}

.detail-content {
  margin-bottom: $spacing-xl;
}

.content-block {
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  padding: $spacing-lg;
}

.info-row {
  display: flex;
  padding: $spacing-md 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
  &:last-child {
    border-bottom: none;
  }
}

.info-label {
  width: 120px;
  font-size: $text-sm;
  color: $color-text-tertiary;
}

.info-value {
  flex: 1;
  font-size: $text-sm;
  color: $color-text-primary;
}

.experience-item {
  padding: $spacing-lg 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
  &:first-child {
    padding-top: 0;
  }
  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
}

.exp-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xs;
}

.exp-title {
  font-size: $text-base;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.exp-period {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.exp-position {
  font-size: $text-sm;
  color: $color-accent;
  margin-bottom: $spacing-sm;
}

.exp-desc {
  font-size: $text-sm;
  color: $color-text-secondary;
  line-height: $leading-relaxed;
  margin-bottom: $spacing-sm;
}

.exp-achievements {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
}

.achievement-tag {
  padding: $spacing-xs $spacing-sm;
  background: $color-success-bg;
  color: $color-success;
  font-size: $text-xs;
  border-radius: $radius-sm;
}

.skill-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  li {
    position: relative;
    padding-left: $spacing-lg;
    font-size: $text-sm;
    color: $color-text-secondary;
    &::before {
      content: '▸';
      position: absolute;
      left: 0;
      color: $color-accent;
    }
  }
}

.suggestions-block {
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  padding: $spacing-lg;
}

.suggestions-title {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  font-size: $text-base;
  font-weight: $weight-medium;
  color: $color-text-primary;
  margin-bottom: $spacing-lg;
}

.suggestion-item {
  display: flex;
  align-items: flex-start;
  gap: $spacing-md;
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  margin-bottom: $spacing-sm;
  border-left: 3px solid;
  &.critical {
    border-color: $color-error;
  }
  &.improvement {
    border-color: $color-warning;
  }
  &.enhancement {
    border-color: $color-info;
  }
}

.sug-icon {
  font-size: $text-lg;
}

.sug-text {
  flex: 1;
  font-size: $text-sm;
  color: $color-text-secondary;
  line-height: $leading-relaxed;
}

.apply-sug-btn {
  padding: $spacing-xs $spacing-md;
  background: $color-accent-glow;
  color: $color-accent;
  font-size: $text-xs;
  font-weight: $weight-medium;
  border-radius: $radius-sm;
  transition: all $transition-fast;
  white-space: nowrap;
  &:hover {
    background: rgba(212, 168, 83, 0.2);
  }
}

// 动画
.animate-in {
  animation: slideUp 0.6s ease forwards;
  animation-delay: calc(var(--delay) * 0.1s);
  opacity: 0;
}
</style>
