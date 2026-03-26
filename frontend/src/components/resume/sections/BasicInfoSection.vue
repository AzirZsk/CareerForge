<!--=====================================================
  基本信息模块展示组件
  @author Azir
=====================================================-->

<template>
  <div class="content-block basic-info-block">
    <div
      v-for="{ key, value } in orderedFields"
      :key="key"
      class="info-row"
    >
      <span class="info-label">{{ getFieldLabel(key) }}</span>
      <span class="info-value">{{ value }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useSectionHelper } from '@/composables/useSectionHelper'
import type { BasicInfoContent } from '@/types'

const props = defineProps<{
  content: BasicInfoContent
}>()

const { getFieldLabel, getOrderedBasicInfoFields } = useSectionHelper()

const orderedFields = computed(() => {
  return getOrderedBasicInfoFields(props.content)
})
</script>

<style lang="scss" scoped>
.basic-info-block {
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  padding: $spacing-lg;
}

.info-row {
  display: flex;
  padding: $spacing-md 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
  &:last-child {
    border-bottom: none;
  }
}

.info-label {
  width: 120px;
  font-size: $text-sm;
  color: $color-text-tertiary;
}

.info-value {
  flex: 1;
  font-size: $text-sm;
  color: $color-text-primary;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
