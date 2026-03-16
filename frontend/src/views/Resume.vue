<!--=====================================================
  LandIt 简历管理页面
  @author Azir
=====================================================-->

<template>
  <div class="resume-page">
    <div class="container">
      <!-- 页面标题 -->
      <header class="page-header animate-in" style="--delay: 0">
        <div class="header-content">
          <h1 class="page-title">简历管理</h1>
          <p class="page-desc">创建、管理和优化你的求职简历</p>
        </div>
        <button class="create-btn" @click="createNewResume">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          创建新简历
        </button>
      </header>

      <!-- 主简历卡片 -->
      <section class="primary-resume-section animate-in" style="--delay: 1" v-if="store.primaryResume">
        <div class="section-label">
          <span class="label-dot"></span>
          主简历
        </div>
        <div class="primary-resume-card">
          <div class="resume-main">
            <div class="resume-icon">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
              </svg>
            </div>
            <div class="resume-info">
              <h3 class="resume-name">{{ store.primaryResume.name }}</h3>
              <p class="resume-target">目标岗位：{{ store.primaryResume.targetPosition || '暂未设置目标职位' }}</p>
            </div>
          </div>
          <div class="resume-stats">
            <div class="stat-item">
              <div class="stat-ring" :style="{ '--score': store.primaryResume.analyzed ? (store.primaryResume.score || 0) : 0 }">
                <span class="ring-value">{{ store.primaryResume.analyzed ? (store.primaryResume.score || 0) : '~' }}</span>
              </div>
              <span class="stat-label">简历评分</span>
            </div>
            <div class="stat-item">
              <div class="stat-ring" :style="{ '--score': store.primaryResume.analyzed ? (store.primaryResume.completeness || 0) : 0 }">
                <span class="ring-value">{{ store.primaryResume.analyzed ? (store.primaryResume.completeness || 0) + '%' : '~' }}</span>
              </div>
              <span class="stat-label">完整度</span>
            </div>
          </div>
          <div class="resume-actions">
            <button class="action-btn primary" @click="viewResume(store.primaryResume.id)">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                <circle cx="12" cy="12" r="3"></circle>
              </svg>
              查看详情
            </button>
            <button class="action-btn secondary" @click="showTailorModal = true" :disabled="!store.primaryResume.analyzed">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
                <polyline points="10 9 9 9 8 9"></polyline>
              </svg>
              定制简历
            </button>

          </div>
        </div>
      </section>

      <!-- 其他简历列表 -->
      <section class="resumes-section animate-in" style="--delay: 2">
        <div class="section-header">
          <h2 class="section-title">所有简历</h2>
          <div class="filter-tabs">
            <button
              v-for="filter in filters"
              :key="filter.key"
              class="filter-tab"
              :class="{ active: activeFilter === filter.key }"
              @click="switchFilter(filter.key)"
            >
              {{ filter.label }}
            </button>
          </div>
        </div>

        <div class="resumes-grid">
          <div
            v-for="(resume, index) in filteredResumes"
            :key="resume.id"
            class="resume-card"
            :style="{ '--index': index }"
            @click="viewResume(resume.id)"
          >
            <div class="card-header">
              <div class="resume-badges">
                <div class="resume-badge primary-badge" v-if="resume.isPrimary">主简历</div>
                <div class="resume-badge" :class="getStatusClass(resume.status)">
                  {{ getStatusText(resume.status) }}
                </div>
              </div>
              <div class="resume-score">
                <span class="score-value">{{ resume.score }}</span>
                <span class="score-max">/100</span>
              </div>
            </div>
            <h4 class="card-title">{{ resume.name }}</h4>
            <p class="card-target">{{ resume.targetPosition || '暂未设置目标职位' }}</p>
            <div class="card-progress">
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: resume.completeness + '%' }"></div>
              </div>
              <span class="progress-text">完整度 {{ resume.completeness }}%</span>
            </div>
            <div class="card-footer">
              <span class="update-time">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"></circle>
                  <polyline points="12 6 12 12 16 14"></polyline>
                </svg>
                {{ resume.updatedAt }}
              </span>
              <div class="card-actions">
                <button class="icon-action" @click.stop="setPrimary(resume.id)" v-if="!resume.isPrimary">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
                  </svg>
                </button>
                <button class="icon-action" @click.stop="deleteResume(resume.id)">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                  </svg>
                </button>
              </div>
            </div>
          </div>

          <!-- 新建简历卡片 -->
          <div class="new-resume-card" @click="createNewResume">
            <div class="new-icon">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
            </div>
            <span class="new-text">创建新简历</span>
          </div>
        </div>
      </section>

      <!-- AI优化建议 -->
      <section class="suggestions-section animate-in" style="--delay: 3">
        <div class="section-header">
          <h2 class="section-title">AI优化建议</h2>
          <span class="suggestion-count">{{ store.suggestions.length }} 条建议</span>
        </div>
        <div class="suggestions-list">
          <div
            v-for="(suggestion, index) in store.suggestions"
            :key="suggestion.id"
            class="suggestion-card"
            :style="{ '--index': index }"
          >
            <div class="suggestion-indicator" :class="suggestion.type"></div>
            <div class="suggestion-content">
              <div class="suggestion-header">
                <span class="suggestion-category">{{ suggestion.category }}</span>
                <span class="suggestion-impact" :class="suggestion.impact">
                  {{ suggestion.impact }}影响
                </span>
              </div>
              <h4 class="suggestion-title">{{ suggestion.title }}</h4>
              <p class="suggestion-desc">{{ suggestion.description }}</p>
              <div class="suggestion-location">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                  <circle cx="12" cy="10" r="3"></circle>
                </svg>
                {{ suggestion.position }}
              </div>
            </div>
            <button class="apply-btn">应用建议</button>
          </div>
        </div>
      </section>
    </div>

    <!-- 优化进度弹窗 -->
    <OptimizeProgressModal
      v-model:visible="showOptimizeModal"
      :state="optimizeState"
      @cancel="handleCancelOptimize"
      @toggle-expand="handleToggleExpand"
    />

    <!-- 定制简历弹窗 -->
    <TailorResumeModal
      v-model:visible="showTailorModal"
      :resume-id="store.primaryResume?.id || ''"
      @complete="handleTailorComplete"
    />

    <!-- 删除确认弹窗 -->
    <ConfirmModal
      v-model:visible="showDeleteConfirm"
      title="删除简历"
      message="确定要删除这份简历吗？此操作不可恢复。"
      confirm-text="删除"
      :danger="true"
      @confirm="confirmDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAppStore } from '@/stores'
import { useRouter } from 'vue-router'
import type { Resume, ResumeStatus } from '@/types'
import type { OptimizeStage } from '@/types/resume-optimize'

// 导入优化相关
import { useResumeOptimize } from '@/composables/useResumeOptimize'
import OptimizeProgressModal from '@/components/resume/OptimizeProgressModal.vue'
import TailorResumeModal from '@/components/resume/TailorResumeModal.vue'
import ConfirmModal from '@/components/common/ConfirmModal.vue'

interface FilterItem {
  key: string
  label: string
}

const store = useAppStore()
const router = useRouter()

const activeFilter = ref<string>('all')
const filters: FilterItem[] = [
  { key: 'all', label: '全部' },
  { key: 'optimized', label: '已优化' },
  { key: 'draft', label: '草稿' }
]

// 删除确认弹窗状态
const showDeleteConfirm = ref(false)
const pendingDeleteId = ref<string | null>(null)

// 优化相关状态
const showOptimizeModal = ref(false)
const showTailorModal = ref(false)
const currentOptimizeResumeId = ref<string | null>(null)
const {
  state: optimizeState,
  startOptimize,
  cancelOptimize,
  toggleStageExpanded
} = useResumeOptimize()

// 页面加载时获取主简历信息和简历列表
onMounted(async () => {
  await Promise.all([
    store.fetchPrimaryResume(),
    store.fetchResumes()
  ])
})

const filteredResumes = computed<Resume[]>(() => store.resumeList)

// 切换筛选标签时调用后端接口
async function switchFilter(key: string): Promise<void> {
  activeFilter.value = key
  const status = key === 'all' ? undefined : key
  await store.fetchResumes(status)
}

function getStatusText(status: ResumeStatus): string {
  const statusMap: Record<string, string> = {
    optimized: '已优化',
    draft: '草稿'
  }
  return statusMap[status] || status
}

function getStatusClass(status: ResumeStatus): string {
  const classMap: Record<string, string> = {
    optimized: 'optimized',
    draft: 'draft'
  }
  return classMap[status] || ''
}

function viewResume(id: string): void {
  router.push(`/resume/${id}`)
}

function optimizeResume(id: string): void {
  currentOptimizeResumeId.value = id
  showOptimizeModal.value = true

  // 获取目标岗位
  const targetPosition = store.primaryResume?.targetPosition || undefined

  // 开始 SSE 优化
  startOptimize(id, {
    mode: 'quick',
    targetPosition
  })
}

async function createNewResume(): Promise<void> {
  try {
    const newResumeId = await store.createResume({
      name: '新简历',
      targetPosition: undefined
    })
    if (newResumeId) {
      router.push(`/resume/${newResumeId}`)
    }
  } catch (error) {
    console.error('创建简历失败', error)
    alert('创建简历失败，请重试')
  }
}

async function setPrimary(id: string): Promise<void> {
  try {
    await store.setPrimaryResumeApi(id)
  } catch (error) {
    console.error('设置主简历失败', error)
    alert('设置主简历失败，请重试')
  }
}

function deleteResume(id: string): void {
  pendingDeleteId.value = id
  showDeleteConfirm.value = true
}

async function confirmDelete(): Promise<void> {
  if (!pendingDeleteId.value) return
  try {
    await store.deleteResumeFromApi(pendingDeleteId.value)
  } catch (error) {
    console.error('删除简历失败', error)
  } finally {
    pendingDeleteId.value = null
  }
}

// 优化相关处理
function handleCancelOptimize(): void {
  cancelOptimize()
}

function handleToggleExpand(stage: OptimizeStage): void {
  toggleStageExpanded(stage)
}

// 定制完成处理
function handleTailorComplete(): void {
  showTailorModal.value = false
  // 刷新简历列表
  store.fetchPrimaryResume()
}
</script>

<style lang="scss" scoped>
@use 'sass:color';

.resume-page {
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
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.create-btn {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md $spacing-lg;
  background: $gradient-gold;
  color: $color-bg-deep;
  font-size: $text-sm;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  transition: all $transition-fast;
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(212, 168, 83, 0.3);
  }
}

// 主简历区域
.primary-resume-section {
  position: relative;
}

.section-label {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
  font-size: $text-sm;
  color: $color-accent;
  font-weight: $weight-medium;
}

.label-dot {
  width: 8px;
  height: 8px;
  background: $color-accent;
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
}

.primary-resume-card {
  display: flex;
  align-items: center;
  gap: $spacing-2xl;
  padding: $spacing-xl;
  background: $gradient-card;
  border-radius: $radius-xl;
  border: 1px solid rgba(212, 168, 83, 0.2);
  position: relative;
  overflow: hidden;
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(135deg, rgba(212, 168, 83, 0.05) 0%, transparent 50%);
    pointer-events: none;
  }
}


.resume-main {
  display: flex;
  align-items: center;
  gap: $spacing-lg;
  flex: 1;
}

.resume-icon {
  width: 64px;
  height: 64px;
  background: $color-accent-glow;
  border-radius: $radius-lg;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $color-accent;
}

.resume-name {
  font-family: $font-display;
  font-size: $text-2xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-xs;
}

.resume-target {
  font-size: $text-sm;
  color: $color-text-secondary;
}

.resume-stats {
  display: flex;
  gap: $spacing-xl;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-sm;
}

.stat-ring {
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
}

.ring-value {
  position: relative;
  z-index: 1;
  font-family: $font-display;
  font-size: $text-lg;
  font-weight: $weight-semibold;
  color: $color-accent;
}

.stat-label {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.resume-actions {
  display: flex;
  gap: $spacing-md;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  font-size: $text-sm;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  transition: all $transition-fast;
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  &.primary {
    background: $color-accent-glow;
    color: $color-accent;
    border: 1px solid rgba(212, 168, 83, 0.3);
    &:hover:not(:disabled) {
      background: rgba(212, 168, 83, 0.2);
    }
  }
  &.secondary {
    background: rgba(255, 255, 255, 0.05);
    color: $color-text-secondary;
    &:hover:not(:disabled) {
      background: rgba(255, 255, 255, 0.1);
      color: $color-text-primary;
    }
  }
}

// 简历列表
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

.filter-tabs {
  display: flex;
  gap: $spacing-xs;
  background: rgba(255, 255, 255, 0.03);
  padding: $spacing-xs;
  border-radius: $radius-md;
}

.filter-tab {
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

.resumes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: $spacing-lg;
}

.resume-card {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
  cursor: pointer;
  transition: all $transition-fast;
  animation: slideUp 0.5s ease forwards;
  animation-delay: calc(var(--index) * 0.1s);
  opacity: 0;
  &:hover {
    transform: translateY(-4px);
    border-color: rgba(212, 168, 83, 0.2);
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-md;
}

.resume-badges {
  display: flex;
  gap: $spacing-xs;
  align-items: center;
}

.resume-badge {
  padding: $spacing-xs $spacing-sm;
  font-size: $text-xs;
  font-weight: $weight-medium;
  border-radius: $radius-sm;
  &.optimized {
    background: $color-success-bg;
    color: $color-success;
  }
  &.draft {
    background: $color-warning-bg;
    color: $color-warning;
  }
  &.primary-badge {
    background: $color-accent-glow;
    color: $color-accent;
    border: 1px solid rgba(212, 168, 83, 0.3);
  }
}

.resume-score {
  .score-value {
    font-family: $font-display;
    font-size: $text-lg;
    font-weight: $weight-semibold;
    color: $color-accent;
  }
  .score-max {
    font-size: $text-xs;
    color: $color-text-tertiary;
  }
}

.card-title {
  font-size: $text-base;
  font-weight: $weight-medium;
  color: $color-text-primary;
  margin-bottom: $spacing-xs;
}

.card-target {
  font-size: $text-sm;
  color: $color-text-tertiary;
  margin-bottom: $spacing-md;
}

.card-progress {
  margin-bottom: $spacing-md;
}

.progress-bar {
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: $radius-full;
  overflow: hidden;
  margin-bottom: $spacing-xs;
}

.progress-fill {
  height: 100%;
  background: $gradient-gold;
  border-radius: $radius-full;
  transition: width 0.6s ease;
}

.progress-text {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.update-time {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.card-actions {
  display: flex;
  gap: $spacing-xs;
}

.icon-action {
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
    color: $color-text-primary;
  }
}

// 新建简历卡片
.new-resume-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: $spacing-md;
  min-height: 200px;
  background: transparent;
  border: 2px dashed rgba(255, 255, 255, 0.1);
  border-radius: $radius-lg;
  cursor: pointer;
  transition: all $transition-fast;
  &:hover {
    border-color: rgba(212, 168, 83, 0.3);
    background: rgba(212, 168, 83, 0.05);
    .new-icon {
      color: $color-accent;
      transform: scale(1.1);
    }
  }
}

.new-icon {
  color: $color-text-tertiary;
  transition: all $transition-fast;
}

.new-text {
  font-size: $text-sm;
  color: $color-text-tertiary;
}

// 建议区域
.suggestion-count {
  font-size: $text-sm;
  color: $color-text-tertiary;
}

.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.suggestion-card {
  display: flex;
  align-items: flex-start;
  gap: $spacing-lg;
  padding: $spacing-lg;
  background: $gradient-card;
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
  animation: slideUp 0.5s ease forwards;
  animation-delay: calc(var(--index) * 0.1s);
  opacity: 0;
  transition: all $transition-fast;
  &:hover {
    border-color: rgba(212, 168, 83, 0.15);
  }
}

.suggestion-indicator {
  width: 4px;
  height: 100%;
  min-height: 80px;
  border-radius: $radius-full;
  &.critical {
    background: $color-error;
  }
  &.improvement {
    background: $color-warning;
  }
  &.enhancement {
    background: $color-info;
  }
}

.suggestion-content {
  flex: 1;
}

.suggestion-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-sm;
}

.suggestion-category {
  font-size: $text-xs;
  color: $color-text-tertiary;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.suggestion-impact {
  padding: 2px 8px;
  font-size: $text-xs;
  font-weight: $weight-medium;
  border-radius: $radius-sm;
  &.高 {
    background: $color-error-bg;
    color: $color-error;
  }
  &.中 {
    background: $color-warning-bg;
    color: $color-warning;
  }
  &.低 {
    background: $color-info-bg;
    color: $color-info;
  }
}

.suggestion-title {
  font-size: $text-base;
  font-weight: $weight-medium;
  color: $color-text-primary;
  margin-bottom: $spacing-xs;
}

.suggestion-desc {
  font-size: $text-sm;
  color: $color-text-secondary;
  line-height: $leading-relaxed;
  margin-bottom: $spacing-sm;
}

.suggestion-location {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.apply-btn {
  padding: $spacing-sm $spacing-md;
  background: $color-accent-glow;
  color: $color-accent;
  font-size: $text-sm;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  border: 1px solid rgba(212, 168, 83, 0.3);
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
