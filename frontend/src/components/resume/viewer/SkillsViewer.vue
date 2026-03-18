<!--=====================================================
  LandIt 专业技能区块查看器
  渲染专业技能区块内容
  @author Azir
=====================================================-->

<template>
  <div class="resume-section" :class="{ editable: showEdit }">
    <h3>
      <span class="section-title-text" :class="titleDiffClass">
        {{ section.title }}
      </span>
    </h3>
    <div class="skills-container">
      <div
        v-for="(skill, idx) in skillsList"
        :key="idx"
        class="skill-item"
        :class="[getFieldClass(idx), { 'has-edit-btn': showEdit }]"
      >
        <SectionEditButton
          v-if="showEdit"
          size="small"
          :visible="false"
          @click="handleEdit(idx)"
        />
        <div class="skill-header">
          <span class="skill-name">{{ skill.name }}</span>
          <span v-if="skill.level" class="skill-level">{{ skill.level }}</span>
        </div>
        <p v-if="skill.description" class="skill-description">
          {{ skill.description }}
        </p>
        <span v-if="skill.category" class="skill-category">
          {{ skill.category }}
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

const { getSkillsList } = useSectionHelper()

const { titleClass, itemFieldClass } = useSectionDiff(
  toRef(props, 'beforeSections'),
  toRef(props, 'afterSections')
)

const skillsList = computed(() => getSkillsList(props.section))

const titleDiffClass = computed(() => {
  if (!props.beforeSections || !props.afterSections) return ''
  return titleClass(props.section.type, props.side)
})

function getFieldClass(itemIndex: number): string {
  if (!props.beforeSections || !props.afterSections) return ''
  return itemFieldClass(props.section.type, itemIndex, undefined, props.side)
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
