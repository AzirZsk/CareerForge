<!--=====================================================
  CareerForge 阶段列表组件
  @author Azir
=====================================================-->

<template>
  <div class="stages-section">
    <StageItem
      v-for="item in sortedStageHistory"
      :key="item.stage"
      :item="item"
      :is-active="item.stage === currentStage && isOptimizing"
      :elapsed="formatElapsed(item)"
      @toggle-expand="$emit('toggleExpand', item.stage)"
      @show-comparison="$emit('showComparison')"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { StageHistoryItem, OptimizeStage } from '@/types/resume-optimize'
import StageItem from './StageItem.vue'

const props = defineProps<{
  stageHistory: StageHistoryItem[]
  currentStage: OptimizeStage
  isOptimizing: boolean
  formatElapsed: (item: StageHistoryItem) => string
}>()

defineEmits<{
  toggleExpand: [stage: OptimizeStage]
  showComparison: []
}>()

// 排序后的阶段历史
const sortedStageHistory = computed(() => {
  const order: OptimizeStage[] = [
    'diagnose_quick',
    'generate_suggestions',
    'optimize_section'
  ]

  return order.map(stage => {
    const historyItem = props.stageHistory.find(h => h.stage === stage)
    return historyItem || {
      stage,
      message: '',
      timestamp: 0,
      completed: false,
      data: null,
      expanded: false
    }
  })
})
</script>

<style lang="scss" scoped>
.stages-section {
  flex: 1;
  overflow-y: auto;
  padding: 0 $spacing-lg $spacing-lg;
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}
</style>
