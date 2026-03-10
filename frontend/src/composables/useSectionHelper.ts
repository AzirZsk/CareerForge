// =====================================================
// 简历模块辅助工具
// @author Azir
// =====================================================

import type { ResumeSection, ResumeSectionItem, BasicInfoContent, WorkExperience, ProjectExperience, EducationContent, Skill, CertificateContent, OpenSourceContribution, CustomSection } from '@/types'

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
  'name',            // 姓名
  'targetPosition',  // 目标岗位（提前，作为简历定位锚点）
  'gender',          // 性别
  'birthday',        // 出生日期
  'age',             // 年龄
  'phone',           // 电话
  'email',           // 邮箱
  'location',        // 所在地
  'linkedin',        // LinkedIn
  'github',          // GitHub
  'website',         // 个人网站
  'summary'          // 个人简介
]

// 聚合类型列表
const AGGREGATE_TYPES = ['EDUCATION', 'WORK', 'PROJECT', 'CERTIFICATE', 'OPEN_SOURCE', 'CUSTOM', 'SKILLS']

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

  /**
   * 解析聚合类型的 content 为数组
   * content 是 JSON 字符串格式的数组
   */
  function parseAggregatedContent<T>(section: ResumeSection): T[] {
    if (!section.content) {
      return []
    }
    try {
      const parsed = typeof section.content === 'string'
        ? JSON.parse(section.content)
        : section.content
      return Array.isArray(parsed) ? parsed : []
    } catch {
      console.warn('解析聚合 content 失败:', section.content)
      return []
    }
  }

  /**
   * 获取工作经历列表
   */
  function getWorkList(section: ResumeSection): WorkExperience[] {
    return parseAggregatedContent<WorkExperience>(section)
  }

  /**
   * 获取工作经历列表（ResumeSectionItem 格式）
   */
  function getWorkItems(section: ResumeSection): ResumeSectionItem<WorkExperience>[] {
    const items = parseAggregatedContent<WorkExperience & { id?: string }>(section)
    return items.map((item, index) => ({
      id: item.id || `work_${index}`,
      content: item
    }))
  }

  /**
   * 获取项目经历列表
   */
  function getProjectList(section: ResumeSection): ProjectExperience[] {
    return parseAggregatedContent<ProjectExperience>(section)
  }

  /**
   * 获取项目经历列表（ResumeSectionItem 格式）
   */
  function getProjectItems(section: ResumeSection): ResumeSectionItem<ProjectExperience>[] {
    const items = parseAggregatedContent<ProjectExperience & { id?: string }>(section)
    return items.map((item, index) => ({
      id: item.id || `project_${index}`,
      content: item
    }))
  }

  /**
   * 获取教育经历列表
   */
  function getEducationList(section: ResumeSection): EducationContent[] {
    return parseAggregatedContent<EducationContent>(section)
  }

  /**
   * 获取教育经历列表（ResumeSectionItem 格式）
   */
  function getEducationItems(section: ResumeSection): ResumeSectionItem<EducationContent>[] {
    const items = parseAggregatedContent<EducationContent & { id?: string }>(section)
    return items.map((item, index) => ({
      id: item.id || `education_${index}`,
      content: item
    }))
  }

  /**
   * 获取技能列表
   * content 是 JSON 字符串格式的数组
   */
  function getSkillsList(section: ResumeSection): Skill[] {
    return parseAggregatedContent<Skill>(section)
  }

  /**
   * 获取证书列表
   */
  function getCertificateList(section: ResumeSection): CertificateContent[] {
    return parseAggregatedContent<CertificateContent>(section)
  }

  /**
   * 获取证书列表（ResumeSectionItem 格式）
   */
  function getCertificateItems(section: ResumeSection): ResumeSectionItem<CertificateContent>[] {
    const items = parseAggregatedContent<CertificateContent & { id?: string }>(section)
    return items.map((item, index) => ({
      id: item.id || `certificate_${index}`,
      content: item
    }))
  }

  /**
   * 获取开源贡献列表
   */
  function getOpenSourceList(section: ResumeSection): OpenSourceContribution[] {
    return parseAggregatedContent<OpenSourceContribution>(section)
  }

  /**
   * 获取开源贡献列表（ResumeSectionItem 格式）
   */
  function getOpenSourceItems(section: ResumeSection): ResumeSectionItem<OpenSourceContribution>[] {
    const items = parseAggregatedContent<OpenSourceContribution & { id?: string }>(section)
    return items.map((item, index) => ({
      id: item.id || `opensource_${index}`,
      content: item
    }))
  }

  /**
   * 获取自定义区块列表
   */
  function getCustomList(section: ResumeSection): CustomSection[] {
    return parseAggregatedContent<CustomSection>(section)
  }

  /**
   * 获取自定义区块列表（ResumeSectionItem 格式）
   */
  function getCustomItems(section: ResumeSection): ResumeSectionItem<CustomSection>[] {
    const items = parseAggregatedContent<CustomSection & { id?: string; score?: number }>(section)
    return items.map((item, index) => ({
      id: item.id || `custom_${index}`,
      title: item.title || '自定义区块',
      content: item,
      score: item.score
    }))
  }

  // 判断是否为聚合类型
  function isAggregateType(type: string): boolean {
    return AGGREGATE_TYPES.includes(type)
  }

  // 获取模块图标
  function getSectionIcon(type: string): string {
    return sectionIcons[type] || '📄'
  }

  // 获取模块预览文本
  function getSectionPreview(section: ResumeSection): string {
    // 技能类型：解析 content 中的 skills 数组
    if (section.type === 'SKILLS') {
      const skills = getSkillsList(section)
      return `${skills.length} 项技能`
    }
    // 聚合类型：解析 content 数组
    if (isAggregateType(section.type)) {
      const items = parseAggregatedContent(section)
      if (items.length > 0) {
        if (section.type === 'OPEN_SOURCE') {
          return `${items.length} 个项目`
        }
        return `${items.length} 条记录`
      }
      return '暂无记录'
    }
    // 单条类型（BASIC_INFO）- 需要解析 JSON 字符串
    if (section.type === 'BASIC_INFO') {
      const content = parseContent<BasicInfoContent>(section.content)
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

  /**
   * 更新聚合类型中的某一条记录
   * @param section 原始 section
   * @param index 条目索引
   * @param newContent 新内容
   * @returns 更新后的整个数组 JSON 字符串
   */
  function updateAggregatedItem(
    section: ResumeSection,
    index: number,
    newContent: Record<string, unknown>
  ): string {
    const items = parseAggregatedContent(section)
    if (index < 0 || index >= items.length) {
      throw new Error(`索引 ${index} 超出范围`)
    }
    items[index] = newContent
    return JSON.stringify(items)
  }

  /**
   * 新增聚合类型条目（追加到数组末尾）
   * @param section 原始 section
   * @param newContent 新内容
   * @returns 新增后的整个数组 JSON 字符串
   */
  function addAggregatedItem(
    section: ResumeSection,
    newContent: Record<string, unknown>
  ): string {
    const items = parseAggregatedContent(section)
    items.push(newContent)
    return JSON.stringify(items)
  }

  /**
   * 删除聚合类型中的某一条记录
   * @param section 原始 section
   * @param index 条目索引
   * @returns 删除后的整个数组 JSON 字符串
   */
  function deleteAggregatedItem(
    section: ResumeSection,
    index: number
  ): string {
    const items = parseAggregatedContent(section)
    if (index < 0 || index >= items.length) {
      throw new Error(`索引 ${index} 超出范围`)
    }
    items.splice(index, 1)
    return JSON.stringify(items)
  }

  /**
   * 获取聚合类型条目的索引（通过虚拟 ID）
   * 虚拟 ID 格式：type_index（如 work_0, education_1）
   */
  function getAggregatedItemIndex(virtualId: string): number {
    const parts = virtualId.split('_')
    if (parts.length < 2) {
      return -1
    }
    const index = parseInt(parts[parts.length - 1], 10)
    return isNaN(index) ? -1 : index
  }

  return {
    sectionIcons,
    fieldLabels,
    parseContent,
    parseAggregatedContent,
    getWorkList,
    getWorkItems,
    getProjectList,
    getProjectItems,
    getEducationList,
    getEducationItems,
    getSkillsList,
    getCertificateList,
    getCertificateItems,
    getOpenSourceList,
    getOpenSourceItems,
    getCustomList,
    getCustomItems,
    isAggregateType,
    getSectionIcon,
    getSectionPreview,
    getCustomItemPreview,
    getScoreClass,
    getFieldLabel,
    getOrderedBasicInfoFields,
    // 聚合类型条目操作
    updateAggregatedItem,
    addAggregatedItem,
    deleteAggregatedItem,
    getAggregatedItemIndex
  }
}
