<!--=====================================================
  LandIt 开源贡献区块查看器
  渲染开源贡献区块内容
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
      v-for="(item, idx) in openSourceList"
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
        <span class="exp-title">
          {{ item.projectName }}
          <a
            v-if="item.url"
            :href="item.url"
            target="_blank"
            class="exp-link"
            title="访问项目"
          >
            <svg
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
              <polyline points="15 3 21 3 21 9" />
              <line
                x1="10"
                y1="14"
                x2="21"
                y2="3"
              />
            </svg>
          </a>
        </span>
        <span
          v-if="item.period"
          class="exp-period"
        >
          {{ item.period }}
        </span>
      </div>
      <p
        v-if="item.role"
        class="exp-position"
      >
        {{ item.role }}
      </p>
      <p
        v-if="item.description"
        class="exp-description"
      >
        {{ item.description }}
      </p>
      <div
        v-if="item.achievements?.length"
        class="exp-achievements"
      >
        <span
          v-for="a in item.achievements"
          :key="a"
          class="achievement-tag"
        >
          {{ a }}
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

const { getOpenSourceList } = useSectionHelper()

const { titleClass } = useSectionDiff(
  toRef(props, 'beforeSections'),
  toRef(props, 'afterSections')
)

const openSourceList = computed(() => getOpenSourceList(props.section))

const titleDiffClass = computed(() => {
  if (!props.beforeSections || !props.afterSections) return ''
  return titleClass(props.section.type, props.side)
})

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
