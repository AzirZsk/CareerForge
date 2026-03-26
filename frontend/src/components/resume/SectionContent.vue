<!--=====================================================
  简历模块内容容器组件
  统一从 content 解析数据
  @author Azir
=====================================================-->

<template>
  <div class="detail-content">
    <!-- 自定义区块单个 item（从侧边栏选中） -->
    <CustomSection
      v-if="section?.type === 'CUSTOM_ITEM'"
      :content="section.content ?? undefined"
      :title="section.title"
      :is-single-item="true"
    />

    <!-- 基本信息（单条） -->
    <BasicInfoSection
      v-else-if="section?.type === 'BASIC_INFO' && basicContent"
      :content="basicContent"
    />

    <!-- 技能（SKILLS 类型 content 是数组 [...] -->
    <SkillsSection
      v-else-if="section?.type === 'SKILLS' && skillsList.length"
      :skills="skillsList"
    />

    <!-- 教育经历列表 -->
    <EducationSection
      v-else-if="section?.type === 'EDUCATION' && educationItems.length"
      :items="educationItems"
      @edit-item="(id) => $emit('edit-item', id)"
      @delete-item="(id) => $emit('delete-item', id)"
    />

    <!-- 工作经历列表 -->
    <WorkSection
      v-else-if="section?.type === 'WORK' && workItems.length"
      :items="workItems"
      @edit-item="(id) => $emit('edit-item', id)"
      @delete-item="(id) => $emit('delete-item', id)"
    />

    <!-- 项目经历列表 -->
    <ProjectSection
      v-else-if="section?.type === 'PROJECT' && projectItems.length"
      :items="projectItems"
      @edit-item="(id) => $emit('edit-item', id)"
      @delete-item="(id) => $emit('delete-item', id)"
    />

    <!-- 证书列表 -->
    <CertificateSection
      v-else-if="section?.type === 'CERTIFICATE' && certificateItems.length"
      :items="certificateItems"
      @edit-item="(id) => $emit('edit-item', id)"
      @delete-item="(id) => $emit('delete-item', id)"
    />

    <!-- 开源贡献列表 -->
    <OpenSourceSection
      v-else-if="section?.type === 'OPEN_SOURCE' && openSourceItems.length"
      :items="openSourceItems"
      @edit-item="(id) => $emit('edit-item', id)"
      @delete-item="(id) => $emit('delete-item', id)"
    />

    <!-- 自定义区块列表 -->
    <CustomSection
      v-else-if="section?.type === 'CUSTOM' && customItems.length"
      :items="customItems"
      @edit-item="(index) => $emit('edit-item', index)"
      @delete-item="(index) => $emit('delete-item', index)"
    />

    <!-- 聚合类型无数据提示 -->
    <div
      v-else-if="isAggregateSection && !hasItems"
      class="empty-block"
    >
      <p class="empty-text">
        暂无记录
      </p>
      <button
        class="add-item-btn"
        @click="$emit('add-item')"
      >
        <svg
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <line
            x1="12"
            y1="5"
            x2="12"
            y2="19"
          />
          <line
            x1="5"
            y1="12"
            x2="19"
            y2="12"
          />
        </svg>
        添加第一条记录
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import BasicInfoSection from './sections/BasicInfoSection.vue'
import SkillsSection from './sections/SkillsSection.vue'
import EducationSection from './sections/EducationSection.vue'
import WorkSection from './sections/WorkSection.vue'
import ProjectSection from './sections/ProjectSection.vue'
import CertificateSection from './sections/CertificateSection.vue'
import OpenSourceSection from './sections/OpenSourceSection.vue'
import CustomSection from './sections/CustomSection.vue'
import type { ResumeSection, BasicInfoContent, Skill } from '@/types'
import { useSectionHelper } from '@/composables/useSectionHelper'

const props = defineProps<{
  section: ResumeSection | undefined
}>()

defineEmits<{
  edit: []
  'edit-item': [index: number]
  'add-item': []
  'delete-item': [index: number]
}>()

const {
  parseContent,
  getWorkItems,
  getProjectItems,
  getEducationItems,
  getSkillsList,
  getCertificateItems,
  getOpenSourceItems,
  getCustomItems,
  isAggregateType
} = useSectionHelper()

// 判断当前模块是否为聚合类型
const isAggregateSection = computed<boolean>(() => {
  const type = props.section?.type
  return isAggregateType(type ?? '')
})

// 基本信息（BASIC_INFO）- 单条类型，需要解析 JSON 字符串
const basicContent = computed<BasicInfoContent | null>(() => {
  if (props.section?.type !== 'BASIC_INFO') {
    return null
  }
  return parseContent<BasicInfoContent>(props.section.content)
})

// 技能（SKILLS）- content 存储的是 { skills: [...] }
const skillsList = computed<Skill[]>(() => {
  if (props.section?.type !== 'SKILLS') {
    return []
  }
  return getSkillsList(props.section)
})

// 工作经历列表
const workItems = computed(() => {
  if (props.section?.type !== 'WORK') {
    return []
  }
  return getWorkItems(props.section)
})

// 项目经历列表
const projectItems = computed(() => {
  if (props.section?.type !== 'PROJECT') {
    return []
  }
  return getProjectItems(props.section)
})

// 教育经历列表
const educationItems = computed(() => {
  if (props.section?.type !== 'EDUCATION') {
    return []
  }
  return getEducationItems(props.section)
})

// 证书列表
const certificateItems = computed(() => {
  if (props.section?.type !== 'CERTIFICATE') {
    return []
  }
  return getCertificateItems(props.section)
})

// 开源贡献列表
const openSourceItems = computed(() => {
  if (props.section?.type !== 'OPEN_SOURCE') {
    return []
  }
  return getOpenSourceItems(props.section)
})

// 自定义区块列表
const customItems = computed(() => {
  if (props.section?.type !== 'CUSTOM') {
    return []
  }
  return getCustomItems(props.section)
})

// 是否有任何项
const hasItems = computed(() => {
  return (
    workItems.value.length > 0 ||
    projectItems.value.length > 0 ||
    educationItems.value.length > 0 ||
    skillsList.value.length > 0 ||
    certificateItems.value.length > 0 ||
    openSourceItems.value.length > 0 ||
    customItems.value.length > 0
  )
})
</script>

