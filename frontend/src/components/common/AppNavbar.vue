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
          <span class="nav-icon">
            <font-awesome-icon :icon="item.icon" />
          </span>
          <span class="nav-label">{{ item.label }}</span>
          <span
            v-if="item.badge"
            class="nav-badge"
          >{{ item.badge }}</span>
        </router-link>
      </nav>

      <!-- 用户区域 -->
      <div class="user-area">
        <!-- 通知按钮 -->
        <button
          class="icon-btn notification-btn"
          :class="{ 'has-unread': notificationStore.hasUnread }"
          @click="toggleNotification"
        >
          <font-awesome-icon :icon="['fa-solid', 'fa-bell']" />
          <!-- 未读数量徽章 -->
          <span
            v-if="notificationStore.unreadCount > 0"
            class="notification-badge"
          >
            {{ notificationStore.unreadCount > 99 ? '99+' : notificationStore.unreadCount }}
          </span>
          <!-- 红点（有未读但无数量） -->
          <span
            v-else-if="notificationStore.hasUnread"
            class="notification-dot"
          />
        </button>

        <!-- 用户头像下拉菜单 -->
        <UserDropdown />
      </div>
    </div>

    <!-- 底部装饰线 -->
    <div class="navbar-glow" />

    <!-- 通知下拉面板 -->
    <NotificationDropdown
      :is-open="showNotificationDropdown"
      @close="closeNotification"
      @apply-transcript="handleApplyTranscript"
    />
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useAppStore } from '@/stores'
import { useNotificationStore } from '@/stores/notification'
import { useRoute } from 'vue-router'
import type { NavItem } from '@/types'
import NotificationDropdown from './NotificationDropdown.vue'
import UserDropdown from './UserDropdown.vue'
import type { AsyncTask } from '@/types/notification'

const store = useAppStore()
const notificationStore = useNotificationStore()
const route = useRoute()

// 通知下拉面板状态
const showNotificationDropdown = ref(false)

const navItems: NavItem[] = [
  {
    key: 'home',
    label: '工作台',
    path: '/',
    icon: 'fa-solid fa-table-cells'
  },
  {
    key: 'resume',
    label: '简历管理',
    path: '/resume',
    icon: 'fa-solid fa-file-lines'
  },
  {
    key: 'interview-center',
    label: '面试中心',
    path: '/interview-center',
    badge: 'NEW',
    icon: 'fa-solid fa-comments'
  }
]

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

// 通知相关方法
function toggleNotification() {
  showNotificationDropdown.value = !showNotificationDropdown.value
}

function closeNotification() {
  showNotificationDropdown.value = false
}

function handleApplyTranscript(task: AsyncTask) {
  // 触发全局事件，由 InterviewDetail 页面处理
  window.dispatchEvent(new CustomEvent('apply-transcript', {
    detail: { task }
  }))
}

// 生命周期钩子
onMounted(() => {
  // 初始化通知 store
  notificationStore.init()
})

onUnmounted(() => {
  // 清理通知 store
  notificationStore.dispose()
})
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

.notification-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background: $color-accent;
  color: $color-bg-deep;
  font-size: 10px;
  font-weight: $weight-bold;
  border-radius: $radius-full;
  display: flex;
  align-items: center;
  justify-content: center;
}

.notification-btn.has-unread {
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
