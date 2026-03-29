<!--=====================================================
  AI建议变更卡片组件
  根据不同的简历模块类型差异化渲染内容
  复用现有的 Viewer 组件
  @author Azir
=====================================================-->

<template>
  <div
    class="section-change-card"
    :class="[change.changeType, { 'is-rejected': change.status === 'rejected', 'is-applied': change.status === 'applied' }]"
  >
    <!-- 头部：点击可折叠 -->
    <div
      class="card-header"
      @click="isCollapsed = !isCollapsed"
    >
      <div class="header-left">
        <span class="section-icon">{{ sectionIcon }}</span>
        <span
          class="change-type-badge"
          :class="change.changeType"
        >
          {{ getTypeLabel(change.changeType) }}
        </span>
        <span class="section-title">{{ change.sectionTitle || change.sectionType || '内容修改' }}</span>
      </div>
      <div class="header-right">
        <!-- 状态标记 -->
        <span
          v-if="change.status === 'applied'"
          class="status-tag applied"
        >已应用</span>
        <span
          v-if="change.status === 'rejected'"
          class="status-tag rejected"
        >已忽略</span>
        <!-- 折叠箭头 -->
        <span
          class="collapse-toggle"
          :class="{ 'is-expanded': !isCollapsed }"
        >
          <svg
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <polyline points="6 9 12 15 18 9" />
          </svg>
        </span>
      </div>
    </div>

    <!-- 描述（折叠时隐藏） -->
    <div
      v-if="!isCollapsed && change.description"
      class="card-desc"
    >
      {{ change.description }}
    </div>

    <!-- 内容对比（折叠时隐藏） -->
    <div
      v-if="!isCollapsed"
      class="card-content"
    >
      <!-- update 模式：字段级对比 -->
      <FieldDiffViewer
        v-if="change.changeType === 'update'"
        :section-type="change.sectionType || ''"
        :section-title="change.sectionTitle || ''"
        :before-content="change.beforeContent || null"
        :after-content="change.afterContent"
      />

      <!-- add 模式：只显示新增内容 -->
      <div
        v-else-if="change.changeType === 'add'"
        class="diff-section diff-after"
      >
        <span class="diff-label">新增内容</span>
        <component
          :is="viewerComponent"
          :section="afterSection"
          :section-index="0"
          side="after"
        />
      </div>

      <!-- delete 模式：只显示删除内容 -->
      <div
        v-else-if="change.changeType === 'delete'"
        class="diff-section diff-before"
      >
        <span class="diff-label">删除内容</span>
        <component
          :is="viewerComponent"
          :section="beforeSection"
          :section-index="0"
          side="before"
        />
      </div>
    </div>

    <!-- 单卡片操作按钮：仅 pending 且展开时显示 -->
    <div
      v-if="!isCollapsed && (change.status === 'pending' || !change.status)"
      class="card-actions"
    >
      <button
        class="btn-apply"
        @click="emit('apply')"
      >
        <svg
          width="14"
          height="14"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <polyline points="20 6 9 17 4 12" />
        </svg>
        应用
      </button>
      <button
        class="btn-ignore"
        @click="emit('ignore')"
      >
        忽略
      </button>
    </div>

    <!-- failed 状态：展开时显示重试 -->
    <div
      v-else-if="!isCollapsed && change.status === 'failed'"
      class="card-actions"
    >
      <button
        class="btn-retry"
        @click="emit('apply')"
      >
        重试
      </button>
      <button
        class="btn-ignore"
        @click="emit('ignore')"
      >
        忽略
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, ref } from 'vue'
import type { SectionChange } from '@/types/ai-chat'
import type { ResumeSection } from '@/types'
import { useSectionHelper } from '@/composables/useSectionHelper'
import FieldDiffViewer from './FieldDiffViewer.vue'

// 复用现有 Viewer 组件
import BasicInfoViewer from '@/components/resume/viewer/BasicInfoViewer.vue'
import EducationViewer from '@/components/resume/viewer/EducationViewer.vue'
import WorkViewer from '@/components/resume/viewer/WorkViewer.vue'
import ProjectViewer from '@/components/resume/viewer/ProjectViewer.vue'
import SkillsViewer from '@/components/resume/viewer/SkillsViewer.vue'
import CertificateViewer from '@/components/resume/viewer/CertificateViewer.vue'
import OpenSourceViewer from '@/components/resume/viewer/OpenSourceViewer.vue'
import CustomViewer from '@/components/resume/viewer/CustomViewer.vue'

// 通用内容渲染组件（兜底）- 使用 render 函数避免 this 类型问题
const GenericViewer = defineComponent({
  props: {
    section: { type: Object as () => ResumeSection | null, default: null },
    sectionIndex: { type: Number, default: 0 },
    side: { type: String as () => 'before' | 'after', default: 'after' }
  },
  setup(props) {
    const contentText = computed(() => {
      if (!props.section?.content) return ''
      try {
        const parsed = JSON.parse(props.section.content)
        return JSON.stringify(parsed, null, 2)
      } catch {
        return props.section.content
      }
    })
    return () => h('div', { class: 'generic-content' }, [
      h('pre', { class: 'content-text' }, contentText.value)
    ])
  }
})

// SectionType 到 Viewer 组件的映射
const viewerMap: Record<string, any> = {
  BASIC_INFO: BasicInfoViewer,
  EDUCATION: EducationViewer,
  WORK: WorkViewer,
  PROJECT: ProjectViewer,
  SKILLS: SkillsViewer,
  CERTIFICATE: CertificateViewer,
  OPEN_SOURCE: OpenSourceViewer,
  CUSTOM: CustomViewer
}

interface Props {
  change: SectionChange
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'apply': []
  'ignore': []
}>()

const { getSectionIcon } = useSectionHelper()

// 折叠状态，默认展开
const isCollapsed = ref(false)

// 获取模块图标
const sectionIcon = computed(() => {
  return getSectionIcon(props.change.sectionType || '')
})

// 根据 sectionType 获取对应的 Viewer 组件
const viewerComponent = computed(() => {
  const type = props.change.sectionType
  return type && viewerMap[type] ? viewerMap[type] : GenericViewer
})

// 把 beforeContent 转换成 ResumeSection 格式
const beforeSection = computed<ResumeSection | null>(() => {
  if (!props.change.beforeContent) return null
  return {
    id: props.change.sectionId || '',
    resumeId: '',
    type: props.change.sectionType || '',
    title: props.change.sectionTitle || '',
    content: props.change.beforeContent,
    score: 0,
    suggestions: null
  }
})

// 把 afterContent 转换成 ResumeSection 格式
const afterSection = computed<ResumeSection | null>(() => {
  if (!props.change.afterContent) return null
  return {
    id: props.change.sectionId || '',
    resumeId: '',
    type: props.change.sectionType || '',
    title: props.change.sectionTitle || '',
    content: props.change.afterContent,
    score: 0,
    suggestions: null
  }
})

// 获取操作类型标签
function getTypeLabel(type: string): string {
  const labels: Record<string, string> = {
    update: '修改',
    add: '新增',
    delete: '删除'
  }
  return labels[type] || type
}
</script>

<style lang="scss" scoped>
.section-change-card {
  padding: $spacing-sm $spacing-md;
  background: rgba(0, 0, 0, 0.2);
  border-radius: $radius-sm;
  border: 1px solid rgba(255, 255, 255, 0.04);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: $spacing-sm;
  margin-bottom: $spacing-xs;
  cursor: pointer;
  user-select: none;

  &:hover {
    .section-title {
      color: $color-text-primary;
    }
  }
}

.header-left {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  min-width: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  flex-shrink: 0;
}

.section-icon {
  font-size: 16px;
}

.change-type-badge {
  padding: 2px 8px;
  border-radius: $radius-sm;
  font-size: $text-xs;
  font-weight: $weight-medium;

  &.update {
    background: rgba($color-accent, 0.2);
    color: $color-accent;
  }

  &.add {
    background: rgba($color-success, 0.2);
    color: $color-success;
  }

  &.delete {
    background: rgba($color-error, 0.2);
    color: $color-error;
  }
}

.section-title {
  font-size: $text-sm;
  color: $color-text-secondary;
  font-weight: $weight-medium;
}

.card-desc {
  font-size: $text-sm;
  color: $color-text-tertiary;
  line-height: $leading-relaxed;
  margin-bottom: $spacing-sm;
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.diff-section {
  padding: $spacing-sm;
  border-radius: $radius-xs;
  background: rgba(0, 0, 0, 0.15);

  .diff-label {
    display: inline-block;
    font-size: $text-xs;
    color: $color-text-tertiary;
    margin-bottom: $spacing-xs;
    padding: 1px 6px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: $radius-xs;
  }

  // 复用的 Viewer 组件样式覆盖
  :deep(.resume-section) {
    padding: 0;
    margin: 0;
    background: transparent;
    border: none;

    h3 {
      display: none;
    }
  }

  :deep(.experience-item) {
    padding: $spacing-xs 0;

    &:first-child {
      padding-top: 0;
    }
  }

  :deep(.info-grid) {
    gap: $spacing-xs;
  }

  :deep(.info-item) {
    padding: $spacing-xs 0;
  }

  :deep(.info-label) {
    font-size: $text-xs;
  }

  :deep(.info-value) {
    font-size: $text-sm;
  }

  :deep(.exp-title),
  :deep(.exp-position) {
    font-size: $text-sm;
  }

  :deep(.exp-period),
  :deep(.exp-location) {
    font-size: $text-xs;
  }

  :deep(.exp-description) {
    font-size: $text-xs;
    line-height: $leading-normal;
  }

  :deep(.tech-tag),
  :deep(.skill-tag) {
    font-size: $text-xs;
    padding: 2px 6px;
  }

  :deep(.generic-content) {
    .content-text {
      font-size: $text-xs;
      font-family: monospace;
      white-space: pre-wrap;
      word-break: break-word;
      color: $color-text-secondary;
      margin: 0;
      max-height: 100px;
      overflow-y: auto;
    }
  }
}

.diff-before {
  border-left: 2px solid rgba($color-error, 0.5);

  .diff-label {
    color: $color-error;
    background: rgba($color-error, 0.1);
  }

  :deep(.exp-title),
  :deep(.info-value) {
    opacity: 0.7;
  }
}

.diff-after {
  border-left: 2px solid $color-accent;

  .diff-label {
    color: $color-accent;
    background: rgba($color-accent, 0.1);
  }
}

// 状态样式
.section-change-card {
  &.is-rejected {
    opacity: 0.45;
    pointer-events: none;
  }

  &.is-applied {
    border-color: rgba($color-success, 0.15);
  }
}

.status-tag {
  padding: 2px 8px;
  border-radius: $radius-sm;
  font-size: $text-xs;
  font-weight: $weight-medium;
  flex-shrink: 0;

  &.applied {
    color: $color-success;
    background: rgba($color-success, 0.15);
  }

  &.rejected {
    color: $color-text-tertiary;
    background: rgba(255, 255, 255, 0.05);
  }
}

// 折叠箭头
.collapse-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  color: $color-text-tertiary;
  transition: transform $transition-fast;

  &.is-expanded {
    transform: rotate(180deg);
  }
}

// 卡片操作按钮
.card-actions {
  display: flex;
  gap: $spacing-sm;
  margin-top: $spacing-sm;
  padding-top: $spacing-sm;
  border-top: 1px solid rgba(255, 255, 255, 0.04);

  button {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: $spacing-xs $spacing-sm;
    font-size: $text-xs;
    font-weight: $weight-medium;
    border-radius: $radius-sm;
    cursor: pointer;
    transition: all $transition-fast;
    border: none;
  }

  .btn-apply {
    color: $color-bg-primary;
    background: $color-accent;

    &:hover {
      background: $color-accent-light;
    }

    &:active {
      transform: scale(0.96);
    }
  }

  .btn-retry {
    color: $color-error;
    background: rgba($color-error, 0.1);
    border: 1px solid rgba($color-error, 0.2);

    &:hover {
      background: rgba($color-error, 0.15);
    }
  }

  .btn-ignore {
    color: $color-text-tertiary;
    background: transparent;
    border: 1px solid rgba(255, 255, 255, 0.08);

    &:hover {
      color: $color-text-secondary;
      background: rgba(255, 255, 255, 0.04);
    }
  }
}
</style>
