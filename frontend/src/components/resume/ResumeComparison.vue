<!--=====================================================
  LandIt 简历对比组件
  展示优化前后的简历对比
  @author Azir
=====================================================-->

<template>
  <div class="resume-comparison">
    <div class="comparison-header">
      <div class="resume-column before">
        <h4>优化前</h4>
      </div>
      <div class="vs-divider">
        <span class="improvement-badge" v-if="improvementScore">
          +{{ improvementScore }} 分
        </span>
      </div>
      <div class="resume-column after">
        <h4>优化后</h4>
      </div>
    </div>

    <div class="comparison-content">
      <div class="resume-panel before-panel">
        <ResumeContentViewer
          v-if="beforeSection?.length"
          :sections="beforeSection"
          side="before"
          :changes="changes"
        />
        <div v-else class="empty-state">
          <p>暂无对比数据</p>
        </div>
      </div>
      <div class="resume-panel after-panel">
        <ResumeContentViewer
          v-if="afterSection?.length"
          :sections="afterSection"
          side="after"
          :changes="changes"
          :editable="editable"
          @edit="handleEdit"
        />
        <div v-else class="empty-state">
          <p>暂无对比数据</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import ResumeContentViewer from './ResumeContentViewer.vue'
import type { ResumeSection } from '@/types'
import type { ChangeItem, ComparisonEditEvent } from '@/types/resume-optimize'

interface Props {
  /** 优化前的区块数据（新格式） */
  beforeSection?: ResumeSection[]
  /** 优化后的区块数据（新格式） */
  afterSection?: ResumeSection[]
  /** 变更列表 */
  changes?: ChangeItem[]
  /** 预计提升分数 */
  improvementScore?: number
  /** 兼容旧格式 */
  beforeResume?: Record<string, unknown>
  /** 是否可编辑 */
  editable?: boolean
}

withDefaults(defineProps<Props>(), {
  editable: false
})

const emit = defineEmits<{
  /** 编辑区块事件 */
  (e: 'edit-section', payload: ComparisonEditEvent): void
}>()

// 向上传递编辑事件
function handleEdit(payload: ComparisonEditEvent) {
  emit('edit-section', payload)
}
</script>

<style lang="scss" scoped>
.resume-comparison {
  width: 100%;
}

.comparison-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-lg;
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;

  .resume-column {
    text-align: center;
    flex: 1;

    h4 {
      font-size: $text-sm;
      color: $color-text-secondary;
      margin-bottom: $spacing-xs;
    }
  }

  .vs-divider {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 $spacing-lg;
  }

  .improvement-badge {
    display: inline-flex;
    align-items: center;
    padding: $spacing-xs $spacing-md;
    background: rgba(52, 211, 153, 0.15);
    color: $color-success;
    border-radius: $radius-full;
    font-size: $text-sm;
    font-weight: $weight-semibold;
  }
}

.comparison-content {
  display: flex;
  gap: $spacing-md;
  min-height: 400px;
}

.resume-panel {
  flex: 1;
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  border: 1px solid rgba(255, 255, 255, 0.05);
  overflow-y: auto;
  max-height: 500px;

  &.before-panel {
    border-color: rgba(248, 113, 113, 0.2);
  }

  &.after-panel {
    border-color: rgba(52, 211, 153, 0.2);
  }
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: $color-text-tertiary;
}
</style>
