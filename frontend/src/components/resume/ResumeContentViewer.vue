`<!--=====================================================
  LandIt 简历内容查看器组件
  展示简历的具体内容（用于对比视图）
  统一从 content 解析数据
  @author Azir
=====================================================-->

<template>
  <div class="resume-content-viewer">
    <div v-if="isEmpty" class="empty-state">
      <p>暂无简历内容</p>
    </div>

    <div v-else class="resume-content">
      <template v-for="(section, sectionIdx) in sections" :key="section.id">
        <!-- 基本信息 -->
        <div v-if="section.type === 'BASIC_INFO'" class="resume-section basic-info-section" :class="{ editable: showEdit }">
          <div class="section-header">
            <h3><span class="section-title-text" :class="getTitleChangeClass(section.type)">{{ section.title }}</span></h3>
            <button v-if="showEdit" class="section-edit-btn" @click="handleEditSection(sectionIdx)" title="编辑">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
              </svg>
            </button>
          </div>
          <div class="info-grid">
            <template v-for="{ key, value } in getOrderedBasicInfoFields(parseContent(section.content))" :key="key">
              <div
                class="info-item"
                :class="[
                  getChangeClass(section.type, key),
                  { 'full-width': key === 'summary' }
                ]"
              >
                <span class="info-label">{{ getFieldLabel(key) }}</span>
                <span class="info-value" :class="{ 'summary-text': key === 'summary' }">{{ value }}</span>
              </div>
            </template>
          </div>
        </div>

        <!-- 教育经历 -->
        <div v-else-if="section.type === 'EDUCATION'" class="resume-section" :class="{ editable: showEdit }">
          <h3><span class="section-title-text" :class="getTitleChangeClass(section.type)">{{ section.title }}</span></h3>
          <div v-for="(item, idx) in getEducationList(section)" :key="idx" class="experience-item" :class="{ 'has-edit-btn': showEdit }">
            <button v-if="showEdit" class="item-edit-btn" @click="handleEditSection(sectionIdx, idx)" title="编辑">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
              </svg>
            </button>
            <div class="exp-header">
              <span class="exp-title" :class="getChangeClass(section.type, 'school', idx)">{{ item.school }}</span>
              <span class="exp-period" :class="getChangeClass(section.type, 'period', idx)">{{ item.period }}</span>
            </div>
            <div class="exp-meta">
              <span class="exp-degree" v-if="item.degree" :class="getChangeClass(section.type, 'degree', idx)">{{ item.degree }}</span>
              <span class="exp-major" v-if="item.major" :class="getChangeClass(section.type, 'major', idx)">{{ item.major }}</span>
              <span class="exp-gpa" v-if="item.gpa" :class="getChangeClass(section.type, 'gpa', idx)">GPA: {{ item.gpa }}</span>
            </div>
            <div v-if="item.courses?.length" class="exp-courses" :class="getChangeClass(section.type, 'courses', idx)">
              <span v-for="course in item.courses" :key="course" class="course-tag">{{ course }}</span>
            </div>
            <div v-if="item.honors?.length" class="exp-honors" :class="getChangeClass(section.type, 'honors', idx)">
              <span v-for="honor in item.honors" :key="honor" class="honor-tag">{{ honor }}</span>
            </div>
          </div>
        </div>

        <!-- 工作经历 -->
        <div v-else-if="section.type === 'WORK'" class="resume-section" :class="{ editable: showEdit }">
          <h3><span class="section-title-text" :class="getTitleChangeClass(section.type)">{{ section.title }}</span></h3>
          <div v-for="(item, idx) in getWorkList(section)" :key="idx" class="experience-item" :class="{ 'has-edit-btn': showEdit }">
            <button v-if="showEdit" class="item-edit-btn" @click="handleEditSection(sectionIdx, idx)" title="编辑">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
              </svg>
            </button>
            <div class="exp-header">
              <span class="exp-title" :class="getChangeClass(section.type, 'company', idx)">{{ item.company }}</span>
              <span class="exp-period" :class="getChangeClass(section.type, 'period', idx)">{{ item.period }}</span>
            </div>
            <div class="exp-meta">
              <span class="exp-position" v-if="item.position" :class="getChangeClass(section.type, 'position', idx)">{{ item.position }}</span>
              <span class="exp-location" v-if="item.location" :class="getChangeClass(section.type, 'location', idx)">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                  <circle cx="12" cy="10" r="3"></circle>
                </svg>
                {{ item.location }}
              </span>
              <span class="exp-industry" v-if="item.industry">{{ item.industry }}</span>
            </div>
            <p v-if="item.description" class="exp-description" :class="getChangeClass(section.type, 'description', idx)">
              {{ item.description }}
            </p>
            <!-- 工作经历新增的 achievements 字段 -->
            <div v-if="item.achievements?.length" class="exp-achievements" :class="getChangeClass(section.type, 'achievements', idx)">
              <div
                v-for="(ach, achIdx) in item.achievements"
                :key="achIdx"
                class="achievement-item"
              >
                {{ ach }}
              </div>
            </div>
            <div v-if="item.technologies?.length" class="exp-technologies" :class="getChangeClass(section.type, 'technologies', idx)">
              <span v-for="tech in item.technologies" :key="tech" class="tech-tag">{{ tech }}</span>
            </div>
            <div v-if="item.products?.length" class="exp-products" :class="getChangeClass(section.type, 'products', idx)">
              <span class="products-label">代表产品:</span>
              <span v-for="p in item.products" :key="p" class="product-tag">{{ p }}</span>
            </div>
          </div>
        </div>

        <!-- 项目经历 -->
        <div v-else-if="section.type === 'PROJECT'" class="resume-section" :class="{ editable: showEdit }">
          <h3><span class="section-title-text" :class="getTitleChangeClass(section.type)">{{ section.title }}</span></h3>
          <div v-for="(item, idx) in getProjectList(section)" :key="idx" class="experience-item" :class="{ 'has-edit-btn': showEdit }">
            <button v-if="showEdit" class="item-edit-btn" @click="handleEditSection(sectionIdx, idx)" title="编辑">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
              </svg>
            </button>
            <div class="exp-header">
              <span class="exp-title" :class="getChangeClass(section.type, 'name', idx)">
                {{ item.name }}
                <a v-if="item.url" :href="item.url" target="_blank" class="exp-link" title="访问项目">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path>
                    <polyline points="15 3 21 3 21 9"></polyline>
                    <line x1="10" y1="14" x2="21" y2="3"></line>
                  </svg>
                </a>
              </span>
              <span class="exp-period" :class="getChangeClass(section.type, 'period', idx)">{{ item.period }}</span>
            </div>
            <div class="exp-role" :class="getChangeClass(section.type, 'role', idx)">{{ item.role }}</div>
            <p v-if="item.description" class="exp-description" :class="getChangeClass(section.type, 'description', idx)">
              {{ item.description }}
            </p>
            <div v-if="item.technologies?.length" class="exp-technologies" :class="getChangeClass(section.type, 'technologies', idx)">
              <span v-for="tech in item.technologies" :key="tech" class="tech-tag">{{ tech }}</span>
            </div>
            <div v-if="item.achievements?.length" class="exp-achievements" :class="getChangeClass(section.type, 'achievements', idx)">
              <div
                v-for="(ach, achIdx) in item.achievements"
                :key="achIdx"
                class="achievement-item"
              >
                {{ ach }}
              </div>
            </div>
          </div>
        </div>

        <!-- 专业技能 -->
        <div v-else-if="section.type === 'SKILLS'" class="resume-section" :class="{ editable: showEdit }">
          <h3><span class="section-title-text" :class="getTitleChangeClass(section.type)">{{ section.title }}</span></h3>
          <div class="skills-container">
            <div
              v-for="(skill, idx) in getSkillsList(section)"
              :key="idx"
              class="skill-item"
              :class="[getChangeClass(section.type, '', idx), { 'has-edit-btn': showEdit }]"
            >
              <button v-if="showEdit" class="item-edit-btn" @click="handleEditSection(sectionIdx, idx)" title="编辑">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                </svg>
              </button>
              <div class="skill-header">
                <span class="skill-name">{{ skill.name }}</span>
                <span v-if="skill.level" class="skill-level">{{ skill.level }}</span>
              </div>
              <p v-if="skill.description" class="skill-description">{{ skill.description }}</p>
              <span v-if="skill.category" class="skill-category">{{ skill.category }}</span>
            </div>
          </div>
        </div>

        <!-- 证书荣誉 -->
        <div v-else-if="section.type === 'CERTIFICATE'" class="resume-section" :class="{ editable: showEdit }">
          <h3><span class="section-title-text" :class="getTitleChangeClass(section.type)">{{ section.title }}</span></h3>
          <div v-for="(item, idx) in getCertificateList(section)" :key="idx" class="certificate-item" :class="{ 'has-edit-btn': showEdit }">
            <button v-if="showEdit" class="item-edit-btn" @click="handleEditSection(sectionIdx, idx)" title="编辑">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
              </svg>
            </button>
            <div class="cert-header">
              <span class="cert-name" :class="getChangeClass(section.type, 'name', idx)">
                {{ item.name }}
                <a v-if="item.url" :href="item.url" target="_blank" class="cert-link" title="查看证书">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path>
                    <polyline points="15 3 21 3 21 9"></polyline>
                    <line x1="10" y1="14" x2="21" y2="3"></line>
                  </svg>
                </a>
              </span>
              <span v-if="item.date" class="cert-date">{{ item.date }}</span>
            </div>
            <div class="cert-meta" v-if="item.issuer || item.credentialId">
              <span class="cert-issuer" v-if="item.issuer">{{ item.issuer }}</span>
              <span class="cert-credential" v-if="item.credentialId">编号: {{ item.credentialId }}</span>
            </div>
          </div>
        </div>

        <!-- 开源贡献 -->
        <div v-else-if="section.type === 'OPEN_SOURCE'" class="resume-section" :class="{ editable: showEdit }">
          <h3><span class="section-title-text" :class="getTitleChangeClass(section.type)">{{ section.title }}</span></h3>
          <div v-for="(item, idx) in getOpenSourceList(section)" :key="idx" class="experience-item" :class="{ 'has-edit-btn': showEdit }">
            <button v-if="showEdit" class="item-edit-btn" @click="handleEditSection(sectionIdx, idx)" title="编辑">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
              </svg>
            </button>
            <div class="exp-header">
              <span class="exp-title">
                {{ item.projectName }}
                <a v-if="item.url" :href="item.url" target="_blank" class="exp-link" title="访问项目">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path>
                    <polyline points="15 3 21 3 21 9"></polyline>
                    <line x1="10" y1="14" x2="21" y2="3"></line>
                  </svg>
                </a>
              </span>
              <span class="exp-period" v-if="item.period">{{ item.period }}</span>
            </div>
            <p class="exp-position" v-if="item.role">{{ item.role }}</p>
            <p v-if="item.description" class="exp-description">
              {{ item.description }}
            </p>
            <div v-if="item.achievements?.length" class="exp-achievements">
              <span v-for="a in item.achievements" :key="a" class="achievement-tag">{{ a }}</span>
            </div>
          </div>
        </div>

        <!-- 自定义区块（扁平结构：ContentItem[]） -->
        <div v-else-if="section.type === 'CUSTOM'" class="resume-section" :class="{ editable: showEdit }">
          <h3><span class="section-title-text" :class="getTitleChangeClass(section.type)">{{ section.title }}</span></h3>
          <div v-for="(item, itemIdx) in getCustomList(section)" :key="itemIdx" class="experience-item" :class="{ 'has-edit-btn': showEdit }">
            <button v-if="showEdit" class="item-edit-btn" @click="handleEditSection(sectionIdx, itemIdx)" title="编辑">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
              </svg>
            </button>
            <div class="exp-header">
              <span class="exp-title" :class="getChangeClass(section.type, 'name', itemIdx)">{{ item.name }}</span>
              <span v-if="item.period" class="exp-period" :class="getChangeClass(section.type, 'period', itemIdx)">{{ item.period }}</span>
            </div>
            <div v-if="item.role" class="exp-role" :class="getChangeClass(section.type, 'role', itemIdx)">{{ item.role }}</div>
            <p v-if="item.description" class="exp-description" :class="getChangeClass(section.type, 'description', itemIdx)">{{ item.description }}</p>
            <div v-if="item.highlights?.length" class="exp-achievements" :class="getChangeClass(section.type, 'highlights', itemIdx)">
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

        <!-- 默认渲染 -->
        <div v-else class="resume-section">
          <h3>{{ section.title }}</h3>
          <pre class="raw-content">{{ JSON.stringify(section, null, 2) }}</pre>
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

const {
  parseContent,
  getWorkList,
  getProjectList,
  getEducationList,
  getSkillsList,
  getCertificateList,
  getOpenSourceList,
  getCustomList,
  getFieldLabel,
  getOrderedBasicInfoFields
} = useSectionHelper()

// 初始化差异对比
const { titleClass: sectionTitleClass, fieldClass, itemFieldClass } = useSectionDiff(
  toRef(props, 'beforeSections'),
  toRef(props, 'afterSections')
)

// 是否显示编辑功能（仅 after 侧且 editable 为 true）
const showEdit = computed(() => props.editable && props.side === 'after')

const isEmpty = computed(() => !props.sections || props.sections.length === 0)

// 处理编辑区块
function handleEditSection(sectionIndex: number, itemIndex?: number) {
  if (!showEdit.value || !props.sections[sectionIndex]) return

  emit('edit', {
    sectionIndex,
    section: props.sections[sectionIndex],
    itemIndex
  })
}

/**
 * 获取高亮 class（统一入口）
 * @param sectionType 区块类型，如 'BASIC_INFO', 'WORK'
 * @param fieldKey 字段名，如 'name', 'summary'
 * @param itemIndex 数组索引（聚合类型时使用）
 */
function getChangeClass(sectionType: string, fieldKey: string, itemIndex?: number): string {
  if (!props.beforeSections || !props.afterSections) return ''
  if (itemIndex !== undefined) {
    return itemFieldClass(sectionType, itemIndex, fieldKey || undefined, props.side)
  }
  return fieldClass(sectionType, fieldKey, props.side)
}

/**
 * 获取区块标题的高亮 class
 * @param sectionType 区块类型
 */
function getTitleChangeClass(sectionType: string): string {
  if (!props.beforeSections || !props.afterSections) return ''
  return sectionTitleClass(sectionType, props.side)
}
</script>

<style lang="scss" scoped>
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
  gap: $spacing-lg;
}

.resume-section {
  h3 {
    font-size: $text-xl;
    font-weight: $weight-semibold;
    color: $color-text-primary;
    margin-bottom: $spacing-md;
    padding-bottom: $spacing-sm;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  // 区块头部（带编辑按钮）
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-md;
    padding-bottom: $spacing-sm;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);

    h3 {
      margin-bottom: 0;
      padding-bottom: 0;
      border-bottom: none;
    }
  }

  // 区块编辑按钮（基本信息）
  .section-edit-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    border-radius: $radius-sm;
    color: $color-text-tertiary;
    background: rgba(255, 255, 255, 0.05);
    transition: all $transition-fast;

    &:hover {
      background: rgba(212, 168, 83, 0.15);
      color: $color-accent;
    }
  }
}

// 可编辑状态下的区块
.resume-section.editable {
  .experience-item,
  .skill-item,
  .certificate-item {
    position: relative;
  }
}

// 带编辑按钮的项目
.experience-item.has-edit-btn,
.skill-item.has-edit-btn,
.certificate-item.has-edit-btn {
  position: relative;
  padding-right: $spacing-xl;

  &:hover {
    .item-edit-btn {
      opacity: 1;
    }
  }
}

// 项目编辑按钮（聚合类型）
.item-edit-btn {
  position: absolute;
  top: $spacing-sm;
  right: $spacing-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: $radius-sm;
  color: $color-text-tertiary;
  background: rgba(255, 255, 255, 0.05);
  opacity: 0;
  transition: all $transition-fast;
  z-index: 1;

  &:hover {
    background: rgba(212, 168, 83, 0.15);
    color: $color-accent;
    opacity: 1;
  }
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-md;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;

  &.full-width {
    grid-column: span 2;
  }
}

.info-label {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.info-value {
  font-size: $text-sm;
  color: $color-text-primary;
  font-weight: $weight-medium;
  padding: 2px 4px;
  border-radius: $radius-sm;
  transition: background-color 0.2s;

  &.summary-text {
    font-weight: $weight-regular;
    line-height: $leading-relaxed;
    white-space: pre-wrap;
    padding: $spacing-sm;
    background: rgba(255, 255, 255, 0.02);
  }
}

.extra-field {
  margin-top: $spacing-md;
  padding-top: $spacing-md;
  border-top: 1px dashed rgba(255, 255, 255, 0.1);
}

.extra-label {
  display: block;
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-bottom: $spacing-xs;
}

.extra-value {
  font-size: $text-sm;
  color: $color-text-primary;
  line-height: $leading-relaxed;
  padding: $spacing-sm;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-sm;
  white-space: pre-wrap;

  &.description {
    white-space: pre-wrap;
  }
}

.experience-item {
  padding: $spacing-md 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);

  &:last-child {
    border-bottom: none;
  }
}

.exp-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xs;
}

.exp-title {
  font-size: $text-base;
  font-weight: $weight-medium;
  color: $color-text-primary;
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.exp-link {
  display: inline-flex;
  align-items: center;
  color: $color-text-tertiary;
  transition: color $transition-fast;

  &:hover {
    color: $color-accent;
  }
}

.exp-period {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.exp-details {
  font-size: $text-sm;
  color: $color-text-secondary;
}

.exp-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: $spacing-xs;
  margin-bottom: $spacing-sm;
}

.exp-degree {
  font-size: $text-sm;
  color: $color-accent;
}

.exp-major {
  font-size: $text-sm;
  color: $color-text-secondary;

  &::before {
    content: '·';
    margin-right: $spacing-xs;
    color: $color-text-tertiary;
  }
}

.exp-gpa {
  font-size: $text-xs;
  color: $color-success;
  background: $color-success-bg;
  padding: 2px $spacing-sm;
  border-radius: $radius-sm;
}

.exp-courses {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
  margin-bottom: $spacing-sm;
}

.course-tag {
  padding: $spacing-xs $spacing-sm;
  background: rgba(96, 165, 250, 0.1);
  color: $color-info;
  font-size: $text-xs;
  border-radius: $radius-sm;
}

.exp-honors {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.honor-tag {
  padding: $spacing-xs $spacing-sm;
  background: rgba(212, 168, 83, 0.12);
  color: $color-accent;
  font-size: $text-xs;
  border-radius: $radius-sm;
  width: fit-content;
}

.exp-role {
  font-size: $text-sm;
  color: $color-accent;
  margin-bottom: $spacing-sm;
}

.exp-position {
  font-size: $text-sm;
  color: $color-accent;
  margin-bottom: $spacing-sm;
}

.exp-location {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.exp-industry {
  font-size: $text-xs;
  color: $color-text-tertiary;
  padding: 2px $spacing-sm;
  background: rgba(255, 255, 255, 0.05);
  border-radius: $radius-sm;
}

.exp-technologies {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
  margin-bottom: $spacing-sm;
}

.tech-tag {
  padding: $spacing-xs $spacing-sm;
  background: rgba(96, 165, 250, 0.1);
  color: $color-info;
  font-size: $text-xs;
  border-radius: $radius-sm;
}

.exp-products {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: $spacing-xs;
}

.products-label {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.product-tag {
  padding: $spacing-xs $spacing-sm;
  background: rgba(212, 168, 83, 0.15);
  color: $color-accent;
  font-size: $text-xs;
  border-radius: $radius-sm;
}

.exp-description {
  font-size: $text-sm;
  color: $color-text-secondary;
  line-height: $leading-relaxed;
  margin-bottom: $spacing-sm;
  white-space: pre-wrap;
  padding: 2px 4px;
  border-radius: $radius-sm;
  transition: background-color 0.2s;
}

.exp-achievements {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
  margin-top: $spacing-sm;

  &.extra {
    margin-top: $spacing-md;
    padding-top: $spacing-sm;
    border-top: 1px dashed rgba(52, 211, 153, 0.2);
  }
}

.achievement-tag {
  width: fit-content;
  padding: $spacing-xs $spacing-sm;
  background: $color-success-bg;
  color: $color-success;
  font-size: $text-xs;
  border-radius: $radius-sm;
}

.achievement-item {
  font-size: $text-sm;
  color: $color-text-secondary;
  padding-left: $spacing-md;
  position: relative;

  &::before {
    content: '•';
    position: absolute;
    left: 0;
    color: $color-accent;
  }

  &.highlight {
    background: rgba(52, 211, 153, 0.1);
    border-radius: $radius-sm;
    padding: $spacing-sm $spacing-md;
    margin-left: -$spacing-md;
    padding-left: $spacing-md;

    &::before {
      content: '★';
      color: $color-success;
    }
  }
}

.skills-container {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.skill-item {
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.2s;
  &:hover {
    border-color: rgba(255, 255, 255, 0.1);
  }
}

.skill-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-xs;
}

.skill-name {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.skill-level {
  padding: 2px $spacing-sm;
  font-size: $text-xs;
  background: rgba(212, 168, 83, 0.15);
  color: $color-accent;
  border-radius: $radius-sm;
}

.skill-description {
  font-size: $text-sm;
  color: $color-text-secondary;
  line-height: $leading-relaxed;
  margin-bottom: $spacing-xs;
}

.skill-category {
  display: inline-block;
  padding: 2px $spacing-xs;
  font-size: $text-xs;
  background: rgba(255, 255, 255, 0.05);
  color: $color-text-tertiary;
  border-radius: $radius-sm;
}

.certificate-item {
  padding: $spacing-md 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);

  &:last-child {
    border-bottom: none;
  }
}

.cert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cert-name {
  font-size: $text-base;
  font-weight: $weight-medium;
  color: $color-text-primary;
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: 2px 4px;
  border-radius: $radius-sm;
  transition: background-color 0.2s;
}

.cert-link {
  display: inline-flex;
  align-items: center;
  color: $color-text-tertiary;
  transition: color $transition-fast;

  &:hover {
    color: $color-accent;
  }
}

.cert-date {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.cert-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: $spacing-sm;
  margin-top: $spacing-xs;
}

.cert-issuer {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.cert-credential {
  font-size: $text-xs;
  color: $color-text-tertiary;
  background: rgba(255, 255, 255, 0.05);
  padding: 2px $spacing-sm;
  border-radius: $radius-sm;
}

.raw-content {
  font-size: $text-xs;
  color: $color-text-tertiary;
  background: rgba(0, 0, 0, 0.2);
  padding: $spacing-sm;
  border-radius: $radius-sm;
  overflow-x: auto;
  max-height: 200px;
}

// 高亮样式
.highlight-added {
  background: rgba(52, 211, 153, 0.15);
  color: $color-success;
}

.highlight-removed {
  background: rgba(248, 113, 113, 0.15);
  color: $color-error;
}

// 确保高亮样式能覆盖各元素的默认颜色
.exp-title, .exp-period, .exp-position, .exp-location,
.exp-degree, .exp-major, .exp-gpa, .exp-role,
.exp-description, .cert-name,
.exp-courses, .exp-honors, .exp-technologies, .exp-products {
  &.highlight-added {
    background: rgba(52, 211, 153, 0.15);
    color: $color-success;
    border-radius: $radius-sm;
    padding: 2px 4px;
  }

  &.highlight-removed {
    background: rgba(248, 113, 113, 0.15);
    color: $color-error;
    border-radius: $radius-sm;
    padding: 2px 4px;
  }
}

// 区块标题高亮（自定义区块标题变更时）
.section-title-text {
  border-radius: $radius-sm;
  padding: 2px 4px;
  transition: background-color 0.2s;

  &.highlight-added {
    background: rgba(52, 211, 153, 0.15);
    color: $color-success;
  }

  &.highlight-removed {
    background: rgba(248, 113, 113, 0.15);
    color: $color-error;
  }
}

// 自定义区块样式
.custom-block {
  margin-bottom: $spacing-md;

  &:last-child {
    margin-bottom: 0;
  }
}

.custom-block-title {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-accent;
  margin-bottom: $spacing-sm;
  padding-bottom: $spacing-xs;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.1);
}

// 基本信息区块特殊样式
.basic-info-section {
  .info-grid {
    grid-template-columns: repeat(2, 1fr);

    @media (max-width: 600px) {
      grid-template-columns: 1fr;
    }
  }

  .info-item {
    padding: $spacing-xs 0;

    &.highlight-added .info-value {
      background: rgba(52, 211, 153, 0.15);
      color: $color-success;
    }

    &.highlight-removed .info-value {
      background: rgba(248, 113, 113, 0.15);
      color: $color-error;
    }
  }

  .info-value {
    word-break: break-word;
  }
}
</style>
