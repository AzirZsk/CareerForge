<template>
  <Teleport to="body">
    <div class="dialog-overlay" @click.self="$emit('close')">
      <div class="dialog-content">
        <header class="dialog-header">
          <h2>创建面试</h2>
          <button class="close-btn" @click="$emit('close')">×</button>
        </header>

        <form class="dialog-form" @submit.prevent="handleSubmit">
          <!-- 模式切换 -->
          <div class="mode-switch">
            <button
              type="button"
              :class="['mode-btn', { active: mode === 'select' }]"
              @click="mode = 'select'"
            >
              选择已有职位
            </button>
            <button
              type="button"
              :class="['mode-btn', { active: mode === 'new' }]"
              @click="mode = 'new'"
            >
              新建职位
            </button>
          </div>

          <!-- 选择已有职位模式 -->
          <template v-if="mode === 'select'">
            <div class="form-group">
              <label class="form-label required">选择职位</label>
              <div class="position-select" v-if="!loadingPositions">
                <select v-model="selectedPositionId" class="form-select" required>
                  <option value="">请选择职位</option>
                  <option
                    v-for="pos in jobPositions"
                    :key="pos.id"
                    :value="pos.id"
                  >
                    {{ pos.companyName }} - {{ pos.title }}
                  </option>
                </select>
              </div>
              <div v-else class="loading-positions">
                加载职位列表...
              </div>
            </div>

            <div class="selected-info" v-if="selectedPosition">
              <div class="info-item">
                <span class="info-label">公司</span>
                <span class="info-value">{{ selectedPosition.companyName }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">职位</span>
                <span class="info-value">{{ selectedPosition.title }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">历史面试</span>
                <span class="info-value">{{ selectedPosition.interviewCount }} 次</span>
              </div>
            </div>
          </template>

          <!-- 新建职位模式 -->
          <template v-else>
            <div class="form-group">
              <label class="form-label required">公司名称</label>
              <input
                v-model="form.companyName"
                type="text"
                class="form-input"
                placeholder="请输入公司名称"
                :required="mode === 'new'"
              />
            </div>

            <div class="form-group">
              <label class="form-label required">目标岗位</label>
              <input
                v-model="form.position"
                type="text"
                class="form-input"
                placeholder="请输入目标岗位"
                :required="mode === 'new'"
              />
            </div>

            <div class="form-group">
              <label class="form-label">JD 内容（可选）</label>
              <textarea
                v-model="form.jdContent"
                class="form-textarea"
                placeholder="粘贴职位描述..."
                rows="4"
              ></textarea>
            </div>
          </template>

          <!-- 通用字段 -->
          <div class="form-group">
            <label class="form-label required">轮次类型</label>
            <select v-model="form.roundType" class="form-select" required>
              <option v-for="(label, type) in ROUND_TYPE_LABELS" :key="type" :value="type">
                {{ label }}
              </option>
            </select>
          </div>

          <div v-if="form.roundType === 'custom'" class="form-group">
            <label class="form-label required">轮次名称</label>
            <input
              v-model="form.roundName"
              type="text"
              class="form-input"
              placeholder="请输入轮次名称"
              :required="form.roundType === 'custom'"
            />
          </div>

          <div class="form-group">
            <label class="form-label">面试类型</label>
            <div class="interview-type-switch">
              <button
                type="button"
                :class="['type-btn', { active: !form.interviewType || form.interviewType === 'onsite' }]"
                @click="form.interviewType = 'onsite'"
              >
                现场面试
              </button>
              <button
                type="button"
                :class="['type-btn', { active: form.interviewType === 'online' }]"
                @click="form.interviewType = 'online'"
              >
                线上面试
              </button>
            </div>
          </div>

          <!-- 现场面试地址 -->
          <div v-if="form.interviewType === 'onsite' || !form.interviewType" class="form-group">
            <label class="form-label">面试地址</label>
            <input
              v-model="form.location"
              type="text"
              class="form-input"
              placeholder="请输入面试地址"
            />
          </div>

          <!-- 线上面试信息 -->
          <template v-if="form.interviewType === 'online'">
            <div class="form-group">
              <label class="form-label">面试链接</label>
              <input
                v-model="form.onlineLink"
                type="text"
                class="form-input"
                placeholder="如 Zoom/腾讯会议链接"
              />
            </div>
            <div class="form-group">
              <label class="form-label">会议密码</label>
              <input
                v-model="form.meetingPassword"
                type="text"
                class="form-input"
                placeholder="会议密码/会议号"
              />
            </div>
          </template>

          <div class="form-group">
            <label class="form-label required">面试时间</label>
            <DateTimePicker v-model="form.interviewDate" />
          </div>

          <div class="form-group">
            <label class="form-label">备注</label>
            <textarea
              v-model="form.notes"
              class="form-textarea"
              placeholder="其他备注..."
              rows="2"
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
            {{ submitting ? '创建中...' : '创建' }}
          </button>
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { useScrollLock } from '@vueuse/core'
import { createInterview } from '@/api/interview-center'
import { useToast } from '@/composables/useToast'
import { getJobPositionList } from '@/api/job-position'
import type { CreateInterviewRequest, RoundType, InterviewType } from '@/types/interview-center'
import { ROUND_TYPE_LABELS } from '@/types/interview-center'
import type { JobPositionListItem } from '@/types/job-position'
import DateTimePicker from '@/components/common/DateTimePicker.vue'

const props = defineProps<{
  preselectedPositionId?: string | null
}>()

const emit = defineEmits<{
  close: []
  created: [id: string]
}>()

const mode = ref<'select' | 'new'>('select')
const submitting = ref(false)
const loadingPositions = ref(false)
const jobPositions = ref<JobPositionListItem[]>([])
const selectedPositionId = ref('')
const toast = useToast()

// 锁定背景滚动，防止滚动穿透
const isScrollLocked = useScrollLock(document.body)

const form = reactive<CreateInterviewRequest>({
  jobPositionId: '',
  companyName: '',
  position: '',
  interviewDate: '',
  roundType: 'technical_1' as RoundType,
  roundName: '',
  interviewType: 'onsite' as InterviewType,
  location: '',
  onlineLink: '',
  meetingPassword: '',
  jdContent: '',
  notes: ''
})

const selectedPosition = computed(() => {
  if (!selectedPositionId.value) return null
  return jobPositions.value.find(p => p.id === selectedPositionId.value)
})

const isFormValid = computed(() => {
  if (!form.interviewDate) return false
  // 自定义轮次必须填写名称
  if (form.roundType === 'custom' && !form.roundName?.trim()) {
    return false
  }
  if (mode.value === 'select') {
    return !!selectedPositionId.value
  } else {
    return form.companyName.trim() && form.position.trim()
  }
})

// 监听选中职位变化
watch(selectedPositionId, (id) => {
  form.jobPositionId = id
  if (selectedPosition.value) {
    form.companyName = selectedPosition.value.companyName
    form.position = selectedPosition.value.title
  }
})

// 监听模式切换，清空相关数据
watch(mode, (newMode) => {
  if (newMode === 'select') {
    form.companyName = ''
    form.position = ''
    form.jdContent = ''
    form.jobPositionId = selectedPositionId.value
  } else {
    selectedPositionId.value = ''
    form.jobPositionId = ''
  }
})

// 如果有预选职位ID，保持在选择模式并自动选中
watch(() => props.preselectedPositionId, (id) => {
  if (id) {
    mode.value = 'select'
    selectedPositionId.value = id
  }
}, { immediate: true })

async function loadPositions() {
  loadingPositions.value = true
  try {
    const result = await getJobPositionList({ page: 1, size: 100 })
    jobPositions.value = result?.list || []
  } catch (error) {
    console.error('加载职位列表失败:', error)
    jobPositions.value = []
  } finally {
    loadingPositions.value = false
  }
}

async function handleSubmit() {
  if (submitting.value || !isFormValid.value) return

  submitting.value = true
  try {
    const requestData: CreateInterviewRequest = {
      ...form,
      jobPositionId: mode.value === 'select' ? selectedPositionId.value : undefined
    }
    const result = await createInterview(requestData)
    emit('created', result.id)
  } catch (error) {
    console.error('创建面试失败:', error)
    toast.error('创建失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadPositions()
  // 弹窗打开时锁定滚动
  isScrollLocked.value = true
})

onUnmounted(() => {
  // 弹窗关闭时解锁滚动
  isScrollLocked.value = false
})
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

.mode-switch {
  display: flex;
  gap: $spacing-sm;
  margin-bottom: $spacing-lg;
  background: $color-bg-tertiary;
  padding: 4px;
  border-radius: $radius-md;
}

.mode-btn {
  flex: 1;
  padding: $spacing-sm $spacing-md;
  border: none;
  background: transparent;
  color: $color-text-secondary;
  font-size: 0.875rem;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: all 0.2s;

  &.active {
    background: $color-accent;
    color: $color-bg-primary;
  }

  &:hover:not(.active) {
    color: $color-text-primary;
  }
}

.interview-type-switch {
  display: flex;
  gap: $spacing-sm;
  background: $color-bg-tertiary;
  padding: 4px;
  border-radius: $radius-md;
}

.type-btn {
  flex: 1;
  padding: $spacing-sm $spacing-md;
  border: none;
  background: transparent;
  color: $color-text-secondary;
  font-size: 0.875rem;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: all 0.2s;

  &.active {
    background: $color-accent;
    color: $color-bg-primary;
  }

  &:hover:not(.active) {
    color: $color-text-primary;
  }
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

.form-input, .form-textarea, .form-select {
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

.form-select {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2371717a' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 32px;
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.selected-info {
  background: $color-bg-tertiary;
  border-radius: $radius-md;
  padding: $spacing-md;
  margin-bottom: $spacing-lg;
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.info-item {
  display: flex;
  justify-content: space-between;
  font-size: 0.875rem;
}

.info-label {
  color: $color-text-tertiary;
}

.info-value {
  color: $color-text-primary;
  font-weight: 500;
}

.loading-positions {
  padding: $spacing-md;
  text-align: center;
  color: $color-text-tertiary;
  font-size: 0.875rem;
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
