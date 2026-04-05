// =====================================================
// LandIt 路由配置
// @author Azir
// =====================================================

import { createRouter, createWebHistory, type RouteRecordRaw, type NavigationGuardNext, type RouteLocationNormalized } from 'vue-router'
import { useAppStore } from '@/stores'

const routes: RouteRecordRaw[] = [
  {
    path: '/onboarding',
    name: 'Onboarding',
    component: () => import('@/views/Onboarding.vue'),
    meta: { title: '欢迎', public: true }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/resume',
    name: 'Resume',
    component: () => import('@/views/Resume.vue'),
    meta: { title: '简历管理' }
  },
  {
    path: '/resume/:id',
    name: 'ResumeDetail',
    component: () => import('@/views/ResumeDetail.vue'),
    meta: { title: '简历详情' }
  },
  // ==================== 面试中心路由（新增） ====================
  {
    path: '/interview-center',
    component: () => import('@/views/interview-center/Layout.vue'),
    children: [
      {
        path: '',
        name: 'InterviewCenter',
        component: () => import('@/views/interview-center/InterviewList.vue'),
        meta: { title: '面试中心' }
      },
      {
        path: 'create',
        name: 'CreateInterview',
        component: () => import('@/views/interview-center/CreateInterview.vue'),
        meta: { title: '创建面试' }
      },
      {
        path: 'position/:id',
        name: 'PositionDetail',
        component: () => import('@/views/interview-center/PositionDetail.vue'),
        meta: { title: '职位详情' }
      },
      {
        path: ':id',
        name: 'InterviewDetail',
        component: () => import('@/views/interview-center/InterviewDetail.vue'),
        meta: { title: '面试详情' }
      },
      // 模拟面试路由（从详情页进入）
      {
        path: ':id/mock/:sessionId',
        name: 'MockInterviewSession',
        component: () => import('@/views/InterviewSession.vue'),
        meta: { title: '面试进行中' }
      },
      {
        path: 'mock/:sessionId/recording',
        name: 'MockInterviewRecording',
        component: () => import('@/views/InterviewRecording.vue'),
        meta: { title: '面试录音回放' }
      },
      {
        path: 'reviews',
        name: 'ReviewList',
        component: () => import('@/views/Review.vue'),
        meta: { title: '复盘历史' }
      },
      {
        path: 'reviews/:id',
        name: 'ReviewDetail',
        component: () => import('@/views/ReviewDetail.vue'),
        meta: { title: '复盘详情' }
      }
    ]
  },
  // ==================== 旧路由重定向（兼容性） ====================
  {
    path: '/interview',
    redirect: '/interview-center'
  },
  {
    path: '/interview/:id',
    redirect: '/interview-center/:id'
  },
  {
    path: '/review',
    redirect: '/interview-center/reviews'
  },
  {
    path: '/review/:id',
    redirect: '/interview-center/reviews/:id'
  },
  // ==================== 个人中心 ====================
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '个人中心' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_to: RouteLocationNormalized, _from: RouteLocationNormalized, savedPosition: { left: number, top: number } | null) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  }
})

router.beforeEach(async (to: RouteLocationNormalized, _from: RouteLocationNormalized, next: NavigationGuardNext) => {
  const meta = to.meta as { title?: string; public?: boolean }
  document.title = `${meta.title ?? 'LandIt'} - LandIt智能求职助手`

  const store = useAppStore()

  // 检查用户是否已初始化
  if (!store.isInitialized) {
    const status = await store.checkUserExists()
    // 用户不存在 → 跳转 onboarding
    if (!status.exists && to.name !== 'Onboarding') {
      return next({ name: 'Onboarding' })
    }
    // 用户已存在 → 访问 onboarding 时跳转首页
    if (status.exists && to.name === 'Onboarding') {
      return next({ name: 'Home' })
    }
  }

  // 未初始化用户跳转到引导页
  if (!store.isLoggedIn && to.name !== 'Onboarding' && !meta.public) {
    return next({ name: 'Onboarding' })
  }

  next()
})

export default router
