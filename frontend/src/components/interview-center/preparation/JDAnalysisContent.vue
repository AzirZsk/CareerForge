<!--=====================================================
  LandIt JD 分析阶段内容组件
  @author Azir
=====================================================-->

<template>
  <div class="jd-analysis-content">
    <!-- 概览 -->
    <div v-if="data.overview" class="analysis-section">
      <h5 class="section-title">职位概览</h5>
      <p class="section-text">{{ data.overview }}</p>
    </div>

    <!-- 必备技能 -->
    <div v-if="data.requiredSkills?.length" class="analysis-section">
      <h5 class="section-title required">必备技能</h5>
      <div class="skill-tags">
        <span v-for="(skill, index) in data.requiredSkills" :key="index" class="skill-tag required">
          {{ skill }}
        </span>
      </div>
    </div>

    <!-- 加分技能 -->
    <div v-if="data.plusSkills?.length" class="analysis-section">
      <h5 class="section-title plus">加分技能</h5>
      <div class="skill-tags">
        <span v-for="(skill, index) in data.plusSkills" :key="index" class="skill-tag plus">
          {{ skill }}
        </span>
      </div>
    </div>

    <!-- 关键词 -->
    <div v-if="data.keywords?.length" class="analysis-section">
      <h5 class="section-title">关键词</h5>
      <div class="keyword-tags">
        <span v-for="(keyword, index) in data.keywords" :key="index" class="keyword-tag">
          {{ keyword }}
        </span>
      </div>
    </div>

    <!-- 职责 -->
    <div v-if="data.responsibilities?.length" class="analysis-section">
      <h5 class="section-title">主要职责</h5>
      <ul class="section-list">
        <li v-for="(item, index) in data.responsibilities" :key="index">{{ item }}</li>
      </ul>
    </div>

    <!-- 要求 -->
    <div v-if="data.requirements?.length" class="analysis-section">
      <h5 class="section-title">任职要求</h5>
      <ul class="section-list">
        <li v-for="(item, index) in data.requirements" :key="index">{{ item }}</li>
      </ul>
    </div>

    <!-- 面试重点 -->
    <div v-if="data.interviewFocus?.length" class="analysis-section highlight">
      <h5 class="section-title">面试重点</h5>
      <ul class="section-list highlight">
        <li v-for="(item, index) in data.interviewFocus" :key="index">{{ item }}</li>
      </ul>
    </div>

    <!-- 准备建议 -->
    <div v-if="data.preparationTips?.length" class="analysis-section tips">
      <h5 class="section-title">准备建议</h5>
      <ul class="section-list tips-list">
        <li v-for="(item, index) in data.preparationTips" :key="index">{{ item }}</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { JDAnalysisResult } from '@/types/interview-center'

defineProps<{
  data: JDAnalysisResult
}>()
</script>

<style lang="scss" scoped>
.jd-analysis-content {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.analysis-section {
  &:not(:last-child) {
    padding-bottom: $spacing-md;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  }
}

.section-title {
  font-size: $text-sm;
  font-weight: $weight-semibold;
  color: $color-accent;
  margin: 0 0 $spacing-sm;

  &.required {
    color: $color-error;
  }

  &.plus {
    color: $color-success;
  }
}

.section-text {
  font-size: $text-sm;
  color: $color-text-secondary;
  line-height: 1.6;
  margin: 0;
}

.section-list {
  list-style: none;
  padding: 0;
  margin: 0;

  li {
    position: relative;
    padding-left: $spacing-md;
    font-size: $text-sm;
    color: $color-text-secondary;
    line-height: 1.6;
    margin-bottom: $spacing-xs;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0.6em;
      width: 4px;
      height: 4px;
      background: $color-text-tertiary;
      border-radius: 50%;
    }
  }

  &.highlight li::before {
    background: $color-accent;
  }
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
}

.skill-tag {
  font-size: $text-xs;
  padding: 4px 8px;
  border-radius: $radius-sm;

  &.required {
    background: rgba($color-error, 0.15);
    color: $color-error;
  }

  &.plus {
    background: rgba($color-success, 0.15);
    color: $color-success;
  }
}

.keyword-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
}

.keyword-tag {
  font-size: $text-xs;
  padding: 4px 8px;
  background: rgba(255, 255, 255, 0.05);
  color: $color-text-tertiary;
  border-radius: $radius-sm;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.highlight {
  background: rgba(212, 168, 83, 0.05);
  padding: $spacing-sm;
  border-radius: $radius-md;
  border: 1px solid rgba(212, 168, 83, 0.1);
}

.tips {
  background: rgba(52, 211, 153, 0.05);
  padding: $spacing-sm;
  border-radius: $radius-md;
  border: 1px solid rgba(52, 211, 153, 0.1);
}

.tips-list {
  li {
    color: $color-success;

    &::before {
      background: $color-success;
    }
  }
}
</style>
