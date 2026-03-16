<!--=====================================================
  简历头部组件
  @author Azir
=====================================================-->

<template>
  <header class="resume-header animate-in" style="--delay: 1">
    <div class="header-left">
      <h1 class="resume-title">{{ name }}</h1>
      <p class="resume-target">目标岗位：{{ targetPosition }}</p>
    </div>
    <div class="header-right">
      <div class="score-overview">
        <div class="score-main">
          <div class="score-ring" :style="{ '--score': analyzed ? overallScore : 0 }">
            <span>{{ analyzed ? overallScore : '~' }}</span>
          </div>
          <div class="score-labels">
            <span class="score-title">综合评分</span>
            <span class="score-detail">结构规范 {{ analyzed ? structureScore + '%' : '~' }}</span>
          </div>
        </div>
      </div>
      <div class="header-actions">
        <button class="action-btn primary" @click="$emit('optimize')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
          </svg>
          {{ analyzed ? '一键优化' : 'AI分析' }}
        </button>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
defineProps<{
  name: string
  targetPosition: string
  analyzed: boolean
  overallScore: number
  structureScore: number
}>()

defineEmits<{
  optimize: []
}>()
</script>

<style lang="scss" scoped>
.resume-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: $spacing-xl;
  background: $gradient-card;
  border-radius: $radius-xl;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.resume-title {
  font-family: $font-display;
  font-size: $text-3xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-xs;
}

.resume-target {
  font-size: $text-base;
  color: $color-text-secondary;
}

.header-right {
  display: flex;
  align-items: center;
  gap: $spacing-2xl;
}

.score-main {
  display: flex;
  align-items: center;
  gap: $spacing-lg;
}

.score-ring {
  width: 72px;
  height: 72px;
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
    inset: 6px;
    background: $color-bg-tertiary;
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

.score-labels {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.score-title {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.score-detail {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.header-actions {
  display: flex;
  gap: $spacing-md;
}

.action-btn {
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
      transform: translateY(-2px);
      box-shadow: 0 4px 16px rgba(212, 168, 83, 0.3);
    }
  }
  &.secondary {
    background: rgba(255, 255, 255, 0.05);
    color: $color-text-secondary;
    border: 1px solid rgba(255, 255, 255, 0.1);
    &:hover {
      background: rgba(255, 255, 255, 0.1);
    }
  }
}

.animate-in {
  animation: slideUp 0.6s ease forwards;
  animation-delay: calc(var(--delay) * 0.1s);
  opacity: 0;
}
</style>
