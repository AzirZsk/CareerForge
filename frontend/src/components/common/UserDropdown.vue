<!--=====================================================
  CareerForge 用户头像下拉菜单
  @author Azir
=====================================================-->

<template>
  <div class="user-dropdown-container">
    <!-- 用户头像（触发器） -->
    <div
      class="user-avatar-trigger"
      @click="toggleDropdown"
    >
      <span class="avatar-text">{{ userInitial }}</span>
    </div>

    <!-- 遮罩层 -->
    <div
      v-if="isOpen"
      class="dropdown-overlay"
      @click="closeDropdown"
    />

    <!-- 下拉菜单 -->
    <Transition name="dropdown">
      <div
        v-if="isOpen"
        class="user-dropdown"
      >
        <!-- 用户信息 -->
        <div class="dropdown-header">
          <div class="user-name">{{ store.user.name }}</div>
        </div>

        <!-- 分隔线 -->
        <div class="dropdown-divider" />

        <!-- 菜单项 -->
        <div class="dropdown-menu">
          <button
            class="menu-item"
            @click="goToProfile"
          >
            <font-awesome-icon :icon="['fa-solid', 'fa-user']" />
            <span>个人中心</span>
          </button>
          <button
            class="menu-item danger"
            @click="handleLogout"
          >
            <font-awesome-icon :icon="['fa-solid', 'fa-right-from-bracket']" />
            <span>退出登录</span>
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAppStore } from '@/stores'
import { useRouter } from 'vue-router'

const store = useAppStore()
const router = useRouter()

const isOpen = ref(false)

const userInitial = computed((): string => {
  return store.user.name ? store.user.name.charAt(0) : 'U'
})

function toggleDropdown() {
  isOpen.value = !isOpen.value
}

function closeDropdown() {
  isOpen.value = false
}

function goToProfile() {
  closeDropdown()
  router.push('/profile')
  store.setActiveNav('profile')
}

async function handleLogout() {
  closeDropdown()
  try {
    await store.logout()
    router.push('/login')
  } catch (error) {
    console.error('退出登录失败', error)
  }
}

// 点击外部关闭（键盘ESC）
function handleEscape(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    closeDropdown()
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleEscape)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleEscape)
})
</script>

<style lang="scss" scoped>
.user-dropdown-container {
  position: relative;
}

.user-avatar-trigger {
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

.dropdown-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
}

.user-dropdown {
  position: absolute;
  top: calc(100% + $spacing-sm);
  right: 0;
  min-width: 200px;
  background: $color-bg-elevated;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-lg;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  z-index: 1000;
  overflow: hidden;
}

.dropdown-header {
  padding: $spacing-md $spacing-lg;
  background: rgba(212, 168, 83, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.user-name {
  font-size: $text-sm;
  font-weight: $weight-semibold;
  color: $color-text-primary;
}

.dropdown-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.05);
}

.dropdown-menu {
  padding: $spacing-xs;
}

.menu-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  border-radius: $radius-sm;
  color: $color-text-secondary;
  font-size: $text-sm;
  font-weight: $weight-medium;
  transition: all $transition-fast;
  background: transparent;
  border: none;
  cursor: pointer;
  text-align: left;
  &:hover {
    color: $color-text-primary;
    background: rgba(255, 255, 255, 0.05);
  }
  &.danger {
    color: $color-error;
    &:hover {
      background: rgba(248, 113, 113, 0.1);
    }
  }
}

// 下拉动画
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
