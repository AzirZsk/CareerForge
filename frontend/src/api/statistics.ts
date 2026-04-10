// =====================================================
// 统计数据 API
// @author Azir
// =====================================================

import type { ApiResponse, Statistics } from '@/types'
import { API_BASE } from './config'

/**
 * 获取统计数据
 */
export async function getStatistics(): Promise<Statistics> {
  const response = await fetch(`${API_BASE}/statistics`)
  const result: ApiResponse<Statistics> = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message || '获取统计数据失败')
  }
  return result.data
}
