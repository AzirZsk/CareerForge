<!--=====================================================
  LandIt 教育经历区块查看器
  渲染教育经历区块内容
  @author Azir
=====================================================-->

<template>
  <div class="resume-section" :class="{ editable: showEdit }">
    <h3>
      <span class="section-title-text" :class="titleDiffClass">
        {{ section.title }}
      </span>
    </h3>
    <div
      v-for="(item, idx) in educationList"
      :key="idx"
      class="experience-item"
      :class="{ 'has-edit-btn': showEdit }"
    >
      <SectionEditButton
        v-if="showEdit"
        size="small"
        :visible="false"
        @click="handleEdit(idx)"
      />
      <div class="exp-header">
        <span class="exp-title" :class="getFieldClass('school', idx)">
          {{ item.school }}
        </span>
        <span class="exp-period" :class="getFieldClass('period', idx)">
          {{ item.period }}
        </span>
      </div>
      <div class="exp-meta">
        <span v-if="item.degree" class="exp-degree" :class="getFieldClass('degree', idx)">
          {{ item.degree }}
        </span>
        <span v-if="item.major" class="exp-major" :class="getFieldClass('major', idx)">
          {{ item.major }}
        </span>
        <span v-if="item.gpa" class="exp-gpa" :class="getFieldClass('gpa', idx)">
          GPA: {{ item.gpa }}
        </span>
      </div>
      <div v-if="item.courses?.length" class="exp-courses" :class="getFieldClass('courses', idx)">
        <span v-for="course in item.courses" :key="course" class="course-tag">
          {{ course }}
        </span>
      </div>
      <div v-if="item.honors?.length" class="exp-honors" :class="getFieldClass('honors', idx)">
        <span v-for="honor in item.honors" :key="honor" class="honor-tag">
          {{ honor }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, toRef } from 'vue'
import type { ResumeSection } from '@/types'
import type { ComparisonEditEvent } from '@/types/resume-optimize'
import { useSectionHelper } from '@/composables/useSectionHelper'
import { useSectionDiff } from '@/composables/useSectionDiff'
import SectionEditButton from './SectionEditButton.vue'

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

const { getEducationList } = useSectionHelper()

const { titleClass, itemFieldClass } = useSectionDiff(
  toRef(props, 'beforeSections'),
  toRef(props, 'afterSections')
)

const educationList = computed(() => getEducationList(props.section))

const titleDiffClass = computed(() => {
  if (!props.beforeSections || !props.afterSections) return ''
  return titleClass(props.section.type, props.side)
})

function getFieldClass(fieldKey: string, itemIndex: number): string {
  if (!props.beforeSections || !props.afterSections) return ''
  return itemFieldClass(props.section.type, itemIndex, fieldKey, props.side)
}

function handleEdit(itemIndex: number) {
  emit('edit', {
    sectionIndex: props.sectionIndex,
    section: props.section,
    itemIndex
  })
}
</script>

<style lang="scss" scoped>
@use './shared.scss' as *;
</style>
