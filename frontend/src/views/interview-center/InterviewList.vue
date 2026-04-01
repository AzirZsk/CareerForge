<template>
  <div class="interview-list-page">
    <header class="page-header">
      <h1 class="page-title">面试中心</h1>
      <div class="header-actions">
        <button class="btn btn-primary" @click="showCreateDialog = true">
          <span class="icon">+</span>
          <span>新建面试</span>
        </button>
      </div>
    </header>

    <!-- 职位分组视图 -->
    <div class="position-view" v-if="!loading">
      <div class="position-list">
        <JobPositionCard
          v-for="position in jobPositions"
          :key="position.id"
          :job-position="position"
          @click="goToPositionDetail(position.id)"
          @add-interview="handleAddInterviewForPosition"
          @view-detail="goToPositionDetail(position.id)"
          @delete="handleDeletePosition"
        />
      </div>

      <div v-if="jobPositions.length === 0" class="empty-state">
        <p>暂无职位记录</p>
        <p class="hint">创建面试时会自动关联或创建职位</p>
        <button class="btn btn-primary" @click="showCreateDialog = true">
          创建第一个面试
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <p>加载中...</p>
    </div>

    <CreateInterviewDialog
      v-if="showCreateDialog"
      :preselected-position-id="preselectedPositionId"
      @close="handleCloseCreateDialog"
      @created="handleInterviewCreated"
    />

    <ConfirmModal
      :visible="confirmVisible"
      :title="confirmTitle"
      :message="confirmMessage"
      :confirm-text="confirmText"
      :danger="confirmDanger"
      @confirm="handleConfirm"
      @cancel="handleCancel"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getJobPositionList, deleteJobPosition } from '@/api/job-position'
import type { JobPositionListItem } from '@/types/job-position'
import CreateInterviewDialog from '@/components/interview-center/CreateInterviewDialog.vue'
import JobPositionCard from '@/components/interview-center/JobPositionCard.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useToast } from '@/composables/useToast'
import ConfirmModal from '@/components/common/ConfirmModal.vue'

const { confirm, visible: confirmVisible, title: confirmTitle, message: confirmMessage, confirmText, danger: confirmDanger, handleConfirm, handleCancel } = useConfirm()
const toast = useToast()

const router = useRouter()
const loading = ref(false)
const showCreateDialog = ref(false)
const preselectedPositionId = ref<string | null>(null)
const jobPositions = ref<JobPositionListItem[]>([])

function goToPositionDetail(id: string) {
  router.push(`/interview-center/position/${id}`)
}

function handleAddInterviewForPosition(position: JobPositionListItem) {
  preselectedPositionId.value = position.id
  showCreateDialog.value = true
}

function handleCloseCreateDialog() {
  showCreateDialog.value = false
  preselectedPositionId.value = null
}

function handleInterviewCreated(id: string) {
  showCreateDialog.value = false
  preselectedPositionId.value = null
  loadData()
  router.push(`/interview-center/${id}`)
}

async function handleDeletePosition(position: JobPositionListItem) {
  const confirmed = await confirm({
    title: '删除确认',
    message: `确定要删除职位「${position.companyName} - ${position.title}」吗？删除后无法恢复，该职位下的面试记录也会被删除`,
    danger: true
  })
  if (!confirmed) return

  try {
    await deleteJobPosition(position.id)
    toast.success('职位已删除')
    loadData()
  } catch (error) {
    const message = error instanceof Error ? error.message : '删除失败'
    toast.error(message)
  }
}

async function loadData() {
  loading.value = true
  try {
    const positionData = await getJobPositionList({ page: 1, size: 100 })
    jobPositions.value = positionData?.list || []
  } catch (error) {
    console.error('加载数据失败:', error)
    jobPositions.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.interview-list-page {
  padding: $spacing-xl;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xl;
}

.page-title {
  font-size: 2rem;
  font-weight: 700;
  color: $color-text-primary;
}

.position-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: $spacing-lg;
}

.empty-state, .loading-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: $spacing-2xl;
  color: $color-text-tertiary;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-md;

  .hint {
    font-size: 0.875rem;
    color: $color-text-tertiary;
  }
}
</style>
