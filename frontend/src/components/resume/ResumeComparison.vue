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
        <div v-if="originalScore" class="resume-score before">
          {{ originalScore }} 分
        </div>
      </div>
      <div class="vs-divider">VS</div>
      <div class="resume-column after">
        <h4>优化后</h4>
        <div v-if="optimizedScore" class="resume-score after">
          {{ optimizedScore }} 分
          <span v-if="scoreImprovement" class="improvement">+{{ scoreImprovement }}</span>
        </div>
      </div>
    </div>

    <div class="comparison-content">
      <div class="resume-panel before-panel">
        <ResumeContentViewer :resume="beforeResume" side="before" :changes="changes" />
      </div>
      <div class="resume-panel after-panel">
        <ResumeContentViewer :resume="afterResume" side="after" :changes="changes" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ResumeContentViewer from './ResumeContentViewer.vue'

interface Props {
  beforeResume: Record<string, any>
  changes: ChangeItem[]
  originalScore?: number
  optimizedScore?: number
}

interface ChangeItem {
  field: string
  type?: 'added' | 'modified' | 'removed'
  before?: any
  after?: any
  reason?: string
}

const props = defineProps<Props>()

const scoreImprovement = computed(() => {
  if (props.originalScore && props.optimizedScore) {
    const diff = props.optimizedScore - props.originalScore
    return diff > 0 ? diff : null
  }
  return null
})

// 应用 changes 构建优化后的简历
const afterResume = computed(() => {
  if (!props.beforeResume || !props.changes?.length) {
    return props.beforeResume
  }

  const result = JSON.parse(JSON.stringify(props.beforeResume))
  props.changes.forEach(change => applyChange(result, change))
  return result
})

// 应用单个变更到简历对象
function applyChange(resume: Record<string, any>, change: ChangeItem): void {
  if (!change.field) return

  const parts = parseFieldPath(change.field)
  const target = navigateToParent(resume, parts.slice(0, -1))
  const lastKey = parts[parts.length - 1]

  if (target && lastKey !== undefined) {
    if (change.type === 'removed') {
      Array.isArray(target) ? target.splice(Number(lastKey), 1) : delete target[lastKey]
    } else {
      target[lastKey] = change.after
    }
  }
}

// 解析字段路径为数组（支持 . 和 [] 语法）
function parseFieldPath(field: string): (string | number)[] {
  return field.split(/[\.\[\]]+/).filter(Boolean).map(part =>
    /^\d+$/.test(part) ? parseInt(part, 10) : part
  )
}

// 导航到父级对象
function navigateToParent(obj: any, parts: (string | number)[]): any {
  return parts.reduce((current, key) => {
    if (current && typeof current === 'object' && !(key in current)) {
      current[key] = typeof key === 'number' ? [] : {}
    }
    return current?.[key]
  }, obj)
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

    .resume-score {
      font-size: $text-2xl;
      font-weight: $weight-bold;

      &.before {
        color: $color-error;
      }

      &.after {
        color: $color-success;
      }

      .improvement {
        font-size: $text-sm;
        margin-left: $spacing-xs;
      }
    }
  }

  .vs-divider {
    font-size: $text-lg;
    font-weight: $weight-bold;
    color: $color-accent;
    padding: 0 $spacing-lg;
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
</style>
