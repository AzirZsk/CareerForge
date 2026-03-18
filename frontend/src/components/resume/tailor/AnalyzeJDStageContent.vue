<!--=====================================================
  LandIt JD分析阶段内容组件
  @author Azir
=====================================================-->

<template>
  <div class="data-content">
    <!-- 必备技能 -->
    <div class="skills-section" v-if="data.requiredSkills?.length">
      <div class="skills-title">必备技能</div>
      <div class="skill-tags">
        <span v-for="skill in data.requiredSkills" :key="skill" class="skill-tag required">{{ skill }}</span>
      </div>
    </div>

    <!-- 优先技能 -->
    <div class="skills-section" v-if="data.preferredSkills?.length">
      <div class="skills-title">优先技能</div>
      <div class="skill-tags">
        <span v-for="skill in data.preferredSkills" :key="skill" class="skill-tag preferred">{{ skill }}</span>
      </div>
    </div>

    <!-- 关键词 -->
    <div class="skills-section" v-if="data.keywords?.length">
      <div class="skills-title">关键词</div>
      <div class="skill-tags">
        <span v-for="keyword in data.keywords" :key="keyword" class="skill-tag keyword">{{ keyword }}</span>
      </div>
    </div>

    <!-- 职位职责 -->
    <div class="responsibilities-section" v-if="data.responsibilities?.length">
      <div class="skills-title">职位职责</div>
      <ul class="responsibilities-list">
        <li v-for="(resp, idx) in data.responsibilities" :key="idx">{{ resp }}</li>
      </ul>
    </div>

    <!-- 资历级别和行业领域 -->
    <div class="info-grid">
      <div class="info-item" v-if="data.seniorityLevel">
        <span class="info-label">资历级别</span>
        <span class="info-value">{{ data.seniorityLevel }}</span>
      </div>
      <div class="info-item" v-if="data.industryDomain">
        <span class="info-label">行业领域</span>
        <span class="info-value">{{ data.industryDomain }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { JobRequirements } from '@/types/resume-tailor'

defineProps<{
  data: JobRequirements
}>()
</script>

<style lang="scss" scoped>
.data-content {
  font-size: $text-sm;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-sm;
  padding: $spacing-md;
}

.skills-section {
  margin-bottom: $spacing-md;
  &:last-child {
    margin-bottom: 0;
  }
}

.skills-title {
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-bottom: $spacing-sm;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
}

.skill-tag {
  padding: 2px 8px;
  font-size: $text-xs;
  border-radius: $radius-sm;

  &.required {
    background: rgba(212, 168, 83, 0.15);
    color: $color-accent;
  }

  &.preferred {
    background: rgba(96, 165, 250, 0.15);
    color: $color-info;
  }

  &.keyword {
    background: rgba(139, 92, 246, 0.15);
    color: #a78bfa;
  }
}

.responsibilities-section {
  margin-bottom: $spacing-md;
  &:last-child {
    margin-bottom: 0;
  }
}

.responsibilities-list {
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
      color: $color-accent;
    }
  }
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-md;
  margin-top: $spacing-md;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-label {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.info-value {
  font-size: $text-sm;
  color: $color-text-primary;
}
</style>
