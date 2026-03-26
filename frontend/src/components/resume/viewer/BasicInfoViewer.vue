<!--=====================================================
  LandIt 基本信息区块查看器
  渲染基本信息区块内容
  @author Azir
=====================================================-->

<template>
  <div
    class="resume-section basic-info-section"
    :class="{ editable: showEdit }"
  >
    <SectionHeader
      :title="section.title"
      :diff-type="titleDiffType"
      :show-edit="showEdit"
      @edit="handleEdit"
    />
    <div class="info-grid">
      <template
        v-for="{ key, value } in orderedFields"
        :key="key"
      >
        <div
          class="info-item"
          :class="[
            getFieldDiffClass(key),
            { 'full-width': key === 'summary' }
          ]"
        >
          <span class="info-label">{{ getFieldLabel(key) }}</span>
          <span
            class="info-value"
            :class="{ 'summary-text': key === 'summary' }"
          >
            {{ value }}
          </span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, toRef } from 'vue'
import type { ResumeSection } from '@/types'
import type { ComparisonEditEvent } from '@/types/resume-optimize'
import { useSectionHelper } from '@/composables/useSectionHelper'
import { useSectionDiff } from '@/composables/useSectionDiff'
import SectionHeader from './SectionHeader.vue'

interface Props {
  section: ResumeSection
  sectionIndex: number
  showEdit?: boolean
  beforeSections?: ResumeSection[]
  afterSections?: ResumeSection[]
  side: 'before' | 'after'
}

const props = withDefaults(defineProps<Props>(), {
  showEdit: false
})

const emit = defineEmits<{
  (e: 'edit', payload: ComparisonEditEvent): void
}>()

const { parseContent, getFieldLabel, getOrderedBasicInfoFields } = useSectionHelper()

const { titleClass, fieldClass } = useSectionDiff(
  toRef(props, 'beforeSections'),
  toRef(props, 'afterSections')
)

const orderedFields = computed(() => {
  return getOrderedBasicInfoFields(parseContent(props.section.content))
})

const titleDiffType = computed(() => {
  if (!props.beforeSections || !props.afterSections) return ''
  const cls = titleClass(props.section.type, props.side)
  if (cls === 'highlight-added') return 'added'
  if (cls === 'highlight-removed') return 'removed'
  return ''
})

function getFieldDiffClass(fieldKey: string): string {
  if (!props.beforeSections || !props.afterSections) return ''
  return fieldClass(props.section.type, fieldKey, props.side)
}

function handleEdit() {
  emit('edit', {
    sectionIndex: props.sectionIndex,
    section: props.section,
    itemIndex: undefined
  })
}
</script>

<style lang="scss" scoped>
@use './shared.scss' as *;

.resume-section {
  // 继承共享样式
}
</style>
