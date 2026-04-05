<!--=====================================================
  LandIt 导航栏组件
  @author Azir
=====================================================-->

<template>
  <header class="navbar">
    <div class="navbar-inner">
      <!-- Logo -->
      <router-link
        to="/"
        class="logo"
        @click="store.setActiveNav('home')"
      >
        <span class="logo-icon">L</span>
        <span class="logo-text">Land<span class="accent">It</span></span>
      </router-link>

      <!-- 主导航 -->
      <nav class="main-nav">
        <router-link
          v-for="item in navItems"
          :key="item.key"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.key) }"
          @click="store.setActiveNav(item.key)"
        >
          <span
            class="nav-icon"
            v-html="item.icon"
          />
          <span class="nav-label">{{ item.label }}</span>
          <span
            v-if="item.badge"
            class="nav-badge"
          >{{ item.badge }}</span>
        </router-link>
      </nav>

      <!-- 用户区域 -->
      <div class="user-area">
        <!-- 通知 -->
        <button class="icon-btn notification-btn">
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
            <path d="M13.73 21a2 2 0 0 1-3.46 0" />
          </svg>
          <span class="notification-dot" />
        </button>

        <!-- 用户头像 -->
        <div
          class="user-avatar"
          @click="goToProfile"
        >
          <span class="avatar-text">{{ userInitial }}</span>
        </div>
      </div>
    </div>

    <!-- 底部装饰线 -->
    <div class="navbar-glow" />
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAppStore } from '@/stores'
import { useRouter, useRoute } from 'vue-router'
import type { NavItem } from '@/types'

const store = useAppStore()
const router = useRouter()
const route = useRoute()

const navItems: NavItem[] = [
  {
    key: 'home',
    label: '工作台',
    path: '/',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>'
  },
  {
    key: 'resume',
    label: '简历管理',
    path: '/resume',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>'
  },
  {
    key: 'interview-center',
    label: '面试中心',
    path: '/interview-center',
    badge: 'NEW',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>'
  }
]

const userInitial = computed((): string => {
  return store.user.name ? store.user.name.charAt(0) : 'U'
})

function isActive(key: string): boolean {
  const currentPath = route.path
  // 首页精确匹配
  if (key === 'home') {
    return currentPath === '/'
  }
  // 其他导航项：检查路径前缀
  const navItem = navItems.find(item => item.key === key)
  if (navItem?.path) {
    return currentPath.startsWith(navItem.path)
  }
  return false
}

function goToProfile(): void {
  router.push('/profile')
  store.setActiveNav('profile')
}
</script>

<style lang="scss" scoped>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 80px;
  background: rgba(17, 17, 19, 0.8);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  z-index: $z-sticky;
}

.navbar-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 $spacing-xl;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  text-decoration: none;
  transition: transform $transition-fast;
  &:hover {
    transform: scale(1.02);
  }
}

.logo-icon {
  width: 40px;
  height: 40px;
  background: $gradient-gold;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: $font-display;
  font-size: $text-xl;
  font-weight: $weight-bold;
  color: $color-bg-deep;
}

.logo-text {
  font-family: $font-display;
  font-size: $text-xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  letter-spacing: -0.02em;
  .accent {
    color: $color-accent;
  }
}

.main-nav {
  display: flex;
  gap: $spacing-xs;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  border-radius: $radius-md;
  color: $color-text-secondary;
  font-size: $text-sm;
  font-weight: $weight-medium;
  transition: all $transition-fast;
  text-decoration: none;
  &:hover {
    color: $color-text-primary;
    background: rgba(255, 255, 255, 0.05);
  }
  &.active {
    color: $color-accent;
    background: $color-accent-glow;
  }
}

.nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.8;
  .active & {
    opacity: 1;
  }
}

.nav-label {
  position: relative;
}

.nav-badge {
  position: absolute;
  top: -8px;
  right: -12px;
  padding: 2px 6px;
  background: $color-accent;
  color: $color-bg-deep;
  font-size: 9px;
  font-weight: $weight-bold;
  border-radius: $radius-full;
  letter-spacing: 0.5px;
}

.user-area {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.icon-btn {
  width: 40px;
  height: 40px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $color-text-secondary;
  transition: all $transition-fast;
  position: relative;
  &:hover {
    color: $color-text-primary;
    background: rgba(255, 255, 255, 0.05);
  }
}

.notification-dot {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 8px;
  height: 8px;
  background: $color-accent;
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: $radius-md;
  background: $gradient-card;
  border: 2px solid $color-accent;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all $transition-fast;
  &:hover {
    transform: scale(1.05);
    box-shadow: 0 0 20px rgba(212, 168, 83, 0.3);
  }
}

.avatar-text {
  font-family: $font-display;
  font-size: $text-base;
  font-weight: $weight-semibold;
  color: $color-accent;
}

.navbar-glow {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 200px;
  height: 1px;
  background: $gradient-gold;
  opacity: 0.5;
}
</style>
