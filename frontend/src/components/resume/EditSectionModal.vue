<!--=====================================================
  LandIt 简历模块编辑弹窗
  统一从 content 解析数据
  @author Azir
=====================================================-->

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="visible"
        class="modal-overlay"
        :class="{ 'modal-overlay--high': overlay }"
      >
        <div class="modal-container">
          <div class="modal-header">
            <h3 class="modal-title">{{ modalTitle }}</h3>
            <button class="close-btn" @click="handleCancel">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </div>
          <div class="modal-body">
            <!-- 基本信息 -->
            <BasicInfoForm
              v-if="sectionType === 'BASIC_INFO'"
              v-model="formData"
            />
            <!-- 技能 -->
            <SkillsForm
              v-else-if="sectionType === 'SKILLS'"
              v-model="formData"
            />
            <!-- 自定义区块内容项（添加/编辑模式） -->
            <CustomItemForm
              v-else-if="sectionType === 'CUSTOM' && isCustomItemMode"
              v-model="formData"
            />
            <!-- 其他经历类（教育/工作/项目/证书/开源贡献/新建自定义区块） -->
            <ExperienceForm
              v-else
              v-model="formData"
              :section-type="sectionType"
            />
          </div>
          <div class="modal-footer">
            <button class="btn cancel" @click="handleCancel">取消</button>
            <button class="btn save" :disabled="saving" @click="handleSave">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import BasicInfoForm from './forms/BasicInfoForm.vue'
import SkillsForm from './forms/SkillsForm.vue'
import ExperienceForm from './forms/ExperienceForm.vue'
import CustomItemForm from './forms/CustomItemForm.vue'
import type { ResumeSection } from '@/types'
import { useSectionHelper } from '@/composables/useSectionHelper'

interface Props {
  visible: boolean
  section: ResumeSection | null
  itemIndex?: number | null
  isNew?: boolean
  saving?: boolean
  /** 是否显示在全屏遮罩层之上（如全屏对比视图） */
  overlay?: boolean
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'save', data: { content: Record<string, unknown>; itemIndex?: number; isNew: boolean }): void
  (e: 'cancel'): void
}

const props = withDefaults(defineProps<Props>(), {
  itemIndex: null,
  isNew: false,
  saving: false,
  overlay: false
})

const emit = defineEmits<Emits>()

const {
  parseContent,
  parseAggregatedContent,
  getSkillsList,
  isAggregateType
} = useSectionHelper()

// 模块类型（CUSTOM_ITEM 当作 CUSTOM 处理）
const sectionType = computed(() => {
  const type = props.section?.type ?? ''
  return type === 'CUSTOM_ITEM' ? 'CUSTOM' : type
})

// 是否为聚合类型
const isAggregate = computed(() => {
  return isAggregateType(sectionType.value)
})

// 判断是否为自定义区块内容项模式（添加或编辑单个内容项）
const isCustomItemMode = computed<boolean>(() => {
  // 只处理 CUSTOM 类型
  if (sectionType.value !== 'CUSTOM') return false

  // 如果 section 的原始类型是 CUSTOM_ITEM，说明是内容项模式
  if (props.section?.type === 'CUSTOM_ITEM') return true

  // 新建整个区块时（isNew=true 但 itemIndex=null），使用 ExperienceForm
  if (props.isNew && (props.itemIndex === null || props.itemIndex === undefined)) {
    return false
  }

  // 其他情况（添加/编辑内容项）使用 CustomItemForm
  return true
})

// 弹窗标题
const modalTitle = computed(() => {
  // CUSTOM_ITEM 类型：使用 section 的实际标题
  if (props.section?.type === 'CUSTOM_ITEM' && props.section.title) {
    return props.isNew ? `添加${props.section.title}` : `编辑${props.section.title}`
  }
  const baseTitles: Record<string, string> = {
    BASIC_INFO: '基本信息',
    SKILLS: '技能',
    EDUCATION: '教育经历',
    WORK: '工作经历',
    PROJECT: '项目经历',
    CERTIFICATE: '证书/荣誉',
    OPEN_SOURCE: '开源贡献',
    CUSTOM: '自定义区块'
  }
  const baseTitle = baseTitles[sectionType.value] || '模块'
  if (isAggregate.value) {
    return props.isNew ? `添加${baseTitle}` : `编辑${baseTitle}`
  }
  return `编辑${baseTitle}`
})

// 表单数据（深拷贝）
const formData = ref<Record<string, unknown>>({})

// 锁定背景滚动
watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
  },
  { immediate: true }
)

// 组件销毁时确保恢复滚动
onUnmounted(() => {
  document.body.style.overflow = ''
})

// 监听 section/itemIndex 变化，深拷贝数据到表单
watch(
  [() => props.section, () => props.itemIndex, () => props.visible],
  ([newSection, newItemIndex]) => {
    if (newSection && props.visible) {
      // 技能类型：数据在 content.skills 数组中
      if (sectionType.value === 'SKILLS') {
        const skills = getSkillsList(newSection)
        formData.value = JSON.parse(JSON.stringify({ skills }))
        return
      }
      // 单条类型（BASIC_INFO）：content 是 JSON 字符串，需要解析
      if (!isAggregate.value) {
        formData.value = JSON.parse(JSON.stringify(parseContent(newSection.content)))
        return
      }
      // CUSTOM_ITEM 类型：直接使用 section.content（虚拟 section 的 content）
      if (newSection.type === 'CUSTOM_ITEM' && newSection.content) {
        const parsed = parseContent(newSection.content)
        formData.value = JSON.parse(JSON.stringify(parsed))
        return
      }
      // 聚合类型（包括 CUSTOM）：根据 itemIndex 找到对应的 item
      if (newSection.content && newItemIndex !== null && newItemIndex !== undefined) {
        // 从 content 解析数组
        const items = parseAggregatedContent(newSection)
        // 根据索引获取对应项
        if (items[newItemIndex]) {
          const item = items[newItemIndex]
          // 去除 id 字段，只保留内容
          const { id, ...content } = item as Record<string, unknown>

          // CUSTOM 类型：根据编辑模式决定数据格式
          if (sectionType.value === 'CUSTOM') {
            if (isCustomItemMode.value) {
              // CustomItemForm 需要扁平的内容对象
              formData.value = JSON.parse(JSON.stringify(content))
            } else {
              // ExperienceForm 需要 { title, items: [content] } 格式
              formData.value = {
                title: newSection.title || '',
                items: [JSON.parse(JSON.stringify(content))]
              }
            }
          } else {
            formData.value = JSON.parse(JSON.stringify(content))
          }
          return
        }
      }
      // 新增模式或找不到 item：空表单
      formData.value = {}
    }
  },
  { immediate: true }
)

// 取消
function handleCancel(): void {
  emit('cancel')
  emit('update:visible', false)
}

// 保存
function handleSave(): void {
  let contentToSave = formData.value

  // CUSTOM 类型且使用 CustomItemForm 时，数据直接就是内容项
  if (sectionType.value === 'CUSTOM' && isCustomItemMode.value) {
    // 数据已经是正确的格式，无需转换
    contentToSave = formData.value
  } else if (sectionType.value === 'CUSTOM' && formData.value.items) {
    // 新建自定义区块模式：从 items 数组中提取
    const { items } = formData.value
    if (Array.isArray(items) && items.length > 0) {
      contentToSave = items[0] as Record<string, unknown>
    }
  }

  emit('save', {
    content: contentToSave,
    itemIndex: props.itemIndex ?? undefined,
    isNew: props.isNew
  })
}
</script>

<style lang="scss" scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: $z-modal;
  backdrop-filter: blur(4px);

  // 高层级模式：显示在全屏遮罩层之上
  &.modal-overlay--high {
    z-index: $z-modal-overlay;
  }
}

.modal-container {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.08);
  width: 90%;
  max-width: 600px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-lg;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.modal-title {
  font-family: $font-display;
  font-size: $text-xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
}

.close-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius-sm;
  color: $color-text-tertiary;
  transition: all $transition-fast;
  &:hover {
    background: rgba(255, 255, 255, 0.05);
    color: $color-text-primary;
  }
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-lg;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: $spacing-md;
  padding: $spacing-lg;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.btn {
  padding: $spacing-sm $spacing-xl;
  font-size: $text-sm;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  transition: all $transition-fast;
  &.cancel {
    background: rgba(255, 255, 255, 0.05);
    color: $color-text-secondary;
    &:hover {
      background: rgba(255, 255, 255, 0.1);
    }
  }
  &.save {
    background: $gradient-gold;
    color: $color-bg-deep;
    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 4px 16px rgba(212, 168, 83, 0.3);
    }
    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }
}

// 动画
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
  .modal-container {
    transition: transform 0.2s ease;
  }
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
  .modal-container {
    transform: scale(0.95);
  }
}
</style>
