<!--=====================================================
  字段级差异对比查看器
  使用与 Viewer 组件一致的排版布局
  支持上下对比（旧值删除线 + 新值高亮）
  @author Azir
=====================================================-->

<template>
  <div class="field-diff-viewer">
    <!-- 基本信息类型：网格布局 -->
    <template v-if="sectionType === 'BASIC_INFO'">
      <div class="info-grid">
        <div
          v-for="field in basicInfoFields"
          :key="field.key"
          class="info-item"
          :class="{ 'full-width': field.key === 'summary' }"
        >
          <span class="info-label">{{ field.label }}</span>
          <div class="info-value-wrapper">
            <span
              v-if="field.hasChange && field.oldValue"
              class="info-value old-value"
            >{{ formatValue(field.oldValue) }}</span>
            <span
              class="info-value"
              :class="{ 'new-value': field.hasChange }"
            >{{ formatValue(field.hasChange ? field.newValue : field.value) }}</span>
          </div>
        </div>
      </div>
    </template>

    <!-- 工作经历：时间线布局 -->
    <template v-else-if="sectionType === 'WORK'">
      <div
        v-for="(item, idx) in workItems"
        :key="idx"
        class="experience-item"
      >
        <!-- 头部：公司 + 时间 -->
        <div class="exp-header">
          <div class="exp-title-wrapper">
            <span
              v-if="item.company.hasChange && item.company.oldValue"
              class="exp-title old-value"
            >{{ item.company.oldValue }}</span>
            <span
              class="exp-title"
              :class="{ 'new-value': item.company.hasChange }"
            >{{ item.company.hasChange ? item.company.newValue : item.company.value }}</span>
          </div>
          <span class="exp-period">{{ item.period?.value || '' }}</span>
        </div>
        <!-- 元信息：职位 + 地点 + 行业 -->
        <div class="exp-meta">
          <span
            v-if="item.position?.value"
            class="exp-position"
            :class="{ 'new-value': item.position?.hasChange }"
          >{{ item.position.value }}</span>
          <span
            v-if="item.location?.value"
            class="exp-location"
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
            {{ item.location.value }}
          </span>
          <span
            v-if="item.industry?.value"
            class="exp-industry"
          >{{ item.industry.value }}</span>
        </div>
        <!-- 描述 -->
        <p
          v-if="item.description?.value"
          class="exp-description"
          :class="{ 'new-value': item.description?.hasChange }"
        >
          {{ item.description.value }}
        </p>
        <!-- 成就 -->
        <div
          v-if="getArrayValue(item.achievements?.value)?.length"
          class="exp-achievements"
        >
          <div
            v-for="(ach, achIdx) in getArrayValue(item.achievements?.value)"
            :key="achIdx"
            class="achievement-item"
          >
            {{ ach }}
          </div>
        </div>
        <!-- 技术栈 -->
        <div
          v-if="getArrayValue(item.technologies?.value)?.length"
          class="exp-technologies"
        >
          <span
            v-for="tech in getArrayValue(item.technologies?.value)"
            :key="tech"
            class="tech-tag"
          >{{ tech }}</span>
        </div>
        <!-- 代表产品 -->
        <div
          v-if="getArrayValue(item.products?.value)?.length"
          class="exp-products"
        >
          <span class="products-label">代表产品:</span>
          <span
            v-for="p in getArrayValue(item.products?.value)"
            :key="p"
            class="product-tag"
          >{{ p }}</span>
        </div>
      </div>
    </template>

    <!-- 教育经历 -->
    <template v-else-if="sectionType === 'EDUCATION'">
      <div
        v-for="(item, idx) in educationItems"
        :key="idx"
        class="experience-item"
      >
        <!-- 头部：学校 + 时间 -->
        <div class="exp-header">
          <div class="exp-title-wrapper">
            <span
              v-if="item.school.hasChange && item.school.oldValue"
              class="exp-title old-value"
            >{{ item.school.oldValue }}</span>
            <span
              class="exp-title"
              :class="{ 'new-value': item.school.hasChange }"
            >{{ item.school.hasChange ? item.school.newValue : item.school.value }}</span>
          </div>
          <span class="exp-period">{{ item.period?.value || '' }}</span>
        </div>
        <!-- 元信息：学位 + 专业 + GPA -->
        <div class="exp-meta">
          <span
            v-if="item.degree?.value"
            class="exp-degree"
            :class="{ 'new-value': item.degree?.hasChange }"
          >{{ item.degree.value }}</span>
          <span
            v-if="item.major?.value"
            class="exp-major"
            :class="{ 'new-value': item.major?.hasChange }"
          >{{ item.major.value }}</span>
          <span
            v-if="item.gpa?.value"
            class="exp-gpa"
          >GPA: {{ item.gpa.value }}</span>
        </div>
        <!-- 课程 -->
        <div
          v-if="getArrayValue(item.courses?.value)?.length"
          class="exp-courses"
        >
          <span
            v-for="course in getArrayValue(item.courses?.value)"
            :key="course"
            class="course-tag"
          >{{ course }}</span>
        </div>
        <!-- 荣誉 -->
        <div
          v-if="getArrayValue(item.honors?.value)?.length"
          class="exp-honors"
        >
          <span
            v-for="honor in getArrayValue(item.honors?.value)"
            :key="honor"
            class="honor-tag"
          >{{ honor }}</span>
        </div>
      </div>
    </template>

    <!-- 项目经历 -->
    <template v-else-if="sectionType === 'PROJECT'">
      <div
        v-for="(item, idx) in projectItems"
        :key="idx"
        class="experience-item"
      >
        <!-- 头部：项目名 + 时间 -->
        <div class="exp-header">
          <div class="exp-title-wrapper">
            <span
              v-if="item.name.hasChange && item.name.oldValue"
              class="exp-title old-value"
            >{{ item.name.oldValue }}</span>
            <span
              class="exp-title"
              :class="{ 'new-value': item.name.hasChange }"
            >{{ item.name.hasChange ? item.name.newValue : item.name.value }}</span>
            <a
              v-if="getStringValue(item.url?.value)"
              :href="getStringValue(item.url?.value)!"
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
          </div>
          <span class="exp-period">{{ getStringValue(item.period?.value) || '' }}</span>
        </div>
        <!-- 角色 -->
        <div
          v-if="getStringValue(item.role?.value)"
          class="exp-role"
          :class="{ 'new-value': item.role?.hasChange }"
        >
          {{ getStringValue(item.role?.value) }}
        </div>
        <!-- 描述 -->
        <p
          v-if="getStringValue(item.description?.value)"
          class="exp-description"
          :class="{ 'new-value': item.description?.hasChange }"
        >
          {{ getStringValue(item.description?.value) }}
        </p>
        <!-- 技术栈 -->
        <div
          v-if="getArrayValue(item.technologies?.value)?.length"
          class="exp-technologies"
        >
          <span
            v-for="tech in getArrayValue(item.technologies?.value)"
            :key="tech"
            class="tech-tag"
          >{{ tech }}</span>
        </div>
        <!-- 成就 -->
        <div
          v-if="getArrayValue(item.achievements?.value)?.length"
          class="exp-achievements"
        >
          <div
            v-for="(ach, achIdx) in getArrayValue(item.achievements?.value)"
            :key="achIdx"
            class="achievement-item"
          >
            {{ ach }}
          </div>
        </div>
      </div>
    </template>

    <!-- 技能 -->
    <template v-else-if="sectionType === 'SKILLS'">
      <div class="skills-container">
        <div
          v-for="(item, idx) in skillItems"
          :key="idx"
          class="skill-item"
        >
          <div class="skill-header">
            <span class="skill-name">{{ item.name?.value || '' }}</span>
            <span
              v-if="item.level?.value"
              class="skill-level"
            >{{ item.level.value }}</span>
          </div>
          <p
            v-if="item.description?.value"
            class="skill-description"
          >
            {{ item.description.value }}
          </p>
          <span
            v-if="item.category?.value"
            class="skill-category"
          >
            {{ item.category.value }}
          </span>
        </div>
      </div>
    </template>

    <!-- 证书 -->
    <template v-else-if="sectionType === 'CERTIFICATE'">
      <div
        v-for="(item, idx) in certificateItems"
        :key="idx"
        class="certificate-item"
      >
        <div class="cert-header">
          <div class="cert-name-wrapper">
            <span
              v-if="item.name.hasChange && item.name.oldValue"
              class="cert-name old-value"
            >{{ item.name.oldValue }}</span>
            <span
              class="cert-name"
              :class="{ 'new-value': item.name.hasChange }"
            >{{ item.name.hasChange ? item.name.newValue : item.name.value }}</span>
          </div>
          <span class="cert-date">{{ item.date?.value || '' }}</span>
        </div>
        <div
          v-if="item.issuer?.value || item.credentialId?.value"
          class="cert-meta"
        >
          <span
            v-if="item.issuer?.value"
            class="cert-issuer"
          >{{ item.issuer.value }}</span>
          <span
            v-if="item.credentialId?.value"
            class="cert-credential"
          >{{ item.credentialId.value }}</span>
        </div>
      </div>
    </template>

    <!-- 开源贡献 -->
    <template v-else-if="sectionType === 'OPEN_SOURCE'">
      <div
        v-for="(item, idx) in openSourceItems"
        :key="idx"
        class="experience-item"
      >
        <!-- 头部：项目名 + URL -->
        <div class="exp-header">
          <div class="exp-title-wrapper">
            <span
              v-if="item.name.hasChange && item.name.oldValue"
              class="exp-title old-value"
            >{{ item.name.oldValue }}</span>
            <span
              class="exp-title"
              :class="{ 'new-value': item.name.hasChange }"
            >{{ item.name.hasChange ? item.name.newValue : item.name.value }}</span>
            <a
              v-if="getStringValue(item.url?.value)"
              :href="getStringValue(item.url?.value)!"
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
          </div>
        </div>
        <!-- 角色 -->
        <div
          v-if="getStringValue(item.role?.value)"
          class="exp-role"
        >
          {{ getStringValue(item.role?.value) }}
        </div>
        <!-- 描述 -->
        <p
          v-if="getStringValue(item.description?.value)"
          class="exp-description"
        >
          {{ getStringValue(item.description?.value) }}
        </p>
        <!-- 技术栈 -->
        <div
          v-if="getArrayValue(item.technologies?.value)?.length"
          class="exp-technologies"
        >
          <span
            v-for="tech in getArrayValue(item.technologies?.value)"
            :key="tech"
            class="tech-tag"
          >{{ tech }}</span>
        </div>
      </div>
    </template>

    <!-- 自定义区块 -->
    <template v-else-if="sectionType === 'CUSTOM'">
      <div
        v-for="(item, idx) in customItems"
        :key="idx"
        class="custom-block"
      >
        <div class="custom-block-title">{{ item.title?.value || '' }}</div>
        <p
          v-if="item.description?.value"
          class="extra-value description"
        >
          {{ item.description.value }}
        </p>
      </div>
    </template>

    <!-- 兜底：通用列表布局 -->
    <template v-else>
      <div
        v-for="(item, idx) in genericItems"
        :key="idx"
        class="generic-item"
      >
        <div class="item-title">{{ item.title }}</div>
        <div
          v-for="field in item.fields"
          :key="field.key"
          class="field-row"
        >
          <span class="field-label">{{ field.label }}</span>
          <div class="field-values">
            <span
              v-if="field.hasChange && field.oldValue"
              class="old-value"
            >{{ formatValue(field.oldValue) }}</span>
            <span
              :class="{ 'new-value': field.hasChange }"
            >{{ formatValue(field.hasChange ? field.newValue : field.value) }}</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useSectionHelper } from '@/composables/useSectionHelper'

interface Props {
  sectionType: string
  sectionTitle: string
  beforeContent: string | null
  afterContent: string
}

const props = defineProps<Props>()

const {
  parseContent,
  getFieldLabel,
  getOrderedBasicInfoFields
} = useSectionHelper()

// 字段显示顺序配置
const FIELD_ORDER: Record<string, string[]> = {
  WORK: ['company', 'position', 'period', 'location', 'industry', 'description', 'achievements', 'technologies', 'products'],
  PROJECT: ['name', 'url', 'role', 'period', 'description', 'achievements', 'technologies'],
  EDUCATION: ['school', 'major', 'degree', 'period', 'gpa', 'courses', 'honors'],
  CERTIFICATE: ['name', 'issuer', 'date', 'credentialId', 'url'],
  OPEN_SOURCE: ['name', 'url', 'role', 'description', 'technologies'],
  SKILLS: ['name', 'level', 'category', 'description'],
  CUSTOM: ['title', 'subtitle', 'description', 'url']
}

// 聚合类型条目标题字段
const ITEM_TITLE_FIELD: Record<string, string> = {
  WORK: 'company',
  PROJECT: 'name',
  EDUCATION: 'school',
  CERTIFICATE: 'name',
  OPEN_SOURCE: 'name',
  SKILLS: 'name',
  CUSTOM: 'title'
}

/**
 * 判断两个值是否相等
 */
function isEqual(a: unknown, b: unknown): boolean {
  if (a === b) return true
  if (a == null && b == null) return true
  if (a == null || b == null) return false
  if (Array.isArray(a) && Array.isArray(b)) {
    if (a.length !== b.length) return false
    return a.every((item, i) => isEqual(item, b[i]))
  }
  if (typeof a === 'object' && typeof b === 'object') {
    const aObj = a as Record<string, unknown>
    const bObj = b as Record<string, unknown>
    const allKeys = new Set([...Object.keys(aObj), ...Object.keys(bObj)])
    for (const key of allKeys) {
      if (!isEqual(aObj[key], bObj[key])) return false
    }
    return true
  }
  return String(a) === String(b)
}

/**
 * 格式化显示值
 */
function formatValue(val: unknown): string {
  if (val == null) return ''
  if (Array.isArray(val)) {
    return val.join('、')
  }
  if (typeof val === 'object') {
    return JSON.stringify(val)
  }
  return String(val)
}

/**
 * 判断值是否为空
 */
function isEmpty(val: unknown): boolean {
  if (val == null) return true
  if (typeof val === 'string') return val.trim() === ''
  if (Array.isArray(val)) return val.length === 0
  return false
}

/**
 * 获取数组类型的值（类型安全）
 */
function getArrayValue(val: unknown): string[] | null {
  if (val == null) return null
  if (Array.isArray(val)) return val as string[]
  return null
}

/**
 * 获取字符串类型的值（类型安全）
 */
function getStringValue(val: unknown): string | null {
  if (val == null) return null
  if (typeof val === 'string') return val
  return null
}

// ========== 字段差异类型 ==========
interface FieldDiff {
  key: string
  label: string
  hasChange: boolean
  oldValue: unknown
  newValue: unknown
  value: unknown
}

interface ItemFieldDiff {
  [key: string]: FieldDiff
}

// ========== 基本信息类型处理 ==========
const basicInfoFields = computed<FieldDiff[]>(() => {
  if (props.sectionType !== 'BASIC_INFO') return []

  const before = parseContent<Record<string, unknown>>(props.beforeContent)
  const after = parseContent<Record<string, unknown>>(props.afterContent)

  // 合并所有字段
  const allKeys = new Set([...Object.keys(before), ...Object.keys(after)])
  const fields: FieldDiff[] = []

  for (const key of allKeys) {
    const oldVal = before[key]
    const newVal = after[key]
    const hasChange = !isEqual(oldVal, newVal)

    // 如果两边都为空，跳过
    if (isEmpty(oldVal) && isEmpty(newVal)) continue

    fields.push({
      key,
      label: getFieldLabel(key),
      hasChange,
      oldValue: oldVal,
      newValue: newVal,
      value: hasChange ? newVal : newVal
    })
  }

  // 按 getOrderedBasicInfoFields 的顺序排序
  const orderMap = new Map<string, number>()
  const orderedFields = getOrderedBasicInfoFields({ ...before, ...after } as any)
  orderedFields.forEach((f, i) => orderMap.set(f.key, i))

  return fields.sort((a, b) => {
    const orderA = orderMap.get(a.key) ?? 999
    const orderB = orderMap.get(b.key) ?? 999
    return orderA - orderB
  })
})

// ========== 聚合类型条目匹配 ==========
function matchItems(
  beforeArr: Record<string, unknown>[] | null,
  afterArr: Record<string, unknown>[],
  sectionType: string
): Array<{ before: Record<string, unknown> | null; after: Record<string, unknown> }> {
  const result: Array<{ before: Record<string, unknown> | null; after: Record<string, unknown> }> = []

  for (let i = 0; i < afterArr.length; i++) {
    const afterItem = afterArr[i]
    const beforeItem = beforeArr?.find((b) => {
      if (sectionType === 'WORK') {
        return b.company === afterItem.company && b.position === afterItem.position
      }
      if (sectionType === 'EDUCATION') {
        return b.school === afterItem.school && b.major === afterItem.major
      }
      if (sectionType === 'PROJECT') {
        return b.name === afterItem.name
      }
      if (sectionType === 'CERTIFICATE') {
        return b.name === afterItem.name
      }
      if (sectionType === 'OPEN_SOURCE') {
        return b.name === afterItem.name
      }
      if (sectionType === 'SKILLS') {
        return b.name === afterItem.name
      }
      return beforeArr?.indexOf(b) === i
    }) || beforeArr?.[i]

    result.push({ before: beforeItem || null, after: afterItem })
  }

  return result
}

/**
 * 计算条目字段差异
 */
function computeItemFields(
  beforeItem: Record<string, unknown> | null,
  afterItem: Record<string, unknown>,
  fieldOrder: string[]
): ItemFieldDiff {
  const allKeys = new Set([...Object.keys(beforeItem || {}), ...Object.keys(afterItem)])
  const fields: ItemFieldDiff = {}

  // 按预定义顺序添加字段
  const orderedKeys = [...fieldOrder]
  for (const key of Object.keys(afterItem)) {
    if (!orderedKeys.includes(key)) {
      orderedKeys.push(key)
    }
  }

  for (const key of orderedKeys) {
    if (!allKeys.has(key)) continue

    const oldVal = beforeItem?.[key]
    const newVal = afterItem[key]
    const hasChange = !isEqual(oldVal, newVal)

    // 如果两边都为空，跳过
    if (isEmpty(oldVal) && isEmpty(newVal)) continue

    fields[key] = {
      key,
      label: getFieldLabel(key),
      hasChange,
      oldValue: oldVal,
      newValue: newVal,
      value: hasChange ? newVal : newVal
    }
  }

  return fields
}

// ========== 工作经历 ==========
const workItems = computed<ItemFieldDiff[]>(() => {
  if (props.sectionType !== 'WORK') return []

  const beforeArr = parseContent<Record<string, unknown>[]>(props.beforeContent)
  const afterArr = parseContent<Record<string, unknown>[]>(props.afterContent)

  if (!Array.isArray(afterArr)) return []

  const matches = matchItems(beforeArr, afterArr, 'WORK')
  return matches.map(({ before, after }) => computeItemFields(before, after, FIELD_ORDER.WORK))
})

// ========== 教育经历 ==========
const educationItems = computed<ItemFieldDiff[]>(() => {
  if (props.sectionType !== 'EDUCATION') return []

  const beforeArr = parseContent<Record<string, unknown>[]>(props.beforeContent)
  const afterArr = parseContent<Record<string, unknown>[]>(props.afterContent)

  if (!Array.isArray(afterArr)) return []

  const matches = matchItems(beforeArr, afterArr, 'EDUCATION')
  return matches.map(({ before, after }) => computeItemFields(before, after, FIELD_ORDER.EDUCATION))
})

// ========== 项目经历 ==========
const projectItems = computed<ItemFieldDiff[]>(() => {
  if (props.sectionType !== 'PROJECT') return []

  const beforeArr = parseContent<Record<string, unknown>[]>(props.beforeContent)
  const afterArr = parseContent<Record<string, unknown>[]>(props.afterContent)

  if (!Array.isArray(afterArr)) return []

  const matches = matchItems(beforeArr, afterArr, 'PROJECT')
  return matches.map(({ before, after }) => computeItemFields(before, after, FIELD_ORDER.PROJECT))
})

// ========== 技能 ==========
const skillItems = computed<ItemFieldDiff[]>(() => {
  if (props.sectionType !== 'SKILLS') return []

  const beforeArr = parseContent<Record<string, unknown>[]>(props.beforeContent)
  const afterArr = parseContent<Record<string, unknown>[]>(props.afterContent)

  if (!Array.isArray(afterArr)) return []

  const matches = matchItems(beforeArr, afterArr, 'SKILLS')
  return matches.map(({ before, after }) => computeItemFields(before, after, FIELD_ORDER.SKILLS))
})

// ========== 证书 ==========
const certificateItems = computed<ItemFieldDiff[]>(() => {
  if (props.sectionType !== 'CERTIFICATE') return []

  const beforeArr = parseContent<Record<string, unknown>[]>(props.beforeContent)
  const afterArr = parseContent<Record<string, unknown>[]>(props.afterContent)

  if (!Array.isArray(afterArr)) return []

  const matches = matchItems(beforeArr, afterArr, 'CERTIFICATE')
  return matches.map(({ before, after }) => computeItemFields(before, after, FIELD_ORDER.CERTIFICATE))
})

// ========== 开源贡献 ==========
const openSourceItems = computed<ItemFieldDiff[]>(() => {
  if (props.sectionType !== 'OPEN_SOURCE') return []

  const beforeArr = parseContent<Record<string, unknown>[]>(props.beforeContent)
  const afterArr = parseContent<Record<string, unknown>[]>(props.afterContent)

  if (!Array.isArray(afterArr)) return []

  const matches = matchItems(beforeArr, afterArr, 'OPEN_SOURCE')
  return matches.map(({ before, after }) => computeItemFields(before, after, FIELD_ORDER.OPEN_SOURCE))
})

// ========== 自定义区块 ==========
const customItems = computed<ItemFieldDiff[]>(() => {
  if (props.sectionType !== 'CUSTOM') return []

  const beforeArr = parseContent<Record<string, unknown>[]>(props.beforeContent)
  const afterArr = parseContent<Record<string, unknown>[]>(props.afterContent)

  if (!Array.isArray(afterArr)) return []

  const matches = matchItems(beforeArr, afterArr, 'CUSTOM')
  return matches.map(({ before, after }) => computeItemFields(before, after, FIELD_ORDER.CUSTOM))
})

// ========== 兜底通用类型 ==========
interface GenericItem {
  title: string
  fields: FieldDiff[]
}

const genericItems = computed<GenericItem[]>(() => {
  const beforeArr = parseContent<Record<string, unknown>[]>(props.beforeContent)
  const afterArr = parseContent<Record<string, unknown>[]>(props.afterContent)

  if (!Array.isArray(afterArr)) return []

  const titleField = ITEM_TITLE_FIELD[props.sectionType] || 'name'
  const fieldOrder = FIELD_ORDER[props.sectionType] || []
  const items: GenericItem[] = []

  const matches = matchItems(
    Array.isArray(beforeArr) ? beforeArr : null,
    afterArr,
    props.sectionType
  )

  for (const { before, after } of matches) {
    const allKeys = new Set([...Object.keys(before || {}), ...Object.keys(after)])
    const fields: FieldDiff[] = []

    const orderedKeys = getOrderedFields({ ...before, ...after } as Record<string, unknown>, fieldOrder)

    for (const key of orderedKeys) {
      if (!allKeys.has(key)) continue

      const oldVal = before?.[key]
      const newVal = after[key]
      const hasChange = !isEqual(oldVal, newVal)

      if (isEmpty(oldVal) && isEmpty(newVal)) continue

      fields.push({
        key,
        label: getFieldLabel(key),
        hasChange,
        oldValue: oldVal,
        newValue: newVal,
        value: hasChange ? newVal : newVal
      })
    }

    items.push({
      title: String(after[titleField] || ''),
      fields
    })
  }

  return items
})

/**
 * 获取排序后的字段列表
 */
function getOrderedFields(obj: Record<string, unknown>, order: string[]): string[] {
  const result: string[] = []
  for (const key of order) {
    if (obj[key] !== undefined) {
      result.push(key)
    }
  }
  for (const key of Object.keys(obj)) {
    if (!result.includes(key)) {
      result.push(key)
    }
  }
  return result
}
</script>

<style lang="scss" scoped>
@use '@/components/resume/viewer/shared.scss' as *;

.field-diff-viewer {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

// 旧值样式
.old-value {
  color: $color-error;
  text-decoration: line-through;
  opacity: 0.7;
  display: inline;
  margin-right: $spacing-xs;

  &.exp-title,
  &.cert-name {
    display: block;
    margin-bottom: 2px;
    margin-right: 0;
  }
}

// 新值样式
.new-value {
  color: $color-success;
  background: rgba($color-success, 0.1);
  padding: 2px 6px;
  border-radius: $radius-xs;
  display: inline;
}

// 基本信息网格
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-sm;

  @media (width <= 600px) {
    grid-template-columns: 1fr;
  }
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;

  &.full-width {
    grid-column: span 2;

    @media (width <= 600px) {
      grid-column: span 1;
    }
  }
}

.info-label {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.info-value-wrapper {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-value {
  font-size: $text-sm;
  color: $color-text-primary;
  font-weight: $weight-medium;
  word-break: break-word;
}

// 标题包装器（支持旧值+新值）
.exp-title-wrapper,
.cert-name-wrapper {
  display: flex;
  flex-direction: column;
}

// 经历项
.experience-item {
  padding: $spacing-sm 0;
  border-bottom: 1px solid rgba(255, 255, 255, 5%);

  &:last-child {
    border-bottom: none;
  }
}

// 证书项
.certificate-item {
  padding: $spacing-sm 0;
  border-bottom: 1px solid rgba(255, 255, 255, 5%);

  &:last-child {
    border-bottom: none;
  }
}

// 自定义区块
.custom-block {
  margin-bottom: $spacing-sm;

  &:last-child {
    margin-bottom: 0;
  }
}

// 通用兜底项
.generic-item {
  padding: $spacing-sm;
  background: rgba(255, 255, 255, 2%);
  border-radius: $radius-sm;
  margin-bottom: $spacing-sm;

  &:last-child {
    margin-bottom: 0;
  }
}

.item-title {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-accent;
  margin-bottom: $spacing-sm;
  padding-bottom: $spacing-xs;
  border-bottom: 1px dashed rgba(255, 255, 255, 10%);
}

.field-row {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: $spacing-xs 0;
  border-bottom: 1px solid rgba(255, 255, 255, 3%);

  &:last-child {
    border-bottom: none;
  }
}

.field-label {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.field-values {
  padding-left: $spacing-sm;
  font-size: $text-sm;
  color: $color-text-primary;
}

// 响应式调整
@media (width <= 400px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .info-item.full-width {
    grid-column: span 1;
  }
}
</style>
