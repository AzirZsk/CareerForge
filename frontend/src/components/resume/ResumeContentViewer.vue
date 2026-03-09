`<!--=====================================================
  LandIt 简历内容查看器组件
  展示简历的具体内容（用于对比视图）
  支持新的区块数组格式
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
        <div v-if="section.type === 'BASIC_INFO'" class="resume-section">
          <h3>{{ section.title }}</h3>
          <div class="info-grid">
            <div v-for="{ key, value } in getOrderedBasicInfoFields(parseContent(section.content))" :key="key" class="info-item">
              <span class="info-label">{{ getFieldLabel(key) }}</span>
              <span class="info-value" :class="getChangeClass(section.type, key)">
                {{ formatValue(value) }}
              </span>
            </div>
          </div>
          <!-- 新增的扩展字段 -->
          <div v-if="section.description" class="extra-field">
            <span class="extra-label">工作描述</span>
            <p class="extra-value description" :class="getChangeClass(section.type, 'description')">
              {{ section.description }}
            </p>
          </div>
          <div v-if="section.awards" class="extra-field">
            <span class="extra-label">荣誉奖项</span>
            <p class="extra-value" :class="getChangeClass(section.type, 'awards')">
              {{ section.awards }}
            </p>
          </div>
          <div v-if="section.achievements" class="extra-field">
            <span class="extra-label">主要成就</span>
            <p class="extra-value" :class="getChangeClass(section.type, 'achievements')">
              {{ section.achievements }}
            </p>
          </div>
        </div>

        <!-- 教育经历 -->
        <div v-else-if="section.type === 'EDUCATION'" class="resume-section">
          <h3>{{ section.title }}</h3>
          <div v-for="(item, idx) in section.items" :key="item.id" class="experience-item">
            <div class="exp-header">
              <span class="exp-title">{{ parseContent(item.content)?.school }}</span>
              <span class="exp-period">{{ parseContent(item.content)?.period }}</span>
            </div>
            <div class="exp-details">
              {{ parseContent(item.content)?.degree }}<span v-if="parseContent(item.content)?.major"> · {{ parseContent(item.content)?.major }}</span>
            </div>
            <!-- 教育经历新增的 achievements 字段 -->
            <div v-if="item.achievements" class="exp-achievements">
              <div class="achievement-item" :class="getChangeClass(section.type, 'items[' + idx + '].achievements')">
                {{ item.achievements }}
              </div>
            </div>
          </div>
        </div>

        <!-- 工作经历 -->
        <div v-else-if="section.type === 'WORK'" class="resume-section">
          <h3>{{ section.title }}</h3>
          <div v-for="(item, idx) in section.items" :key="item.id" class="experience-item">
            <div class="exp-header">
              <span class="exp-title">{{ parseContent(item.content)?.company }}</span>
              <span class="exp-period">{{ parseContent(item.content)?.period }}</span>
            </div>
            <div class="exp-role">{{ parseContent(item.content)?.position }}</div>
            <p v-if="parseContent(item.content)?.description" class="exp-description" :class="getChangeClass(section.type, 'items[' + idx + '].content.description')">
              {{ parseContent(item.content)?.description }}
            </p>
            <!-- 工作经历新增的 achievements 字段 -->
            <div v-if="item.achievements" class="exp-achievements">
              <div class="achievement-item" :class="getChangeClass(section.type, 'items[' + idx + '].achievements')">
                {{ item.achievements }}
              </div>
            </div>
          </div>
        </div>

        <!-- 项目经历 -->
        <div v-else-if="section.type === 'PROJECT'" class="resume-section">
          <h3>{{ section.title }}</h3>
          <div v-for="(item, idx) in section.items" :key="item.id" class="experience-item">
            <div class="exp-header">
              <span class="exp-title">{{ parseContent(item.content)?.name }}</span>
              <span class="exp-period">{{ parseContent(item.content)?.period }}</span>
            </div>
            <div class="exp-role">{{ parseContent(item.content)?.role }}</div>
            <p v-if="parseContent(item.content)?.description" class="exp-description">
              {{ parseContent(item.content)?.description }}
            </p>
            <div v-if="getAchievements(item.content)?.length" class="exp-achievements">
              <div
                v-for="(ach, achIdx) in getAchievements(item.content)"
                :key="achIdx"
                class="achievement-item"
                :class="getChangeClass(section.type, 'items[' + idx + '].content.achievements[' + achIdx + ']')"
              >
                {{ ach }}
              </div>
            </div>
            <!-- 项目新增的 achievements 字段 -->
            <div v-if="item.achievements" class="exp-achievements extra">
              <div class="achievement-item highlight" :class="getChangeClass(section.type, 'items[' + idx + '].achievements')">
                {{ item.achievements }}
              </div>
            </div>
          </div>
        </div>

        <!-- 专业技能 -->
        <div v-else-if="section.type === 'SKILLS'" class="resume-section">
          <h3>{{ section.title }}</h3>
          <div class="skills-container">
            <div
              v-for="(skill, idx) in getSkills(section)"
              :key="idx"
              class="skill-item"
              :class="getChangeClass(section.type, 'items[0].content.skills[' + idx + ']')"
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
          <div v-for="(item, idx) in section.items" :key="item.id" class="certificate-item">
            <span class="cert-name" :class="getChangeClass(section.type, 'items[' + idx + '].content.name')">
              {{ parseContent(item.content)?.name }}
            </span>
            <span v-if="parseContent(item.content)?.date" class="cert-date">{{ parseContent(item.content)?.date }}</span>
          </div>
        </div>

        <!-- 开源贡献 -->
        <div v-else-if="section.type === 'OPEN_SOURCE'" class="resume-section">
          <h3>{{ section.title }}</h3>
          <div v-for="item in section.items" :key="item.id" class="experience-item">
            <div class="exp-header">
              <span class="exp-title">{{ parseContent(item.content)?.name }}</span>
              <span class="exp-period">{{ parseContent(item.content)?.url }}</span>
            </div>
            <p v-if="parseContent(item.content)?.description" class="exp-description">
              {{ parseContent(item.content)?.description }}
            </p>
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
import type { ResumeSection, ChangeItem, ResumeSectionType } from '@/types/resume-optimize'
import { useSectionHelper } from '@/composables/useSectionHelper'

interface Props {
  sections: ResumeSection[]
  side: 'before' | 'after'
  changes?: ChangeItem[]
}

const props = defineProps<Props>()

const { parseContent } = useSectionHelper()

const isEmpty = computed(() => !props.sections || props.sections.length === 0)

// 字段标签映射
const FIELD_LABELS: Record<string, string> = {
  name: '姓名',
  gender: '性别',
  phone: '电话',
  email: '邮箱',
  targetPosition: '目标岗位',
  location: '所在地',
  linkedin: 'LinkedIn',
  github: 'GitHub',
  website: '个人网站',
  summary: '个人简介'
}

// 基本信息字段显示顺序（按简历展示习惯排序）
const BASIC_INFO_FIELD_ORDER: string[] = [
  'name',
  'gender',
  'phone',
  'email',
  'targetPosition',
  'location',
  'linkedin',
  'github',
  'website',
  'summary'
]

function getFieldLabel(key: string): string {
  return FIELD_LABELS[key] || key
}

// 获取排序后的基本信息字段
function getOrderedBasicInfoFields(content: Record<string, unknown>): { key: string; value: unknown }[] {
  const result: { key: string; value: unknown }[] = []
  // 先按预定义顺序添加
  for (const key of BASIC_INFO_FIELD_ORDER) {
    if (content[key] !== undefined && content[key] !== null && content[key] !== '') {
      result.push({ key, value: content[key] })
    }
  }
  // 再添加未在预定义顺序中但有值的字段
  for (const [key, value] of Object.entries(content)) {
    if (!BASIC_INFO_FIELD_ORDER.includes(key) && value !== undefined && value !== null && value !== '') {
      result.push({ key, value })
    }
  }
  return result
}

function formatValue(value: any): string {
  if (value === null || value === undefined) return '-'
  if (Array.isArray(value)) return value.join(', ')
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

// 获取技能列表
function getSkills(section: ResumeSection): { name: string; level?: string; description?: string; category?: string }[] {
  const firstItem = section.items?.[0]
  if (!firstItem) return []

  interface SkillItem {
    name: string
    level?: string
    description?: string
    category?: string
  }

  interface SkillsContent {
    skills?: SkillItem[] | string[]
  }

  const content = parseContent<SkillsContent>(firstItem.content)
  const skills = content?.skills
  if (Array.isArray(skills)) {
    return skills.map((s) => {
      if (typeof s === 'string') {
        return { name: s }
      }
      return s
    })
  }
  return []
}

// 获取成就列表
function getAchievements(content: string | Record<string, any> | null | undefined): string[] {
  interface ContentWithAchievements {
    achievements?: string[]
  }
  // 如果已经是对象，直接使用；如果是字符串，需要解析
  const parsed = typeof content === 'string'
    ? parseContent<ContentWithAchievements>(content)
    : (content as ContentWithAchievements)
  return parsed?.achievements ?? []
}

// 判断字段是否有变更
function getChangeClass(sectionType: ResumeSectionType, fieldPath: string): string {
  if (!props.changes?.length) return ''

  // 构建完整路径
  const typePrefix = sectionType.toLowerCase()
  const fullPath = `${typePrefix}.${fieldPath}`

  // 检查是否有匹配的变更
  const hasChange = props.changes.some(change => {
    if (!change.field) return false

    // 精确匹配
    if (change.field === fullPath || change.field === fieldPath) return true

    // 处理不同的路径格式
    const normalizedChangeField = change.field
      .replace(/\[(\d+)\]/g, '[$1]')
      .toLowerCase()

    const normalizedFullPath = fullPath.toLowerCase()

    return normalizedChangeField === normalizedFullPath ||
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
}

.exp-period {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.exp-details {
  font-size: $text-sm;
  color: $color-text-secondary;
}

.exp-role {
  font-size: $text-sm;
  color: $color-accent;
  margin-bottom: $spacing-sm;
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
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-sm 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);

  &:last-child {
    border-bottom: none;
  }
}

.cert-name {
  font-size: $text-sm;
  color: $color-text-primary;
  padding: 2px 4px;
  border-radius: $radius-sm;
  transition: background-color 0.2s;
}

.cert-date {
  font-size: $text-xs;
  color: $color-text-tertiary;
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
</style>
