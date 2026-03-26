<!--=====================================================
  LandIt 个人中心页面
  @author Azir
=====================================================-->

<template>
  <div class="profile-page">
    <div class="container">
      <!-- 个人信息卡片 -->
      <section
        class="profile-card animate-in"
        style="--delay: 0"
      >
        <div class="profile-avatar">
          <div class="avatar-circle">
            <img
              v-if="store.user.avatar"
              :src="store.user.avatar"
              alt="头像"
              class="avatar-image"
            >
            <span
              v-else
              class="avatar-text"
            >{{ userInitial }}</span>
          </div>
          <button
            class="change-avatar-btn"
            @click="triggerAvatarUpload"
          >
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
              <circle
                cx="12"
                cy="13"
                r="4"
              />
            </svg>
          </button>
          <input
            ref="avatarInput"
            type="file"
            accept="image/*"
            class="hidden-input"
            @change="handleAvatarUpload"
          >
        </div>
        <div class="profile-info">
          <h1 class="profile-name">
            {{ store.user.name }}
          </h1>
          <p class="profile-gender">
            {{ genderText }}
          </p>
        </div>
      </section>

      <!-- 基本信息编辑 -->
      <section
        class="info-section animate-in"
        style="--delay: 1"
      >
        <h2 class="section-title">
          基本信息
        </h2>
        <div class="info-form">
          <div class="form-group">
            <label class="form-label">姓名</label>
            <input
              v-model="userInfo.name"
              type="text"
              class="form-input"
            >
          </div>
          <div class="form-group">
            <label class="form-label">性别</label>
            <div class="gender-options">
              <button
                class="gender-btn"
                :class="{ active: userInfo.gender === 'MALE' }"
                @click="userInfo.gender = 'MALE'"
              >
                <span class="gender-icon">👨</span>
                <span>男</span>
              </button>
              <button
                class="gender-btn"
                :class="{ active: userInfo.gender === 'FEMALE' }"
                @click="userInfo.gender = 'FEMALE'"
              >
                <span class="gender-icon">👩</span>
                <span>女</span>
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- 保存按钮 -->
      <div
        class="save-bar animate-in"
        style="--delay: 2"
      >
        <button
          class="save-btn"
          @click="saveProfile"
        >
          <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
            <polyline points="17 21 17 13 7 13 7 21" />
            <polyline points="7 3 7 8 15 8" />
          </svg>
          保存更改
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { useAppStore } from '@/stores'
import type { Gender } from '@/types'

const store = useAppStore()
const avatarInput = ref<HTMLInputElement | null>(null)

const userInitial = computed((): string => store.user.name?.charAt(0) || 'U')

const genderText = computed((): string => {
  if (store.user.gender === 'MALE') return '男'
  if (store.user.gender === 'FEMALE') return '女'
  return '未设置'
})

const userInfo = reactive<{
  name: string
  gender: Gender | null
}>({
  name: store.user.name,
  gender: store.user.gender
})

function triggerAvatarUpload(): void {
  avatarInput.value?.click()
}

function handleAvatarUpload(event: Event): void {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    // TODO: 调用真实 API 上传头像
    console.log('上传头像', file)
  }
}

function saveProfile(): void {
  store.updateUserInfo({
    name: userInfo.name,
    gender: userInfo.gender
  })
  console.log('保存个人信息', userInfo)
}
</script>

<style lang="scss" scoped>
.profile-page {
  padding: $spacing-2xl;
  max-width: 600px;
  margin: 0 auto;
}

.container {
  display: flex;
  flex-direction: column;
  gap: $spacing-2xl;
}

// 个人信息卡片
.profile-card {
  display: flex;
  align-items: center;
  gap: $spacing-2xl;
  padding: $spacing-2xl;
  background: $gradient-card;
  border-radius: $radius-xl;
  border: 1px solid rgba(255, 255, 255, 0.05);
  position: relative;
  overflow: hidden;
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(212, 168, 83, 0.3), transparent);
  }
}

.profile-avatar {
  position: relative;
}

.avatar-circle {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: $gradient-gold;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-text {
  font-family: $font-display;
  font-size: $text-4xl;
  font-weight: $weight-bold;
  color: $color-bg-deep;
}

.change-avatar-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 32px;
  height: 32px;
  background: $color-bg-secondary;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $color-text-secondary;
  border: 2px solid $color-bg-deep;
  transition: all $transition-fast;
  &:hover {
    color: $color-accent;
  }
}

.hidden-input {
  display: none;
}

.profile-info {
  flex: 1;
}

.profile-name {
  font-family: $font-display;
  font-size: $text-3xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-xs;
}

.profile-gender {
  font-size: $text-base;
  color: $color-text-secondary;
}

// 表单区域
.section-title {
  font-family: $font-display;
  font-size: $text-xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-lg;
}

.info-form {
  background: $gradient-card;
  border-radius: $radius-lg;
  padding: $spacing-xl;
  border: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.form-label {
  font-size: $text-sm;
  color: $color-text-tertiary;
  font-weight: $weight-medium;
}

.form-input {
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-md;
  color: $color-text-primary;
  font-size: $text-base;
  transition: all $transition-fast;
  &:focus {
    outline: none;
    border-color: $color-accent;
  }
}

.gender-options {
  display: flex;
  gap: $spacing-md;
}

.gender-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  padding: $spacing-md $spacing-lg;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-md;
  color: $color-text-secondary;
  font-size: $text-base;
  transition: all $transition-fast;
  &.active {
    background: $color-accent-glow;
    border-color: $color-accent;
    color: $color-accent;
  }
  &:hover:not(.active) {
    background: rgba(255, 255, 255, 0.05);
  }
}

.gender-icon {
  font-size: 1.25rem;
}

// 保存按钮
.save-bar {
  display: flex;
  justify-content: center;
  padding-top: $spacing-lg;
}

.save-btn {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md $spacing-3xl;
  background: $gradient-gold;
  color: $color-bg-deep;
  font-size: $text-base;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  transition: all $transition-fast;
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 32px rgba(212, 168, 83, 0.4);
  }
}

// 动画
.animate-in {
  animation: slideUp 0.6s ease forwards;
  animation-delay: calc(var(--delay) * 0.1s);
  opacity: 0;
}
</style>
