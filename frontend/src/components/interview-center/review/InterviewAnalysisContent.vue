<!--=====================================================
  面试分析内容展示组件
  @author Azir
=====================================================-->

<template>
  <div class="interview-analysis">
    <!-- 总体评分 -->
    <div class="overall-score-section">
      <div class="score-circle" :class="getScoreClass(data.overallScore)">
        <span class="score-value">{{ data.overallScore }}</span>
        <span class="score-label">综合评分</span>
      </div>
      <div class="overall-assessment">
        <p>{{ data.overallAssessment }}</p>
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

    <!-- 维度评分 -->
    <div v-if="data.dimensionScores && data.dimensionScores.length > 0" class="dimension-scores">
      <h4>能力维度分析</h4>
      <div class="dimension-list">
        <div v-for="(dim, index) in data.dimensionScores" :key="index" class="dimension-item">
          <div class="dimension-header">
            <span class="dimension-name">{{ dim.dimension }}</span>
            <span class="dimension-score" :class="getScoreClass(dim.score)">
              {{ dim.score }}/5
            </span>
          </div>
          <p class="dimension-reason">{{ dim.reason }}</p>
        </div>
      </div>
    </div>

    <!-- 关键发现 -->
    <div v-if="data.keyFindings" class="key-findings">
      <h4>关键发现</h4>
      <p>{{ data.keyFindings }}</p>
    </div>

    <!-- 建议 -->
    <div v-if="data.recommendations && data.recommendations.length > 0" class="recommendations">
      <h4>综合建议</h4>
      <ul>
        <li v-for="(item, index) in data.recommendations" :key="index">{{ item }}</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { InterviewAnalysisResult } from '@/types/interview-center'

defineProps<{
  data: InterviewAnalysisResult
}>()

// 获取评分样式类名
function getScoreClass(score: number | undefined): string {
  if (score === undefined) return ''
  if (score >= 4) return 'high'
  if (score >= 3) return 'medium'
  return 'low'
}
</script>

<style lang="scss" scoped>
.interview-analysis{
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

// 总体评分区域
.overall-score-section {
  display: flex;
  align-items: center;
  gap: $spacing-lg;
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.03);
  border-radius: $radius-md;
}

.score-circle {
  width: 70px;
  height: 70px;
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

.overall-assessment {
  flex: 1;

  p {
    font-size: $text-sm;
    color: $color-text-secondary;
    line-height: 1.6;
    margin: 0;
  }
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
  }

  li {
    font-size: $text-sm;
    color: $color-text-secondary;
    line-height: 1.6;
    margin-bottom: $spacing-xs;
  }

  &.strengths {
    h4 {
      color: $color-success;
    }
  }

  &.weaknesses {
    h4 {
      color: $color-error;
    }
  }
}

// 维度评分
.dimension-scores {
  h4 {
    font-size: $text-sm;
    font-weight: $weight-medium;
    color: $color-text-tertiary;
    margin: 0 0 $spacing-sm 0;
  }
}

.dimension-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.dimension-item {
  padding: $spacing-sm;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-sm;
}

.dimension-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xs;
}

.dimension-name {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.dimension-score {
  font-size: $text-sm;
  font-weight: $weight-semibold;

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

.dimension-reason {
  font-size: $text-xs;
  color: $color-text-tertiary;
  line-height: 1.5;
  margin: 0;
}

// 关键发现
.key-findings {
  padding: $spacing-md;
  background: rgba($color-accent, 0.08);
  border-radius: $radius-md;

  h4 {
    font-size: $text-sm;
    font-weight: $weight-medium;
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

// 建议
.recommendations {
  padding: $spacing-md;
  background: rgba($color-info, 0.08);
  border-radius: $radius-md;

  h4 {
    font-size: $text-sm;
    font-weight: $weight-medium;
    color: $color-info;
    margin: 0 0 $spacing-sm 0;
  }

  ul {
    margin: 0;
    padding-left: $spacing-md;
  }

  li {
    font-size: $text-sm;
    color: $color-text-secondary;
    line-height: 1.6;
    margin-bottom: $spacing-xs;
  }
}
</style>
