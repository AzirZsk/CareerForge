<!--=====================================================
  LandIt 简历详情页面
  @author Azir
=====================================================-->

<template>
  <div class="resume-detail-page">
    <div class="container">
      <!-- 返回导航 -->
      <nav class="back-nav animate-in" style="--delay: 0">
        <router-link to="/resume" class="back-link">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="19" y1="12" x2="5" y2="12"></line>
            <polyline points="12 19 5 12 12 5"></polyline>
          </svg>
          返回简历列表
        </router-link>
      </nav>

      <!-- 简历头部 -->
      <header class="resume-header animate-in" style="--delay: 1">
        <div class="header-left">
          <h1 class="resume-title">{{ store.currentResume.name }}</h1>
          <p class="resume-target">目标岗位：{{ store.currentResume.targetPosition }}</p>
        </div>
        <div class="header-right">
          <div class="score-overview">
            <div class="score-main">
              <div class="score-ring" :style="{ '--score': store.currentResume.analyzed ? store.currentResume.overallScore : 0 }">
                <span>{{ store.currentResume.analyzed ? store.currentResume.overallScore : '~' }}</span>
              </div>
              <div class="score-labels">
                <span class="score-title">综合评分</span>
                <span class="score-detail">结构规范 {{ store.currentResume.analyzed ? store.currentResume.structureScore + '%' : '~' }}</span>
              </div>
            </div>
          </div>
          <div class="header-actions">
            <button class="action-btn primary" @click="optimizeResume">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
              </svg>
              {{ store.currentResume.analyzed ? 'AI一键优化' : 'AI分析' }}
            </button>
            <button class="action-btn secondary">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                <polyline points="7 10 12 15 17 10"></polyline>
                <line x1="12" y1="15" x2="12" y2="3"></line>
              </svg>
              导出PDF
            </button>
          </div>
        </div>
      </header>

      <!-- 评分指标 -->
      <section class="metrics-section animate-in" style="--delay: 2">
        <div class="metric-card">
          <div class="metric-header">
            <span class="metric-icon content">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
              </svg>
            </span>
            <span class="metric-title">内容质量</span>
          </div>
          <div class="metric-value">{{ store.currentResume.analyzed ? store.currentResume.contentScore : '~' }}</div>
          <div class="metric-bar">
            <div class="metric-fill" :style="{ width: store.currentResume.analyzed ? store.currentResume.contentScore + '%' : '0%' }"></div>
          </div>
        </div>
        <div class="metric-card">
          <div class="metric-header">
            <span class="metric-icon structure">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                <line x1="3" y1="9" x2="21" y2="9"></line>
                <line x1="9" y1="21" x2="9" y2="9"></line>
              </svg>
            </span>
            <span class="metric-title">结构规范</span>
          </div>
          <div class="metric-value">{{ store.currentResume.analyzed ? store.currentResume.structureScore : '~' }}</div>
          <div class="metric-bar">
            <div class="metric-fill" :style="{ width: store.currentResume.analyzed ? store.currentResume.structureScore + '%' : '0%' }"></div>
          </div>
        </div>
        <div class="metric-card">
          <div class="metric-header">
            <span class="metric-icon matching">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
            </span>
            <span class="metric-title">岗位匹配</span>
          </div>
          <div class="metric-value">{{ store.currentResume.analyzed ? store.currentResume.matchingScore : '~' }}</div>
          <div class="metric-bar">
            <div class="metric-fill" :style="{ width: store.currentResume.analyzed ? store.currentResume.matchingScore + '%' : '0%' }"></div>
          </div>
        </div>
        <div class="metric-card">
          <div class="metric-header">
            <span class="metric-icon competitiveness">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
              </svg>
            </span>
            <span class="metric-title">竞争力</span>
          </div>
          <div class="metric-value">{{ store.currentResume.analyzed ? store.currentResume.competitivenessScore : '~' }}</div>
          <div class="metric-bar">
            <div class="metric-fill" :style="{ width: store.currentResume.analyzed ? store.currentResume.competitivenessScore + '%' : '0%' }"></div>
          </div>
        </div>
      </section>

      <!-- 简历内容 -->
      <div class="content-grid">
        <section class="sections-panel animate-in" style="--delay: 3">
          <div class="panel-header">
            <h2 class="panel-title">简历模块</h2>
            <button class="add-section-btn">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
              添加模块
            </button>
          </div>
          <div class="sections-list">
            <div
              v-for="(section, index) in store.currentResume.sections"
              :key="section.id"
              class="section-card"
              :class="{ active: activeSection === section.id }"
              :style="{ '--index': index }"
              @click="activeSection = section.id"
            >
              <div class="section-header">
                <div class="section-info">
                  <span class="section-icon">{{ getSectionIcon(section.type) }}</span>
                  <span class="section-name">{{ section.title }}</span>
                </div>
                <div class="section-score" :class="store.currentResume.analyzed ? getScoreClass(section.score) : ''">
                  {{ store.currentResume.analyzed ? section.score : '~' }}
                </div>
              </div>
              <p class="section-preview">{{ getSectionPreview(section) }}</p>
              <div v-if="section.suggestions?.length" class="section-hint">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="12" y1="16" x2="12" y2="12"></line>
                  <line x1="12" y1="8" x2="12.01" y2="8"></line>
                </svg>
                {{ section.suggestions.length }} 条优化建议
              </div>
            </div>
          </div>
        </section>

        <section class="detail-panel animate-in" style="--delay: 4">
          <div class="panel-header">
            <h2 class="panel-title">{{ currentSectionDetail?.title }}</h2>
            <div class="panel-actions">
              <!-- 单条类型：显示编辑按钮 -->
              <button v-if="!isAggregateSection" class="panel-btn" @click="openEditModal">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                </svg>
                编辑
              </button>
              <!-- 聚合类型：显示添加按钮 -->
              <button v-else class="panel-btn primary" @click="openAddItemModal">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
                添加
              </button>
            </div>
          </div>
          <div class="detail-content">
            <!-- 基本信息（单条） -->
            <div class="content-block" v-if="currentSectionDetail?.type === 'BASIC_INFO' && basicContent">
              <template v-for="(value, key) in basicContent" :key="key">
                <div class="info-row" v-if="value">
                  <span class="info-label">{{ getFieldLabel(key as string) }}</span>
                  <span class="info-value">{{ value }}</span>
                </div>
              </template>
            </div>
            <!-- 技能（单条） -->
            <div class="content-block" v-else-if="currentSectionDetail?.type === 'SKILLS'">
              <ul class="skill-list">
                <li v-for="(skill, idx) in skillContent" :key="idx">{{ skill }}</li>
              </ul>
            </div>
            <!-- 聚合类型（教育、工作、项目、证书） -->
            <div class="content-block" v-else-if="currentSectionDetail?.items?.length">
              <!-- 教育经历列表 -->
              <template v-if="currentSectionDetail?.type === 'EDUCATION'">
                <div class="experience-item" v-for="item in currentSectionDetail.items" :key="item.id">
                  <div class="exp-header">
                    <h4 class="exp-title">{{ item.content.school }}</h4>
                    <div class="exp-actions">
                      <span class="exp-period" v-if="item.content.period">{{ item.content.period }}</span>
                      <button class="item-btn edit" @click="openEditItemModal(item.id)" title="编辑">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                        </svg>
                      </button>
                      <button class="item-btn delete" @click="deleteItem(item.id)" title="删除">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <polyline points="3 6 5 6 21 6"></polyline>
                          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                        </svg>
                      </button>
                    </div>
                  </div>
                  <p class="exp-position" v-if="item.content.major || item.content.degree">
                    {{ item.content.degree }} · {{ item.content.major }}
                  </p>
                </div>
              </template>
              <!-- 工作经历列表 -->
              <template v-else-if="currentSectionDetail?.type === 'WORK'">
                <div class="experience-item" v-for="item in currentSectionDetail.items" :key="item.id">
                  <div class="exp-header">
                    <h4 class="exp-title">{{ item.content.company }}</h4>
                    <div class="exp-actions">
                      <span class="exp-period" v-if="item.content.period">{{ item.content.period }}</span>
                      <button class="item-btn edit" @click="openEditItemModal(item.id)" title="编辑">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                        </svg>
                      </button>
                      <button class="item-btn delete" @click="deleteItem(item.id)" title="删除">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <polyline points="3 6 5 6 21 6"></polyline>
                          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                        </svg>
                      </button>
                    </div>
                  </div>
                  <p class="exp-position" v-if="item.content.position">{{ item.content.position }}</p>
                  <p class="exp-desc" v-if="item.content.description">{{ item.content.description }}</p>
                </div>
              </template>
              <!-- 项目经历列表 -->
              <template v-else-if="currentSectionDetail?.type === 'PROJECT'">
                <div class="experience-item" v-for="item in currentSectionDetail.items" :key="item.id">
                  <div class="exp-header">
                    <h4 class="exp-title">{{ item.content.name }}</h4>
                    <div class="exp-actions">
                      <span class="exp-period" v-if="item.content.period">{{ item.content.period }}</span>
                      <button class="item-btn edit" @click="openEditItemModal(item.id)" title="编辑">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                        </svg>
                      </button>
                      <button class="item-btn delete" @click="deleteItem(item.id)" title="删除">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <polyline points="3 6 5 6 21 6"></polyline>
                          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                        </svg>
                      </button>
                    </div>
                  </div>
                  <p class="exp-position" v-if="item.content.role">{{ item.content.role }}</p>
                  <p class="exp-desc" v-if="item.content.description">{{ item.content.description }}</p>
                  <div v-if="(item.content.achievements as string[])?.length" class="exp-achievements">
                    <span v-for="a in (item.content.achievements as string[])" :key="a" class="achievement-tag">{{ a }}</span>
                  </div>
                </div>
              </template>
              <!-- 证书列表 -->
              <template v-else-if="currentSectionDetail?.type === 'CERTIFICATE'">
                <div class="experience-item" v-for="item in currentSectionDetail.items" :key="item.id">
                  <div class="exp-header">
                    <h4 class="exp-title">{{ item.content.name }}</h4>
                    <div class="exp-actions">
                      <span class="exp-period" v-if="item.content.date">{{ item.content.date }}</span>
                      <button class="item-btn edit" @click="openEditItemModal(item.id)" title="编辑">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                        </svg>
                      </button>
                      <button class="item-btn delete" @click="deleteItem(item.id)" title="删除">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <polyline points="3 6 5 6 21 6"></polyline>
                          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                        </svg>
                      </button>
                    </div>
                  </div>
                </div>
              </template>
            </div>
            <!-- 聚合类型无数据提示 -->
            <div class="empty-block" v-else-if="isAggregateSection && !currentSectionDetail?.items?.length">
              <p class="empty-text">暂无记录</p>
              <button class="add-item-btn" @click="openAddItemModal">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
                添加第一条记录
              </button>
            </div>
          </div>
          <div v-if="hasSuggestions" class="suggestions-block">
            <h4 class="suggestions-title">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"></path>
              </svg>
              优化建议
            </h4>
            <div v-for="sug in sectionSuggestions" :key="sug.content" class="suggestion-item" :class="sug.type">
              <span class="sug-icon">{{ sug.type === 'critical' ? '⚠️' : sug.type === 'improvement' ? '💡' : '✨' }}</span>
              <p class="sug-text">{{ sug.content }}</p>
              <button class="apply-sug-btn">应用</button>
            </div>
          </div>
        </section>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <EditSectionModal
      v-model:visible="isEditModalVisible"
      :section="currentSectionDetail ?? null"
      :item-id="editItemId"
      :is-new="isNewItem"
      :saving="isSaving"
      @save="handleSave"
      @cancel="closeEditModal"
    />

    <!-- 优化进度弹窗 -->
    <OptimizeProgressModal
      v-model:visible="showOptimizeModal"
      :state="optimizeState"
      @cancel="cancelOptimize"
      @retry="retryOptimize"
      @toggle-expand="toggleStageExpanded"
      @complete="handleOptimizeComplete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores'
import EditSectionModal from '@/components/resume/EditSectionModal.vue'
import OptimizeProgressModal from '@/components/resume/OptimizeProgressModal.vue'
import { useResumeOptimize } from '@/composables/useResumeOptimize'
import type {
  ResumeSection,
  ResumeSuggestionItem,
  BasicInfoContent,
  SkillsContent
} from '@/types'

const store = useAppStore()
const route = useRoute()
const activeSection = ref<string>('')
const resumeId = ref<string>('')

// 编辑状态
const isEditModalVisible = ref<boolean>(false)
const isSaving = ref<boolean>(false)
const editItemId = ref<string | null>(null)
const isNewItem = ref<boolean>(false)

// 优化相关状态
const showOptimizeModal = ref<boolean>(false)
const {
  state: optimizeState,
  stageConfig,
  startOptimize,
  cancelOptimize,
  retryOptimize,
  toggleStageExpanded
} = useResumeOptimize()

// 页面加载时获取简历详情
onMounted(async () => {
  const id = route.params.id as string
  if (id) {
    resumeId.value = id
    await store.fetchResumeDetail(id)
    // 默认选中第一个模块
    if (store.currentResume.sections.length > 0) {
      activeSection.value = String(store.currentResume.sections[0].id)
    }
  }
})

const currentSectionDetail = computed<ResumeSection | undefined>(() => {
  return store.currentResume.sections.find((s: ResumeSection) => s.id === activeSection.value)
})

// 判断当前模块是否为聚合类型
const isAggregateSection = computed<boolean>(() => {
  const type = currentSectionDetail.value?.type
  return ['EDUCATION', 'WORK', 'PROJECT', 'CERTIFICATE'].includes(type ?? '')
})

// 基本信息（BASIC_INFO）- 单条类型
const basicContent = computed<BasicInfoContent | null>(() => {
  if (currentSectionDetail.value?.type !== 'BASIC_INFO') {
    return null
  }
  return currentSectionDetail.value.content as BasicInfoContent
})

// 技能（SKILLS）- 聚合类型，数据在 items[0].content.skills
const skillContent = computed<string[]>(() => {
  if (currentSectionDetail.value?.type !== 'SKILLS') {
    return []
  }
  const firstItem = currentSectionDetail.value.items?.[0]
  const content = firstItem?.content as SkillsContent | null | undefined
  return content?.skills ?? []
})

const sectionSuggestions = computed<ResumeSuggestionItem[]>(() => {
  return currentSectionDetail.value?.suggestions ?? []
})

const hasSuggestions = computed<boolean>(() => {
  return (currentSectionDetail.value?.suggestions?.length ?? 0) > 0
})

// 模块图标映射（后端大写格式）
const sectionIcons: Record<string, string> = {
  BASIC_INFO: '👤',
  EDUCATION: '🎓',
  WORK: '💼',
  PROJECT: '🎯',
  SKILLS: '⚡',
  CERTIFICATE: '🏆'
}

function getSectionIcon(type: string): string {
  return sectionIcons[type] || '📄'
}

function getSectionPreview(section: ResumeSection): string {
  // 聚合类型（有items）
  if (section.items?.length) {
    return `${section.items.length} 条记录`
  }
  // 单条类型（有content）
  if (section.type === 'BASIC_INFO') {
    const content = section.content as BasicInfoContent
    return content.name ?? '基本信息'
  }
  if (section.type === 'SKILLS') {
    const firstItem = section.items?.[0]
    const content = firstItem?.content as SkillsContent | undefined
    return `${content?.skills?.length ?? 0} 项技能`
  }
  return section.title ?? ''
}

function getScoreClass(score: number): string {
  if (score >= 90) {
    return 'excellent'
  }
  if (score >= 75) {
    return 'good'
  }
  return 'average'
}

// 字段标签映射（适配后端字段）
const fieldLabels: Record<string, string> = {
  name: '姓名',
  gender: '性别',
  phone: '电话',
  email: '邮箱',
  targetPosition: '目标岗位',
  summary: '简介',
  school: '学校',
  major: '专业',
  degree: '学历',
  period: '时间',
  company: '公司',
  position: '职位',
  description: '描述',
  role: '角色',
  achievements: '成果',
  skills: '技能',
  date: '日期'
}

function getFieldLabel(key: string): string {
  return fieldLabels[key] || key
}

function optimizeResume(): void {
  if (!resumeId.value) {
    return
  }
  showOptimizeModal.value = true

  // 获取目标岗位
  const targetPosition = store.currentResume.targetPosition || undefined

  // 开始 SSE 优化
  startOptimize(resumeId.value, {
    mode: 'quick',
    targetPosition
  })
}

// 优化完成后刷新简历详情
async function handleOptimizeComplete(): Promise<void> {
  if (resumeId.value) {
    await store.fetchResumeDetail(resumeId.value)
  }
}

// ==================== 编辑功能 ====================

// 打开编辑弹窗（单条类型）
function openEditModal(): void {
  if (!currentSectionDetail.value) {
    return
  }
  editItemId.value = null
  isNewItem.value = false
  isEditModalVisible.value = true
}

// 打开编辑弹窗（聚合类型 - 编辑某条记录）
function openEditItemModal(itemId: string): void {
  editItemId.value = itemId
  isNewItem.value = false
  isEditModalVisible.value = true
}

// 打开新增弹窗（聚合类型 - 新增记录）
function openAddItemModal(): void {
  editItemId.value = null
  isNewItem.value = true
  isEditModalVisible.value = true
}

// 关闭编辑弹窗
function closeEditModal(): void {
  isEditModalVisible.value = false
  editItemId.value = null
  isNewItem.value = false
}

// 保存编辑
async function handleSave(data: { content: Record<string, unknown>; itemId?: string; isNew: boolean }): Promise<void> {
  if (!resumeId.value || !activeSection.value) {
    return
  }
  isSaving.value = true
  try {
    if (isAggregateSection.value) {
      // 聚合类型：新增或编辑条目
      if (data.isNew) {
        // 新增条目
        await store.addResumeSectionItem(resumeId.value, activeSection.value, data.content)
      } else if (data.itemId) {
        // 编辑条目
        await store.updateResumeSectionItem(resumeId.value, data.itemId, data.content)
      }
    } else {
      // 单条类型：直接更新
      await store.updateResumeSection(resumeId.value, activeSection.value, data.content)
    }
    closeEditModal()
  } catch (error) {
    console.error('保存失败', error)
    alert('保存失败，请重试')
  } finally {
    isSaving.value = false
  }
}

// 删除条目
async function deleteItem(itemId: string): Promise<void> {
  if (!confirm('确定要删除这条记录吗？')) {
    return
  }
  try {
    await store.deleteResumeSectionItem(resumeId.value, itemId)
  } catch (error) {
    console.error('删除失败', error)
    alert('删除失败，请重试')
  }
}
</script>

<style lang="scss" scoped>
.resume-detail-page {
  padding: $spacing-2xl;
  max-width: 1400px;
  margin: 0 auto;
}

.container {
  display: flex;
  flex-direction: column;
  gap: $spacing-xl;
}

.back-nav {
  margin-bottom: $spacing-sm;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: $spacing-sm;
  font-size: $text-sm;
  color: $color-text-tertiary;
  transition: color $transition-fast;
  &:hover {
    color: $color-accent;
  }
}

// 头部
.resume-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: $spacing-xl;
  background: $gradient-card;
  border-radius: $radius-xl;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.resume-title {
  font-family: $font-display;
  font-size: $text-3xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-xs;
}

.resume-target {
  font-size: $text-base;
  color: $color-text-secondary;
}

.header-right {
  display: flex;
  align-items: center;
  gap: $spacing-2xl;
}

.score-main {
  display: flex;
  align-items: center;
  gap: $spacing-lg;
}

.score-ring {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: conic-gradient(
    $color-accent calc(var(--score) * 3.6deg),
    rgba(255, 255, 255, 0.1) calc(var(--score) * 3.6deg)
  );
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  &::before {
    content: '';
    position: absolute;
    inset: 6px;
    background: $color-bg-tertiary;
    border-radius: 50%;
  }
  span {
    position: relative;
    z-index: 1;
    font-family: $font-display;
    font-size: $text-2xl;
    font-weight: $weight-bold;
    color: $color-accent;
  }
}

.score-labels {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.score-title {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.score-detail {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.header-actions {
  display: flex;
  gap: $spacing-md;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-lg;
  font-size: $text-sm;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  transition: all $transition-fast;
  &.primary {
    background: $gradient-gold;
    color: $color-bg-deep;
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 16px rgba(212, 168, 83, 0.3);
    }
  }
  &.secondary {
    background: rgba(255, 255, 255, 0.05);
    color: $color-text-secondary;
    border: 1px solid rgba(255, 255, 255, 0.1);
    &:hover {
      background: rgba(255, 255, 255, 0.1);
    }
  }
}

// 指标区域
.metrics-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $spacing-lg;
}

.metric-card {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.metric-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
}

.metric-icon {
  width: 32px;
  height: 32px;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  &.keyword {
    background: rgba(96, 165, 250, 0.15);
    color: #60a5fa;
  }
  &.format {
    background: rgba(52, 211, 153, 0.15);
    color: #34d399;
  }
  &.content {
    background: rgba(212, 168, 83, 0.15);
    color: $color-accent;
  }
  &.structure {
    background: rgba(52, 211, 153, 0.15);
    color: #34d399;
  }
  &.matching {
    background: rgba(96, 165, 250, 0.15);
    color: #60a5fa;
  }
  &.competitiveness {
    background: rgba(167, 139, 250, 0.15);
    color: #a78bfa;
  }
}

.metric-title {
  font-size: $text-sm;
  color: $color-text-tertiary;
}

.metric-value {
  font-family: $font-display;
  font-size: $text-3xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-md;
}

.metric-bar {
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: $radius-full;
  overflow: hidden;
}

.metric-fill {
  height: 100%;
  background: $gradient-gold;
  border-radius: $radius-full;
  transition: width 0.8s ease;
}

// 内容区域
.content-grid {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: $spacing-xl;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-lg;
}

.panel-title {
  font-family: $font-display;
  font-size: $text-xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
}

.add-section-btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $text-sm;
  color: $color-accent;
  transition: color $transition-fast;
  &:hover {
    color: $color-accent-light;
  }
}

.sections-panel {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.sections-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.section-card {
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all $transition-fast;
  animation: slideUp 0.4s ease forwards;
  animation-delay: calc(var(--index) * 0.1s);
  opacity: 0;
  &:hover {
    background: rgba(255, 255, 255, 0.04);
  }
  &.active {
    background: $color-accent-glow;
    border-color: rgba(212, 168, 83, 0.3);
  }
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xs;
}

.section-info {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.section-icon {
  font-size: $text-lg;
}

.section-name {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.section-score {
  font-size: $text-sm;
  font-weight: $weight-semibold;
  padding: $spacing-xs $spacing-sm;
  border-radius: $radius-sm;
  &.excellent {
    background: $color-success-bg;
    color: $color-success;
  }
  &.good {
    background: $color-accent-glow;
    color: $color-accent;
  }
  &.average {
    background: $color-warning-bg;
    color: $color-warning;
  }
}

.section-preview {
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-bottom: $spacing-sm;
}

.section-hint {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $text-xs;
  color: $color-warning;
}

// 详情面板
.detail-panel {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.panel-btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-xs $spacing-md;
  font-size: $text-sm;
  color: $color-text-secondary;
  background: rgba(255, 255, 255, 0.05);
  border-radius: $radius-sm;
  transition: all $transition-fast;
  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: $color-text-primary;
  }
  &.primary {
    background: $color-accent-glow;
    color: $color-accent;
    &:hover {
      background: rgba(212, 168, 83, 0.2);
    }
  }
}

.detail-content {
  margin-bottom: $spacing-xl;
}

.content-block {
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  padding: $spacing-lg;
}

.info-row {
  display: flex;
  padding: $spacing-md 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
  &:last-child {
    border-bottom: none;
  }
}

.info-label {
  width: 120px;
  font-size: $text-sm;
  color: $color-text-tertiary;
}

.info-value {
  flex: 1;
  font-size: $text-sm;
  color: $color-text-primary;
  white-space: pre-wrap;
  word-break: break-word;
}

.experience-item {
  padding: $spacing-lg 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
  &:first-child {
    padding-top: 0;
  }
  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
}

.exp-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xs;
}

.exp-title {
  font-size: $text-base;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.exp-period {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.exp-position {
  font-size: $text-sm;
  color: $color-accent;
  margin-bottom: $spacing-sm;
}

.exp-desc {
  font-size: $text-sm;
  color: $color-text-secondary;
  line-height: $leading-relaxed;
  margin-bottom: $spacing-sm;
  white-space: pre-wrap;
  word-break: break-word;
}

.exp-achievements {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.achievement-tag {
  width: fit-content;
  padding: $spacing-xs $spacing-sm;
  background: $color-success-bg;
  color: $color-success;
  font-size: $text-xs;
  border-radius: $radius-sm;
}

.skill-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  li {
    position: relative;
    padding-left: $spacing-lg;
    font-size: $text-sm;
    color: $color-text-secondary;
    &::before {
      content: '▸';
      position: absolute;
      left: 0;
      color: $color-accent;
    }
  }
}

.suggestions-block {
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  padding: $spacing-lg;
}

.suggestions-title {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  font-size: $text-base;
  font-weight: $weight-medium;
  color: $color-text-primary;
  margin-bottom: $spacing-lg;
}

.suggestion-item {
  display: flex;
  align-items: flex-start;
  gap: $spacing-md;
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  margin-bottom: $spacing-sm;
  border-left: 3px solid;
  &.critical {
    border-color: $color-error;
  }
  &.improvement {
    border-color: $color-warning;
  }
  &.enhancement {
    border-color: $color-info;
  }
}

.sug-icon {
  font-size: $text-lg;
}

.sug-text {
  flex: 1;
  font-size: $text-sm;
  color: $color-text-secondary;
  line-height: $leading-relaxed;
}

.apply-sug-btn {
  padding: $spacing-xs $spacing-md;
  background: $color-accent-glow;
  color: $color-accent;
  font-size: $text-xs;
  font-weight: $weight-medium;
  border-radius: $radius-sm;
  transition: all $transition-fast;
  white-space: nowrap;
  &:hover {
    background: rgba(212, 168, 83, 0.2);
  }
}

// 动画
.animate-in {
  animation: slideUp 0.6s ease forwards;
  animation-delay: calc(var(--delay) * 0.1s);
  opacity: 0;
}

// 条目操作按钮
.exp-actions {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.item-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius-sm;
  transition: all $transition-fast;
  opacity: 0.5;
  &.edit {
    color: $color-text-secondary;
    &:hover {
      opacity: 1;
      background: rgba(255, 255, 255, 0.1);
      color: $color-accent;
    }
  }
  &.delete {
    color: $color-text-secondary;
    &:hover {
      opacity: 1;
      background: rgba(248, 113, 113, 0.1);
      color: $color-error;
    }
  }
}

.experience-item:hover .item-btn {
  opacity: 1;
}

// 空状态
.empty-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $spacing-2xl;
  text-align: center;
}

.empty-text {
  font-size: $text-sm;
  color: $color-text-tertiary;
  margin-bottom: $spacing-lg;
}

.add-item-btn {
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-sm $spacing-lg;
  font-size: $text-sm;
  color: $color-accent;
  background: $color-accent-glow;
  border-radius: $radius-md;
  transition: all $transition-fast;
  &:hover {
    background: rgba(212, 168, 83, 0.2);
    transform: translateY(-2px);
  }
}
</style>
