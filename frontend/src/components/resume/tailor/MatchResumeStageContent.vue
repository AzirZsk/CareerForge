<!--=====================================================
  LandIt 匹配分析阶段内容组件
  @author Azir
=====================================================-->

<template>
  <div class="data-content">
    <!-- 匹配度状态栏 -->
    <div class="match-header">
      <span class="match-title">匹配分析</span>
      <div
        class="match-badge"
        :class="matchLevel"
      >
        <span class="match-dot" />
        <span class="match-percent">{{ data.matchScore }}%</span>
        <span class="match-level-text">{{ matchLevelText }}</span>
      </div>
    </div>

    <!-- 匹配详情 -->
    <div class="match-details">
      <!-- 已匹配技能 -->
      <div
        v-if="data.matchedSkills?.length"
        class="match-section"
      >
        <div class="match-title success">
          已匹配技能
        </div>
        <div class="match-tags">
          <span
            v-for="skill in data.matchedSkills"
            :key="skill"
            class="match-tag success"
          >{{ skill }}</span>
        </div>
      </div>

      <!-- 缺失技能 -->
      <div
        v-if="data.missingSkills?.length"
        class="match-section"
      >
        <div class="match-title warning">
          缺失技能
        </div>
        <div class="match-tags">
          <span
            v-for="skill in data.missingSkills"
            :key="skill"
            class="match-tag warning"
          >{{ skill }}</span>
        </div>
      </div>

      <!-- 相关经验 -->
      <div
        v-if="data.relevantExperiences?.length"
        class="match-section"
      >
        <div class="match-title info">
          相关经验
        </div>
        <ul class="match-list">
          <li
            v-for="(exp, idx) in data.relevantExperiences"
            :key="idx"
          >
            {{ exp }}
          </li>
        </ul>
      </div>

      <!-- 优化建议 -->
      <div
        v-if="data.adjustmentSuggestions?.length"
        class="match-section"
      >
        <div class="match-title accent">
          优化建议
        </div>
        <ul class="match-list">
          <li
            v-for="(suggestion, idx) in data.adjustmentSuggestions"
            :key="idx"
          >
            {{ suggestion }}
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { MatchAnalysis } from '@/types/resume-tailor'

const props = defineProps<{
  data: MatchAnalysis
}>()

// 根据分数计算匹配等级
const matchLevel = computed(() => {
  const score = props.data.matchScore
  if (score >= 90) return 'excellent'
  if (score >= 70) return 'good'
  if (score >= 50) return 'fair'
  return 'poor'
})

// 匹配等级文字
const matchLevelText = computed(() => {
  const levelMap = {
    excellent: '优秀',
    good: '良好',
    fair: '一般',
    poor: '较差'
  }
  return levelMap[matchLevel.value]
})
</script>

<style lang="scss" scoped>
.data-content {
  font-size: $text-sm;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-sm;
  padding: $spacing-md;
}

// 匹配度状态栏
.match-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-md;
  padding-bottom: $spacing-sm;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.match-title {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.match-badge {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: 4px 10px;
  border-radius: $radius-full;
  font-size: $text-xs;

  // 优秀 - 绿色
  &.excellent {
    background: rgba(52, 211, 153, 0.15);
    color: $color-success;
  }

  // 良好 - 琥珀金
  &.good {
    background: rgba(212, 168, 83, 0.15);
    color: $color-accent;
  }

  // 一般 - 黄色
  &.fair {
    background: rgba(251, 191, 36, 0.15);
    color: $color-warning;
  }

  // 较差 - 红色
  &.poor {
    background: rgba(248, 113, 113, 0.15);
    color: $color-error;
  }
}

.match-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.match-percent {
  font-weight: $weight-semibold;
}

.match-level-text {
  opacity: 0.9;
}

.match-details {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.match-section {
  .match-title {
    font-size: $text-xs;
    margin-bottom: $spacing-sm;
    text-transform: uppercase;
    letter-spacing: 0.5px;

    &.success {
      color: $color-success;
    }

    &.warning {
      color: $color-warning;
    }

    &.info {
      color: $color-info;
    }

    &.accent {
      color: $color-accent;
    }
  }

  .match-list {
    list-style: none;
    padding: 0;
    margin: 0;

    li {
      position: relative;
      padding-left: $spacing-md;
      font-size: $text-sm;
      color: $color-text-secondary;
      margin-bottom: $spacing-xs;
      line-height: 1.5;

      &::before {
        content: '•';
        position: absolute;
        left: 0;
      }
    }
  }

  &:has(.match-title.info) .match-list li::before {
    color: $color-info;
  }

  &:has(.match-title.accent) .match-list li::before {
    color: $color-accent;
  }

  .match-tags {
    display: flex;
    flex-wrap: wrap;
    gap: $spacing-xs;
  }

  .match-tag {
    padding: 2px 8px;
    font-size: $text-xs;
    border-radius: $radius-sm;

    &.success {
      background: rgba(52, 211, 153, 0.15);
      color: $color-success;
    }

    &.warning {
      background: rgba(251, 191, 36, 0.15);
      color: $color-warning;
    }
  }
}
</style>
