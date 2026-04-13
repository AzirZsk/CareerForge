// =====================================================
// 统计数据 API
// @author Azir
// =====================================================

import type { Statistics } from '@/types'
import request from '@/utils/request'

/**
 * 获取统计数据
 */
export async function getStatistics(): Promise<Statistics> {
  return request({
    url: '/statistics',
    method: 'GET'
  })
}
