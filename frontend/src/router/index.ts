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
  {
    path: '/interview',
    name: 'Interview',
    component: () => import('@/views/Interview.vue'),
    meta: { title: '面试试演' }
  },
  {
    path: '/interview/:id',
    name: 'InterviewSession',
    component: () => import('@/views/InterviewSession.vue'),
    meta: { title: '面试进行中' }
  },
  {
    path: '/review',
    name: 'Review',
    component: () => import('@/views/Review.vue'),
    meta: { title: '面试复盘' }
  },
  {
    path: '/review/:id',
    name: 'ReviewDetail',
    component: () => import('@/views/ReviewDetail.vue'),
    meta: { title: '复盘详情' }
  },
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
