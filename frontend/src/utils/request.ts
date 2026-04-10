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

// 响应拦截器 - 处理 401 自动跳转登录
request.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data
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

export default request
