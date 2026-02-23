<!--=====================================================
  LandIt 简历模块编辑弹窗
  @author Azir
=====================================================-->

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="modal-overlay" @click.self="handleCancel">
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
            <!-- 经历类（教育/工作/项目/证书） -->
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
import { ref, computed, watch } from 'vue'
import BasicInfoForm from './forms/BasicInfoForm.vue'
import SkillsForm from './forms/SkillsForm.vue'
import ExperienceForm from './forms/ExperienceForm.vue'
import type { ResumeSection } from '@/types'

interface Props {
  visible: boolean
  section: ResumeSection | null
  itemId?: string | null
  isNew?: boolean
  saving?: boolean
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'save', data: { content: Record<string, unknown>; itemId?: string; isNew: boolean }): void
  (e: 'cancel'): void
}

const props = withDefaults(defineProps<Props>(), {
  itemId: null,
  isNew: false,
  saving: false
})

const emit = defineEmits<Emits>()

// 模块类型
const sectionType = computed(() => props.section?.type ?? '')

// 是否为聚合类型
const isAggregate = computed(() => {
  return ['EDUCATION', 'WORK', 'PROJECT', 'CERTIFICATE'].includes(sectionType.value)
})

// 弹窗标题
const modalTitle = computed(() => {
  const baseTitles: Record<string, string> = {
    BASIC_INFO: '基本信息',
    SKILLS: '技能',
    EDUCATION: '教育经历',
    WORK: '工作经历',
    PROJECT: '项目经历',
    CERTIFICATE: '证书/荣誉'
  }
  const baseTitle = baseTitles[sectionType.value] || '模块'
  if (isAggregate.value) {
    return props.isNew ? `添加${baseTitle}` : `编辑${baseTitle}`
  }
  return `编辑${baseTitle}`
})

// 表单数据（深拷贝）
const formData = ref<Record<string, unknown>>({})

// 监听 section/itemId 变化，深拷贝数据到表单
watch(
  [() => props.section, () => props.itemId, () => props.visible],
  ([newSection, newItemId]) => {
    if (newSection && props.visible) {
      // 单条类型（BASIC_INFO、SKILLS）
      if (!isAggregate.value) {
        if (newSection.content) {
          formData.value = JSON.parse(JSON.stringify(newSection.content))
        } else {
          formData.value = {}
        }
        return
      }
      // 聚合类型：根据 itemId 找到对应的 item
      if (newSection.items && newItemId) {
        const item = newSection.items.find((i) => i.id === newItemId)
        if (item) {
          formData.value = JSON.parse(JSON.stringify(item.content))
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
  emit('save', {
    content: formData.value,
    itemId: props.itemId ?? undefined,
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
  z-index: 1000;
  backdrop-filter: blur(4px);
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
