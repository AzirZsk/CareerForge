<!--=====================================================
  LandIt 证书荣誉区块查看器
  渲染证书荣誉区块内容
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
      v-for="(item, idx) in certificateList"
      :key="idx"
      class="certificate-item"
      :class="{ 'has-edit-btn': showEdit }"
    >
      <SectionEditButton
        v-if="showEdit"
        size="small"
        :visible="false"
        @click="handleEdit(idx)"
      />
      <div class="cert-header">
        <span class="cert-name" :class="getFieldClass('name', idx)">
          {{ item.name }}
          <a v-if="item.url" :href="item.url" target="_blank" class="cert-link" title="查看证书">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path>
              <polyline points="15 3 21 3 21 9"></polyline>
              <line x1="10" y1="14" x2="21" y2="3"></line>
            </svg>
          </a>
        </span>
        <span v-if="item.date" class="cert-date" :class="getFieldClass('date', idx)">
          {{ item.date }}
        </span>
      </div>
      <div v-if="item.issuer || item.credentialId" class="cert-meta">
        <span v-if="item.issuer" class="cert-issuer">{{ item.issuer }}</span>
        <span v-if="item.credentialId" class="cert-credential">
          编号: {{ item.credentialId }}
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

const { getCertificateList } = useSectionHelper()

const { titleClass, itemFieldClass } = useSectionDiff(
  toRef(props, 'beforeSections'),
  toRef(props, 'afterSections')
)

const certificateList = computed(() => getCertificateList(props.section))

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
