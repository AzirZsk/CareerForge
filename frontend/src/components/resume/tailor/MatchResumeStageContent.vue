<!--=====================================================
  LandIt 匹配分析阶段内容组件
  @author Azir
=====================================================-->

<template>
  <div class="data-content">
    <!-- 匹配分数展示 -->
    <div class="match-score-display">
      <div class="match-ring" :style="{ '--score': data.matchScore }">
        <span>{{ data.matchScore }}</span>
      </div>
      <span class="match-label">匹配度</span>
    </div>

    <!-- 匹配详情 -->
    <div class="match-details">
      <!-- 已匹配技能 -->
      <div class="match-section" v-if="data.matchedSkills?.length">
        <div class="match-title success">已匹配技能</div>
        <div class="match-tags">
          <span v-for="skill in data.matchedSkills" :key="skill" class="match-tag success">{{ skill }}</span>
        </div>
      </div>

      <!-- 缺失技能 -->
      <div class="match-section" v-if="data.missingSkills?.length">
        <div class="match-title warning">缺失技能</div>
        <div class="match-tags">
          <span v-for="skill in data.missingSkills" :key="skill" class="match-tag warning">{{ skill }}</span>
        </div>
      </div>

      <!-- 相关经验 -->
      <div class="match-section" v-if="data.relevantExperiences?.length">
        <div class="match-title info">相关经验</div>
        <ul class="match-list">
          <li v-for="(exp, idx) in data.relevantExperiences" :key="idx">{{ exp }}</li>
        </ul>
      </div>

      <!-- 优化建议 -->
      <div class="match-section" v-if="data.adjustmentSuggestions?.length">
        <div class="match-title accent">优化建议</div>
        <ul class="match-list">
          <li v-for="(suggestion, idx) in data.adjustmentSuggestions" :key="idx">{{ suggestion }}</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { MatchAnalysis } from '@/types/resume-tailor'

defineProps<{
  data: MatchAnalysis
}>()
</script>

<style lang="scss" scoped>
.data-content {
  font-size: $text-sm;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-sm;
  padding: $spacing-md;
}

.match-score-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
}

.match-ring {
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
    background: $color-bg-tertiary;
    border-radius: 50%;
  }

  span {
    position: relative;
    z-index: 1;
    font-family: $font-display;
    font-size: $text-2xl;
    font-weight: $weight-semibold;
    color: $color-accent;
  }
}

.match-label {
  font-size: $text-sm;
  color: $color-text-secondary;
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
