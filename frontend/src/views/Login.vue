<template>
  <div class="login-container">
    <div class="login-card">
      <!-- 标题区域 -->
      <div class="login-header">
        <h1 class="login-title">{{ isLoginMode ? '欢迎回来' : '创建账户' }}</h1>
        <p class="login-subtitle">{{ isLoginMode ? '登录到 CareerForge 智能求职助手' : '开启您的智能求职之旅' }}</p>
      </div>

      <!-- 标签切换 -->
      <div class="login-tabs">
        <button
          :class="['tab-button', { active: isLoginMode }]"
          @click="isLoginMode = true"
        >
          登录
        </button>
        <button
          :class="['tab-button', { active: !isLoginMode }]"
          @click="isLoginMode = false"
        >
          注册
        </button>
      </div>

      <!-- 登录表单 -->
      <form v-if="isLoginMode" class="login-form" @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="login-account">账号</label>
          <input
            id="login-account"
            v-model="loginForm.account"
            type="text"
            placeholder="请输入邮箱或手机号"
            required
          />
        </div>

        <div class="form-group">
          <label for="login-password">密码</label>
          <input
            id="login-password"
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            required
          />
        </div>

        <button type="submit" class="submit-button" :disabled="isLoading">
          {{ isLoading ? '登录中...' : '登录' }}
        </button>
      </form>

      <!-- 注册表单 -->
      <form v-else class="login-form" @submit.prevent="handleRegister">
        <div class="form-group">
          <label for="register-email">邮箱</label>
          <input
            id="register-email"
            v-model="registerForm.email"
            type="email"
            placeholder="请输入邮箱"
            required
          />
        </div>

        <div class="form-group">
          <label for="register-name">姓名</label>
          <input
            id="register-name"
            v-model="registerForm.name"
            type="text"
            placeholder="请输入您的姓名"
            required
          />
        </div>

        <div class="form-group">
          <label for="register-password">密码</label>
          <input
            id="register-password"
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码（8-20位）"
            required
            minlength="8"
            maxlength="20"
          />
          <p v-if="passwordError" class="error-hint">{{ passwordError }}</p>
        </div>

        <div class="form-group">
          <label for="register-confirm-password">确认密码</label>
          <input
            id="register-confirm-password"
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            required
          />
          <p v-if="confirmPasswordError" class="error-hint">{{ confirmPasswordError }}</p>
        </div>

        <div class="form-group">
          <label for="register-gender">性别（可选）</label>
          <select id="register-gender" v-model="registerForm.gender">
            <option value="">请选择</option>
            <option value="MALE">男</option>
            <option value="FEMALE">女</option>
          </select>
        </div>

        <button type="submit" class="submit-button" :disabled="isLoading || !isFormValid">
          {{ isLoading ? '注册中...' : '注册' }}
        </button>
      </form>

      <!-- 错误提示 -->
      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores'
import { useToast } from '@/composables/useToast'
import { updateToken } from '@/utils/request'
import * as authApi from '@/api/auth'
import type { LoginRequest, RegisterRequest } from '@/types/auth'

const router = useRouter()
const store = useAppStore()
const toast = useToast()

// 表单模式
const isLoginMode = ref(true)

// 加载状态
const isLoading = ref(false)

// 错误消息
const errorMessage = ref('')

// 登录表单
const loginForm = ref<LoginRequest>({
  account: '',
  password: ''
})

// 注册表单
const registerForm = ref<RegisterRequest & { confirmPassword: string }>({
  email: '',
  password: '',
  name: '',
  gender: undefined,
  confirmPassword: ''
})

// 密码最小长度
const MIN_PASSWORD_LENGTH = 8

// 密码错误提示
const passwordError = computed(() => {
  if (!registerForm.value.password) return ''
  if (registerForm.value.password.length < MIN_PASSWORD_LENGTH) {
    return `密码长度不能少于${MIN_PASSWORD_LENGTH}位`
  }
  return ''
})

// 确认密码错误提示
const confirmPasswordError = computed(() => {
  if (!registerForm.value.confirmPassword) return ''
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    return '两次密码输入不一致'
  }
  return ''
})

// 表单是否有效
const isFormValid = computed(() => {
  return (
    registerForm.value.email &&
    registerForm.value.name &&
    registerForm.value.password.length >= MIN_PASSWORD_LENGTH &&
    registerForm.value.password === registerForm.value.confirmPassword
  )
})

/**
 * 处理登录
 */
async function handleLogin() {
  try {
    isLoading.value = true
    errorMessage.value = ''

    const response = await authApi.login(loginForm.value)

    // 保存 Token 和用户信息
    updateToken(response.token)
    store.user = response.user
    localStorage.setItem('user', JSON.stringify(response.user))
    store.isLoggedIn = true
    store.isInitialized = response.user.initialized ?? false
    localStorage.setItem('isInitialized', String(store.isInitialized))

    // 根据初始化状态跳转：未初始化去上传简历，已初始化去首页
    if (store.isInitialized) {
      router.push('/')
    } else {
      router.push('/onboarding')
    }
  } catch (error: any) {
    errorMessage.value = error.response?.data?.message || '登录失败，请检查账号密码'
  } finally {
    isLoading.value = false
  }
}

/**
 * 处理注册
 */
async function handleRegister() {
  if (!isFormValid.value) return

  try {
    isLoading.value = true
    errorMessage.value = ''

    const { confirmPassword, gender, ...rest } = registerForm.value
    // gender 为空字符串时不发送该字段
    const data = { ...rest, ...(gender ? { gender } : {}) }
    await authApi.register(data)

    // 注册成功，切换到登录模式
    isLoginMode.value = true
    loginForm.value.account = data.email
    loginForm.value.password = data.password

    // 提示用户登录
    errorMessage.value = ''
    toast.success('注册成功！请使用您的邮箱和密码登录')
  } catch (error: any) {
    errorMessage.value = error.response?.data?.message || '注册失败，请稍后重试'
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $color-bg-deep;
  padding: $spacing-lg;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  padding: $spacing-2xl;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: $spacing-xl;
}

.login-title {
  font-size: 28px;
  font-weight: 600;
  color: $color-text-primary;
  margin-bottom: $spacing-sm;
}

.login-subtitle {
  font-size: 14px;
  color: $color-text-secondary;
}

.login-tabs {
  display: flex;
  background: $color-bg-tertiary;
  border-radius: $radius-md;
  padding: 4px;
  margin-bottom: $spacing-xl;
}

.tab-button {
  flex: 1;
  padding: $spacing-sm $spacing-md;
  background: transparent;
  border: none;
  border-radius: $radius-sm;
  color: $color-text-secondary;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    color: $color-text-primary;
  }

  &.active {
    background: $color-bg-elevated;
    color: $color-text-primary;
    font-weight: 500;
  }
}

.login-form {
  .form-group {
    margin-bottom: $spacing-lg;
  }

  label {
    display: block;
    font-size: 14px;
    color: $color-text-secondary;
    margin-bottom: $spacing-sm;
  }

  input,
  select {
    width: 100%;
    padding: $spacing-sm $spacing-md;
    background: $color-bg-tertiary;
    border: 1px solid transparent;
    border-radius: $radius-sm;
    color: $color-text-primary;
    font-size: 14px;
    transition: all 0.2s;

    &:focus {
      outline: none;
      border-color: $color-accent;
      background: $color-bg-elevated;
    }

    &::placeholder {
      color: $color-text-tertiary;
    }
  }

  .error-hint {
    margin-top: $spacing-xs;
    font-size: 12px;
    color: $color-error;
  }
}

.submit-button {
  width: 100%;
  padding: $spacing-md;
  background: $color-accent;
  color: $color-bg-deep;
  border: none;
  border-radius: $radius-sm;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &:hover:not(:disabled) {
    background: $color-accent-light;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.error-message {
  margin-top: $spacing-lg;
  text-align: center;
  font-size: 14px;
  color: $color-error;
}
</style>
