<!--=====================================================
  面试分析内容展示组件（适配后端 InterviewAnalysisResult DTO）
  @author Azir
=====================================================-->

<template>
  <div class="interview-analysis">
    <!-- 总体评分 + JD 匹配度 -->
    <div class="score-overview">
      <div class="score-circle" :class="getScoreClass(data.overallScore)">
        <span class="score-value">{{ data.overallScore }}</span>
        <span class="score-label">综合评分</span>
      </div>
      <div class="score-details">
        <div class="performance-tag" :class="getPerformanceClass(data.overallPerformance)">
          {{ data.overallPerformance }}
        </div>
        <div v-if="data.jdMatchScore" class="jd-match">
          <span class="jd-label">JD 匹配度</span>
          <span class="jd-score" :class="getJdMatchClass(data.jdMatchScore)">{{ data.jdMatchScore }}/10</span>
        </div>
        <p v-if="data.summary" class="summary-text">{{ data.summary }}</p>
      </div>
    </div>

    <!-- 优劣势分析 -->
    <div class="strength-weakness">
      <div v-if="data.strengths && data.strengths.length > 0" class="analysis-block strengths">
        <h4>
          <font-awesome-icon icon="fa-solid fa-thumbs-up" />
          表现亮点
        </h4>
        <ul>
          <li v-for="(item, index) in data.strengths" :key="index">{{ item }}</li>
        </ul>
      </div>
      <div v-if="data.weaknesses && data.weaknesses.length > 0" class="analysis-block weaknesses">
        <h4>
          <font-awesome-icon icon="fa-solid fa-triangle-exclamation" />
          待改进项
        </h4>
        <ul>
          <li v-for="(item, index) in data.weaknesses" :key="index">{{ item }}</li>
        </ul>
      </div>
    </div>

    <!-- 技能差距分析 -->
    <div v-if="data.skillGaps && data.skillGaps.length > 0" class="skill-gaps">
      <h4>
        <font-awesome-icon icon="fa-solid fa-chart-line" />
        技能差距分析
      </h4>
      <div class="gap-list">
        <div v-for="(gap, index) in data.skillGaps" :key="index" class="gap-item">
          <div class="gap-header">
            <span class="gap-skill">{{ gap.skill }}</span>
            <span class="gap-level" :class="gap.currentLevel">{{ gap.currentLevel }}</span>
          </div>
          <p v-if="gap.jdRequirement" class="gap-requirement">JD 要求：{{ gap.jdRequirement }}</p>
          <p v-if="gap.gapDescription" class="gap-desc">{{ gap.gapDescription }}</p>
        </div>
      </div>
    </div>

    <!-- JD 匹配详情 -->
    <div v-if="data.jdMatchDetails && data.jdMatchDetails.length > 0" class="jd-details">
      <h4>
        <font-awesome-icon icon="fa-solid fa-bullseye" />
        JD 匹配详情
      </h4>
      <div class="jd-detail-list">
        <div v-for="(detail, index) in data.jdMatchDetails" :key="index" class="jd-detail-item">
          <div class="jd-detail-header">
            <span class="jd-skill-name">{{ detail.skill }}</span>
            <div class="jd-detail-badges">
              <span v-if="detail.required" class="badge required">必备</span>
              <span class="badge match" :class="detail.matchLevel">{{ getMatchLevelLabel(detail.matchLevel) }}</span>
            </div>
          </div>
          <p v-if="detail.evidence" class="jd-evidence">{{ detail.evidence }}</p>
        </div>
      </div>
    </div>

    <!-- 简历一致性 -->
    <div v-if="data.resumeConsistency && data.resumeConsistency.findings && data.resumeConsistency.findings.length > 0" class="resume-consistency">
      <h4>
        <font-awesome-icon icon="fa-solid fa-file-lines" />
        简历一致性
      </h4>
      <ul>
        <li v-for="(finding, index) in data.resumeConsistency.findings" :key="index">{{ finding }}</li>
      </ul>
    </div>

    <!-- 轮次分析 -->
    <div v-if="data.roundAnalysis && data.roundAnalysis.performance" class="round-analysis">
      <h4>
        <font-awesome-icon icon="fa-solid fa-comments" />
        轮次评价
      </h4>
      <p>{{ data.roundAnalysis.performance }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { InterviewAnalysisResult } from '@/types/interview-center'

defineProps<{
  data: InterviewAnalysisResult
}>()

// 综合评分样式（0-100）
function getScoreClass(score: number | undefined): string {
  if (score === undefined) return ''
  if (score >= 80) return 'high'
  if (score >= 60) return 'medium'
  return 'low'
}

// 表现等级标签样式
function getPerformanceClass(performance: string | undefined): string {
  if (!performance) return ''
  if (performance.includes('优秀')) return 'excellent'
  if (performance.includes('良好')) return 'good'
  if (performance.includes('一般')) return 'average'
  return 'needs-improvement'
}

// JD 匹配度样式（1-10）
function getJdMatchClass(score: number | undefined): string {
  if (score === undefined) return ''
  if (score >= 7) return 'high'
  if (score >= 4) return 'medium'
  return 'low'
}

// 匹配级别标签
function getMatchLevelLabel(level: string): string {
  const map: Record<string, string> = {
    strong: '强匹配',
    medium: '中等',
    weak: '弱匹配',
    missing: '缺失'
  }
  return map[level] || level
}
</script>

<style lang="scss" scoped>
.interview-analysis {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

// 评分总览
.score-overview {
  display: flex;
  align-items: flex-start;
  gap: $spacing-lg;
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.03);
  border-radius: $radius-md;
}

.score-circle {
  width: 70px;
  height: 70px;
  min-width: 70px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba($color-accent, 0.15);
  border: 2px solid rgba($color-accent, 0.3);

  &.high {
    background: rgba($color-success, 0.15);
    border-color: rgba($color-success, 0.3);
  }

  &.medium {
    background: rgba($color-warning, 0.15);
    border-color: rgba($color-warning, 0.3);
  }

  &.low {
    background: rgba($color-error, 0.15);
    border-color: rgba($color-error, 0.3);
  }
}

.score-value {
  font-size: 1.5rem;
  font-weight: $weight-bold;
  color: $color-text-primary;
  line-height: 1;
}

.score-label {
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-top: $spacing-xs;
}

.score-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.performance-tag {
  display: inline-block;
  align-self: flex-start;
  padding: 2px 10px;
  border-radius: $radius-full;
  font-size: $text-xs;
  font-weight: $weight-medium;

  &.excellent {
    background: rgba($color-success, 0.15);
    color: $color-success;
  }

  &.good {
    background: rgba($color-accent, 0.15);
    color: $color-accent;
  }

  &.average {
    background: rgba($color-warning, 0.15);
    color: $color-warning;
  }

  &.needs-improvement {
    background: rgba($color-error, 0.15);
    color: $color-error;
  }
}

.jd-match {
  display: flex;
  align-items: center;
  gap: $spacing-xs;

  .jd-label {
    font-size: $text-xs;
    color: $color-text-tertiary;
  }

  .jd-score {
    font-size: $text-sm;
    font-weight: $weight-semibold;

    &.high { color: $color-success; }
    &.medium { color: $color-warning; }
    &.low { color: $color-error; }
  }
}

.summary-text {
  font-size: $text-sm;
  color: $color-text-secondary;
  line-height: 1.6;
  margin: $spacing-xs 0 0 0;
}

// 优劣势分析
.strength-weakness {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: $spacing-md;
}

.analysis-block {
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;

  h4 {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-size: $text-sm;
    font-weight: $weight-medium;
    color: $color-text-tertiary;
    margin: 0 0 $spacing-sm 0;
  }

  ul {
    margin: 0;
    padding-left: $spacing-md;
    list-style: disc;
  }

  li {
    font-size: $text-sm;
    color: $color-text-secondary;
    line-height: 1.6;
    margin-bottom: $spacing-xs;
    padding-left: $spacing-xs;
  }

  &.strengths h4 { color: $color-success; }
  &.weaknesses h4 { color: $color-error; }
}

// 技能差距
.skill-gaps {
  h4 {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-size: $text-sm;
    font-weight: $weight-medium;
    color: $color-text-tertiary;
    margin: 0 0 $spacing-sm 0;
  }
}

.gap-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.gap-item {
  padding: $spacing-sm;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-sm;
}

.gap-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xs;
}

.gap-skill {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.gap-level {
  font-size: $text-xs;
  padding: 1px 8px;
  border-radius: $radius-full;
  background: rgba($color-text-tertiary, 0.1);
  color: $color-text-tertiary;
}

.gap-requirement,
.gap-desc {
  font-size: $text-xs;
  color: $color-text-tertiary;
  line-height: 1.5;
  margin: 0;
}

.gap-requirement {
  margin-bottom: 2px;
}

// JD 匹配详情
.jd-details {
  h4 {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-size: $text-sm;
    font-weight: $weight-medium;
    color: $color-text-tertiary;
    margin: 0 0 $spacing-sm 0;
  }
}

.jd-detail-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.jd-detail-item {
  padding: $spacing-sm;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-sm;
}

.jd-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xs;
}

.jd-skill-name {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.jd-detail-badges {
  display: flex;
  gap: $spacing-xs;
}

.badge {
  font-size: $text-xs;
  padding: 1px 8px;
  border-radius: $radius-full;

  &.required {
    background: rgba($color-error, 0.15);
    color: $color-error;
  }

  &.match.strong { background: rgba($color-success, 0.15); color: $color-success; }
  &.match.medium { background: rgba($color-warning, 0.15); color: $color-warning; }
  &.match.weak { background: rgba($color-error, 0.15); color: $color-error; }
  &.match.missing { background: rgba($color-text-tertiary, 0.1); color: $color-text-tertiary; }
}

.jd-evidence {
  font-size: $text-xs;
  color: $color-text-tertiary;
  line-height: 1.5;
  margin: 0;
}

// 简历一致性
.resume-consistency {
  padding: $spacing-md;
  background: rgba($color-accent, 0.08);
  border-radius: $radius-md;

  h4 {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-size: $text-sm;
    font-weight: $weight-medium;
    color: $color-accent;
    margin: 0 0 $spacing-sm 0;
  }

  ul {
    margin: 0;
    padding-left: $spacing-md;
    list-style: disc;
  }

  li {
    font-size: $text-sm;
    color: $color-text-secondary;
    line-height: 1.6;
    margin-bottom: $spacing-xs;
    padding-left: $spacing-xs;
  }
}

// 轮次分析
.round-analysis {
  padding: $spacing-md;
  background: rgba($color-info, 0.08);
  border-radius: $radius-md;

  h4 {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-size: $text-sm;
    font-weight: $weight-medium;
    color: $color-info;
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
