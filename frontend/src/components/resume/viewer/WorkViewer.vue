<!--=====================================================
  LandIt 工作经历区块查看器
  渲染工作经历区块内容
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
      v-for="(item, idx) in workList"
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
        <span
          class="exp-title"
          :class="getFieldClass('company', idx)"
        >
          {{ item.company }}
        </span>
        <span
          class="exp-period"
          :class="getFieldClass('period', idx)"
        >
          {{ item.period }}
        </span>
      </div>
      <div class="exp-meta">
        <span
          v-if="item.position"
          class="exp-position"
          :class="getFieldClass('position', idx)"
        >
          {{ item.position }}
        </span>
        <span
          v-if="item.location"
          class="exp-location"
          :class="getFieldClass('location', idx)"
        >
          <svg
            width="12"
            height="12"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
            <circle
              cx="12"
              cy="10"
              r="3"
            />
          </svg>
          {{ item.location }}
        </span>
        <span
          v-if="item.industry"
          class="exp-industry"
        >
          {{ item.industry }}
        </span>
      </div>
      <p
        v-if="item.description"
        class="exp-description"
        :class="getFieldClass('description', idx)"
      >
        {{ item.description }}
      </p>
      <!-- 成就 -->
      <div
        v-if="item.achievements?.length"
        class="exp-achievements"
        :class="getFieldClass('achievements', idx)"
      >
        <div
          v-for="(ach, achIdx) in item.achievements"
          :key="achIdx"
          class="achievement-item"
        >
          {{ ach }}
        </div>
      </div>
      <!-- 技术栈 -->
      <div
        v-if="item.technologies?.length"
        class="exp-technologies"
        :class="getFieldClass('technologies', idx)"
      >
        <span
          v-for="tech in item.technologies"
          :key="tech"
          class="tech-tag"
        >
          {{ tech }}
        </span>
      </div>
      <!-- 代表产品 -->
      <div
        v-if="item.products?.length"
        class="exp-products"
        :class="getFieldClass('products', idx)"
      >
        <span class="products-label">代表产品:</span>
        <span
          v-for="p in item.products"
          :key="p"
          class="product-tag"
        >{{ p }}</span>
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

const { getWorkList } = useSectionHelper()

const { titleClass, itemFieldClass } = useSectionDiff(
  toRef(props, 'beforeSections'),
  toRef(props, 'afterSections')
)

const workList = computed(() => getWorkList(props.section))

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
