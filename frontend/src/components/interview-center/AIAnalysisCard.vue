<template>
  <div class="ai-analysis-card" :class="{ expanded: isExpanded }">
    <!-- 卡片头部 -->
    <div class="card-header" @click="toggleExpand">
      <div class="header-left">
        <font-awesome-icon icon="fa-solid fa-robot" class="card-icon" />
        <span class="card-title">AI 分析建议</span>
        <span v-if="generatedTime" class="generated-time">
          生成于 {{ formatTime(generatedTime) }}
        </span>
      </div>
      <div class="header-right">
        <button
          v-if="!hideActions"
          class="btn btn-sm btn-reanalyze"
          @click.stop="handleReanalyze"
          :disabled="isAnalyzing"
        >
          {{ isAnalyzing ? '分析中...' : '重新分析' }}
        </button>
        <font-awesome-icon
          icon="fa-solid fa-chevron-down"
          class="expand-icon"
          :class="{ rotated: isExpanded }"
        />
      </div>
    </div>

    <!-- 卡片内容（可折叠） -->
    <div v-if="isExpanded" class="card-content">
      <!-- AI 建议列表 -->
      <div v-if="adviceList.length > 0" class="advice-list">
        <div v-for="(advice, index) in adviceList" :key="index" class="advice-item">
          <div class="advice-header">
            <span v-if="advice.category" class="advice-category">{{ advice.category }}</span>
            <span class="advice-title">{{ advice.title }}</span>
            <span v-if="advice.priority" class="advice-priority" :class="advice.priority">
              {{ getPriorityLabel(advice.priority) }}
            </span>
          </div>
          <p class="advice-description">{{ advice.description }}</p>
          <ul v-if="advice.actionItems && advice.actionItems.length > 0" class="action-items">
            <li v-for="(item, idx) in advice.actionItems" :key="idx">{{ item }}</li>
          </ul>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <p>暂无 AI 分析结果</p>
      </div>

      <!-- 参考建议按钮 -->
      <div v-if="showReferenceButton && adviceList.length > 0" class="card-footer">
        <button class="btn btn-sm btn-reference" @click="handleReference">
          <font-awesome-icon icon="fa-solid fa-clipboard" /> 参考 AI 建议写笔记
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import type { ReviewNoteVO, AdviceItem } from '@/types/interview-center'

const props = defineProps<{
  /** AI 分析记录（包含 suggestions 字段，是 JSON 字符串） */
  aiAnalysisNote?: ReviewNoteVO | null
  /** 是否正在分析中 */
  isAnalyzing?: boolean
  /** 是否显示操作按钮（重新分析、参考建议） */
  hideActions?: boolean
  /** 是否显示参考建议按钮 */
  showReferenceButton?: boolean
  /** 是否默认展开 */
  defaultExpanded?: boolean
}>()

const emit = defineEmits<{
  (e: 'reanalyze'): void
  (e: 'reference', adviceList: AdviceItem[]): void
}>()

const isExpanded = ref(props.defaultExpanded ?? false)

// 解析 AI 建议列表
const adviceList = computed<AdviceItem[]>(() => {
  if (!props.aiAnalysisNote?.suggestions) return []
  try {
    return JSON.parse(props.aiAnalysisNote.suggestions) as AdviceItem[]
  } catch {
    console.error('解析 AI 分析结果失败')
    return []
  }
})

// 生成时间
const generatedTime = computed(() => {
  return props.aiAnalysisNote?.createdAt
})

// 格式化时间
function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取优先级标签
function getPriorityLabel(priority: string): string {
  const labels: Record<string, string> = {
    high: '高',
    medium: '中',
    low: '低'
  }
  return labels[priority] || priority
}

// 切换展开/折叠
function toggleExpand() {
  isExpanded.value = !isExpanded.value
}

// 重新分析
function handleReanalyze() {
  emit('reanalyze')
}

// 参考建议
function handleReference() {
  emit('reference', adviceList.value)
}

// 监听分析状态，完成后自动展开
watch(() => props.isAnalyzing, (newValue, oldValue) => {
  if (oldValue === true && newValue === false) {
    // 分析完成，自动展开
    isExpanded.value = true
  }
})

// 初始化时如果有数据且默认展开，则展开
onMounted(() => {
  if (props.defaultExpanded && adviceList.value.length > 0) {
    isExpanded.value = true
  }
})
</script>

<style scoped lang="scss">
.ai-analysis-card {
  background: rgba($color-info, 0.08);
  border: 1px solid rgba($color-info, 0.2);
  border-radius: $radius-md;
  overflow: hidden;
  transition: all 0.2s;

  &.expanded {
    background: rgba($color-info, 0.1);
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: rgba($color-info, 0.1);
  }
}

.header-left {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.card-icon {
  font-size: 1.125rem;
}

.card-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: $color-info;
}

.generated-time {
  font-size: 0.75rem;
  color: $color-text-tertiary;
  margin-left: $spacing-sm;
}

.header-right {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.btn-reanalyze {
  background: transparent;
  border: 1px solid rgba($color-info, 0.3);
  color: $color-info;
  padding: 4px 10px;
  font-size: 0.75rem;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: all 0.2s;

  &:hover:not(:disabled) {
    background: rgba($color-info, 0.15);
    border-color: $color-info;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.expand-icon {
  color: $color-text-tertiary;
  transition: transform 0.2s;

  &.rotated {
    transform: rotate(180deg);
  }
}

.card-content {
  padding: 0 $spacing-md $spacing-md;
  border-top: 1px solid rgba($color-info, 0.15);
}

.advice-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
  margin-top: $spacing-md;
}

.advice-item {
  padding: $spacing-sm;
  background: $color-bg-secondary;
  border-radius: $radius-sm;
}

.advice-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-xs;
  flex-wrap: wrap;
}

.advice-category {
  font-size: 0.625rem;
  padding: 2px 6px;
  background: $color-bg-tertiary;
  border-radius: $radius-sm;
  color: $color-text-tertiary;
}

.advice-title {
  font-size: 0.875rem;
  font-weight: 500;
  color: $color-text-primary;
}

.advice-priority {
  font-size: 0.625rem;
  padding: 2px 6px;
  border-radius: $radius-sm;

  &.high {
    background: rgba($color-error, 0.15);
    color: $color-error;
  }

  &.medium {
    background: rgba($color-warning, 0.15);
    color: $color-warning;
  }

  &.low {
    background: rgba($color-success, 0.15);
    color: $color-success;
  }
}

.advice-description {
  font-size: 0.8125rem;
  color: $color-text-secondary;
  line-height: 1.5;
  margin: 0;
}

.action-items {
  margin: $spacing-sm 0 0;
  padding-left: $spacing-lg;
  font-size: 0.8125rem;
  color: $color-text-secondary;

  li {
    margin-bottom: 4px;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

.empty-state {
  text-align: center;
  padding: $spacing-xl;
  color: $color-text-tertiary;
}

.card-footer {
  margin-top: $spacing-md;
  padding-top: $spacing-md;
  border-top: 1px solid rgba($color-info, 0.15);
}

.btn-reference {
  width: 100%;
  background: rgba($color-info, 0.15);
  border: 1px solid rgba($color-info, 0.3);
  color: $color-info;
  padding: $spacing-sm;
  font-size: 0.875rem;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba($color-info, 0.25);
    border-color: $color-info;
  }
}
</style>
