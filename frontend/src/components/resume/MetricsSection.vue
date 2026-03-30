<!--=====================================================
  评分指标组件
  @author Azir
=====================================================-->

<template>
  <section
    class="metrics-section animate-in"
    style="--delay: 2"
  >
    <div class="metric-card">
      <div class="metric-header">
        <span class="metric-icon content">
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <polyline points="14 2 14 8 20 8" />
            <line
              x1="16"
              y1="13"
              x2="8"
              y2="13"
            />
            <line
              x1="16"
              y1="17"
              x2="8"
              y2="17"
            />
          </svg>
        </span>
        <span class="metric-title">内容质量</span>
      </div>
      <div class="metric-value">
        {{ analyzed ? contentScore : '~' }}
      </div>
      <div class="metric-bar">
        <div
          class="metric-fill"
          :style="{ width: analyzed ? contentScore + '%' : '0%' }"
        />
      </div>
    </div>
    <div class="metric-card">
      <div class="metric-header">
        <span class="metric-icon structure">
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <rect
              x="3"
              y="3"
              width="18"
              height="18"
              rx="2"
              ry="2"
            />
            <line
              x1="3"
              y1="9"
              x2="21"
              y2="9"
            />
            <line
              x1="9"
              y1="21"
              x2="9"
              y2="9"
            />
          </svg>
        </span>
        <span class="metric-title">结构规范</span>
      </div>
      <div class="metric-value">
        {{ analyzed ? structureScore : '~' }}
      </div>
      <div class="metric-bar">
        <div
          class="metric-fill"
          :style="{ width: analyzed ? structureScore + '%' : '0%' }"
        />
      </div>
    </div>
    <div class="metric-card">
      <div class="metric-header">
        <span class="metric-icon matching">
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <circle
              cx="11"
              cy="11"
              r="8"
            />
            <line
              x1="21"
              y1="21"
              x2="16.65"
              y2="16.65"
            />
          </svg>
        </span>
        <span class="metric-title">岗位匹配</span>
      </div>
      <div class="metric-value">
        {{ analyzed ? matchingScore : '~' }}
      </div>
      <div class="metric-bar">
        <div
          class="metric-fill"
          :style="{ width: analyzed ? matchingScore + '%' : '0%' }"
        />
      </div>
    </div>
    <div class="metric-card">
      <div class="metric-header">
        <span class="metric-icon competitiveness">
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
          </svg>
        </span>
        <span class="metric-title">竞争力</span>
      </div>
      <div class="metric-value">
        {{ analyzed ? competitivenessScore : '~' }}
      </div>
      <div class="metric-bar">
        <div
          class="metric-fill"
          :style="{ width: analyzed ? competitivenessScore + '%' : '0%' }"
        />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  analyzed: boolean
  contentScore: number
  structureScore: number
  matchingScore: number
  competitivenessScore: number
}>()
</script>

<style lang="scss" scoped>
.metrics-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $spacing-lg;
}

.metric-card {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.metric-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
}

.metric-icon {
  width: 32px;
  height: 32px;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  &.content {
    background: rgba(212, 168, 83, 0.15);
    color: $color-accent;
  }
  &.structure {
    background: rgba(52, 211, 153, 0.15);
    color: #34d399;
  }
  &.matching {
    background: rgba(96, 165, 250, 0.15);
    color: #60a5fa;
  }
  &.competitiveness {
    background: rgba(167, 139, 250, 0.15);
    color: #a78bfa;
  }
}

.metric-title {
  font-size: $text-sm;
  color: $color-text-tertiary;
}

.metric-value {
  font-family: $font-display;
  font-size: $text-3xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-md;
}

.metric-bar {
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: $radius-full;
  overflow: hidden;
}

.metric-fill {
  height: 100%;
  background: $gradient-gold;
  border-radius: $radius-full;
  transition: width 0.8s ease;
}

.animate-in {
  animation: slideUp 0.6s ease forwards;
  animation-delay: calc(var(--delay) * 0.1s);
  opacity: 0;
}
</style>
