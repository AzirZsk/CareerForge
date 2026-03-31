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

    <div class="filter-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        :class="['tab', { active: activeTab === tab.value }]"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
      </button>
    </div>

    <div class="interview-list" v-if="!loading">
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
          <span class="date">{{ interview.interviewDate }}</span>
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

    <div v-else class="loading-state">
      <p>加载中...</p>
    </div>

    <CreateInterviewDialog
      v-if="showCreateDialog"
      @close="showCreateDialog = false"
      @created="handleInterviewCreated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getInterviewList } from '@/api/interview-center'
import { INTERVIEW_STATUS_LABELS, type InterviewListItem, type InterviewStatus } from '@/types/interview-center'
import CreateInterviewDialog from '@/components/interview-center/CreateInterviewDialog.vue'

const router = useRouter()
const loading = ref(false)
const showCreateDialog = ref(false)
const interviews = ref<InterviewListItem[]>([])
const activeTab = ref<string>('all')

const tabs = [
  { value: 'all', label: '全部' },
  { value: 'real', label: '真实面试' },
  { value: 'mock', label: '模拟面试' }
]

const filteredInterviews = computed(() => {
  if (activeTab.value === 'all') return interviews.value
  return interviews.value.filter(i => i.source === activeTab.value)
})

function getStatusLabel(status: InterviewStatus): string {
  return INTERVIEW_STATUS_LABELS[status] || status
}

function goToDetail(id: string) {
  router.push(`/interview-center/${id}`)
}

function handleInterviewCreated(id: string) {
  showCreateDialog.value = false
  loadInterviews()
  router.push(`/interview-center/${id}`)
}

async function loadInterviews() {
  loading.value = true
  try {
    const data = await getInterviewList({ type: 'all', page: 1, size: 100 })
    interviews.value = data?.list || []
  } catch (error) {
    console.error('加载面试列表失败:', error)
    interviews.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadInterviews()
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

.filter-tabs {
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

.card-header {
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

.company-name {
  font-size: 1.25rem;
  font-weight: 600;
  color: $color-text-primary;
  margin-bottom: $spacing-xs;
}

.position {
  color: $color-text-secondary;
  margin-bottom: $spacing-md;
}

.card-meta {
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
</style>
