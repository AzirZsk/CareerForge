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
      <div class="form-group">
        <label class="form-label">时间段</label>
        <input
          v-model="localData.period"
          type="text"
          class="form-input"
          placeholder="例如：2020.09 - 2024.06"
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
      <div class="form-group">
        <label class="form-label">工作描述</label>
        <textarea
          v-model="localData.description"
          class="form-textarea"
          rows="4"
          placeholder="请描述工作内容和职责"
        ></textarea>
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
        <label class="form-label">项目描述</label>
        <textarea
          v-model="localData.description"
          class="form-textarea"
          rows="4"
          placeholder="请描述项目内容和技术栈"
        ></textarea>
      </div>
      <div class="form-group">
        <label class="form-label">主要成果</label>
        <div class="achievements-input">
          <div class="achievement-list">
            <div
              v-for="(_achievement, index) in localAchievements"
              :key="index"
              class="achievement-item"
            >
              <input
                v-model="localAchievements[index]"
                type="text"
                class="form-input"
                placeholder="请输入成果描述"
              />
              <button class="remove-btn" @click="removeAchievement(index)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </button>
            </div>
          </div>
          <button class="add-achievement-btn" @click="addAchievement">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            添加成果
          </button>
        </div>
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
          placeholder="请输入证书或荣誉名称"
        />
      </div>
      <div class="form-group">
        <label class="form-label">获得时间</label>
        <input
          v-model="localData.date"
          type="text"
          class="form-input"
          placeholder="例如：2023年6月"
        />
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
        <div class="achievements-input">
          <div class="achievement-list">
            <div
              v-for="(_achievement, index) in localAchievements"
              :key="index"
              class="achievement-item"
            >
              <input
                v-model="localAchievements[index]"
                type="text"
                class="form-input"
                placeholder="请输入成果描述"
              />
              <button class="remove-btn" @click="removeAchievement(index)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </button>
            </div>
          </div>
          <button class="add-achievement-btn" @click="addAchievement">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            添加成果
          </button>
        </div>
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
              <input v-model="item.name" type="text" class="form-input" placeholder="请输入名称" />
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
              <div class="highlights-input">
                <div
                  v-for="(_highlight, hIndex) in item.highlights"
                  :key="hIndex"
                  class="highlight-item"
                >
                  <input v-model="item.highlights[hIndex]" type="text" class="form-input" placeholder="请输入成果或要点" />
                  <button class="remove-btn" @click="removeHighlight(index, hIndex)" type="button">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <line x1="18" y1="6" x2="6" y2="18"></line>
                      <line x1="6" y1="6" x2="18" y2="18"></line>
                    </svg>
                  </button>
                </div>
                <button class="add-highlight-btn" @click="addHighlight(index)" type="button">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="12" y1="5" x2="12" y2="19"></line>
                    <line x1="5" y1="12" x2="19" y2="12"></line>
                  </svg>
                  添加要点
                </button>
              </div>
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

interface Props {
  modelValue: Record<string, unknown>
  sectionType: string
}

interface Emits {
  (e: 'update:modelValue', value: Record<string, unknown>): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 本地数据（使用 string 类型以兼容 v-model）
const localData = ref<Record<string, string>>({})
const localAchievements = ref<string[]>([])

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
    // 其他类型：排除 achievements
    if (key !== 'achievements' && typeof value === 'string') {
      data[key] = value
    }
  }
  localData.value = data
  // 处理项目成果数组
  const achievements = props.modelValue?.achievements
  localAchievements.value = Array.isArray(achievements) ? [...achievements] : []
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
  [localData, localAchievements, localContentItems],
  () => {
    const data: Record<string, unknown> = { ...localData.value }
    // 如果是项目经历或开源贡献，同步成果数组
    if (props.sectionType === 'PROJECT' || props.sectionType === 'OPEN_SOURCE') {
      data.achievements = localAchievements.value.filter((a) => a.trim())
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

// 添加成果
function addAchievement(): void {
  localAchievements.value.push('')
}

// 删除成果
function removeAchievement(index: number): void {
  localAchievements.value.splice(index, 1)
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

// 添加要点
function addHighlight(itemIndex: number): void {
  localContentItems.value[itemIndex].highlights.push('')
}

// 删除要点
function removeHighlight(itemIndex: number, highlightIndex: number): void {
  localContentItems.value[itemIndex].highlights.splice(highlightIndex, 1)
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

.form-select {
  cursor: pointer;
}

.form-textarea {
  resize: vertical;
  min-height: 140px;
}

.achievements-input {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.achievement-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.achievement-item {
  display: flex;
  gap: $spacing-sm;
  .form-input {
    flex: 1;
  }
}

.remove-btn {
  width: 36px;
  height: 36px;
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

.add-achievement-btn {
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-sm $spacing-md;
  font-size: $text-sm;
  color: $color-accent;
  background: $color-accent-glow;
  border-radius: $radius-sm;
  transition: all $transition-fast;
  align-self: flex-start;
  &:hover {
    background: rgba(212, 168, 83, 0.2);
  }
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

.highlights-input {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.highlight-item {
  display: flex;
  gap: $spacing-sm;
  .form-input {
    flex: 1;
  }
}

.add-highlight-btn {
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-xs $spacing-sm;
  font-size: $text-xs;
  color: $color-accent;
  background: transparent;
  border-radius: $radius-sm;
  transition: all $transition-fast;
  align-self: flex-start;
  &:hover {
    background: $color-accent-glow;
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
