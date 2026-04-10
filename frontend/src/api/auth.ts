/**
 * 认证相关 API
 *
 * @author Azir
 */
import request from '@/utils/request'
import type { RegisterRequest, RegisterResponse } from '@/types/auth'
import type { LoginRequest, LoginResponse } from '@/types/auth'

/**
 * 用户注册
 */
export async function register(data: RegisterRequest): Promise<RegisterResponse> {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

/**
 * 用户登录
 */
export async function login(data: LoginRequest): Promise<LoginResponse> {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

/**
 * 用户登出
 */
export async function logout(): Promise<void> {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}
