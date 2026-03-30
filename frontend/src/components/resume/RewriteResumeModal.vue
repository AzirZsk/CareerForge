<!--=====================================================
  LandIt 简历风格改写弹窗
  @author Azir
// =====================================================-->

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="visible"
        class="modal-overlay"
        :class="{ 'modal-overlay--high': overlay }"
        @click.self="handleClose"
      >
        <div
          class="modal-container"
          :class="{
            'modal-container--fullscreen': rewriteState.isRewriting || rewriteState.isCompleted
          }"
        >
          <!-- 头部 -->
          <ModalHeader
            :is-optimizing="rewriteState.isRewriting"
            :is-completed="rewriteState.isCompleted"
            :has-error="rewriteState.hasError"
            custom-title="风格改写"
            @close="handleClose"
          />

          <!-- 表单（未开始时显示） -->
          <div
            v-if="!rewriteState.isRewriting && !rewriteState.isCompleted"
            class="form-section"
          >
            <div class="file-upload-area">
              <input
                ref="fileInput"
                type="file"
                accept=".pdf,.doc,.docx"
                class="hidden"
                @change="handleFileChange"
              />
              <div
                v-if="!formData.referenceFile"
                class="upload-placeholder"
                @click="($refs.fileInput as HTMLInputElement).click()"
              >
                <svg
                  width="48"
                  height="48"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.5"
                >
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                  <polyline points="14 2 14 8 20 8" />
                  <line x1="12" y1="18" x2="12" y2="12" />
                  <line x1="9" y1="15" x2="15" y2="15" />
                </svg>
                <p>点击或拖拽文件到此处</p>
              </div>

              <!-- 已上传文件显示 -->
              <div
                v-else
                class="file-info"
              >
                <svg
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path d="M14 2H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                  <path d="M12 3v12c0-.55.45-1 1-1h3.5l5 5v11a5a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                  <path d="M20 21H5a2 2 0 0 1-2-2v-7" />
                  <polyline points="17 21 17 13 7 14 7 8 15 8" />
                </svg>
                <span class="file-name">{{ formData.referenceFileName }}</span>
                <button
                  class="remove-btn"
                  @click="removeFile"
                >
                  <svg
                    width="16"
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <line x1="18" y1="6" x2="6" y2="18" />
                    <line x1="6" y1="6" x2="18" y2="18" />
                  </svg>
                </button>
              </div>
            </div>

            <!-- 上传错误 -->
            <div
              v-if="uploadError"
              class="upload-error"
            >
              {{ uploadError }}
            </div>

            <!-- 提交按钮 -->
            <div class="submit-section">
              <button
                class="submit-btn"
                :disabled="!formData.tempKey || isLoading"
                @click="startRewriteResume"
              >
                <svg
                  v-if="isLoading"
                  class="spinner"
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <circle
                    cx="12"
                    cy="12"
                    r="10"
                  />
                </svg>
                {{ isLoading ? '解析中...' : '开始改写' }}
              </button>
              <p
                v-if="!formData.referenceFile"
                class="submit-hint"
              >
                请先上传参考简历
              </p>
            </div>
          </div>

          <!-- 进度展示（进行中/完成时显示） -->
          <template v-else>
            <!-- 参考简历文件名 -->
            <div
              v-if="formData.referenceFileName"
              class="target-info"
            >
              <span class="target-label">参考简历</span>
              <span class="target-value">{{ formData.referenceFileName }}</span>
            </div>

            <!-- 进度条 -->
            <ProgressBar
              :progress="rewriteState.progress"
              :message="rewriteState.message"
            />

            <!-- 阶段列表 -->
            <div class="stages-list">
              <RewriteStageItem
                v-for="item in sortedStageHistory"
                :key="item.stage"
                :item="item"
                :is-active="item.stage === rewriteState.currentStage && rewriteState.isRewriting"
                :completed="item.completed"
                :elapsed="formatElapsed(item)"
                @toggle-expand="toggleStageExpanded(item.stage)"
              />
            </div>

            <!-- 错误信息 -->
            <div
              v-if="rewriteState.hasError"
              class="error-section"
            >
              <svg
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <circle
                  cx="12"
                  cy="12"
                  r="10"
                />
                <line
                  x1="12"
                  y1="8"
                  x2="12"
                  y2="12"
                />
                <line
                  x1="12"
                  y1="16"
                  x2="12.01"
                  y2="16"
                />
              </svg>
              <span>{{ rewriteState.errorMessage }}</span>
            </div>
          </template>

          <!-- 底部操作 -->
          <div
            v-if="rewriteState.isRewriting || rewriteState.isCompleted"
            class="modal-footer"
          >
            <template v-if="rewriteState.hasError">
              <button
                class="footer-btn secondary"
                @click="handleClose"
              >
                退出
              </button>
            </template>
            <template v-else-if="rewriteState.isRewriting">
              <button
                class="footer-btn secondary"
                @click="handleCancel"
              >
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <line
                    x1="18"
                    y1="6"
                    x2="6"
                    y2="18"
                  />
                  <line
                    x1="6"
                    y1="6"
                    x2="18"
                    y2="18"
                  />
                </svg>
                取消改写
              </button>
            </template>
            <template v-else-if="rewriteState.isCompleted">
              <button
                class="footer-btn secondary"
                @click="handleClose"
              >
                不保存
              </button>
              <button
                class="footer-btn primary"
                @click="handleApplyRewrite"
              >
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <polyline points="20 6 9 17 4 12" />
                  <polyline points="10 11 6 6 4" />
                </svg>
                应用改写
              </button>
            </template>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { REWRITE_STAGE_CONFIG } from '@/types/resume-rewrite'
import ModalHeader from './optimize/ModalHeader.vue'
import ProgressBar from './optimize/ProgressBar.vue'
import RewriteStageItem from './rewrite/RewriteStageItem.vue'
import { useResumeRewrite } from '@/composables/useResumeRewrite'
import { useToast } from '@/composables/useToast'
import { parseReferenceResume } from '@/api/resume'

// Props
const props = defineProps<{
  visible: boolean
  resumeId: string
  overlay?: boolean
}>()

const emit = defineEmits(['update:visible'])

// Toast
const toast = useToast()

// Composable
const {
  state: rewriteState,
  startRewrite,
  cancelRewrite,
  resetState,
  toggleStageExpanded,
  applyChanges
} = useResumeRewrite()

// 表单数据
const formData = ref({
  referenceFile: null as File | null,
  referenceFileName: null as string | null,
  tempKey: null as string | null
})

// 上传相关状态
const isLoading = ref(false)
const uploadError = ref<string | null>(null)

// 计算排序后的阶段历史
const sortedStageHistory = computed(() => {
  return [...rewriteState.stageHistory]
    .sort((a, b) => {
      const orderA = REWRITE_STAGE_CONFIG[a.stage]?.order ?? 0
      const orderB = REWRITE_STAGE_CONFIG[b.stage]?.order ?? 0
      return orderA - orderB
    })
})

// 监听可见性变化
watch(() => props.visible, (val) => {
  if (val) {
    // 重置状态
    resetState()
    formData.value = {
      referenceFile: null,
      referenceFileName: null,
      tempKey: null
    }
  } else {
    // 关闭连接
    cancelRewrite()
  }
})

// 处理文件选择
async function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  // 验证文件类型
  const allowedTypes = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document']
  if (!allowedTypes.includes(file.type)) {
    toast.error('请上传 PDF 或 Word 格式的简历文件')
    return
  }

  formData.value.referenceFile = file
  formData.value.referenceFileName = file.name
  formData.value.tempKey = null

  // 上传并解析文件
  isLoading.value = true
  uploadError.value = null

  try {
    // 调用统一的API模块解析参考简历
    const result = await parseReferenceResume(props.resumeId, file)
    formData.value.tempKey = result.tempKey
    toast.success('参考简历解析成功')
  } catch (error) {
    uploadError.value = error instanceof Error ? error.message : '上传失败'
    toast.error(uploadError.value)
  } finally {
    isLoading.value = false
  }
}

// 移除文件
function removeFile() {
  formData.value.referenceFile = null
  formData.value.referenceFileName = null
  formData.value.tempKey = null
}

// 开始风格改写
function startRewriteResume() {
  if (!formData.value.tempKey) {
    toast.error('请先上传参考简历')
    return
  }

  startRewrite(props.resumeId, formData.value.tempKey)
}

// 取消改写
function handleCancel() {
  cancelRewrite()
  toast.info('已取消改写')
}

// 关闭弹窗
function handleClose() {
  cancelRewrite()
  resetState()
  formData.value = {
    referenceFile: null,
    referenceFileName: null,
    tempKey: null
  }
  emit('update:visible', false)
}

// 应用改写
async function handleApplyRewrite() {
  const success = await applyChanges()
  if (success) {
    toast.success('风格改写已应用成功')
    handleClose()
  } else {
    toast.error('应用改写失败')
  }
}

// 格式化耗时
function formatElapsed(item: { startTime?: number; endTime?: number }): string {
  if (!item.startTime) return '-'
  const end = item.endTime || Date.now()
  const elapsed = Math.floor((end - item.startTime) / 1000)
  if (elapsed < 60) {
    return `${elapsed}秒`
  } else {
    const minutes = Math.floor(elapsed / 60)
    return `${minutes}分${elapsed % 60}秒`
  }
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.modal-overlay {
  position: fixed;
  inset: 0;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba($color-bg-primary, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;

  &.modal-overlay--high {
    z-index: 1100;
  }
}

.modal-container {
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  background: $color-bg-secondary;
  border-radius: $radius-xl;
  padding: $spacing-xl;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba($color-bg-elevated, 0.15);

  &--fullscreen {
    max-width: 95vw;
    max-height: 90vh;
  }
}

.form-section {
  padding: $spacing-lg;
}

.file-upload-area {
  border: 2px dashed rgba($color-border, 0.3);
  border-radius: $radius-lg;
  padding: $spacing-xl;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-sm;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    border-color: $color-accent;
    background: rgba($color-accent, 0.05);
  }

  &.has-file {
    border-color: $color-accent;
    background: rgba($color-accent, 0.1);
  }
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-sm;

  svg {
    color: $color-text-tertiary;
    margin-bottom: $spacing-sm;
  }

  p {
    color: $color-text-secondary;
    font-size: $text-sm;
  }
}

.file-info {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-md;
  background: rgba($color-bg-tertiary, 0.5);
  border-radius: $radius-md;
  width: 100%;

  svg {
    color: $color-accent;
  }

  .file-name {
    color: $color-text-primary;
    font-size: $text-sm;
    flex: 1;
  }

  .remove-btn {
    padding: $spacing-xs;
    background: transparent;
    border: none;
    color: $color-text-tertiary;
    cursor: pointer;
    transition: color $transition-fast;

    &:hover {
      color: $color-error;
    }
  }
}

.upload-error {
  color: $color-error;
  font-size: $text-sm;
  margin-top: $spacing-sm;
}

.submit-section {
  margin-top: $spacing-lg;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-sm;
}

.submit-btn {
  width: 100%;
  padding: $spacing-md $spacing-lg;
  background: $color-accent;
  color: $color-bg-deep;
  border: none;
  border-radius: $radius-md;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;

  &:hover:not(:disabled) {
    background: $color-accent-light;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .spinner {
    animation: spin 1s linear infinite;
  }
}

.submit-hint {
  color: $color-text-tertiary;
  font-size: $text-sm;
}

.target-info {
  padding: $spacing-md $spacing-lg;
  margin-bottom: $spacing-md;
  display: flex;
  align-items: center;
  gap: $spacing-sm;

  .target-label {
    color: $color-text-tertiary;
    font-size: $text-sm;
  }

  .target-value {
    color: $color-accent;
    font-weight: 500;
  }
}

.stages-list {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-lg;
  gap: $spacing-lg;
}

.error-section {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md $spacing-lg;
  background: rgba($color-error, 0.1);
  border-radius: $radius-md;
  color: $color-error;
  font-size: $text-sm;

  svg {
    flex-shrink: 0;
  }
}

.modal-footer {
  padding: $spacing-lg;
  border-top: 1px solid $color-border;
  display: flex;
  justify-content: flex-end;
  gap: $spacing-md;
}

.footer-btn {
  padding: $spacing-sm $spacing-md;
  border-radius: $radius-md;
  font-size: $text-sm;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: $spacing-xs;

  svg {
    flex-shrink: 0;
  }

  &.primary {
    background: $color-accent;
    color: $color-bg-deep;
    border: none;

    &:hover {
      background: $color-accent-light;
    }
  }

  &.secondary {
    background: $color-bg-tertiary;
    color: $color-text-secondary;
    border: 1px solid $color-border;

    &:hover {
      background: $color-bg-elevated;
    }
  }
}

.modal-enter-active,
.modal-leave-active {
  animation: modal 0.3s ease;
}

.modal-enter-from {
  opacity: 0;
  transform: scale(0.95);
}

.modal-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
