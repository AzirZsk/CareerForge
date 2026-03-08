<!--=====================================================
  简历模块内容容器组件
  @author Azir
=====================================================-->

<template>
  <div class="detail-content">
    <!-- 自定义区块单个 item（从侧边栏选中） -->
    <CustomSection
      v-if="section?.type === 'CUSTOM_ITEM'"
      :content="section.content ?? undefined"
      :is-single-item="true"
    />

    <!-- 基本信息（单条） -->
    <BasicInfoSection
      v-else-if="section?.type === 'BASIC_INFO' && basicContent"
      :content="basicContent"
    />

    <!-- 技能（单条） -->
    <SkillsSection
      v-else-if="section?.type === 'SKILLS'"
      :skills="skillContent"
    />

    <!-- 教育经历列表 -->
    <EducationSection
      v-else-if="section?.type === 'EDUCATION' && section.items?.length"
      :items="section.items"
      @edit-item="(id) => $emit('edit-item', id)"
      @delete-item="(id) => $emit('delete-item', id)"
    />

    <!-- 工作经历列表 -->
    <WorkSection
      v-else-if="section?.type === 'WORK' && section.items?.length"
      :items="section.items"
      @edit-item="(id) => $emit('edit-item', id)"
      @delete-item="(id) => $emit('delete-item', id)"
    />

    <!-- 项目经历列表 -->
    <ProjectSection
      v-else-if="section?.type === 'PROJECT' && section.items?.length"
      :items="section.items"
      @edit-item="(id) => $emit('edit-item', id)"
      @delete-item="(id) => $emit('delete-item', id)"
    />

    <!-- 证书列表 -->
    <CertificateSection
      v-else-if="section?.type === 'CERTIFICATE' && section.items?.length"
      :items="section.items"
      @edit-item="(id) => $emit('edit-item', id)"
      @delete-item="(id) => $emit('delete-item', id)"
    />

    <!-- 开源贡献列表 -->
    <OpenSourceSection
      v-else-if="section?.type === 'OPEN_SOURCE' && section.items?.length"
      :items="section.items"
      @edit-item="(id) => $emit('edit-item', id)"
      @delete-item="(id) => $emit('delete-item', id)"
    />

    <!-- 自定义区块列表 -->
    <CustomSection
      v-else-if="section?.type === 'CUSTOM' && section.items?.length"
      :items="section.items"
      @edit-item="(id) => $emit('edit-item', id)"
      @delete-item="(id) => $emit('delete-item', id)"
    />

    <!-- 聚合类型无数据提示 -->
    <div class="empty-block" v-else-if="isAggregateSection && !section?.items?.length">
      <p class="empty-text">暂无记录</p>
      <button class="add-item-btn" @click="$emit('add-item')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19"></line>
          <line x1="5" y1="12" x2="19" y2="12"></line>
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
import type { ResumeSection, BasicInfoContent, SkillsContent, Skill } from '@/types'

const props = defineProps<{
  section: ResumeSection | undefined
}>()

defineEmits<{
  edit: []
  'edit-item': [itemId: string]
  'add-item': []
  'delete-item': [itemId: string]
}>()

// 判断当前模块是否为聚合类型
const isAggregateSection = computed<boolean>(() => {
  const type = props.section?.type
  return ['EDUCATION', 'WORK', 'PROJECT', 'CERTIFICATE', 'OPEN_SOURCE', 'CUSTOM'].includes(type ?? '')
})

// 基本信息（BASIC_INFO）- 单条类型
const basicContent = computed<BasicInfoContent | null>(() => {
  if (props.section?.type !== 'BASIC_INFO') {
    return null
  }
  return props.section.content as BasicInfoContent
})

// 技能（SKILLS）- 聚合类型，数据在 items[0].content.skills
const skillContent = computed<Skill[]>(() => {
  if (props.section?.type !== 'SKILLS') {
    return []
  }
  const firstItem = props.section.items?.[0]
  const content = firstItem?.content as SkillsContent | null | undefined
  return content?.skills ?? []
})
</script>

<style lang="scss" scoped>
.detail-content {
  margin-bottom: $spacing-xl;
}

// 空状态
.empty-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $spacing-2xl;
  text-align: center;
}

.empty-text {
  font-size: $text-sm;
  color: $color-text-tertiary;
  margin-bottom: $spacing-lg;
}

.add-item-btn {
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-sm $spacing-lg;
  font-size: $text-sm;
  color: $color-accent;
  background: $color-accent-glow;
  border-radius: $radius-md;
  transition: all $transition-fast;
  &:hover {
    background: rgba(212, 168, 83, 0.2);
    transform: translateY(-2px);
  }
}
</style>
