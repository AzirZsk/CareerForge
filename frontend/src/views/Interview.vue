<!--=====================================================
  CareerForge 面试试演页面
  @author Azir
=====================================================-->

<template>
  <div class="interview-page">
    <div class="container">
      <!-- 页面标题 -->
      <header
        class="page-header animate-in"
        style="--delay: 0"
      >
        <div class="header-content">
          <h1 class="page-title">
            面试试演
          </h1>
          <p class="page-desc">
            AI模拟真实面试场景，帮助你从容应对每一次挑战
          </p>
        </div>
      </header>

      <!-- 开始面试区域 -->
      <section
        class="start-section animate-in"
        style="--delay: 1"
      >
        <div class="start-card">
          <div class="start-visual">
            <div class="visual-circle">
              <div class="circle-ring ring-1" />
              <div class="circle-ring ring-2" />
              <div class="circle-ring ring-3" />
              <div class="circle-core">
                <svg
                  width="48"
                  height="48"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.5"
                >
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                </svg>
              </div>
            </div>
          </div>
          <div class="start-content">
            <h2 class="start-title">
              准备好开始了吗？
            </h2>
            <p class="start-desc">
              选择面试类型，我们将为你定制专属的模拟面试体验
            </p>
            <div class="interview-types">
              <button
                v-for="type in interviewTypes"
                :key="type.key"
                class="type-btn"
                :class="{ active: selectedType === type.key }"
                @click="selectedType = type.key"
              >
                <span class="type-icon">{{ type.icon }}</span>
                <span class="type-name">{{ type.name }}</span>
                <span class="type-desc">{{ type.desc }}</span>
              </button>
            </div>
            <button
              class="start-btn"
              :disabled="!selectedType"
              @click="startInterview"
            >
              <svg
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <polygon points="5 3 19 12 5 21 5 3" />
              </svg>
              开始面试
            </button>
          </div>
        </div>
      </section>

      <!-- 面试设置 -->
      <section
        class="settings-section animate-in"
        style="--delay: 2"
      >
        <h3 class="settings-title">
          面试设置
        </h3>
        <div class="settings-grid">
          <div class="setting-item">
            <label class="setting-label">目标岗位</label>
            <div class="setting-value">
              <select
                v-model="settings.position"
                class="setting-select"
              >
                <option value="frontend">
                  前端工程师
                </option>
                <option value="backend">
                  后端工程师
                </option>
                <option value="fullstack">
                  全栈工程师
                </option>
                <option value="architect">
                  架构师
                </option>
              </select>
            </div>
          </div>
          <div class="setting-item">
            <label class="setting-label">难度等级</label>
            <div class="difficulty-options">
              <button
                v-for="level in difficultyLevels"
                :key="level.key"
                class="difficulty-btn"
                :class="{ active: settings.difficulty === level.key }"
                @click="settings.difficulty = level.key"
              >
                {{ level.label }}
              </button>
            </div>
          </div>
          <div class="setting-item">
            <label class="setting-label">问题数量</label>
            <div class="question-counter">
              <button
                class="counter-btn"
                @click="decreaseQuestions"
              >
                -
              </button>
              <span class="counter-value">{{ settings.questionCount }}</span>
              <button
                class="counter-btn"
                @click="increaseQuestions"
              >
                +
              </button>
            </div>
          </div>
          <div class="setting-item">
            <label class="setting-label">面试时长</label>
            <div class="setting-value">
              <span class="duration-display">{{ estimatedDuration }} 分钟</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 历史记录 -->
      <section
        class="history-section animate-in"
        style="--delay: 3"
      >
        <div class="section-header">
          <h2 class="section-title">
            历史记录
          </h2>
          <button class="view-all-btn">
            查看全部
          </button>
        </div>
        <div class="history-list">
          <div
            v-for="(record, index) in store.recentInterviews"
            :key="record.id"
            class="history-card"
            :style="{ '--index': index }"
            @click="viewHistory(record.id)"
          >
            <div
              class="history-icon"
              :class="record.type"
            >
              <span v-if="record.type === 'technical'">🔬</span>
              <span v-else>💬</span>
            </div>
            <div class="history-info">
              <div class="history-header">
                <h4 class="history-position">
                  {{ record.position }}
                </h4>
                <span class="history-company">{{ record.company }}</span>
              </div>
              <div class="history-meta">
                <span class="meta-item">
                  <svg
                    width="14"
                    height="14"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <rect
                      x="3"
                      y="4"
                      width="18"
                      height="18"
                      rx="2"
                      ry="2"
                    />
                    <line
                      x1="16"
                      y1="2"
                      x2="16"
                      y2="6"
                    />
                    <line
                      x1="8"
                      y1="2"
                      x2="8"
                      y2="6"
                    />
                    <line
                      x1="3"
                      y1="10"
                      x2="21"
                      y2="10"
                    />
                  </svg>
                  {{ record.date }}
                </span>
                <span class="meta-item">
                  <svg
                    width="14"
                    height="14"
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
                  {{ record.duration }}分钟
                </span>
                <span class="meta-item">
                  <svg
                    width="14"
                    height="14"
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
                    <line
                      x1="12"
                      y1="16"
                      x2="12"
                      y2="12"
                    />
                    <line
                      x1="12"
                      y1="8"
                      x2="12.01"
                      y2="8"
                    />
                  </svg>
                  {{ record.questions }}题
                </span>
              </div>
            </div>
            <div class="history-score">
              <div
                class="score-ring"
                :style="{ '--score': record.score }"
              >
                <span>{{ record.score }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 热门题目 -->
      <section
        class="questions-section animate-in"
        style="--delay: 4"
      >
        <div class="section-header">
          <h2 class="section-title">
            热门题目
          </h2>
          <div class="category-tabs">
            <button
              v-for="cat in categories"
              :key="cat.key"
              class="category-tab"
              :class="{ active: activeCategory === cat.key }"
              @click="activeCategory = cat.key"
            >
              {{ cat.label }}
            </button>
          </div>
        </div>
        <div class="questions-grid">
          <div
            v-for="(q, index) in filteredQuestions"
            :key="q.id"
            class="question-card"
            :style="{ '--index': index }"
          >
            <div class="question-header">
              <span class="question-category">{{ q.category }}</span>
              <span
                class="question-difficulty"
                :class="q.difficulty"
              >
                {{ getDifficultyText(q.difficulty) }}
              </span>
            </div>
            <p class="question-text">
              {{ q.question }}
            </p>
            <div class="question-footer">
              <span class="key-points">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <polyline points="9 11 12 14 22 4" />
                  <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
                </svg>
                {{ q.keyPoints.length }} 个要点
              </span>
              <button
                class="practice-btn"
                @click="practiceQuestion(q)"
              >
                练习此题
              </button>
            </div>
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
import type { InterviewQuestion, QuestionDifficulty, InterviewSettings } from '@/types'

interface InterviewType {
  key: string
  name: string
  desc: string
  icon: string
}

interface DifficultyLevel {
  key: QuestionDifficulty
  label: string
}

interface CategoryItem {
  key: string
  label: string
}

const store = useAppStore()
const router = useRouter()

const selectedType = ref<string>('technical')
const activeCategory = ref<string>('all')

const interviewTypes: InterviewType[] = [
  { key: 'technical', name: '技术面试', desc: '专业能力考察', icon: '🔬' },
  { key: 'behavioral', name: '行为面试', desc: '软技能评估', icon: '💬' },
  { key: 'mixed', name: '综合面试', desc: '全面能力测试', icon: '🎯' }
]

const settings = ref<InterviewSettings>({
  position: 'frontend',
  difficulty: 'medium',
  questionCount: 10
})

const difficultyLevels: DifficultyLevel[] = [
  { key: 'easy', label: '简单' },
  { key: 'medium', label: '中等' },
  { key: 'hard', label: '困难' }
]

const categories: CategoryItem[] = [
  { key: 'all', label: '全部' },
  { key: 'Vue.js', label: 'Vue.js' },
  { key: 'JavaScript', label: 'JavaScript' },
  { key: 'TypeScript', label: 'TypeScript' },
  { key: '性能优化', label: '性能优化' }
]

const estimatedDuration = computed<number>(() => {
  return settings.value.questionCount * 3
})

const filteredQuestions = computed<InterviewQuestion[]>(() => {
  const allQuestions: InterviewQuestion[] = [...store.questions.technical, ...store.questions.behavioral]
  if (activeCategory.value === 'all') {
    return allQuestions.slice(0, 6)
  }
  return allQuestions.filter((q: InterviewQuestion) => q.category === activeCategory.value).slice(0, 6)
})

function getDifficultyText(difficulty: QuestionDifficulty): string {
  const map: Record<QuestionDifficulty, string> = { easy: '简单', medium: '中等', hard: '困难' }
  return map[difficulty] || difficulty
}

function decreaseQuestions(): void {
  if (settings.value.questionCount > 5) {
    settings.value.questionCount -= 5
  }
}

function increaseQuestions(): void {
  if (settings.value.questionCount < 30) {
    settings.value.questionCount += 5
  }
}

function startInterview(): void {
  router.push('/interview/session')
}

function viewHistory(id: string): void {
  router.push(`/review/${id}`)
}

function practiceQuestion(question: InterviewQuestion): void {
  console.log('练习题目', question)
}
</script>

<style lang="scss" scoped>
.interview-page {
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

// 开始面试区域
.start-card {
  display: flex;
  gap: $spacing-3xl;
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

.start-visual {
  display: flex;
  align-items: center;
  justify-content: center;
}

.visual-circle {
  position: relative;
  width: 180px;
  height: 180px;
}

.circle-ring {
  position: absolute;
  inset: 0;
  border: 1px solid rgba(212, 168, 83, 0.2);
  border-radius: 50%;
  animation: rotate 20s linear infinite;
  &.ring-1 {
    animation-duration: 15s;
  }
  &.ring-2 {
    inset: 15px;
    animation-duration: 20s;
    animation-direction: reverse;
  }
  &.ring-3 {
    inset: 30px;
    animation-duration: 25s;
  }
}

.circle-core {
  position: absolute;
  inset: 45px;
  background: $gradient-gold;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $color-bg-deep;
  animation: pulse 3s ease-in-out infinite;
}

.start-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.start-title {
  font-family: $font-display;
  font-size: $text-3xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-sm;
}

.start-desc {
  font-size: $text-base;
  color: $color-text-secondary;
  margin-bottom: $spacing-xl;
}

.interview-types {
  display: flex;
  gap: $spacing-md;
  margin-bottom: $spacing-xl;
}

.type-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-lg;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-lg;
  transition: all $transition-fast;
  &:hover {
    background: rgba(255, 255, 255, 0.05);
  }
  &.active {
    background: $color-accent-glow;
    border-color: rgba(212, 168, 83, 0.3);
    .type-icon {
      transform: scale(1.1);
    }
  }
}

.type-icon {
  font-size: $text-2xl;
  transition: transform $transition-fast;
}

.type-name {
  font-size: $text-base;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.type-desc {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.start-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  padding: $spacing-md $spacing-2xl;
  background: $gradient-gold;
  color: $color-bg-deep;
  font-size: $text-base;
  font-weight: $weight-semibold;
  border-radius: $radius-md;
  transition: all $transition-fast;
  align-self: flex-start;
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  &:not(:disabled):hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 32px rgba(212, 168, 83, 0.4);
  }
}

// 设置区域
.settings-title {
  font-family: $font-display;
  font-size: $text-xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-lg;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $spacing-lg;
}

.setting-item {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.setting-label {
  display: block;
  font-size: $text-sm;
  color: $color-text-tertiary;
  margin-bottom: $spacing-md;
}

.setting-select {
  width: 100%;
  padding: $spacing-sm $spacing-md;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-md;
  color: $color-text-primary;
  font-size: $text-sm;
  cursor: pointer;
  &:focus {
    outline: none;
    border-color: $color-accent;
  }
  option {
    background: $color-bg-secondary;
    color: $color-text-primary;
  }
}

.difficulty-options {
  display: flex;
  gap: $spacing-xs;
}

.difficulty-btn {
  flex: 1;
  padding: $spacing-sm;
  font-size: $text-sm;
  color: $color-text-tertiary;
  background: rgba(255, 255, 255, 0.03);
  border-radius: $radius-sm;
  transition: all $transition-fast;
  &.active {
    background: $color-accent-glow;
    color: $color-accent;
  }
  &:hover:not(.active) {
    background: rgba(255, 255, 255, 0.08);
  }
}

.question-counter {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-md;
}

.counter-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.05);
  border-radius: $radius-md;
  font-size: $text-lg;
  color: $color-text-secondary;
  transition: all $transition-fast;
  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: $color-text-primary;
  }
}

.counter-value {
  font-family: $font-display;
  font-size: $text-2xl;
  font-weight: $weight-semibold;
  color: $color-accent;
  min-width: 40px;
  text-align: center;
}

.duration-display {
  font-family: $font-display;
  font-size: $text-xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
}

// 历史记录
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

.view-all-btn {
  font-size: $text-sm;
  color: $color-accent;
  transition: color $transition-fast;
  &:hover {
    color: $color-accent-light;
  }
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.history-card {
  display: flex;
  align-items: center;
  gap: $spacing-lg;
  padding: $spacing-lg;
  background: $gradient-card;
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
  cursor: pointer;
  transition: all $transition-fast;
  animation: slideUp 0.5s ease forwards;
  animation-delay: calc(var(--index) * 0.1s);
  opacity: 0;
  &:hover {
    transform: translateX(4px);
    border-color: rgba(212, 168, 83, 0.15);
  }
}

.history-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.05);
  border-radius: $radius-md;
  font-size: $text-xl;
}

.history-info {
  flex: 1;
}

.history-header {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  margin-bottom: $spacing-sm;
}

.history-position {
  font-size: $text-base;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.history-company {
  font-size: $text-sm;
  color: $color-accent;
}

.history-meta {
  display: flex;
  gap: $spacing-md;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.history-score {
  .score-ring {
    width: 56px;
    height: 56px;
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
      inset: 5px;
      background: $color-bg-tertiary;
      border-radius: 50%;
    }
    span {
      position: relative;
      z-index: 1;
      font-family: $font-display;
      font-size: $text-lg;
      font-weight: $weight-semibold;
      color: $color-accent;
    }
  }
}

// 题目区域
.category-tabs {
  display: flex;
  gap: $spacing-xs;
}

.category-tab {
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

.questions-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-lg;
}

.question-card {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
  animation: slideUp 0.5s ease forwards;
  animation-delay: calc(var(--index) * 0.1s);
  opacity: 0;
  transition: all $transition-fast;
  &:hover {
    border-color: rgba(212, 168, 83, 0.15);
  }
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-md;
}

.question-category {
  font-size: $text-xs;
  color: $color-accent;
  font-weight: $weight-medium;
}

.question-difficulty {
  padding: $spacing-xs $spacing-sm;
  font-size: $text-xs;
  border-radius: $radius-sm;
  &.easy {
    background: $color-success-bg;
    color: $color-success;
  }
  &.medium {
    background: $color-warning-bg;
    color: $color-warning;
  }
  &.hard {
    background: $color-error-bg;
    color: $color-error;
  }
}

.question-text {
  font-size: $text-sm;
  color: $color-text-primary;
  line-height: $leading-relaxed;
  margin-bottom: $spacing-md;
}

.question-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.key-points {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.practice-btn {
  padding: $spacing-xs $spacing-md;
  background: $color-accent-glow;
  color: $color-accent;
  font-size: $text-xs;
  font-weight: $weight-medium;
  border-radius: $radius-sm;
  transition: all $transition-fast;
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

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
