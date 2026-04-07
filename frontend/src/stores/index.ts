// =====================================================
// LandIt 状态管理 - 主Store
// @author Azir
// =====================================================

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  currentUser,
  resumeDetail,
  interviewHistory,
  interviewQuestions,
  interviewDetail,
  statistics,
  jobRecommendations
} from '@/mock/data'
import * as userApi from '@/api/user'
import * as resumeApi from '@/api/resume'
import type {
  User,
  Resume,
  ResumeDetail,
  ResumeSuggestion,
  ResumeSuggestionsGroup,
  Interview,
  InterviewQuestions,
  InterviewDetail,
  Statistics,
  Job,
  UserUpdateInfo,
  UserStatusResponse,
  Gender,
  PrimaryResumeVO,
  ResumeListItem,
  CreateResumeRequest
} from '@/types'

export const useAppStore = defineStore('app', () => {
  // 用户状态
  const user = ref<User>(currentUser)
  const isLoggedIn = ref<boolean>(true)
  const isInitialized = ref<boolean>(false)

  // 简历相关
  const resumeList = ref<Resume[]>([])
  const currentResume = ref<ResumeDetail>(resumeDetail)
  const suggestions = ref<ResumeSuggestion[]>([])
  const suggestionsByResume = ref<ResumeSuggestionsGroup[]>([])
  const primaryResume = ref<PrimaryResumeVO | null>(null)

  // 面试相关
  const interviews = ref<Interview[]>(interviewHistory)
  const questions = ref<InterviewQuestions>(interviewQuestions)
  const currentInterview = ref<InterviewDetail>(interviewDetail)

  // 统计数据
  const stats = ref<Statistics>(statistics)

  // 职位推荐
  const jobs = ref<Job[]>(jobRecommendations)

  // 当前活跃的导航
  const activeNav = ref<string>('home')

  // 侧边栏状态
  const sidebarCollapsed = ref<boolean>(false)

  // 计算属性
  const recentInterviews = computed(() => {
    return interviews.value.slice(0, 3)
  })

  const averageInterviewScore = computed(() => {
    if (interviews.value.length === 0) return 0
    const sum = interviews.value.reduce((acc: number, i: Interview) => acc + i.score, 0)
    return Math.round(sum / interviews.value.length)
  })

  // 方法
  function setActiveNav(nav: string): void {
    activeNav.value = nav
  }

  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function updateUserInfo(info: UserUpdateInfo): void {
    user.value = { ...user.value, ...info }
  }

  function addInterview(interview: Interview): void {
    interviews.value.unshift(interview)
  }

  // 检查用户是否存在
  async function checkUserExists(): Promise<UserStatusResponse> {
    try {
      const status = await userApi.getUserStatus()
      if (status.exists && status.user) {
        user.value = {
          ...user.value,
          id: String(status.user.id),
          name: status.user.name,
          gender: status.user.gender as Gender | null,
          avatar: status.user.avatar
        }
        isInitialized.value = true
        isLoggedIn.value = true
      }
      return status
    } catch (error) {
      console.error('检查用户状态失败', error)
      return { exists: false }
    }
  }

  // 初始化用户（上传简历文件）
  async function initUser(file: File): Promise<void> {
    try {
      const result = await userApi.initUser(file)
      user.value = {
        ...user.value,
        name: result.name,
        gender: result.gender
      }
      isInitialized.value = true
      isLoggedIn.value = true
    } catch (error) {
      console.error('初始化用户失败', error)
      throw error
    }
  }

  // 获取主简历信息
  async function fetchPrimaryResume(): Promise<void> {
    try {
      const result = await resumeApi.getPrimaryResume()
      primaryResume.value = result
    } catch (error) {
      console.error('获取主简历失败', error)
    }
  }

  // 获取所有简历列表
  async function fetchResumes(status?: string): Promise<void> {
    try {
      const result = await resumeApi.getResumes(status)
      resumeList.value = result.map((item: ResumeListItem): Resume => ({
        id: item.id,
        name: item.name,
        targetPosition: item.targetPosition || '暂未设置目标职位',
        updatedAt: formatDateTime(item.updatedAt),
        status: item.status,
        score: item.score || 0,
        completeness: item.completeness || 0,
        isPrimary: item.isPrimary
      }))
    } catch (error) {
      console.error('获取简历列表失败', error)
    }
  }

  // 创建新简历
  async function createResume(data: CreateResumeRequest): Promise<string | undefined> {
    try {
      const result = await resumeApi.createResume(data)
      await fetchResumes()
      await fetchPrimaryResume()
      return result.id
    } catch (error) {
      console.error('创建简历失败', error)
      throw error
    }
  }

  // 删除简历
  async function deleteResumeFromApi(resumeId: string): Promise<void> {
    try {
      await resumeApi.deleteResume(resumeId)
      resumeList.value = resumeList.value.filter((r: Resume) => r.id !== resumeId)
    } catch (error) {
      console.error('删除简历失败', error)
      throw error
    }
  }

  // 设置主简历（调用API）
  async function setPrimaryResumeApi(resumeId: string): Promise<void> {
    try {
      const result = await resumeApi.setPrimaryResume(resumeId)
      primaryResume.value = result
      resumeList.value.forEach((r: Resume) => {
        r.isPrimary = r.id === resumeId
      })
    } catch (error) {
      console.error('设置主简历失败', error)
      throw error
    }
  }

  // 日期格式化辅助函数
  function formatDateTime(dateStr: string): string {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).replace(/\//g, '-')
  }

  // 获取简历详情
  async function fetchResumeDetail(id: string): Promise<void> {
    try {
      const result = await resumeApi.getResumeDetail(id)
      currentResume.value = result
    } catch (error) {
      console.error('获取简历详情失败', error)
    }
  }

  // 更新简历模块
  async function updateResumeSection(
    resumeId: string,
    sectionId: string,
    content: Record<string, unknown>
  ): Promise<void> {
    try {
      const result = await resumeApi.updateSection(resumeId, sectionId, content)
      currentResume.value = result
    } catch (error) {
      console.error('更新模块失败', error)
      throw error
    }
  }

  // 新增简历模块
  async function addResumeSection(
    resumeId: string,
    data: { type: string; title: string; content: Record<string, unknown> | Record<string, unknown>[] }
  ): Promise<string | undefined> {
    try {
      const result = await resumeApi.createSection(resumeId, data)
      currentResume.value = result
      // 返回新创建的模块 ID（根据类型查找最新添加的）
      const newSection = result.sections.find((s) => s.type === data.type)
      return newSection?.id ? String(newSection.id) : undefined
    } catch (error) {
      console.error('新增模块失败', error)
      throw error
    }
  }

  // 删除简历模块
  async function deleteResumeSection(resumeId: string, sectionId: string): Promise<void> {
    try {
      const result = await resumeApi.deleteSection(resumeId, sectionId)
      currentResume.value = result
    } catch (error) {
      console.error('删除模块失败', error)
      throw error
    }
  }

  // 新增聚合类型条目
  async function addResumeSectionItem(
    resumeId: string,
    parentSectionId: string,
    content: Record<string, unknown>
  ): Promise<void> {
    try {
      // 从当前简历中获取父模块
      const parentSection = currentResume.value.sections.find((s) => s.id === parentSectionId)
      if (!parentSection) {
        throw new Error('找不到父模块')
      }
      // 解析现有数组，追加新条目，然后更新整个数组
      const existingContent = parentSection.content
      let items: Record<string, unknown>[] = []
      if (existingContent) {
        const parsed = typeof existingContent === 'string' ? JSON.parse(existingContent) : existingContent
        if (Array.isArray(parsed)) {
          items = parsed
        }
      }
      items.push(content)
      const result = await resumeApi.updateSection(resumeId, parentSectionId, items)
      currentResume.value = result
    } catch (error) {
      console.error('新增条目失败', error)
      throw error
    }
  }

  // 更新聚合类型条目
  async function updateResumeSectionItem(
    resumeId: string,
    parentSectionId: string,
    itemIndex: number,
    content: Record<string, unknown>
  ): Promise<void> {
    try {
      // 从当前简历中获取父模块
      const parentSection = currentResume.value.sections.find((s) => s.id === parentSectionId)
      if (!parentSection) {
        throw new Error('找不到父模块')
      }
      // 解析现有数组，更新指定索引的条目，然后更新整个数组
      const existingContent = parentSection.content
      let items: Record<string, unknown>[] = []
      if (existingContent) {
        const parsed = typeof existingContent === 'string' ? JSON.parse(existingContent) : existingContent
        if (Array.isArray(parsed)) {
          items = parsed
        }
      }
      if (itemIndex < 0 || itemIndex >= items.length) {
        throw new Error(`索引 ${itemIndex} 超出范围`)
      }
      items[itemIndex] = content
      const result = await resumeApi.updateSection(resumeId, parentSectionId, items)
      currentResume.value = result
    } catch (error) {
      console.error('更新条目失败', error)
      throw error
    }
  }

  // 删除聚合类型条目
  async function deleteResumeSectionItem(
    resumeId: string,
    parentSectionId: string,
    itemIndex: number
  ): Promise<void> {
    try {
      // 从当前简历中获取父模块
      const parentSection = currentResume.value.sections.find((s) => s.id === parentSectionId)
      if (!parentSection) {
        throw new Error('找不到父模块')
      }
      // 解析现有数组，删除指定索引的条目，然后更新整个数组
      const existingContent = parentSection.content
      let items: Record<string, unknown>[] = []
      if (existingContent) {
        const parsed = typeof existingContent === 'string' ? JSON.parse(existingContent) : existingContent
        if (Array.isArray(parsed)) {
          items = parsed
        }
      }
      if (itemIndex < 0 || itemIndex >= items.length) {
        throw new Error(`索引 ${itemIndex} 超出范围`)
      }
      items.splice(itemIndex, 1)
      const result = await resumeApi.updateSection(resumeId, parentSectionId, items)
      currentResume.value = result
    } catch (error) {
      console.error('删除条目失败', error)
      throw error
    }
  }

  // 删除优化建议
  async function deleteSuggestion(resumeId: string, suggestionId: string): Promise<void> {
    try {
      await resumeApi.deleteSuggestion(resumeId, suggestionId)
      // 从本地状态中移除该建议
      for (const section of currentResume.value.sections) {
        if (section.suggestions) {
          section.suggestions = section.suggestions.filter((s) => s.id !== suggestionId)
        }
      }
    } catch (error) {
      console.error('删除建议失败', error)
      throw error
    }
  }

  // 获取简历优化建议
  async function fetchSuggestions(resumeId: string): Promise<void> {
    try {
      const result = await resumeApi.getSuggestions(resumeId)
      suggestions.value = result
    } catch (error) {
      console.error('获取建议列表失败', error)
      suggestions.value = []
    }
  }

  // 获取所有简历的优化建议（按简历分组）
  async function fetchAllSuggestions(): Promise<void> {
    try {
      const result = await resumeApi.getAllSuggestions()
      suggestionsByResume.value = result
    } catch (error) {
      console.error('获取所有建议失败', error)
      suggestionsByResume.value = []
    }
  }

  // 更新简历基本信息（名称和目标岗位）
  async function updateResumeBasicInfo(
    resumeId: string,
    data: { name: string; targetPosition?: string }
  ): Promise<void> {
    try {
      const result = await resumeApi.updateResume(resumeId, data)
      currentResume.value = result
      // 同步更新简历列表
      const index = resumeList.value.findIndex((r: Resume) => r.id === resumeId)
      if (index !== -1) {
        resumeList.value[index] = {
          ...resumeList.value[index],
          name: result.name,
          targetPosition: result.targetPosition
        }
      }
    } catch (error) {
      console.error('更新简历基本信息失败', error)
      throw error
    }
  }

  return {
    // 状态
    user,
    isLoggedIn,
    isInitialized,
    resumeList,
    currentResume,
    suggestions,
    suggestionsByResume,
    primaryResume,
    interviews,
    questions,
    currentInterview,
    stats,
    jobs,
    activeNav,
    sidebarCollapsed,
    // 计算属性
    recentInterviews,
    averageInterviewScore,
    // 方法
    setActiveNav,
    toggleSidebar,
    updateUserInfo,
    addInterview,
    checkUserExists,
    initUser,
    fetchPrimaryResume,
    fetchResumes,
    createResume,
    deleteResumeFromApi,
    setPrimaryResumeApi,
    fetchResumeDetail,
    updateResumeSection,
    addResumeSection,
    deleteResumeSection,
    addResumeSectionItem,
    updateResumeSectionItem,
    deleteResumeSectionItem,
    deleteSuggestion,
    fetchSuggestions,
    fetchAllSuggestions,
    updateResumeBasicInfo
  }
})
