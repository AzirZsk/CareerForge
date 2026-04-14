<!--=====================================================
  对话分析内容展示组件
  @author Azir
=====================================================-->

<template>
  <div class="transcript-analysis">
    <!-- 整体指标卡片 -->
    <div class="metrics-cards">
      <div class="metric-card">
        <span class="metric-label">整体清晰度</span>
        <div class="metric-value" :class="getClarityClass(data.overallClarity)">
          {{ data.overallClarity != null ? `${data.overallClarity.toFixed(1)}/5` : '-' }}
        </div>
      </div>
      <div class="metric-card">
        <span class="metric-label">简历匹配度</span>
        <div class="metric-value" :class="getResumeMatchClass(data.overallResumeMatch)">
          {{ data.overallResumeMatch != null ? `${data.overallResumeMatch.toFixed(1)}/5` : '-' }}
        </div>
      </div>
      <div class="metric-card">
        <span class="metric-label">JD 匹配度</span>
        <div class="metric-value">{{ data.jdMatchScore != null ? `${data.jdMatchScore}/5` : '-' }}</div>
      </div>
    </div>

    <!-- Q&A 对分析列表 -->
    <div v-if="data.qaPairs && data.qaPairs.length > 0" class="qa-list">
      <h4>问答对分析</h4>
      <div v-for="(qa, index) in data.qaPairs" :key="index" class="qa-item">
        <div class="qa-header">
          <span class="qa-question">Q{{ index + 1 }}</span>
          <span v-if="qa.assessmentTarget" class="qa-tag">
            {{ qa.assessmentTarget }}
          </span>
          <span v-if="qa.jdRelevance" class="qa-tag jd">
            {{ qa.jdRelevance }}
          </span>
        </div>
        <div class="qa-body">
          <div class="qa-row">
            <span class="qa-label">面试官问题:</span>
            <div class="qa-content">{{ qa.question }}</div>
          </div>
          <div class="qa-row">
            <span class="qa-label">问题意图:</span>
            <span class="qa-value">{{ qa.questionIntent }}</span>
          </div>
          <div class="qa-row">
            <span class="qa-label">考察能力:</span>
            <span class="qa-value">{{ qa.assessmentTarget }}</span>
          </div>
          <div class="qa-row">
            <span class="qa-label">JD 关联:</span>
            <span class="qa-value">{{ qa.jdRelevance || '-' }}</span>
          </div>
          <div class="qa-row">
            <span class="qa-label">候选人回答:</span>
            <div class="qa-content">{{ qa.answer }}</div>
          </div>
          <div class="qa-row">
            <span class="qa-label">清晰度:</span>
            <span class="qa-value">
              <span class="score-badge" :class="getScoreClass(qa.clarityScore)">
                {{ qa.clarityScore }}/5
              </span>
            </span>
          </div>
          <div class="qa-row">
            <span class="qa-label">清晰度理由:</span>
            <span class="qa-value">{{ qa.clarityReason }}</span>
          </div>
          <div class="qa-row">
            <span class="qa-label">简历匹配:</span>
            <span class="qa-value">
              <span class="score-badge" :class="getResumeMatchClass(qa.resumeMatchScore)">
                {{ qa.resumeMatchScore }}/5
              </span>
            </span>
          </div>
          <div class="qa-row">
            <span class="qa-label">匹配理由:</span>
            <span class="qa-value">{{ qa.resumeMatchReason }}</span>
          </div>
          <div class="qa-row">
            <span class="qa-label">改进建议:</span>
            <span class="qa-value suggestion">{{ qa.improvementSuggestion }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 整体总结 -->
    <div v-if="data.summary" class="summary-section">
      <h4>整体对话质量总结</h4>
      <p>{{ data.summary }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { TranscriptAnalysisResult } from '@/types/interview-center'

defineProps<{
  data: TranscriptAnalysisResult
}>()

// 获取清晰度评分样式
function getClarityClass(score: number | undefined): string {
  if (!score) return ''
  if (score >= 4) return 'high'
  if (score >= 3) return 'medium'
  if (score >= 2) return 'low'
  return ''
}

// 获取简历匹配度评分样式
function getResumeMatchClass(score: number | undefined): string {
  if (!score) return ''
  if (score >= 4) return 'high'
  if (score >= 3) return 'medium'
  if (score >= 2) return 'low'
  return ''
}

// 获取通用评分样式
function getScoreClass(score: number | undefined): string {
  if (!score) return ''
  if (score >= 4) return 'high'
  if (score >= 3) return 'medium'
  return 'low'
}
</script>

<style lang="scss" scoped>
.transcript-analysis {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.metrics-cards{
  display: flex;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
}

.metric-card {
  flex: 1;
  padding: $spacing-sm $spacing-md;
  background: rgba(255, 255, 255, 0.03);
  border-radius: $radius-md;
  text-align: center;
}

.metric-label {
  font-size: $text-xs;
  color: $color-text-tertiary;
  display: block;
  margin-bottom: $spacing-xs;
}

.metric-value {
  font-size: 1.25rem;
  font-weight: $weight-semibold;
  color: $color-text-primary;

  &.high {
    color: $color-success;
  }
  &.medium {
    color: $color-warning;
  }
  &.low {
    color: $color-error;
  }
}

.qa-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;

  h4 {
    font-size: $text-sm;
    color: $color-text-tertiary;
    margin-bottom: $spacing-sm;
    font-weight: $weight-medium;
  }
}

.qa-item {
  padding: $spacing-sm;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  transition: background $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.04);
  }
}

.qa-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding-bottom: $spacing-sm;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  margin-bottom: $spacing-sm;
}

.qa-question {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-primary;
  flex: 1;
}

.qa-tag {
  font-size: 0.625rem;
  padding: 2px 8px;
  background: $color-bg-tertiary;
  border-radius: $radius-sm;
  color: $color-text-tertiary;
  flex-shrink: 0;

  &.jd {
    background: rgba($color-accent, 0.15);
    color: $color-accent;
  }
}

.qa-body {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.qa-row {
  display: flex;
  gap: $spacing-sm;
  font-size: $text-xs;

  .qa-label {
    color: $color-text-tertiary;
    min-width: 80px;
    flex-shrink: 0;
    line-height: 1.4;
  }

  .qa-value {
    flex: 1;
    color: $color-text-secondary;
    font-size: $text-sm;
    line-height: 1.4;
  }

  &:has(.qa-content) {
    align-items: flex-start;

    .qa-label {
      padding-top: $spacing-xs;
    }
  }
}

.qa-content {
  font-size: $text-sm;
  color: $color-text-secondary;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  padding: $spacing-xs;
  background: rgba(0, 0, 0, 0.2);
  border-radius: $radius-sm;
}

.score-badge {
  font-size: 0.625rem;
  padding: 2px 8px;
  border-radius: $radius-sm;
  font-weight: $weight-medium;

  &.high {
    background: rgba($color-success, 0.15);
    color: $color-success;
  }
  &.medium {
    background: rgba($color-warning, 0.15);
    color: $color-warning;
  }
  &.low {
    background: rgba($color-error, 0.15);
    color: $color-error;
  }
}

.suggestion {
  font-size: $text-xs;
  color: $color-accent;
  font-style: italic;
}

.summary-section {
  margin-top: $spacing-md;
  padding: $spacing-md;
  background: rgba($color-accent, 0.08);
  border-radius: $radius-md;

  h4 {
    font-size: $text-sm;
    color: $color-accent;
    margin: 0 0 $spacing-sm 0;
  }

  p {
    font-size: $text-sm;
    color: $color-text-secondary;
    line-height: 1.6;
    margin: 0;
  }
}
</style>
