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
      <template v-for="section in sections" :key="section.id">
        <!-- 基本信息 -->
        <div v-if="section.type === 'BASIC_INFO'" class="resume-section basic-info-section">
          <h3>{{ section.title }}</h3>
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
        <div v-else-if="section.type === 'EDUCATION'" class="resume-section">
          <h3>{{ section.title }}</h3>
          <div v-for="(item, idx) in getEducationList(section)" :key="idx" class="experience-item">
            <div class="exp-header">
              <span class="exp-title">{{ item.school }}</span>
              <span class="exp-period">{{ item.period }}</span>
            </div>
            <div class="exp-meta">
              <span class="exp-degree" v-if="item.degree">{{ item.degree }}</span>
              <span class="exp-major" v-if="item.major">{{ item.major }}</span>
              <span class="exp-gpa" v-if="item.gpa">GPA: {{ item.gpa }}</span>
            </div>
            <div v-if="item.courses?.length" class="exp-courses">
              <span v-for="course in item.courses" :key="course" class="course-tag">{{ course }}</span>
            </div>
            <div v-if="item.honors?.length" class="exp-honors">
              <span v-for="honor in item.honors" :key="honor" class="honor-tag">{{ honor }}</span>
            </div>
          </div>
        </div>

        <!-- 工作经历 -->
        <div v-else-if="section.type === 'WORK'" class="resume-section">
          <h3>{{ section.title }}</h3>
          <div v-for="(item, idx) in getWorkList(section)" :key="idx" class="experience-item">
            <div class="exp-header">
              <span class="exp-title">{{ item.company }}</span>
              <span class="exp-period">{{ item.period }}</span>
            </div>
            <div class="exp-meta">
              <span class="exp-position" v-if="item.position">{{ item.position }}</span>
              <span class="exp-location" v-if="item.location">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                  <circle cx="12" cy="10" r="3"></circle>
                </svg>
                {{ item.location }}
              </span>
              <span class="exp-industry" v-if="item.industry">{{ item.industry }}</span>
            </div>
            <p v-if="item.description" class="exp-description" :class="getChangeClass(section.type, 'work[' + idx + '].description')">
              {{ item.description }}
            </p>
            <!-- 工作经历新增的 achievements 字段 -->
            <div v-if="item.achievements?.length" class="exp-achievements">
              <div
                v-for="(ach, achIdx) in item.achievements"
                :key="achIdx"
                class="achievement-item"
                :class="getChangeClass(section.type, 'work[' + idx + '].achievements[' + achIdx + ']')"
              >
                {{ ach }}
              </div>
            </div>
            <div v-if="item.technologies?.length" class="exp-technologies">
              <span v-for="tech in item.technologies" :key="tech" class="tech-tag">{{ tech }}</span>
            </div>
            <div v-if="item.products?.length" class="exp-products">
              <span class="products-label">代表产品:</span>
              <span v-for="p in item.products" :key="p" class="product-tag">{{ p }}</span>
            </div>
          </div>
        </div>

        <!-- 项目经历 -->
        <div v-else-if="section.type === 'PROJECT'" class="resume-section">
          <h3>{{ section.title }}</h3>
          <div v-for="(item, idx) in getProjectList(section)" :key="idx" class="experience-item">
            <div class="exp-header">
              <span class="exp-title">
                {{ item.name }}
                <a v-if="item.url" :href="item.url" target="_blank" class="exp-link" title="访问项目">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path>
                    <polyline points="15 3 21 3 21 9"></polyline>
                    <line x1="10" y1="14" x2="21" y2="3"></line>
                  </svg>
                </a>
              </span>
              <span class="exp-period">{{ item.period }}</span>
            </div>
            <div class="exp-role">{{ item.role }}</div>
            <p v-if="item.description" class="exp-description">
              {{ item.description }}
            </p>
            <div v-if="item.technologies?.length" class="exp-technologies">
              <span v-for="tech in item.technologies" :key="tech" class="tech-tag">{{ tech }}</span>
            </div>
            <div v-if="item.achievements?.length" class="exp-achievements">
              <div
                v-for="(ach, achIdx) in item.achievements"
                :key="achIdx"
                class="achievement-item"
                :class="getChangeClass(section.type, 'project[' + idx + '].achievements[' + achIdx + ']')"
              >
                {{ ach }}
              </div>
            </div>
          </div>
        </div>

        <!-- 专业技能 -->
        <div v-else-if="section.type === 'SKILLS'" class="resume-section">
          <h3>{{ section.title }}</h3>
          <div class="skills-container">
            <div
              v-for="(skill, idx) in getSkillsList(section)"
              :key="idx"
              class="skill-item"
              :class="getChangeClass(section.type, 'skills[' + idx + ']')"
            >
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
        <div v-else-if="section.type === 'CERTIFICATE'" class="resume-section">
          <h3>{{ section.title }}</h3>
          <div v-for="(item, idx) in getCertificateList(section)" :key="idx" class="certificate-item">
            <div class="cert-header">
              <span class="cert-name" :class="getChangeClass(section.type, 'certificate[' + idx + '].name')">
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
        <div v-else-if="section.type === 'OPEN_SOURCE'" class="resume-section">
          <h3>{{ section.title }}</h3>
          <div v-for="(item, idx) in getOpenSourceList(section)" :key="idx" class="experience-item">
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
        <div v-else-if="section.type === 'CUSTOM'" class="resume-section">
          <h3>{{ section.title }}</h3>
          <div v-for="(item, itemIdx) in getCustomList(section)" :key="itemIdx" class="experience-item">
            <div class="exp-header">
              <span class="exp-title">{{ item.name }}</span>
              <span v-if="item.period" class="exp-period">{{ item.period }}</span>
            </div>
            <div v-if="item.role" class="exp-role">{{ item.role }}</div>
            <p v-if="item.description" class="exp-description">{{ item.description }}</p>
            <div v-if="item.highlights?.length" class="exp-achievements">
              <div
                v-for="(highlight, hIdx) in item.highlights"
                :key="hIdx"
                class="achievement-item"
                :class="getChangeClass(section.type, `custom[${itemIdx}].highlights[${hIdx}]`)"
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
import { computed } from 'vue'
import type { ResumeSection } from '@/types'
import type { ChangeItem } from '@/types/resume-optimize'
import { useSectionHelper } from '@/composables/useSectionHelper'

interface Props {
  sections: ResumeSection[]
  side: 'before' | 'after'
  changes?: ChangeItem[]
}

const props = defineProps<Props>()

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

const isEmpty = computed(() => !props.sections || props.sections.length === 0)

// 区块类型到驼峰命名的映射（与后端 AI 生成的路径格式一致）
const SECTION_TYPE_TO_CAMEL: Record<string, string> = {
  BASIC_INFO: 'basicInfo',
  EDUCATION: 'education',
  WORK: 'work',
  PROJECT: 'projects',      // 注意：项目是复数形式
  SKILLS: 'skills',
  CERTIFICATE: 'certificates',
  OPEN_SOURCE: 'openSource',
  CUSTOM: 'customSections'
}

// 判断字段是否有变更
function getChangeClass(sectionType: string, fieldPath: string): string {
  if (!props.changes?.length) return ''

  // 使用驼峰命名的区块前缀（与后端 AI 路径格式一致）
  const typePrefix = SECTION_TYPE_TO_CAMEL[sectionType] || sectionType.toLowerCase()

  // 判断 fieldPath 是否已经包含区块前缀（如 work[0].description）
  // 如果包含数组索引格式 [n]，说明已经是完整路径
  let fullPath: string
  if (fieldPath.includes('[')) {
    // 已经是完整路径，但可能需要修正区块名称
    // 例如：project[0] -> projects[0]
    fullPath = fieldPath
  } else {
    // 只是字段名，需要添加区块前缀
    fullPath = `${typePrefix}.${fieldPath}`
  }

  // 检查是否有匹配的变更
  const hasChange = props.changes.some(change => {
    if (!change.field) return false

    // 精确匹配
    if (change.field === fullPath || change.field === fieldPath) return true

    // 尝试修正路径格式后匹配（如 project -> projects）
    const correctedPath = fullPath.replace(/^project\[/, 'projects[')
    if (change.field === correctedPath) return true

    // 处理不同的路径格式（统一转为小写比较）
    const normalizedChangeField = change.field.toLowerCase()
    const normalizedFullPath = fullPath.toLowerCase()
    const normalizedCorrectedPath = correctedPath.toLowerCase()

    return normalizedChangeField === normalizedFullPath ||
           normalizedChangeField === normalizedCorrectedPath ||
           normalizedChangeField.endsWith('.' + normalizedFullPath)
  })

  if (!hasChange) return ''

  // 根据侧边返回不同的高亮类
  if (props.side === 'after') {
    return 'highlight-added'
  } else {
    return 'highlight-removed'
  }
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
    font-size: $text-base;
    font-weight: $weight-semibold;
    color: $color-text-primary;
    margin-bottom: $spacing-md;
    padding-bottom: $spacing-sm;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
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
  background: $color-success-bg;
  color: $color-success;
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
