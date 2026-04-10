/**
 * 认证相关类型定义
 *
 * @author Azir
 */
import type { Gender, User } from './index'

/**
 * 注册请求
 */
export interface RegisterRequest {
  email: string
  password: string
  name: string
  gender?: Gender
}

/**
 * 登录请求
 */
export interface LoginRequest {
  account: string
  password: string
}

/**
 * 登录响应
 */
export interface LoginResponse {
  token: string
  user: User
}

/**
 * 注册响应
 */
export interface RegisterResponse {
  id: string
  name: string
  email: string
}
