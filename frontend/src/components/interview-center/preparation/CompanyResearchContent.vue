<!--=====================================================
  CareerForge 公司调研阶段内容组件
  @author Azir
=====================================================-->

<template>
  <div class="company-research-content">
    <!-- 概览 -->
    <div v-if="data.overview" class="research-section">
      <h5 class="section-title">公司概览</h5>
      <p class="section-text">{{ data.overview }}</p>
    </div>

    <!-- 核心业务 -->
    <div v-if="data.coreBusiness?.length" class="research-section">
      <h5 class="section-title">核心业务</h5>
      <ul class="section-list">
        <li v-for="(item, index) in data.coreBusiness" :key="index">{{ item }}</li>
      </ul>
    </div>

    <!-- 企业文化 -->
    <div v-if="data.culture" class="research-section">
      <h5 class="section-title">企业文化</h5>
      <p class="section-text">{{ data.culture }}</p>
    </div>

    <!-- 技术栈 -->
    <div v-if="data.techStack?.length" class="research-section">
      <h5 class="section-title">技术栈</h5>
      <div class="tech-tags">
        <span v-for="(tech, index) in data.techStack" :key="index" class="tech-tag">
          {{ tech }}
        </span>
      </div>
    </div>

    <!-- 面试特点 -->
    <div v-if="data.interviewCharacteristics?.length" class="research-section">
      <h5 class="section-title">面试特点</h5>
      <ul class="section-list highlight">
        <li v-for="(item, index) in data.interviewCharacteristics" :key="index">{{ item }}</li>
      </ul>
    </div>

    <!-- 近期动态 -->
    <div v-if="data.recentNews?.length" class="research-section">
      <h5 class="section-title">近期动态</h5>
      <ul class="section-list">
        <li v-for="(item, index) in data.recentNews" :key="index">{{ item }}</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { CompanyResearchResult } from '@/types/interview-center'

defineProps<{
  data: CompanyResearchResult
}>()
</script>

<style lang="scss" scoped>
.company-research-content {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.research-section {
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

.tech-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
}

.tech-tag {
  font-size: $text-xs;
  padding: 4px 8px;
  background: rgba(212, 168, 83, 0.15);
  color: $color-accent;
  border-radius: $radius-sm;
}
</style>
