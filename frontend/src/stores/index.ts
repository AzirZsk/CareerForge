// =====================================================
// LandIt 状态管理 - 主Store
// @author Azir
// =====================================================

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  currentUser,
  resumes,
  resumeDetail,
  resumeSuggestions,
  interviewHistory,
  interviewQuestions,
  interviewDetail,
  interviewReview,
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
  Interview,
  InterviewQuestions,
  InterviewDetail,
  InterviewReview,
  Statistics,
  Job,
  UserUpdateInfo,
  UserStatusResponse,
  Gender,
  PrimaryResumeVO
} from '@/types'

export const useAppStore = defineStore('app', () => {
  // 用户状态
  const user = ref<User>(currentUser)
  const isLoggedIn = ref<boolean>(true)
  const isInitialized = ref<boolean>(false)

  // 简历相关
  const resumeList = ref<Resume[]>(resumes)
  const currentResume = ref<ResumeDetail>(resumeDetail)
  const suggestions = ref<ResumeSuggestion[]>(resumeSuggestions)
  const primaryResume = ref<PrimaryResumeVO | null>(null)

  // 面试相关
  const interviews = ref<Interview[]>(interviewHistory)
  const questions = ref<InterviewQuestions>(interviewQuestions)
  const currentInterview = ref<InterviewDetail>(interviewDetail)
  const currentReview = ref<InterviewReview>(interviewReview)

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

  function setPrimaryResume(resumeId: string): void {
    resumeList.value.forEach((r: Resume) => {
      r.isPrimary = r.id === resumeId
    })
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
    data: { type: string; title: string; content: Record<string, unknown> }
  ): Promise<void> {
    try {
      const result = await resumeApi.createSection(resumeId, data)
      currentResume.value = result
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
      // 从当前简历中获取父模块的类型
      const parentSection = currentResume.value.sections.find((s) => s.id === parentSectionId)
      if (!parentSection) {
        throw new Error('找不到父模块')
      }
      const type = parentSection.type
      const result = await resumeApi.createSectionItem(resumeId, type, content)
      currentResume.value = result
    } catch (error) {
      console.error('新增条目失败', error)
      throw error
    }
  }

  // 更新聚合类型条目
  async function updateResumeSectionItem(
    resumeId: string,
    itemId: string,
    content: Record<string, unknown>
  ): Promise<void> {
    try {
      const result = await resumeApi.updateSection(resumeId, itemId, content)
      currentResume.value = result
    } catch (error) {
      console.error('更新条目失败', error)
      throw error
    }
  }

  // 删除聚合类型条目
  async function deleteResumeSectionItem(resumeId: string, itemId: string): Promise<void> {
    try {
      const result = await resumeApi.deleteSection(resumeId, itemId)
      currentResume.value = result
    } catch (error) {
      console.error('删除条目失败', error)
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
    primaryResume,
    interviews,
    questions,
    currentInterview,
    currentReview,
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
    setPrimaryResume,
    addInterview,
    checkUserExists,
    initUser,
    fetchPrimaryResume,
    fetchResumeDetail,
    updateResumeSection,
    addResumeSection,
    deleteResumeSection,
    addResumeSectionItem,
    updateResumeSectionItem,
    deleteResumeSectionItem
  }
})
