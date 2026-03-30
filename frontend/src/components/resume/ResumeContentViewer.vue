<!--=====================================================
  LandIt 简历内容查看器组件
  展示简历的具体内容（用于对比视图）
  重构版本：使用子组件渲染各区块类型
  @author Azir
=====================================================-->

<template>
  <div class="resume-content-viewer">
    <div
      v-if="isEmpty"
      class="empty-state"
    >
      <p>暂无简历内容</p>
    </div>

    <div
      v-else
      class="resume-content"
    >
      <template
        v-for="(section, sectionIdx) in sortedSections"
        :key="section.id"
      >
        <!-- 基本信息 -->
        <BasicInfoViewer
          v-if="section.type === 'BASIC_INFO'"
          :section="section"
          :section-index="sectionIdx"
          :show-edit="showEdit"
          :before-sections="beforeSections"
          :after-sections="afterSections"
          :side="side"
          @edit="handleEdit"
        />

        <!-- 教育经历 -->
        <EducationViewer
          v-else-if="section.type === 'EDUCATION'"
          :section="section"
          :section-index="sectionIdx"
          :show-edit="showEdit"
          :before-sections="beforeSections"
          :after-sections="afterSections"
          :side="side"
          @edit="handleEdit"
        />

        <!-- 工作经历 -->
        <WorkViewer
          v-else-if="section.type === 'WORK'"
          :section="section"
          :section-index="sectionIdx"
          :show-edit="showEdit"
          :before-sections="beforeSections"
          :after-sections="afterSections"
          :side="side"
          @edit="handleEdit"
        />

        <!-- 项目经历 -->
        <ProjectViewer
          v-else-if="section.type === 'PROJECT'"
          :section="section"
          :section-index="sectionIdx"
          :show-edit="showEdit"
          :before-sections="beforeSections"
          :after-sections="afterSections"
          :side="side"
          @edit="handleEdit"
        />

        <!-- 专业技能 -->
        <SkillsViewer
          v-else-if="section.type === 'SKILLS'"
          :section="section"
          :section-index="sectionIdx"
          :show-edit="showEdit"
          :before-sections="beforeSections"
          :after-sections="afterSections"
          :side="side"
          @edit="handleEdit"
        />

        <!-- 证书荣誉 -->
        <CertificateViewer
          v-else-if="section.type === 'CERTIFICATE'"
          :section="section"
          :section-index="sectionIdx"
          :show-edit="showEdit"
          :before-sections="beforeSections"
          :after-sections="afterSections"
          :side="side"
          @edit="handleEdit"
        />

        <!-- 开源贡献 -->
        <OpenSourceViewer
          v-else-if="section.type === 'OPEN_SOURCE'"
          :section="section"
          :section-index="sectionIdx"
          :show-edit="showEdit"
          :before-sections="beforeSections"
          :after-sections="afterSections"
          :side="side"
          @edit="handleEdit"
        />

        <!-- 自定义区块 -->
        <CustomViewer
          v-else-if="section.type === 'CUSTOM'"
          :section="section"
          :section-index="sectionIdx"
          :show-edit="showEdit"
          :before-sections="beforeSections"
          :after-sections="afterSections"
          :side="side"
          @edit="handleEdit"
        />

        <!-- 默认渲染（未知区块类型） -->
        <div
          v-else
          class="resume-section"
        >
          <h3>{{ section.title }}</h3>
          <pre class="raw-content">{{ JSON.stringify(section, null, 2) }}</pre>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ResumeSection } from '@/types'
import type { ComparisonEditEvent } from '@/types/resume-optimize'
// Viewer components
import BasicInfoViewer from './viewer/BasicInfoViewer.vue'
import EducationViewer from './viewer/EducationViewer.vue'
import WorkViewer from './viewer/WorkViewer.vue'
import ProjectViewer from './viewer/ProjectViewer.vue'
import SkillsViewer from './viewer/SkillsViewer.vue'
import CertificateViewer from './viewer/CertificateViewer.vue'
import OpenSourceViewer from './viewer/OpenSourceViewer.vue'
import CustomViewer from './viewer/CustomViewer.vue'

interface Props {
  sections: ResumeSection[]
  side: 'before' | 'after'
  /** 优化前的区块数据（用于差异对比） */
  beforeSections?: ResumeSection[]
  /** 优化后的区块数据（用于差异对比） */
  afterSections?: ResumeSection[]
  /** 是否可编辑（仅对 after 侧生效） */
  editable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  editable: false
})

const emit = defineEmits<{
  /** 编辑事件 */
  (e: 'edit', payload: ComparisonEditEvent): void
}>()

// 区块类型排序顺序（与后端 SectionType 枚举顺序一致）
const SECTION_ORDER: Record<string, number> = {
  'BASIC_INFO': 1,
  'EDUCATION': 2,
  'WORK': 3,
  'PROJECT': 4,
  'SKILLS': 5,
  'CERTIFICATE': 6,
  'OPEN_SOURCE': 7,
  'CUSTOM': 8,
  'RAW_TEXT': 99
}

// 是否显示编辑功能（仅 after 侧且 editable 为 true）
const showEdit = computed(() => props.editable && props.side === 'after')

const isEmpty = computed(() => !props.sections || props.sections.length === 0)

// 按区块类型排序后的 sections
const sortedSections = computed(() => {
  if (!props.sections) return []
  return [...props.sections].sort((a, b) => {
    const orderA = SECTION_ORDER[a.type] ?? 99
    const orderB = SECTION_ORDER[b.type] ?? 99
    return orderA - orderB
  })
})

// 统一处理编辑事件（直接透传）
function handleEdit(payload: ComparisonEditEvent) {
  emit('edit', payload)
}
</script>

<style lang="scss" scoped>
@use './viewer/shared.scss' as *;

.resume-content-viewer {
  min-height: 300px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: $color-text-tertiary;
}

.resume-content {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}
</style>
