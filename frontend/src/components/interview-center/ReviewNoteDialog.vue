<template>
  <Teleport to="body">
    <div class="dialog-overlay" @click.self="$emit('close')">
    <div class="dialog-content">
      <header class="dialog-header">
        <h2>{{ isEditing ? '编辑复盘笔记' : '添加复盘笔记' }}</h2>
        <button class="close-btn" @click="$emit('close')">×</button>
      </header>

      <form class="dialog-form" @submit.prevent="handleSubmit">
        <div class="form-group">
          <label class="form-label">整体感受</label>
          <textarea
            v-model="form.overallFeeling"
            class="form-textarea"
            placeholder="这次面试的整体感受如何？"
            rows="3"
          ></textarea>
        </div>

        <div class="form-group">
          <label class="form-label">好的方面</label>
          <textarea
            v-model="form.highPoints"
            class="form-textarea"
            placeholder="面试中表现得比较好的地方..."
            rows="3"
          ></textarea>
        </div>

        <div class="form-group">
          <label class="form-label">不足之处</label>
          <textarea
            v-model="form.weakPoints"
            class="form-textarea"
            placeholder="面试中表现不够好的地方..."
            rows="3"
          ></textarea>
        </div>

        <div class="form-group">
          <label class="form-label">经验教训</label>
          <textarea
            v-model="form.lessonsLearned"
            class="form-textarea"
            placeholder="从这次面试中学到了什么？"
            rows="3"
          ></textarea>
        </div>
      </form>

      <footer class="dialog-footer">
        <button type="button" class="btn btn-secondary" @click="$emit('close')">取消</button>
        <button
          type="button"
          class="btn btn-primary"
          :disabled="submitting"
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
import { ref, reactive, watch, computed, onMounted, onUnmounted } from 'vue'
import { useScrollLock } from '@vueuse/core'
import { saveReviewNote } from '@/api/interview-center'
import type { SaveReviewNoteRequest, ReviewNoteVO } from '@/types/interview-center'
import { useToast } from '@/composables/useToast'

const toast = useToast()

const props = defineProps<{
  interviewId: string
  existingNote?: ReviewNoteVO | null
}>()

const emit = defineEmits<{
  close: []
  saved: []
}>()

const submitting = ref(false)

// 锁定背景滚动，防止滚动穿透
const isScrollLocked = useScrollLock(document.body)

const isEditing = computed(() => !!props.existingNote)

const form = reactive<SaveReviewNoteRequest>({
  overallFeeling: '',
  highPoints: '',
  weakPoints: '',
  lessonsLearned: ''
})

// 监听现有笔记变化，填充表单
watch(() => props.existingNote, (note) => {
  if (note) {
    form.overallFeeling = note.overallFeeling || ''
    form.highPoints = note.highPoints || ''
    form.weakPoints = note.weakPoints || ''
    form.lessonsLearned = note.lessonsLearned || ''
  } else {
    // 重置表单
    form.overallFeeling = ''
    form.highPoints = ''
    form.weakPoints = ''
    form.lessonsLearned = ''
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

async function handleSubmit() {
  if (submitting.value) return

  submitting.value = true
  try {
    await saveReviewNote(props.interviewId, form)
    emit('saved')
    emit('close')
  } catch (error) {
    console.error('保存复盘笔记失败:', error)
    toast.error('保存失败，请稍后重试')
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
  max-width: 550px;
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
}

.form-textarea {
  width: 100%;
  padding: $spacing-md;
  background: $color-bg-tertiary;
  border: 1px solid $color-bg-tertiary;
  border-radius: $radius-md;
  color: $color-text-primary;
  font-size: 0.875rem;
  transition: border-color 0.2s;
  resize: vertical;
  min-height: 80px;

  &:focus {
    outline: none;
    border-color: $color-accent;
  }

  &::placeholder {
    color: $color-text-tertiary;
  }
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
