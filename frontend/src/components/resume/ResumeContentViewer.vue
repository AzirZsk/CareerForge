<!--=====================================================
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
            <div v-for="(value, key) in section.content" :key="key" class="info-item">
              <span class="info-label">{{ getFieldLabel(String(key)) }}</span>
              <span class="info-value" :class="getChangeClass(section.type, String(key))">
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
              <span class="exp-title">{{ item.content?.school }}</span>
              <span class="exp-period">{{ item.content?.period }}</span>
            </div>
            <div class="exp-details">
              {{ item.content?.degree }}<span v-if="item.content?.major"> · {{ item.content?.major }}</span>
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
              <span class="exp-title">{{ item.content?.company }}</span>
              <span class="exp-period">{{ item.content?.period }}</span>
            </div>
            <div class="exp-role">{{ item.content?.position }}</div>
            <p v-if="item.content?.description" class="exp-description" :class="getChangeClass(section.type, 'items[' + idx + '].content.description')">
              {{ item.content?.description }}
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
              <span class="exp-title">{{ item.content?.name }}</span>
              <span class="exp-period">{{ item.content?.period }}</span>
            </div>
            <div class="exp-role">{{ item.content?.role }}</div>
            <p v-if="item.content?.description" class="exp-description">
              {{ item.content?.description }}
            </p>
            <div v-if="item.content?.achievements?.length" class="exp-achievements">
              <div
                v-for="(ach, achIdx) in item.content.achievements"
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
          <div class="skills-list">
            <span
              v-for="(skill, idx) in getSkills(section)"
              :key="idx"
              class="skill-tag"
              :class="getChangeClass(section.type, 'items[0].content.skills[' + idx + ']')"
            >
              {{ skill }}
            </span>
          </div>
        </div>

        <!-- 证书荣誉 -->
        <div v-else-if="section.type === 'CERTIFICATE'" class="resume-section">
          <h3>{{ section.title }}</h3>
          <div v-for="(item, idx) in section.items" :key="item.id" class="certificate-item">
            <span class="cert-name" :class="getChangeClass(section.type, 'items[' + idx + '].content.name')">
              {{ item.content?.name }}
            </span>
            <span v-if="item.content?.date" class="cert-date">{{ item.content?.date }}</span>
          </div>
        </div>

        <!-- 开源贡献 -->
        <div v-else-if="section.type === 'OPEN_SOURCE'" class="resume-section">
          <h3>{{ section.title }}</h3>
          <div v-for="item in section.items" :key="item.id" class="experience-item">
            <div class="exp-header">
              <span class="exp-title">{{ item.content?.name }}</span>
              <span class="exp-period">{{ item.content?.url }}</span>
            </div>
            <p v-if="item.content?.description" class="exp-description">
              {{ item.content?.description }}
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

interface Props {
  sections: ResumeSection[]
  side: 'before' | 'after'
  changes?: ChangeItem[]
}

const props = defineProps<Props>()

const isEmpty = computed(() => !props.sections || props.sections.length === 0)

// 字段标签映射
const FIELD_LABELS: Record<string, string> = {
  name: '姓名',
  gender: '性别',
  phone: '电话',
  email: '邮箱',
  targetPosition: '目标岗位',
  summary: '简介'
}

function getFieldLabel(key: string): string {
  return FIELD_LABELS[key] || key
}

function formatValue(value: any): string {
  if (value === null || value === undefined) return '-'
  if (Array.isArray(value)) return value.join(', ')
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

// 获取技能列表
function getSkills(section: ResumeSection): string[] {
  if (section.items?.[0]?.content?.skills) {
    return section.items[0].content.skills
  }
  return []
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
      .replace(/\[(\d+)\]/g, '[$1]')  // 保持数组索引格式
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

.skills-list {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
}

.skill-tag {
  padding: $spacing-xs $spacing-sm;
  background: rgba(212, 168, 83, 0.1);
  color: $color-accent;
  border-radius: $radius-sm;
  font-size: $text-sm;
  transition: all 0.2s;
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
