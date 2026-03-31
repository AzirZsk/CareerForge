// =====================================================
// 职位管理类型定义
// @author Azir
// =====================================================

// 职位列表项
export interface JobPositionListItem {
  id: string
  companyName: string
  title: string
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
}

// 职位详情
export interface JobPositionDetail {
  id: string
  companyId: string
  companyName: string
  title: string
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
  title?: string
  jdContent?: string
}
