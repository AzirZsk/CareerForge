// =====================================================
// CareerForge 路由配置
// @author Azir
// =====================================================

import { createRouter, createWebHistory, type RouteRecordRaw, type NavigationGuardNext, type RouteLocationNormalized } from 'vue-router'
import { useAppStore } from '@/stores'

const routes: RouteRecordRaw[] = [
  {
    path: '/onboarding',
    name: 'Onboarding',
    component: () => import('@/views/Onboarding.vue'),
    meta: { title: '欢迎', hideAIChat: true }
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
        meta: { title: '面试进行中', hideAIChat: true }
      },
      {
        path: 'mock/:sessionId/recording',
        name: 'MockInterviewRecording',
        component: () => import('@/views/InterviewRecording.vue'),
        meta: { title: '面试录音回放' }
      },
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
  // ==================== 个人中心 ====================
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '个人中心' }
  },
  // ==================== 认证相关 ====================
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true }
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

router.beforeEach((to: RouteLocationNormalized, _from: RouteLocationNormalized, next: NavigationGuardNext) => {
  const meta = to.meta as { title?: string; public?: boolean }
  document.title = `${meta.title ?? 'CareerForge'} - CareerForge智能求职助手`

  const store = useAppStore()

  // 初始化认证状态（从 localStorage 恢复）
  if (!store.isLoggedIn) {
    store.initAuthState()
  }

  // 公开页面直接放行
  if (meta.public) {
    return next()
  }

  // 未登录用户跳转到登录页
  if (!store.isLoggedIn && to.name !== 'Login') {
    return next({ name: 'Login' })
  }

  // 已登录用户访问登录页时跳转首页
  if (store.isLoggedIn && to.name === 'Login') {
    return next({ name: 'Home' })
  }

  // 已登录但未初始化的用户，必须先去上传简历
  if (store.isLoggedIn && !store.isInitialized && to.name !== 'Onboarding') {
    return next({ name: 'Onboarding' })
  }

  // 已初始化的用户访问 onboarding 页面时重定向到首页
  if (store.isLoggedIn && store.isInitialized && to.name === 'Onboarding') {
    return next({ name: 'Home' })
  }

  next()
})

export default router
