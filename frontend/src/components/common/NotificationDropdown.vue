<template>
  <Teleport to="body">
    <!-- 遮罩层 -->
    <Transition name="fade">
      <div
        v-if="isOpen"
        class="notification-overlay"
        @click="close"
      />
    </Transition>

    <!-- 下拉面板 -->
    <Transition name="slide">
      <div
        v-if="isOpen"
        class="notification-dropdown"
      >
        <!-- 头部 -->
        <div class="dropdown-header">
          <h3>任务通知</h3>
          <div class="header-actions">
            <button
              v-if="completedTasks.length > 0"
              class="btn-clear"
              @click="clearCompleted"
            >
              清理已完成
            </button>
          </div>
        </div>

        <!-- 任务列表 -->
        <div class="dropdown-content">
          <!-- 加载中 -->
          <div
            v-if="notificationStore.isLoading && tasks.length === 0"
            class="empty-state"
          >
            <div class="loading-icon">⏳</div>
            <p>加载中...</p>
          </div>

          <!-- 空状态 -->
          <div
            v-else-if="tasks.length === 0"
            class="empty-state"
          >
            <div class="empty-icon">📭</div>
            <p>暂无任务通知</p>
          </div>

          <!-- 任务列表 -->
          <div
            v-else
            class="task-list"
          >
            <div
              v-for="task in sortedTasks"
              :key="task.id"
              class="task-item"
              :class="`status-${task.status}`"
              @click="handleTaskClick(task)"
            >
              <!-- 任务图标 -->
              <div
                class="task-icon"
                :style="{ background: statusConfig[task.status]?.bgColor }"
              >
                <span v-if="task.status === 'running'" class="spinning">
                  {{ statusConfig[task.status]?.icon }}
                </span>
                <span v-else>{{ statusConfig[task.status]?.icon }}</span>
              </div>

              <!-- 任务内容 -->
              <div class="task-content">
                <div class="task-title">
                  {{ task.taskTypeLabel || TASK_TYPE_LABELS[task.taskType as TaskType] }}
                </div>
                <div class="task-message">{{ task.message }}</div>
                <div class="task-time">{{ formatTime(task.createdAt) }}</div>
              </div>

              <!-- 进度条（进行中时显示） -->
              <div
                v-if="task.status === 'running' || task.status === 'pending'"
                class="task-progress"
              >
                <div class="progress-bar">
                  <div
                    class="progress-fill"
                    :style="{ width: `${task.progress}%` }"
                  />
                </div>
                <span class="progress-text">{{ task.progress }}%</span>
              </div>

              <!-- 操作按钮 -->
              <div class="task-actions">
                <button
                  v-if="task.status === 'completed'"
                  class="btn-action btn-apply"
                  @click.stop="applyResult(task)"
                >
                  应用
                </button>
                <button
                  v-if="task.status === 'failed'"
                  class="btn-action btn-retry"
                  @click.stop="retryTask(task)"
                >
                  重试
                </button>
                <button
                  class="btn-action btn-dismiss"
                  @click.stop="dismissTask(task.id)"
                >
                  ×
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部 -->
        <div class="dropdown-footer">
          <span class="footer-hint">
            {{ runningTasks.length > 0 ? `${runningTasks.length} 个任务进行中` : '所有任务已完成' }}
          </span>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '@/stores/notification'
import { TASK_TYPE_LABELS, TASK_STATUS_CONFIG } from '@/types/notification'
import type { AsyncTask, TaskStatus, TaskType } from '@/types/notification'
import { useToast } from '@/composables/useToast'

defineProps<{
  isOpen: boolean
}>()

const emit = defineEmits<{
  close: []
  'apply-transcript': [task: AsyncTask]
}>()

const router = useRouter()
const notificationStore = useNotificationStore()
const toast = useToast()

// 状态配置
const statusConfig = TASK_STATUS_CONFIG

// 计算属性
const tasks = computed(() => notificationStore.tasks)
const runningTasks = computed(() => notificationStore.runningTasks)
const completedTasks = computed(() => notificationStore.completedTasks)

// 按时间和状态排序（进行中优先）
const sortedTasks = computed(() => {
  return [...tasks.value].sort((a, b) => {
    // 进行中优先
    const statusOrder: Record<TaskStatus, number> = {
      running: 0,
      pending: 1,
      completed: 2,
      failed: 3
    }
    const orderDiff = (statusOrder[a.status] ?? 99) - (statusOrder[b.status] ?? 99)
    if (orderDiff !== 0) return orderDiff
    // 按时间倒序
    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  })
})

// 方法
function close() {
  emit('close')
}

function formatTime(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  return date.toLocaleDateString()
}

function handleTaskClick(task: AsyncTask) {
  // 根据任务类型跳转
  if (task.taskType === 'audio_transcribe' && task.businessId) {
    router.push(`/interview-center/${task.businessId}`)
  }
  close()
}

function applyResult(task: AsyncTask) {
  // 应用转录结果（由父组件处理）
  if (task.taskType === 'audio_transcribe' && task.result) {
    emit('apply-transcript', task)
  }
  close()
}

function retryTask(_task: AsyncTask) {
  // TODO: 实现重试逻辑
  toast.info('重试功能开发中')
}

async function dismissTask(taskId: string) {
  notificationStore.removeTask(taskId)
}

async function clearCompleted() {
  await notificationStore.clearCompletedTasks()
  toast.success('已清理完成')
}
</script>

<style scoped lang="scss">
.notification-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
}

.notification-dropdown {
  position: fixed;
  top: 60px;
  right: 80px;
  width: 380px;
  max-height: 500px;
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md $spacing-lg;
  border-bottom: 1px solid $color-bg-elevated;

  h3 {
    font-size: 1rem;
    font-weight: 600;
    color: $color-text-primary;
    margin: 0;
  }

  .btn-clear {
    background: transparent;
    border: none;
    color: $color-text-tertiary;
    font-size: 0.75rem;
    cursor: pointer;
    padding: $spacing-xs $spacing-sm;
    border-radius: $radius-sm;
    transition: all 0.2s;

    &:hover {
      color: $color-accent;
      background: rgba($color-accent, 0.1);
    }
  }
}

.dropdown-content {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-sm;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $spacing-2xl;
  color: $color-text-tertiary;

  .empty-icon, .loading-icon {
    font-size: 2.5rem;
    margin-bottom: $spacing-md;
  }

  p {
    margin: 0;
    font-size: 0.875rem;
  }
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.task-item {
  display: flex;
  align-items: flex-start;
  gap: $spacing-md;
  padding: $spacing-md;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: $color-bg-tertiary;
  }

  &.status-running {
    border-left: 3px solid $color-warning;
  }

  &.status-completed {
    border-left: 3px solid $color-success;
  }

  &.status-failed {
    border-left: 3px solid $color-error;
  }

  &.status-pending {
    border-left: 3px solid $color-text-tertiary;
  }
}

.task-icon {
  width: 36px;
  height: 36px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  flex-shrink: 0;

  .spinning {
    display: inline-block;
    animation: spin 1s linear infinite;
  }
}

.task-content {
  flex: 1;
  min-width: 0;

  .task-title {
    font-size: 0.875rem;
    font-weight: 500;
    color: $color-text-primary;
    margin-bottom: 2px;
  }

  .task-message {
    font-size: 0.75rem;
    color: $color-text-secondary;
    margin-bottom: 4px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .task-time {
    font-size: 0.6875rem;
    color: $color-text-tertiary;
  }
}

.task-progress {
  width: 60px;
  text-align: right;

  .progress-bar {
    height: 4px;
    background: $color-bg-elevated;
    border-radius: 2px;
    overflow: hidden;
    margin-bottom: 4px;
  }

  .progress-fill {
    height: 100%;
    background: linear-gradient(90deg, $color-accent, $color-accent-light);
    transition: width 0.3s ease;
  }

  .progress-text {
    font-size: 0.6875rem;
    color: $color-accent;
    font-weight: 500;
  }
}

.task-actions {
  display: flex;
  gap: $spacing-xs;
  margin-left: $spacing-sm;
}

.btn-action {
  padding: 4px 8px;
  font-size: 0.6875rem;
  border-radius: $radius-sm;
  border: none;
  cursor: pointer;
  transition: all 0.2s;

  &.btn-apply {
    background: $color-accent;
    color: $color-bg-primary;

    &:hover {
      opacity: 0.9;
    }
  }

  &.btn-retry {
    background: rgba($color-warning, 0.2);
    color: $color-warning;

    &:hover {
      background: rgba($color-warning, 0.3);
    }
  }

  &.btn-dismiss {
    background: transparent;
    color: $color-text-tertiary;
    font-size: 1rem;
    padding: 0 4px;

    &:hover {
      color: $color-error;
    }
  }
}

.dropdown-footer {
  padding: $spacing-sm $spacing-lg;
  border-top: 1px solid $color-bg-elevated;
  text-align: center;

  .footer-hint {
    font-size: 0.75rem;
    color: $color-text-tertiary;
  }
}

// 动画
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.2s ease;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
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
