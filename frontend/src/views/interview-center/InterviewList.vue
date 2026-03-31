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

    <div class="view-tabs">
      <button
        :class="['tab', { active: viewMode === 'position' }]"
        @click="viewMode = 'position'"
      >
        按职位分组
      </button>
      <button
        :class="['tab', { active: viewMode === 'list' }]"
        @click="viewMode = 'list'"
      >
        面试列表
      </button>
    </div>

    <!-- 职位分组视图 -->
    <div class="position-view" v-if="viewMode === 'position' && !loading">
      <div class="position-list">
        <JobPositionCard
          v-for="position in jobPositions"
          :key="position.id"
          :job-position="position"
          @click="goToPositionDetail(position.id)"
          @add-interview="handleAddInterviewForPosition"
          @view-detail="goToPositionDetail(position.id)"
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

    <!-- 面试列表视图 -->
    <div class="list-view" v-if="viewMode === 'list' && !loading">
      <div class="filter-tabs">
        <button
          v-for="tab in sourceTabs"
          :key="tab.value"
          :class="['tab', { active: activeSourceTab === tab.value }]"
          @click="activeSourceTab = tab.value"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="interview-list">
        <div
          v-for="interview in filteredInterviews"
          :key="interview.id"
          class="interview-card"
          @click="goToDetail(interview.id)"
        >
          <div class="card-header">
            <span class="source-badge" :class="interview.source">
              {{ interview.source === 'real' ? '真实面试' : '模拟面试' }}
            </span>
            <span class="status-badge" :class="interview.status">
              {{ getStatusLabel(interview.status) }}
            </span>
          </div>
          <h3 class="company-name">{{ interview.companyName }}</h3>
          <p class="position">{{ interview.position }}</p>
          <div class="card-meta">
            <span class="date">{{ formatDateTime(interview.interviewDate) }}</span>
            <span class="rounds">{{ interview.completedRounds }}/{{ interview.roundCount }} 轮</span>
          </div>
        </div>

        <div v-if="filteredInterviews.length === 0" class="empty-state">
          <p>暂无面试记录</p>
          <button class="btn btn-primary" @click="showCreateDialog = true">
            创建第一个面试
          </button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <p>加载中...</p>
    </div>

    <CreateInterviewDialog
      v-if="showCreateDialog"
      :preselected-position="preselectedPosition"
      @close="handleCloseCreateDialog"
      @created="handleInterviewCreated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getInterviewList } from '@/api/interview-center'
import { getJobPositionList } from '@/api/job-position'
import { INTERVIEW_STATUS_LABELS, type InterviewListItem, type InterviewStatus } from '@/types/interview-center'
import type { JobPositionListItem } from '@/types/job-position'
import CreateInterviewDialog from '@/components/interview-center/CreateInterviewDialog.vue'
import JobPositionCard from '@/components/interview-center/JobPositionCard.vue'

const router = useRouter()
const loading = ref(false)
const showCreateDialog = ref(false)
const preselectedPosition = ref<{ companyName: string; title: string } | null>(null)
const interviews = ref<InterviewListItem[]>([])
const jobPositions = ref<JobPositionListItem[]>([])
const viewMode = ref<'position' | 'list'>('position')
const activeSourceTab = ref<string>('all')

const sourceTabs = [
  { value: 'all', label: '全部' },
  { value: 'real', label: '真实面试' },
  { value: 'mock', label: '模拟面试' }
]

const filteredInterviews = computed(() => {
  if (activeSourceTab.value === 'all') return interviews.value
  return interviews.value.filter(i => i.source === activeSourceTab.value)
})

function getStatusLabel(status: InterviewStatus): string {
  return INTERVIEW_STATUS_LABELS[status] || status
}

function formatDateTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function goToDetail(id: string) {
  router.push(`/interview-center/${id}`)
}

function goToPositionDetail(id: string) {
  // 暂时跳转到面试详情页，后续可以增加职位详情页
  router.push(`/interview-center?position=${id}`)
}

function handleAddInterviewForPosition(position: JobPositionListItem) {
  preselectedPosition.value = {
    companyName: position.companyName,
    title: position.title
  }
  showCreateDialog.value = true
}

function handleCloseCreateDialog() {
  showCreateDialog.value = false
  preselectedPosition.value = null
}

function handleInterviewCreated(id: string) {
  showCreateDialog.value = false
  preselectedPosition.value = null
  loadData()
  router.push(`/interview-center/${id}`)
}

async function loadData() {
  loading.value = true
  try {
    // 并行加载职位列表和面试列表
    const [positionData, interviewData] = await Promise.all([
      getJobPositionList({ page: 1, size: 100 }),
      getInterviewList({ type: 'all', page: 1, size: 100 })
    ])
    jobPositions.value = positionData?.list || []
    interviews.value = interviewData?.list || []
  } catch (error) {
    console.error('加载数据失败:', error)
    jobPositions.value = []
    interviews.value = []
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

.view-tabs {
  display: flex;
  gap: $spacing-sm;
  margin-bottom: $spacing-lg;
}

.tab {
  padding: $spacing-sm $spacing-lg;
  border-radius: $radius-full;
  background: $color-bg-secondary;
  color: $color-text-secondary;
  border: none;
  cursor: pointer;
  transition: all 0.2s;

  &.active {
    background: $color-accent;
    color: $color-bg-primary;
  }
}

.filter-tabs {
  display: flex;
  gap: $spacing-sm;
  margin-bottom: $spacing-lg;
}

.position-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: $spacing-lg;
}

.interview-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: $spacing-lg;
}

.interview-card {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;

  &:hover {
    border-color: $color-accent;
    transform: translateY(-2px);
  }
}

.interview-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-md;
}

.source-badge {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: $radius-sm;

  &.real {
    background: rgba($color-accent, 0.2);
    color: $color-accent;
  }

  &.mock {
    background: rgba($color-info, 0.2);
    color: $color-info;
  }
}

.status-badge {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: $radius-sm;
  background: $color-bg-tertiary;
  color: $color-text-secondary;

  &.completed { color: $color-success; }
  &.cancelled { color: $color-error; }
}

.interview-card .company-name {
  font-size: 1.25rem;
  font-weight: 600;
  color: $color-text-primary;
  margin-bottom: $spacing-xs;
}

.interview-card .position {
  color: $color-text-secondary;
  margin-bottom: $spacing-md;
}

.interview-card .card-meta {
  display: flex;
  justify-content: space-between;
  font-size: 0.875rem;
  color: $color-text-tertiary;
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
