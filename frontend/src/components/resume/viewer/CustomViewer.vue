<!--=====================================================
  LandIt 自定义区块查看器
  渲染自定义区块内容（扁平结构：ContentItem[]）
  @author Azir
=====================================================-->

<template>
  <div
    class="resume-section"
    :class="{ editable: showEdit }"
  >
    <h3>
      <span
        class="section-title-text"
        :class="titleDiffClass"
      >
        {{ section.title }}
      </span>
    </h3>
    <div
      v-for="(item, itemIdx) in customList"
      :key="itemIdx"
      class="experience-item"
      :class="{ 'has-edit-btn': showEdit }"
    >
      <SectionEditButton
        v-if="showEdit"
        size="small"
        :visible="false"
        @click="handleEdit(itemIdx)"
      />
      <div class="exp-header">
        <span
          class="exp-title"
          :class="getFieldClass('name', itemIdx)"
        >
          {{ item.name }}
        </span>
        <span
          v-if="item.period"
          class="exp-period"
          :class="getFieldClass('period', itemIdx)"
        >
          {{ item.period }}
        </span>
      </div>
      <div
        v-if="item.role"
        class="exp-role"
        :class="getFieldClass('role', itemIdx)"
      >
        {{ item.role }}
      </div>
      <p
        v-if="item.description"
        class="exp-description"
        :class="getFieldClass('description', itemIdx)"
      >
        {{ item.description }}
      </p>
      <div
        v-if="item.highlights?.length"
        class="exp-achievements"
        :class="getFieldClass('highlights', itemIdx)"
      >
        <div
          v-for="(highlight, hIdx) in item.highlights"
          :key="hIdx"
          class="achievement-item"
        >
          {{ highlight }}
        </div>
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

const { getCustomList } = useSectionHelper()

const { titleClass, itemFieldClass } = useSectionDiff(
  toRef(props, 'beforeSections'),
  toRef(props, 'afterSections')
)

const customList = computed(() => getCustomList(props.section))

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
