<!--=====================================================
  LandIt 经历表单（通用）
  @author Azir
=====================================================-->

<template>
  <div class="experience-form">
    <!-- 教育经历 -->
    <template v-if="sectionType === 'EDUCATION'">
      <div class="form-group">
        <label class="form-label required">学校名称</label>
        <input
          v-model="localData.school"
          type="text"
          class="form-input"
          :class="{ 'form-input--error': hasError('school') }"
          placeholder="请输入学校名称"
        />
      </div>
      <div class="form-row">
        <div class="form-group">
          <label class="form-label">学历</label>
          <select v-model="localData.degree" class="form-select">
            <option value="">请选择</option>
            <option value="高中">高中</option>
            <option value="大专">大专</option>
            <option value="本科">本科</option>
            <option value="硕士">硕士</option>
            <option value="博士">博士</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">专业</label>
          <input
            v-model="localData.major"
            type="text"
            class="form-input"
            placeholder="请输入专业名称"
          />
        </div>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label class="form-label">时间段</label>
          <input
            v-model="localData.period"
            type="text"
            class="form-input"
            placeholder="例如：2020.09 - 2024.06"
          />
        </div>
        <div class="form-group">
          <label class="form-label">绩点（GPA）</label>
          <input
            v-model="localData.gpa"
            type="text"
            class="form-input"
            placeholder="例如：3.8/4.0"
          />
        </div>
      </div>
      <div class="form-group">
        <label class="form-label">主修课程</label>
        <div class="tags-input">
          <div class="tags-list">
            <span v-for="(course, index) in localCourses" :key="index" class="tag-item">
              {{ course }}
              <button class="tag-remove" @click="removeCourse(index)" type="button">×</button>
            </span>
          </div>
          <input
            v-model="courseInput"
            type="text"
            class="form-input tag-input"
            placeholder="输入课程后按回车添加（如：数据结构、操作系统）"
            @keydown.enter="addCourse"
          />
        </div>
      </div>
      <div class="form-group">
        <label class="form-label">校内荣誉</label>
        <DynamicListInput
          v-model="localHonors"
          item-placeholder="请输入荣誉名称（如：国家奖学金、优秀毕业生）"
          add-button-text="添加荣誉"
        />
      </div>
    </template>

    <!-- 工作经历 -->
    <template v-else-if="sectionType === 'WORK'">
      <div class="form-group">
        <label class="form-label required">公司名称</label>
        <input
          v-model="localData.company"
          type="text"
          class="form-input"
          :class="{ 'form-input--error': hasError('company') }"
          placeholder="请输入公司名称"
        />
      </div>
      <div class="form-row">
        <div class="form-group">
          <label class="form-label">职位</label>
          <input
            v-model="localData.position"
            type="text"
            class="form-input"
            placeholder="请输入职位名称"
          />
        </div>
        <div class="form-group">
          <label class="form-label">时间段</label>
          <input
            v-model="localData.period"
            type="text"
            class="form-input"
            placeholder="例如：2022.03 - 至今"
          />
        </div>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label class="form-label">工作地点</label>
          <input
            v-model="localData.location"
            type="text"
            class="form-input"
            placeholder="例如：北京市"
          />
        </div>
        <div class="form-group">
          <label class="form-label">公司行业</label>
          <input
            v-model="localData.industry"
            type="text"
            class="form-input"
            placeholder="例如：互联网/金融/制造业"
          />
        </div>
      </div>
      <div class="form-group">
        <label class="form-label">工作描述</label>
        <textarea
          v-model="localData.description"
          class="form-textarea"
          rows="4"
          placeholder="请描述工作内容和职责"
        ></textarea>
      </div>
      <div class="form-group">
        <label class="form-label">工作成果</label>
        <DynamicListInput
          v-model="localAchievements"
          item-placeholder="请输入成果描述（如：提升系统性能30%）"
          add-button-text="添加成果"
        />
      </div>
      <div class="form-group">
        <label class="form-label">技术栈</label>
        <div class="tags-input">
          <div class="tags-list">
            <span v-for="(tag, index) in localTechnologies" :key="index" class="tag-item">
              {{ tag }}
              <button class="tag-remove" @click="removeTechnology(index)" type="button">×</button>
            </span>
          </div>
          <input
            v-model="technologyInput"
            type="text"
            class="form-input tag-input"
            placeholder="输入技术后按回车添加（如：Java、Spring）"
            @keydown.enter="addTechnology"
          />
        </div>
      </div>
      <div class="form-group">
        <label class="form-label">代表产品</label>
        <DynamicListInput
          v-model="localProducts"
          item-placeholder="请输入产品名称"
          add-button-text="添加产品"
        />
      </div>
    </template>

    <!-- 项目经历 -->
    <template v-else-if="sectionType === 'PROJECT'">
      <div class="form-group">
        <label class="form-label required">项目名称</label>
        <input
          v-model="localData.name"
          type="text"
          class="form-input"
          :class="{ 'form-input--error': hasError('name') }"
          placeholder="请输入项目名称"
        />
      </div>
      <div class="form-row">
        <div class="form-group">
          <label class="form-label">角色</label>
          <input
            v-model="localData.role"
            type="text"
            class="form-input"
            placeholder="请输入你的角色"
          />
        </div>
        <div class="form-group">
          <label class="form-label">时间段</label>
          <input
            v-model="localData.period"
            type="text"
            class="form-input"
            placeholder="例如：2023.01 - 2023.06"
          />
        </div>
      </div>
      <div class="form-group">
        <label class="form-label">项目链接</label>
        <input
          v-model="localData.url"
          type="text"
          class="form-input"
          placeholder="项目在线地址或仓库链接"
        />
      </div>
      <div class="form-group">
        <label class="form-label">项目描述</label>
        <textarea
          v-model="localData.description"
          class="form-textarea"
          rows="4"
          placeholder="请描述项目内容和技术栈"
        ></textarea>
      </div>
      <div class="form-group">
        <label class="form-label">技术栈</label>
        <div class="tags-input">
          <div class="tags-list">
            <span v-for="(tech, index) in localTechnologies" :key="index" class="tag-item">
              {{ tech }}
              <button class="tag-remove" @click="removeTechnology(index)" type="button">×</button>
            </span>
          </div>
          <input
            v-model="technologyInput"
            type="text"
            class="form-input tag-input"
            placeholder="输入技术后按回车添加（如：Vue、Spring Boot）"
            @keydown.enter="addTechnology"
          />
        </div>
      </div>
      <div class="form-group">
        <label class="form-label">主要成果</label>
        <DynamicListInput
          v-model="localAchievements"
          item-placeholder="请输入成果描述"
          add-button-text="添加成果"
        />
      </div>
    </template>

    <!-- 证书 -->
    <template v-else-if="sectionType === 'CERTIFICATE'">
      <div class="form-group">
        <label class="form-label required">证书/荣誉名称</label>
        <input
          v-model="localData.name"
          type="text"
          class="form-input"
          :class="{ 'form-input--error': hasError('name') }"
          placeholder="请输入证书或荣誉名称"
        />
      </div>
      <div class="form-row">
        <div class="form-group">
          <label class="form-label">获得时间</label>
          <input
            v-model="localData.date"
            type="text"
            class="form-input"
            placeholder="例如：2023年6月"
          />
        </div>
        <div class="form-group">
          <label class="form-label">颁发机构</label>
          <input
            v-model="localData.issuer"
            type="text"
            class="form-input"
            placeholder="例如：中国信息通信研究院"
          />
        </div>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label class="form-label">证书编号</label>
          <input
            v-model="localData.credentialId"
            type="text"
            class="form-input"
            placeholder="例如：CERT-2023-XXXXX"
          />
        </div>
        <div class="form-group">
          <label class="form-label">证书链接</label>
          <input
            v-model="localData.url"
            type="text"
            class="form-input"
            placeholder="证书在线验证链接"
          />
        </div>
      </div>
    </template>

    <!-- 开源贡献 -->
    <template v-else-if="sectionType === 'OPEN_SOURCE'">
      <div class="form-group">
        <label class="form-label required">项目名称</label>
        <input
          v-model="localData.projectName"
          type="text"
          class="form-input"
          :class="{ 'form-input--error': hasError('projectName') }"
          placeholder="请输入开源项目名称"
        />
      </div>
      <div class="form-group">
        <label class="form-label">项目链接</label>
        <input
          v-model="localData.url"
          type="text"
          class="form-input"
          placeholder="例如：https://github.com/xxx/xxx"
        />
      </div>
      <div class="form-row">
        <div class="form-group">
          <label class="form-label">角色</label>
          <input
            v-model="localData.role"
            type="text"
            class="form-input"
            placeholder="例如：核心贡献者、维护者"
          />
        </div>
        <div class="form-group">
          <label class="form-label">时间段</label>
          <input
            v-model="localData.period"
            type="text"
            class="form-input"
            placeholder="例如：2023.01 - 至今"
          />
        </div>
      </div>
      <div class="form-group">
        <label class="form-label">项目描述</label>
        <textarea
          v-model="localData.description"
          class="form-textarea"
          rows="4"
          placeholder="请描述项目内容和你贡献的内容"
        ></textarea>
      </div>
      <div class="form-group">
        <label class="form-label">主要成果</label>
        <DynamicListInput
          v-model="localAchievements"
          item-placeholder="请输入成果描述"
          add-button-text="添加成果"
        />
      </div>
    </template>

    <!-- 自定义区块 -->
    <template v-else-if="sectionType === 'CUSTOM'">
      <div class="form-group">
        <label class="form-label required">区块标题</label>
        <input
          v-model="localData.title"
          type="text"
          class="form-input"
          :class="{ 'form-input--error': hasError('title') }"
          placeholder="例如：游戏经历、志愿者经历、竞赛经历"
        />
      </div>
      <div class="form-group">
        <label class="form-label">内容项</label>
        <div class="content-items">
          <div
            v-for="(item, index) in localContentItems"
            :key="index"
            class="content-item-card"
          >
            <div class="item-header">
              <span class="item-index">内容项 {{ index + 1 }}</span>
              <button class="remove-item-btn" @click="removeContentItem(index)" type="button">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </button>
            </div>
            <div class="form-group">
              <label class="form-label required">名称</label>
              <input
                v-model="item.name"
                type="text"
                class="form-input"
                :class="{ 'form-input--error': hasError(`items.${index}.name`) }"
                placeholder="请输入名称"
              />
            </div>
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">角色/职位</label>
                <input v-model="item.role" type="text" class="form-input" placeholder="请输入角色或职位" />
              </div>
              <div class="form-group">
                <label class="form-label">时间段</label>
                <input v-model="item.period" type="text" class="form-input" placeholder="例如：2023.01 - 2023.06" />
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">详细描述</label>
              <textarea v-model="item.description" class="form-textarea" rows="3" placeholder="请输入详细描述"></textarea>
            </div>
            <div class="form-group">
              <label class="form-label">成果/要点</label>
              <DynamicListInput
                v-model="item.highlights"
                item-placeholder="请输入成果或要点"
                add-button-text="添加要点"
              />
            </div>
          </div>
          <button class="add-item-btn" @click="addContentItem" type="button">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            添加内容项
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useValidationInject } from '@/composables/useFormValidation'
import DynamicListInput from './DynamicListInput.vue'

interface Props {
  modelValue: Record<string, unknown>
  sectionType: string
}

interface Emits {
  (e: 'update:modelValue', value: Record<string, unknown>): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 获取校验上下文
const validation = useValidationInject()
const hasError = (field: string) => validation?.hasError(field) ?? false

// 本地数据（使用 string 类型以兼容 v-model）
const localData = ref<Record<string, string>>({})
const localAchievements = ref<string[]>([])

// 工作经历专用
const localTechnologies = ref<string[]>([])
const localProducts = ref<string[]>([])
const technologyInput = ref('')

// 教育经历专用
const localCourses = ref<string[]>([])
const localHonors = ref<string[]>([])
const courseInput = ref('')

// 自定义区块内容项
interface ContentItemData {
  name: string
  role: string
  period: string
  description: string
  highlights: string[]
}
const localContentItems = ref<ContentItemData[]>([])

// 初始化数据
function initData(): void {
  const data: Record<string, string> = {}
  for (const [key, value] of Object.entries(props.modelValue)) {
    // 自定义区块特殊处理：title 单独存储，items 存入 localContentItems
    if (props.sectionType === 'CUSTOM') {
      if (key === 'title' && typeof value === 'string') {
        data[key] = value
      }
      continue
    }
    // 工作经历：排除数组和复杂数据类型
    if (props.sectionType === 'WORK') {
      if (!['achievements', 'technologies', 'products'].includes(key) && typeof value === 'string') {
        data[key] = value
      }
      continue
    }
    // 其他类型：排除 achievements
    if (key !== 'achievements' && typeof value === 'string') {
      data[key] = value
    }
  }
  localData.value = data
  // 处理项目成果数组
  const achievements = props.modelValue?.achievements
  localAchievements.value = Array.isArray(achievements) ? [...achievements] : []
  // 处理工作经历的技术栈和产品
  if (props.sectionType === 'WORK') {
    const technologies = props.modelValue?.technologies
    localTechnologies.value = Array.isArray(technologies) ? [...technologies] : []
    const products = props.modelValue?.products
    localProducts.value = Array.isArray(products) ? [...products] : []
  }
  // 处理项目经历的技术栈
  if (props.sectionType === 'PROJECT') {
    const technologies = props.modelValue?.technologies
    localTechnologies.value = Array.isArray(technologies) ? [...technologies] : []
  }
  // 处理教育经历的课程和荣誉
  if (props.sectionType === 'EDUCATION') {
    const courses = props.modelValue?.courses
    localCourses.value = Array.isArray(courses) ? [...courses] : []
    const honors = props.modelValue?.honors
    localHonors.value = Array.isArray(honors) ? [...honors] : []
  }
  // 处理自定义区块的 items
  if (props.sectionType === 'CUSTOM') {
    const items = props.modelValue?.items
    if (Array.isArray(items)) {
      localContentItems.value = items.map((item: Record<string, unknown>) => ({
        name: (item.name as string) || '',
        role: (item.role as string) || '',
        period: (item.period as string) || '',
        description: (item.description as string) || '',
        highlights: Array.isArray(item.highlights) ? [...(item.highlights as string[])] : []
      }))
    } else {
      localContentItems.value = []
    }
  }
}

// 标记是否正在同步，避免无限循环
let isSyncing = false

// 监听外部变化
watch(
  () => props.modelValue,
  () => {
    if (!isSyncing) {
      initData()
    }
  },
  { immediate: true, deep: true }
)

// 监听本地数据变化，同步到父组件
watch(
  [localData, localAchievements, localContentItems, localTechnologies, localProducts, localCourses, localHonors],
  () => {
    const data: Record<string, unknown> = { ...localData.value }
    // 如果是项目经历或开源贡献，同步成果数组
    if (props.sectionType === 'PROJECT' || props.sectionType === 'OPEN_SOURCE') {
      data.achievements = localAchievements.value.filter((a) => a.trim())
      // 项目经历也同步技术栈
      if (props.sectionType === 'PROJECT') {
        data.technologies = localTechnologies.value.filter((t) => t.trim())
      }
    }
    // 如果是工作经历，同步成果、技术栈和产品
    if (props.sectionType === 'WORK') {
      data.achievements = localAchievements.value.filter((a) => a.trim())
      data.technologies = localTechnologies.value.filter((t) => t.trim())
      data.products = localProducts.value.filter((p) => p.trim())
    }
    // 如果是教育经历，同步课程和荣誉
    if (props.sectionType === 'EDUCATION') {
      data.courses = localCourses.value.filter((c) => c.trim())
      data.honors = localHonors.value.filter((h) => h.trim())
    }
    // 如果是自定义区块，同步 items 数组
    if (props.sectionType === 'CUSTOM') {
      data.items = localContentItems.value.map((item) => ({
        name: item.name,
        role: item.role || undefined,
        period: item.period || undefined,
        description: item.description || undefined,
        highlights: item.highlights.filter((h) => h.trim())
      }))
    }
    isSyncing = true
    emit('update:modelValue', data)
    // 使用 nextTick 确保 Vue 完成更新后再重置标志
    setTimeout(() => {
      isSyncing = false
    }, 0)
  },
  { deep: true }
)

// 添加技术栈标签
function addTechnology(event: KeyboardEvent): void {
  event.preventDefault()
  const tech = technologyInput.value.trim()
  if (tech && !localTechnologies.value.includes(tech)) {
    localTechnologies.value.push(tech)
    technologyInput.value = ''
  }
}

// 删除技术栈标签
function removeTechnology(index: number): void {
  localTechnologies.value.splice(index, 1)
}

// 添加课程标签
function addCourse(event: KeyboardEvent): void {
  event.preventDefault()
  const course = courseInput.value.trim()
  if (course && !localCourses.value.includes(course)) {
    localCourses.value.push(course)
    courseInput.value = ''
  }
}

// 删除课程标签
function removeCourse(index: number): void {
  localCourses.value.splice(index, 1)
}

// 添加内容项
function addContentItem(): void {
  localContentItems.value.push({
    name: '',
    role: '',
    period: '',
    description: '',
    highlights: []
  })
}

// 删除内容项
function removeContentItem(index: number): void {
  localContentItems.value.splice(index, 1)
}
</script>

<style lang="scss" scoped>
.experience-form {
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: $spacing-lg;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.form-label {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-secondary;
  &.required::after {
    content: '*';
    color: $color-error;
    margin-left: 4px;
  }
}

.form-input,
.form-select,
.form-textarea {
  padding: $spacing-sm $spacing-md;
  font-size: $text-sm;
  font-family: $font-body;
  color: $color-text-primary;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-sm;
  transition: all $transition-fast;
  &:focus {
    outline: none;
    border-color: $color-accent;
    background: rgba(255, 255, 255, 0.05);
  }
  &::placeholder {
    color: $color-text-tertiary;
  }
}

// 错误状态样式
.form-input--error,
.form-select--error,
.form-textarea--error {
  border-color: $color-error !important;
  background: rgba(248, 113, 113, 0.05) !important;
  &:focus {
    border-color: $color-error !important;
    box-shadow: 0 0 0 2px rgba(248, 113, 113, 0.2);
  }
}

.form-select {
  cursor: pointer;
  option {
    background: $color-bg-secondary;
    color: $color-text-primary;
  }
}

.form-textarea {
  resize: vertical;
  min-height: 140px;
}

// 技术栈标签输入
.tags-input {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-xs $spacing-sm;
  background: rgba(212, 168, 83, 0.15);
  color: $color-accent;
  font-size: $text-xs;
  border-radius: $radius-sm;
}

.tag-remove {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  background: transparent;
  color: $color-accent;
  font-size: 14px;
  line-height: 1;
  border-radius: 50%;
  transition: all $transition-fast;
  &:hover {
    background: rgba(212, 168, 83, 0.3);
  }
}

.tag-input {
  width: 100%;
}

// 自定义区块样式
.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
  margin-bottom: $spacing-sm;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-xs $spacing-sm;
  background: rgba(212, 168, 83, 0.15);
  color: $color-accent;
  font-size: $text-xs;
  border-radius: $radius-sm;
}

.tag-remove {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  background: transparent;
  color: $color-accent;
  font-size: 14px;
  line-height: 1;
  border-radius: 50%;
  transition: all $transition-fast;
  &:hover {
    background: rgba(212, 168, 83, 0.3);
  }
}

.tag-input {
  width: 100%;
}

// 自定义区块样式
.content-items {
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.content-item-card {
  padding: $spacing-lg;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: $radius-md;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-md;
}

.item-index {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-accent;
}

.remove-item-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $color-text-tertiary;
  border-radius: $radius-sm;
  transition: all $transition-fast;
  &:hover {
    background: rgba(248, 113, 113, 0.1);
    color: $color-error;
  }
}

.add-item-btn {
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-sm $spacing-md;
  font-size: $text-sm;
  color: $color-accent;
  background: $color-accent-glow;
  border-radius: $radius-sm;
  transition: all $transition-fast;
  &:hover {
    background: rgba(212, 168, 83, 0.2);
  }
}
</style>
