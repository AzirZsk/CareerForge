// =====================================================
// 职位管理类型定义
// @author Azir
// =====================================================

// 职位状态（用户手动设置）
export type PositionStatus = 'draft' | 'applied' | 'interviewing' | 'offered' | 'rejected' | 'withdrawn'

// 职位列表项
export interface JobPositionListItem {
  id: string
  companyName: string
  title: string
  status: PositionStatus
  nextInterviewDate?: string
  nextInterviewRound?: string
  interviewCount: number
  latestInterviewDate?: string
  createdAt: string
  updatedAt: string
}

// 面试简要信息
export interface InterviewBrief {
  id: string
  status: string
  date?: string
  overallResult?: string
  // 新增字段
  source?: string           // 面试来源（real/mock）
  roundType?: string        // 轮次类型
  roundName?: string        // 自定义轮次名称
  interviewType?: string    // 面试类型（onsite/online）
  location?: string         // 现场地址
  onlineLink?: string       // 线上链接
}

// 职位详情
export interface JobPositionDetail {
  id: string
  companyId: string
  companyName: string
  title: string
  status: PositionStatus
  jdContent?: string
  jdAnalysis?: string
  jdAnalysisUpdatedAt?: string
  interviews: InterviewBrief[]
  createdAt: string
  updatedAt: string
}

// 创建职位请求
export interface CreateJobPositionRequest {
  companyName: string
  title: string
  jdContent?: string
}

// 更新职位请求
export interface UpdateJobPositionRequest {
  companyName?: string
  title?: string
  jdContent?: string
}
