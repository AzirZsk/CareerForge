<!--=====================================================
  CareerForge 诊断阶段内容组件
  @author Azir
=====================================================-->

<template>
  <div class="data-content">
    <!-- 评分环 -->
    <div class="score-display">
      <div
        class="score-ring"
        :style="{ '--score': data.overallScore }"
      >
        <span>{{ data.overallScore }}</span>
      </div>
      <span class="score-label">综合评分</span>
    </div>

    <!-- 维度网格 -->
    <div
      v-if="data.dimensionScores"
      class="dimensions-grid"
    >
      <div
        v-for="(dim, key) in data.dimensionScores"
        :key="key"
        class="dimension-item"
      >
        <span class="dim-name">{{ getDimensionLabel(String(key)) }}</span>
        <span class="dim-score">{{ dim }}</span>
      </div>
    </div>

    <!-- 待改进项 -->
    <div
      v-if="data.weaknesses?.length"
      class="issues-section"
    >
      <div class="issues-title">
        待改进 ({{ data.weaknesses.length }})
      </div>
      <div
        v-for="(weakness, idx) in data.weaknesses"
        :key="idx"
        class="issue-item"
        :class="getWeaknessSeverity(weakness)"
      >
        <span class="issue-severity">
          <font-awesome-icon :icon="getSeverityIcon(getWeaknessSeverity(weakness))" />
        </span>
        {{ getWeaknessContent(weakness) }}
      </div>
    </div>

    <!-- 亮点 -->
    <div
      v-if="data.strengths?.length"
      class="highlights-section"
    >
      <div class="highlights-title">
        亮点
      </div>
      <div
        v-for="(h, idx) in data.strengths"
        :key="idx"
        class="highlight-tag"
      >
        {{ h }}
      </div>
    </div>

    <!-- 快速改进建议（仅精准诊断阶段展示） -->
    <div
      v-if="isPrecise && data.quickWins?.length"
      class="quickwins-section"
    >
      <div class="quickwins-title">
        快速改进建议
      </div>
      <div
        v-for="(quickWin, idx) in data.quickWins"
        :key="idx"
        class="quickwin-item"
      >
        ✓ {{ quickWin }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { DiagnoseData } from '@/types/resume-optimize'
import { getDimensionLabel } from '@/types/resume-optimize'
import { getSeverityIcon, getWeaknessContent, getWeaknessSeverity } from '@/utils/stageHelpers'

const props = defineProps<{
  data: DiagnoseData
  stage: 'diagnose_quick' | 'diagnose_precise'
}>()

const isPrecise = props.stage === 'diagnose_precise'
</script>

<style lang="scss" scoped>
.data-content {
  font-size: $text-sm;
}

// 评分展示
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

// 维度网格
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

// 待改进项
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

// 亮点标签
.highlight-tag {
  display: inline-block;
  padding: 2px 8px;
  margin: 2px;
  background: rgba(52, 211, 153, 0.1);
  color: $color-success;
  border-radius: $radius-sm;
  font-size: $text-xs;
}

// 快速改进建议
.quickwins-section {
  margin-top: $spacing-sm;
}

.quickwins-title {
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-bottom: $spacing-xs;
}

.quickwin-item {
  display: flex;
  align-items: flex-start;
  gap: $spacing-xs;
  padding: $spacing-xs;
  font-size: $text-xs;
  color: $color-text-secondary;
  background: rgba(251, 191, 36, 0.05);
  border-radius: $radius-sm;
  margin-bottom: 4px;
}
</style>
