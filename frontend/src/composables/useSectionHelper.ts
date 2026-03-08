// =====================================================
// 简历模块辅助工具
// @author Azir
// =====================================================

import type { ResumeSection, BasicInfoContent, SkillsContent } from '@/types'

// 模块图标映射（后端大写格式）
const sectionIcons: Record<string, string> = {
  BASIC_INFO: '👤',
  EDUCATION: '🎓',
  WORK: '💼',
  PROJECT: '🎯',
  SKILLS: '⚡',
  CERTIFICATE: '🏆',
  OPEN_SOURCE: '🌐',
  CUSTOM: '📋'
}

// 字段标签映射（适配后端字段）
const fieldLabels: Record<string, string> = {
  name: '姓名',
  gender: '性别',
  phone: '电话',
  email: '邮箱',
  targetPosition: '目标岗位',
  location: '所在地',
  linkedin: 'LinkedIn',
  github: 'GitHub',
  website: '个人网站',
  summary: '个人简介',
  school: '学校',
  major: '专业',
  degree: '学历',
  period: '时间',
  company: '公司',
  position: '职位',
  description: '描述',
  role: '角色',
  achievements: '成果',
  skills: '技能',
  date: '日期',
  birthday: '出生日期',
  age: '年龄'
}

// 基本信息字段显示顺序（按简历展示习惯排序）
const BASIC_INFO_FIELD_ORDER: string[] = [
  'name',
  'gender',
  'birthday',
  'age',
  'phone',
  'email',
  'targetPosition',
  'location',
  'linkedin',
  'github',
  'website',
  'summary'
]

export function useSectionHelper() {
  // 解析 content 字符串为对象
  function parseContent<T = Record<string, unknown>>(content: string | Record<string, unknown> | undefined | null): T {
    if (!content) {
      return {} as T
    }
    // 如果已经是对象，直接返回
    if (typeof content === 'object') {
      return content as T
    }
    try {
      return JSON.parse(content) as T
    } catch {
      console.warn('解析 content 失败:', content)
      return {} as T
    }
  }

  // 获取模块图标
  function getSectionIcon(type: string): string {
    return sectionIcons[type] || '📄'
  }

  // 获取模块预览文本
  function getSectionPreview(section: ResumeSection): string {
    // 技能类型：统计 skills 数组长度
    if (section.type === 'SKILLS') {
      const firstItem = section.items?.[0]
      const content = firstItem?.content as SkillsContent | undefined
      return `${content?.skills?.length ?? 0} 项技能`
    }
    // 聚合类型（有items）
    if (section.items?.length) {
      if (section.type === 'OPEN_SOURCE') {
        return `${section.items.length} 个项目`
      }
      return `${section.items.length} 条记录`
    }
    // 单条类型（有content）
    if (section.type === 'BASIC_INFO') {
      const content = section.content as BasicInfoContent
      return content.name ?? '基本信息'
    }
    return section.title ?? ''
  }

  // 获取自定义区块 item 的预览文本
  function getCustomItemPreview(item: { content: string | Record<string, unknown> }): string {
    // content 可能是 JSON 字符串，需要先解析
    const parsedContent = parseContent<{ items?: Array<Record<string, unknown>> }>(item.content)
    if (parsedContent.items?.length) {
      return `${parsedContent.items.length} 条记录`
    }
    return '暂无内容'
  }

  // 获取评分样式类名
  function getScoreClass(score: number): string {
    if (score >= 90) {
      return 'excellent'
    }
    if (score >= 75) {
      return 'good'
    }
    return 'average'
  }

  // 获取字段标签
  function getFieldLabel(key: string): string {
    return fieldLabels[key] || key
  }

  // 获取排序后的基本信息字段
  function getOrderedBasicInfoFields(content: BasicInfoContent): { key: string; value: unknown }[] {
    const result: { key: string; value: unknown }[] = []
    // 先按预定义顺序添加
    for (const key of BASIC_INFO_FIELD_ORDER) {
      const value = content[key as keyof BasicInfoContent]
      if (value !== undefined && value !== null && value !== '') {
        result.push({ key, value })
      }
    }
    // 再添加未在预定义顺序中但有值的字段
    for (const [key, value] of Object.entries(content)) {
      if (!BASIC_INFO_FIELD_ORDER.includes(key) && value !== undefined && value !== null && value !== '') {
        result.push({ key, value })
      }
    }
    return result
  }

  return {
    sectionIcons,
    fieldLabels,
    parseContent,
    getSectionIcon,
    getSectionPreview,
    getCustomItemPreview,
    getScoreClass,
    getFieldLabel,
    getOrderedBasicInfoFields
  }
}
