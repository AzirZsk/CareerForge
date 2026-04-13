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
          <font-awesome-icon icon="fa-solid fa-file-lines" />
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
          <font-awesome-icon icon="fa-solid fa-table-cells" />
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
          <font-awesome-icon icon="fa-solid fa-magnifying-glass" />
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
          <font-awesome-icon icon="fa-solid fa-star" />
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
