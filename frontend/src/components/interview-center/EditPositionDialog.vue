<template>
  <Teleport to="body">
    <div class="dialog-overlay" @click.self="$emit('close')">
      <div class="dialog-content">
        <header class="dialog-header">
          <h2>编辑职位</h2>
          <button class="close-btn" @click="$emit('close')">×</button>
        </header>

        <form class="dialog-form" @submit.prevent="handleSubmit">
          <div class="form-group">
            <label class="form-label required">公司名称</label>
            <input
              v-model="form.companyName"
              type="text"
              class="form-input"
              placeholder="请输入公司名称"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label required">职位名称</label>
            <input
              v-model="form.title"
              type="text"
              class="form-input"
              placeholder="请输入职位名称"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label">JD 内容（可选）</label>
            <textarea
              v-model="form.jdContent"
              class="form-textarea"
              placeholder="粘贴职位描述..."
              rows="6"
            ></textarea>
          </div>
        </form>

        <footer class="dialog-footer">
          <button type="button" class="btn btn-secondary" @click="$emit('close')">取消</button>
          <button
            type="button"
            class="btn btn-primary"
            :disabled="submitting || !isFormValid"
            @click="handleSubmit"
          >
            {{ submitting ? '保存中...' : '保存' }}
          </button>
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { useScrollLock } from '@vueuse/core'
import { updateJobPosition } from '@/api/job-position'
import type { JobPositionDetail, UpdateJobPositionRequest } from '@/types/job-position'

const props = defineProps<{
  position: JobPositionDetail
}>()

const emit = defineEmits<{
  close: []
  updated: [position: JobPositionDetail]
}>()

const submitting = ref(false)

// 锁定背景滚动，防止滚动穿透
const isScrollLocked = useScrollLock(document.body)

const form = reactive<UpdateJobPositionRequest>({
  companyName: '',
  title: '',
  jdContent: ''
})

// 初始化表单数据
watch(() => props.position, (pos) => {
  if (pos) {
    form.companyName = pos.companyName || ''
    form.title = pos.title || ''
    form.jdContent = pos.jdContent || ''
  }
}, { immediate: true })

// 组件挂载时锁定滚动
onMounted(() => {
  isScrollLocked.value = true
})

// 组件卸载时解锁滚动
onUnmounted(() => {
  isScrollLocked.value = false
})

const isFormValid = computed(() => {
  return form.companyName?.trim() && form.title?.trim()
})

async function handleSubmit() {
  if (submitting.value || !isFormValid.value) return

  submitting.value = true
  try {
    const result = await updateJobPosition(props.position.id, {
      companyName: form.companyName?.trim(),
      title: form.title?.trim(),
      jdContent: form.jdContent?.trim() || undefined
    })
    emit('updated', result)
  } catch (error) {
    console.error('更新职位失败:', error)
    alert('更新失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: $z-modal-overlay;
}

.dialog-content {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-lg;
  border-bottom: 1px solid $color-bg-tertiary;
  flex-shrink: 0;

  h2 {
    font-size: 1.25rem;
    font-weight: 600;
    color: $color-text-primary;
  }

  .close-btn {
    background: none;
    border: none;
    color: $color-text-tertiary;
    font-size: 1.5rem;
    cursor: pointer;
    line-height: 1;

    &:hover {
      color: $color-text-primary;
    }
  }
}

.dialog-form {
  padding: $spacing-lg;
  flex: 1;
  overflow-y: auto;
}

.form-group {
  margin-bottom: $spacing-lg;

  &:last-child {
    margin-bottom: 0;
  }
}

.form-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: $color-text-secondary;
  margin-bottom: $spacing-sm;

  &.required::after {
    content: ' *';
    color: $color-error;
  }
}

.form-input, .form-textarea {
  width: 100%;
  padding: $spacing-md;
  background: $color-bg-tertiary;
  border: 1px solid $color-bg-tertiary;
  border-radius: $radius-md;
  color: $color-text-primary;
  font-size: 0.875rem;
  transition: border-color 0.2s;

  &:focus {
    outline: none;
    border-color: $color-accent;
  }

  &::placeholder {
    color: $color-text-tertiary;
  }
}

.form-textarea {
  resize: vertical;
  min-height: 100px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: $spacing-md;
  padding: $spacing-lg;
  border-top: 1px solid $color-bg-tertiary;
  flex-shrink: 0;
}
</style>
