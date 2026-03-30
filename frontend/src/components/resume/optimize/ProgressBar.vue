<!--=====================================================
  LandIt 进度条组件
  @author Azir
=====================================================-->

<template>
  <div class="progress-section">
    <div class="progress-bar">
      <div
        class="progress-fill"
        :style="{ width: progress + '%' }"
      />
    </div>
    <div class="progress-info">
      <span class="progress-text">{{ message }}</span>
      <span class="progress-percent">{{ progress }}%</span>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  progress: number
  message: string
}>()
</script>

<style lang="scss" scoped>
.progress-section {
  padding: $spacing-lg;
}

.progress-bar {
  height: 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: $radius-full;
  overflow: hidden;
  margin-bottom: $spacing-sm;
}

.progress-fill {
  height: 100%;
  background: $gradient-gold;
  border-radius: $radius-full;
  transition: width 0.3s ease;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-text {
  font-size: $text-sm;
  color: $color-text-secondary;
}

.progress-percent {
  font-size: $text-sm;
  font-weight: $weight-semibold;
  color: $color-accent;
}
</style>
