<!--=====================================================
  LandIt 简历优化进度弹窗
  @author Azir
=====================================================-->

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="modal-overlay" @click.self="handleOverlayClick">
        <div class="modal-container">
          <!-- 头部 -->
          <div class="modal-header">
            <div class="header-icon" :class="headerIconClass">
              <svg v-if="state.isOptimizing" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2v4m0 12v4M4.93 4.93l2.83 2.83m8.48 8.48l2.83 2.83M2 12h4m12 0h4M4.93 19.07l2.83-2.83m8.48-8.48l2.83-2.83">
                  <animateTransform attributeName="transform" type="rotate" from="0 12 12" to="360 12 12" dur="2s" repeatCount="indefinite"/>
                </path>
              </svg>
              <svg v-else-if="state.isCompleted" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
              <svg v-else-if="state.hasError" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <line x1="15" y1="9" x2="9" y2="15"/>
                <line x1="9" y1="9" x2="15" y2="15"/>
              </svg>
            </div>
            <h3 class="header-title">
              {{ headerTitle }}
            </h3>
            <button class="close-btn" @click="handleClose">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
          
          <!-- 目标岗位 -->
          <div class="target-info" v-if="state.targetPosition">
            <span class="target-label">目标岗位</span>
            <span class="target-value">{{ state.targetPosition }}</span>
          </div>
          
          <!-- 进度条 -->
          <div class="progress-section">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: state.progress + '%' }"></div>
            </div>
            <div class="progress-info">
              <span class="progress-text">{{ state.message }}</span>
              <span class="progress-percent">{{ state.progress }}%</span>
            </div>
          </div>
          
          <!-- 阶段列表 -->
          <div class="stages-section">
            <div 
              v-for="item in sortedStageHistory" 
              :key="item.stage"
              class="stage-item"
              :class="{ 
                active: item.stage === state.currentStage && state.isOptimizing,
                completed: item.completed
              }"
            >
              <div class="stage-main">
                <div class="stage-left">
                  <div class="stage-indicator">
                    <svg v-if="item.completed" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                    <div v-else-if="item.stage === state.currentStage && state.isOptimizing" class="spinner"></div>
                    <div v-else class="dot"></div>
                  </div>
                  <span class="stage-label">{{ getStageLabel(item.stage) }}</span>
                </div>
                <button 
                  v-if="item.completed && item.data" 
                  class="expand-btn"
                  @click="toggleExpand(item.stage)"
                >
                  <svg 
                    width="16" 
                    height="16" 
                    viewBox="0 0 24 24" 
                    fill="none" 
                    stroke="currentColor" 
                    stroke-width="2"
                    :class="{ rotated: item.expanded }"
                  >
                    <polyline points="6 9 12 15 18 9"/>
                  </svg>
                  {{ item.expanded ? '收起' : '展开' }}
                </button>
              </div>
              
              <!-- 展开的数据区域 -->
              <Transition name="expand">
                <div v-if="item.expanded && item.data" class="stage-data">
                  <!-- 解析简历数据 -->
                  <div v-if="item.stage === 'parse_resume'" class="data-content">
                    <div class="data-row">
                      <span class="data-label">模块数量</span>
                      <span class="data-value">{{ item.data.moduleCount }}</span>
                    </div>
                    <div class="data-row">
                      <span class="data-label">完整度</span>
                      <span class="data-value">{{ item.data.completeness }}%</span>
                    </div>
                    <div class="modules-list" v-if="item.data.modules?.length">
                      <div class="modules-title">解析到的模块：</div>
                      <div class="module-tag" v-for="mod in item.data.modules" :key="mod.id">
                        {{ mod.title }} ({{ mod.score }}分)
                      </div>
                    </div>
                  </div>
                  
                  <!-- 诊断数据 -->
                  <div v-else-if="item.stage === 'diagnose_quick' || item.stage === 'diagnose_precise'" class="data-content">
                    <div class="score-display">
                      <div class="score-ring" :style="{ '--score': item.data.overallScore }">
                        <span>{{ item.data.overallScore }}</span>
                      </div>
                      <span class="score-label">综合评分</span>
                    </div>
                    <div class="dimensions-grid" v-if="item.data.dimensions">
                      <div class="dimension-item" v-for="(dim, key) in item.data.dimensions" :key="key">
                        <span class="dim-name">{{ getDimensionLabel(String(key)) }}</span>
                        <span class="dim-score">{{ dim.score }}</span>
                      </div>
                    </div>
                    <div class="issues-section" v-if="item.data.issues?.length">
                      <div class="issues-title">问题 ({{ item.data.issues.length }})</div>
                      <div 
                        class="issue-item" 
                        v-for="(issue, idx) in item.data.issues.slice(0, 3)" 
                        :key="idx"
                        :class="issue.severity"
                      >
                        <span class="issue-severity">{{ issue.severity === 'high' ? '⚠️' : issue.severity === 'medium' ? '💡' : '✨' }}</span>
                        {{ issue.content }}
                      </div>
                    </div>
                    <div class="highlights-section" v-if="item.data.highlights?.length">
                      <div class="highlights-title">亮点</div>
                      <div class="highlight-tag" v-for="(h, idx) in item.data.highlights" :key="idx">{{ h }}</div>
                    </div>
                  </div>
                  
                  <!-- 生成建议数据 -->
                  <div v-else-if="item.stage === 'generate_suggestions'" class="data-content">
                    <div class="suggestions-summary">
                      <span class="suggestions-count">{{ item.data.totalSuggestions }} 条建议</span>
                      <span class="suggestions-improvement">{{ item.data.estimatedImprovement }}</span>
                    </div>
                    <div class="suggestions-list" v-if="item.data.suggestions?.length">
                      <div class="suggestion-item" v-for="sug in item.data.suggestions.slice(0, 3)" :key="sug.id">
                        <div class="sug-header">
                          <span class="sug-priority" :class="sug.priority">{{ sug.priority }}</span>
                          <span class="sug-title">{{ sug.title }}</span>
                        </div>
                        <div class="sug-section">模块: {{ sug.section }}</div>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 内容优化数据 -->
                  <div v-else-if="item.stage === 'optimize_section'" class="data-content">
                    <div class="changes-summary">
                      <span class="changes-count">{{ item.data.changeCount }} 处变更</span>
                      <span class="changes-improvement">预计提升 {{ item.data.improvementScore }} 分</span>
                    </div>
                    <div class="confidence-badge" :class="item.data.confidence">
                      置信度: {{ item.data.confidence }}
                    </div>
                    <div class="changes-list" v-if="item.data.changes?.length">
                      <div class="change-item" v-for="(change, idx) in item.data.changes.slice(0, 2)" :key="idx">
                        <div class="change-header">
                          <span class="change-section">{{ change.sectionTitle }}</span>
                          <span class="change-field">{{ change.field }}</span>
                        </div>
                        <div class="change-reason">{{ change.reason }}</div>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 保存版本数据 -->
                  <div v-else-if="item.stage === 'save_version'" class="data-content">
                    <div class="version-info">
                      <div class="version-name">{{ item.data.versionName }}</div>
                      <div class="score-comparison">
                        <span class="old-score">{{ item.data.originalScore }}</span>
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <line x1="5" y1="12" x2="19" y2="12"/>
                          <polyline points="12 5 19 12 12 19"/>
                        </svg>
                        <span class="new-score">{{ item.data.newScore }}</span>
                      </div>
                      <div class="improvement-badge">+{{ item.data.improvementScore }} 分</div>
                    </div>
                  </div>
                  
                  <!-- 默认 JSON 显示 -->
                  <div v-else class="data-content">
                    <pre class="json-display">{{ JSON.stringify(item.data, null, 2) }}</pre>
                  </div>
                </div>
              </Transition>
            </div>
          </div>
          
          <!-- 底部操作 -->
          <div class="modal-footer">
            <template v-if="state.isOptimizing">
              <button class="footer-btn secondary" @click="handleBackgroundRun">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="22 12 16 12 14 15 10 15 8 12 2 12"/>
                  <path d="M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"/>
                </svg>
                后台运行
              </button>
              <button class="footer-btn danger" @click="handleCancel">
                取消优化
              </button>
            </template>
            <template v-else>
              <button class="footer-btn primary" @click="handleClose">
                {{ state.isCompleted ? '完成' : '关闭' }}
              </button>
            </template>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { OptimizeState, OptimizeStage } from '@/types/resume-optimize'
import { getStageLabel, getDimensionLabel } from '@/types/resume-optimize'

const props = defineProps<{
  visible: boolean
  state: OptimizeState
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'cancel': []
  'background': []
  'toggleExpand': [stage: OptimizeStage]
}>()

// 头部图标类
const headerIconClass = computed(() => ({
  'optimizing': props.state.isOptimizing,
  'completed': props.state.isCompleted,
  'error': props.state.hasError
}))

// 头部标题
const headerTitle = computed(() => {
  if (props.state.isOptimizing) return 'AI 简历优化中...'
  if (props.state.isCompleted) return '优化完成'
  if (props.state.hasError) return '优化失败'
  return '简历优化'
})

// 排序后的阶段历史
const sortedStageHistory = computed(() => {
  const order: OptimizeStage[] = [
    'parse_resume',
    'diagnose_quick',
    'generate_suggestions',
    'optimize_section',
    'save_version'
  ]

  return order.map(stage => {
    const historyItem = props.state.stageHistory.find(h => h.stage === stage)
    return historyItem || {
      stage,
      message: '',
      timestamp: 0,
      completed: false,
      data: null,
      expanded: false
    }
  })
})

function handleOverlayClick() {
  if (!props.state.isOptimizing) {
    handleClose()
  }
}

function handleClose() {
  emit('update:visible', false)
}

function handleCancel() {
  emit('cancel')
}

function handleBackgroundRun() {
  emit('background')
  // 后台运行：关闭弹窗但不断开连接
  emit('update:visible', false)
}

function toggleExpand(stage: OptimizeStage) {
  emit('toggleExpand', stage)
}
</script>

<style lang="scss" scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-container {
  width: 520px;
  max-height: 80vh;
  background: $color-bg-secondary;
  border-radius: $radius-xl;
  border: 1px solid rgba(255, 255, 255, 0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

// 头部
.modal-header {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-lg;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.header-icon {
  width: 40px;
  height: 40px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  
  &.optimizing {
    background: rgba(212, 168, 83, 0.15);
    color: $color-accent;
  }
  
  &.completed {
    background: rgba(52, 211, 153, 0.15);
    color: $color-success;
  }
  
  &.error {
    background: rgba(248, 113, 113, 0.15);
    color: $color-error;
  }
}

.header-title {
  flex: 1;
  font-size: $text-lg;
  font-weight: $weight-semibold;
  color: $color-text-primary;
}

.close-btn {
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

// 目标岗位
.target-info {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-lg;
  background: rgba(255, 255, 255, 0.02);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.target-label {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.target-value {
  font-size: $text-sm;
  color: $color-accent;
  font-weight: $weight-medium;
}

// 进度条
.progress-section {
  padding: $spacing-lg;
}

.progress-bar {
  height: 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: $radius-full;
  overflow: hidden;
  margin-bottom: $spacing-sm;
}

.progress-fill {
  height: 100%;
  background: $gradient-gold;
  border-radius: $radius-full;
  transition: width 0.3s ease;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-text {
  font-size: $text-sm;
  color: $color-text-secondary;
}

.progress-percent {
  font-size: $text-sm;
  font-weight: $weight-semibold;
  color: $color-accent;
}

// 阶段列表
.stages-section {
  flex: 1;
  overflow-y: auto;
  padding: 0 $spacing-lg $spacing-lg;
}

.stage-item {
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  margin-bottom: $spacing-sm;
  border: 1px solid transparent;
  transition: all $transition-fast;
  
  &.active {
    border-color: rgba(212, 168, 83, 0.3);
    background: rgba(212, 168, 83, 0.05);
  }
  
  &.completed {
    border-color: rgba(52, 211, 153, 0.2);
  }
}

.stage-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stage-left {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.stage-indicator {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $color-success;
  
  .spinner {
    width: 16px;
    height: 16px;
    border: 2px solid rgba(212, 168, 83, 0.3);
    border-top-color: $color-accent;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }
  
  .dot {
    width: 8px;
    height: 8px;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 50%;
  }
}

.stage-label {
  font-size: $text-sm;
  color: $color-text-secondary;
}

.expand-btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $text-xs;
  color: $color-text-tertiary;
  transition: color $transition-fast;
  
  svg {
    transition: transform $transition-fast;
    
    &.rotated {
      transform: rotate(180deg);
    }
  }
  
  &:hover {
    color: $color-accent;
  }
}

// 展开数据区域
.stage-data {
  margin-top: $spacing-md;
  padding-top: $spacing-md;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.data-content {
  font-size: $text-sm;
}

.data-row {
  display: flex;
  justify-content: space-between;
  padding: $spacing-xs 0;
}

.data-label {
  color: $color-text-tertiary;
}

.data-value {
  color: $color-text-primary;
  font-weight: $weight-medium;
}

.modules-list {
  margin-top: $spacing-sm;
}

.modules-title {
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-bottom: $spacing-xs;
}

.module-tag {
  display: inline-block;
  padding: 2px 8px;
  margin: 2px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: $radius-sm;
  font-size: $text-xs;
  color: $color-text-secondary;
}

// 诊断数据样式
.score-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: $spacing-md;
}

.score-ring {
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
    background: $color-bg-secondary;
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

.score-label {
  margin-top: $spacing-xs;
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.dimensions-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-xs;
  margin-bottom: $spacing-md;
}

.dimension-item {
  display: flex;
  justify-content: space-between;
  padding: $spacing-xs $spacing-sm;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-sm;
}

.dim-name {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.dim-score {
  font-size: $text-xs;
  font-weight: $weight-semibold;
  color: $color-accent;
}

.issues-section {
  margin-bottom: $spacing-sm;
}

.issues-title,
.highlights-title {
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-bottom: $spacing-xs;
}

.issue-item {
  display: flex;
  align-items: flex-start;
  gap: $spacing-xs;
  padding: $spacing-xs;
  font-size: $text-xs;
  color: $color-text-secondary;
  
  &.high {
    color: $color-error;
  }
  &.medium {
    color: $color-warning;
  }
}

.issue-severity {
  flex-shrink: 0;
}

.highlight-tag {
  display: inline-block;
  padding: 2px 8px;
  margin: 2px;
  background: rgba(52, 211, 153, 0.1);
  color: $color-success;
  border-radius: $radius-sm;
  font-size: $text-xs;
}

// 建议数据样式
.suggestions-summary {
  display: flex;
  justify-content: space-between;
  margin-bottom: $spacing-sm;
}

.suggestions-count {
  font-size: $text-sm;
  color: $color-text-primary;
  font-weight: $weight-medium;
}

.suggestions-improvement {
  font-size: $text-xs;
  color: $color-success;
}

.suggestion-item {
  padding: $spacing-sm;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-sm;
  margin-bottom: $spacing-xs;
}

.sug-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: 4px;
}

.sug-priority {
  padding: 2px 6px;
  border-radius: $radius-sm;
  font-size: $text-xs;
  text-transform: uppercase;
  
  &.high {
    background: rgba(248, 113, 113, 0.15);
    color: $color-error;
  }
  &.medium {
    background: rgba(251, 191, 36, 0.15);
    color: $color-warning;
  }
  &.low {
    background: rgba(96, 165, 250, 0.15);
    color: $color-info;
  }
}

.sug-title {
  font-size: $text-sm;
  color: $color-text-primary;
}

.sug-section {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

// 变更数据样式
.changes-summary {
  display: flex;
  justify-content: space-between;
  margin-bottom: $spacing-sm;
}

.changes-count {
  font-size: $text-sm;
  color: $color-text-primary;
  font-weight: $weight-medium;
}

.changes-improvement {
  font-size: $text-xs;
  color: $color-success;
}

.confidence-badge {
  display: inline-block;
  padding: 2px 8px;
  margin-bottom: $spacing-sm;
  border-radius: $radius-sm;
  font-size: $text-xs;
  
  &.high {
    background: rgba(52, 211, 153, 0.15);
    color: $color-success;
  }
  &.medium {
    background: rgba(251, 191, 36, 0.15);
    color: $color-warning;
  }
  &.low {
    background: rgba(248, 113, 113, 0.15);
    color: $color-error;
  }
}

.change-item {
  padding: $spacing-sm;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-sm;
  margin-bottom: $spacing-xs;
}

.change-header {
  display: flex;
  gap: $spacing-sm;
  margin-bottom: 4px;
}

.change-section {
  font-size: $text-sm;
  color: $color-text-primary;
  font-weight: $weight-medium;
}

.change-field {
  font-size: $text-xs;
  color: $color-accent;
}

.change-reason {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

// 版本数据样式
.version-info {
  text-align: center;
}

.version-name {
  font-size: $text-sm;
  color: $color-text-secondary;
  margin-bottom: $spacing-sm;
}

.score-comparison {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-md;
  margin-bottom: $spacing-sm;
  
  svg {
    color: $color-success;
  }
}

.old-score {
  font-size: $text-xl;
  color: $color-text-tertiary;
  text-decoration: line-through;
}

.new-score {
  font-size: $text-3xl;
  font-weight: $weight-bold;
  color: $color-success;
}

.improvement-badge {
  display: inline-block;
  padding: 4px 12px;
  background: rgba(52, 211, 153, 0.15);
  color: $color-success;
  border-radius: $radius-full;
  font-size: $text-sm;
  font-weight: $weight-medium;
}

// JSON 显示
.json-display {
  font-size: $text-xs;
  color: $color-text-tertiary;
  background: rgba(0, 0, 0, 0.2);
  padding: $spacing-sm;
  border-radius: $radius-sm;
  overflow-x: auto;
  max-height: 200px;
}

// 底部
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: $spacing-md;
  padding: $spacing-lg;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.footer-btn {
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
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(212, 168, 83, 0.3);
    }
  }
  
  &.secondary {
    background: rgba(255, 255, 255, 0.05);
    color: $color-text-secondary;
    
    &:hover {
      background: rgba(255, 255, 255, 0.1);
      color: $color-text-primary;
    }
  }
  
  &.danger {
    background: rgba(248, 113, 113, 0.1);
    color: $color-error;
    
    &:hover {
      background: rgba(248, 113, 113, 0.2);
    }
  }
}

// 动画
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
  
  .modal-container {
    transition: transform 0.3s ease;
  }
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
  
  .modal-container {
    transform: scale(0.95);
  }
}

.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
}

.expand-enter-to,
.expand-leave-from {
  max-height: 500px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
