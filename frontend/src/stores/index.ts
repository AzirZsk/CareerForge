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
  UserExistsResponse,
  ResumeParseResult,
  Gender
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
  const primaryResume = computed(() => {
    return resumeList.value.find((r: Resume) => r.isPrimary)
  })

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
  async function checkUserExists(): Promise<boolean> {
    try {
      // TODO: 调用真实 API
      // const response = await fetch('/landit/api/user/exists')
      // const data: ApiResponse<UserExistsResponse> = await response.json()
      // return data.data.exists
      // 暂时使用 mock 数据
      return user.value.name !== ''
    } catch (error) {
      console.error('检查用户状态失败', error)
      return false
    }
  }

  // 初始化用户
  async function initUser(data: { name: string; gender: Gender | null }): Promise<void> {
    try {
      // TODO: 调用真实 API
      // const response = await fetch('/landit/api/user/init', {
      //   method: 'POST',
      //   headers: { 'Content-Type': 'application/json' },
      //   body: JSON.stringify(data)
      // })
      // const result: ApiResponse<User> = await response.json()
      // user.value = result.data
      // 暂时使用 mock 数据
      user.value = {
        ...user.value,
        name: data.name,
        gender: data.gender
      }
      isInitialized.value = true
      isLoggedIn.value = true
    } catch (error) {
      console.error('初始化用户失败', error)
      throw error
    }
  }

  // 解析简历
  async function parseResume(file: File): Promise<ResumeParseResult> {
    try {
      // TODO: 调用真实 API
      // const formData = new FormData()
      // formData.append('file', file)
      // const response = await fetch('/landit/api/resumes/parse', {
      //   method: 'POST',
      //   body: formData
      // })
      // const result: ApiResponse<ResumeParseResult> = await response.json()
      // return result.data
      // 暂时返回空结果
      return {
        name: '',
        gender: null,
        rawText: ''
      }
    } catch (error) {
      console.error('解析简历失败', error)
      return {
        name: '',
        gender: null,
        rawText: ''
      }
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
    interviews,
    questions,
    currentInterview,
    currentReview,
    stats,
    jobs,
    activeNav,
    sidebarCollapsed,
    // 计算属性
    primaryResume,
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
    parseResume
  }
})
