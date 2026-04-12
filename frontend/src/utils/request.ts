/**
 * Axios 封装
 * 统一处理请求拦截和响应拦截
 *
 * @author Azir
 */
import axios, { type AxiosError, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'

// 缓存 token，避免每次请求都读取 localStorage
let cachedToken: string | null = null

// 创建 Axios 实例
const request = axios.create({
  baseURL: '/landit',
  timeout: 30000
})

// 更新缓存的 token
export function updateToken(token: string | null): void {
  cachedToken = token
  if (token) {
    localStorage.setItem('token', token)
  } else {
    localStorage.removeItem('token')
  }
}

// 请求拦截器 - 自动添加 Authorization header
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 使用缓存的 token，如果为空则从 localStorage 读取
    if (!cachedToken) {
      cachedToken = localStorage.getItem('token')
    }
    if (cachedToken && config.headers) {
      config.headers.Authorization = `Bearer ${cachedToken}`
    }
    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 自动解包 ApiResponse 并处理 401
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    // 后端统一 ApiResponse 格式：{ code, data, message, timestamp }
    // 自动解包 data 字段，让调用方直接拿到业务数据
    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code === 200) {
        return res.data
      }
      // 业务错误（参数错误、权限不足等）
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    // 兼容非 ApiResponse 格式
    return res
  },
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      // 清除本地 Token
      updateToken(null)
      // 跳转到登录页
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

/**
 * 带 Token 的 fetch 封装
 * 自动从 localStorage 读取 token 并添加 Authorization header
 * 用于 SSE 流式请求等原生 fetch 场景
 */
export function authFetch(url: string, options: RequestInit = {}): Promise<Response> {
  const token = cachedToken || localStorage.getItem('token')
  if (token) {
    options.headers = {
      ...options.headers,
      Authorization: `Bearer ${token}`
    }
  }
  return fetch(url, options)
}

export default request
