// =====================================================
// 职位状态常量配置
// @author Azir
// =====================================================

import type { PositionStatus } from '@/types/job-position'

// 状态配置接口
export interface StatusConfig {
  label: string
  color: string
  bgColor: string
  borderColor: string
  icon: string
  pulse?: boolean
}

// 状态配置映射
export const POSITION_STATUS_CONFIG: Record<PositionStatus, StatusConfig> = {
  draft: {
    label: '草稿',
    color: '#71717a',
    bgColor: 'rgba(113, 113, 122, 0.15)',
    borderColor: 'rgba(113, 113, 122, 0.3)',
    icon: 'fa-solid fa-pen-to-square'
  },
  applied: {
    label: '已投递',
    color: '#60a5fa',
    bgColor: 'rgba(96, 165, 250, 0.15)',
    borderColor: 'rgba(96, 165, 250, 0.3)',
    icon: 'fa-solid fa-paper-plane'
  },
  interviewing: {
    label: '面试中',
    color: '#fbbf24',
    bgColor: 'rgba(251, 191, 36, 0.15)',
    borderColor: 'rgba(251, 191, 36, 0.3)',
    icon: 'fa-solid fa-bullseye',
    pulse: true
  },
  offered: {
    label: '已获Offer',
    color: '#34d399',
    bgColor: 'rgba(52, 211, 153, 0.15)',
    borderColor: 'rgba(52, 211, 153, 0.3)',
    icon: 'fa-solid fa-champagne-glasses'
  },
  rejected: {
    label: '未通过',
    color: '#f87171',
    bgColor: 'rgba(248, 113, 113, 0.15)',
    borderColor: 'rgba(248, 113, 113, 0.3)',
    icon: 'fa-solid fa-circle-xmark'
  },
  withdrawn: {
    label: '已撤回',
    color: '#71717a',
    bgColor: 'rgba(113, 113, 122, 0.15)',
    borderColor: 'rgba(113, 113, 122, 0.3)',
    icon: 'fa-solid fa-arrow-rotate-left'
  }
}

// 获取状态配置
export function getStatusConfig(status: PositionStatus): StatusConfig {
  return POSITION_STATUS_CONFIG[status] || POSITION_STATUS_CONFIG.draft
}

// 获取状态标签
export function getStatusLabel(status: PositionStatus): string {
  return getStatusConfig(status).label
}
