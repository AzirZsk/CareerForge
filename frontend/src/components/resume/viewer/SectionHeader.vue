<!--=====================================================
  CareerForge 区块头部组件
  区块标题 + 编辑按钮
  @author Azir
=====================================================-->

<template>
  <div class="section-header">
    <h3>
      <span
        class="section-title-text"
        :class="diffClass"
      >
        {{ title }}
      </span>
    </h3>
    <SectionEditButton
      v-if="showEdit"
      size="medium"
      :visible="true"
      @click="handleEdit"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import SectionEditButton from './SectionEditButton.vue'

interface Props {
  /** 区块标题 */
  title: string
  /** 差异类型 */
  diffType?: 'added' | 'modified' | 'removed' | ''
  /** 是否显示编辑按钮 */
  showEdit?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  diffType: '',
  showEdit: false
})

const emit = defineEmits<{
  (e: 'edit'): void
}>()

const diffClass = computed(() => {
  if (!props.diffType) return ''
  return props.diffType === 'added'
    ? 'highlight-added'
    : props.diffType === 'removed'
      ? 'highlight-removed'
      : ''
})

function handleEdit() {
  emit('edit')
}
</script>

<style lang="scss" scoped>
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-md;
  padding-bottom: $spacing-sm;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);

  h3 {
    font-size: $text-xl;
    font-weight: $weight-semibold;
    color: $color-text-primary;
    margin: 0;
    padding: 0;
    border: none;
  }
}

.section-title-text {
  border-radius: $radius-sm;
  padding: 2px 4px;
  transition: background-color 0.2s;

  &.highlight-added {
    background: rgba(52, 211, 153, 0.15);
    color: $color-success;
  }

  &.highlight-removed {
    background: rgba(248, 113, 113, 0.15);
    color: $color-error;
  }
}
</style>
