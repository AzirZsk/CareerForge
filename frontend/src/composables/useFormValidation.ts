/**
 * LandIt 表单校验 Composable
 * 统一管理简历模块表单的必填字段校验逻辑
 * @author Azir
 */

import { ref, inject, type InjectionKey, type Ref } from 'vue'

// 必填字段配置
const VALIDATION_RULES: Record<string, { field: string; label: string }[]> = {
  BASIC_INFO: [
    { field: 'name', label: '姓名' },
    { field: 'phone', label: '电话' },
    { field: 'email', label: '邮箱' }
  ],
  EDUCATION: [
    { field: 'school', label: '学校名称' }
  ],
  WORK: [
    { field: 'company', label: '公司名称' }
  ],
  PROJECT: [
    { field: 'name', label: '项目名称' }
  ],
  CERTIFICATE: [
    { field: 'name', label: '证书/荣誉名称' }
  ],
  OPEN_SOURCE: [
    { field: 'projectName', label: '项目名称' }
  ],
  CUSTOM: [
    { field: 'title', label: '区块标题' }
  ],
  CUSTOM_ITEM: [
    { field: 'name', label: '名称' }
  ]
}

// 错误状态接口
export interface ValidationContext {
  hasError: (field: string) => boolean
  getFieldError: (field: string) => string
  errors: Ref<Record<string, string>>
}

// 注入 key
export const ValidationKey: InjectionKey<ValidationContext> = Symbol('form-validation')

/**
 * 表单校验 Composable
 */
export function useFormValidation() {
  // 错误信息映射
  const errors = ref<Record<string, string>>({})

  /**
   * 校验指定区块类型的表单数据
   * @param sectionType 区块类型
   * @param data 表单数据
   * @returns 校验是否通过
   */
  function validate(sectionType: string, data: Record<string, unknown>): boolean {
    // 清空之前的错误
    errors.value = {}

    // 技能表单特殊处理
    if (sectionType === 'SKILLS') {
      return validateSkills(data)
    }

    // 自定义区块内容项特殊处理
    if (sectionType === 'CUSTOM_ITEM') {
      return validateCustomItem(data)
    }

    // 自定义区块（包含 items 数组）
    if (sectionType === 'CUSTOM') {
      return validateCustom(data)
    }

    // 获取该区块类型的校验规则
    const rules = VALIDATION_RULES[sectionType]
    if (!rules || rules.length === 0) {
      return true
    }

    // 执行校验
    let isValid = true
    for (const rule of rules) {
      const value = data[rule.field]
      if (isEmpty(value)) {
        errors.value[rule.field] = `请输入${rule.label}`
        isValid = false
      }
    }

    return isValid
  }

  /**
   * 校验技能表单
   */
  function validateSkills(data: Record<string, unknown>): boolean {
    const skills = data.skills as Array<{ name: string }> | undefined

    if (!Array.isArray(skills) || skills.length === 0) {
      errors.value['skills'] = '请至少添加一个技能'
      return false
    }

    // 检查是否有技能名称为空
    let hasValidSkill = false
    for (let i = 0; i < skills.length; i++) {
      if (!isEmpty(skills[i]?.name)) {
        hasValidSkill = true
      } else {
        errors.value[`skills.${i}.name`] = '请输入技能名称'
      }
    }

    if (!hasValidSkill) {
      errors.value['skills'] = '请至少添加一个技能名称'
      return false
    }

    return Object.keys(errors.value).length === 0
  }

  /**
   * 校验自定义区块内容项
   */
  function validateCustomItem(data: Record<string, unknown>): boolean {
    if (isEmpty(data.name)) {
      errors.value['name'] = '请输入名称'
      return false
    }
    return true
  }

  /**
   * 校验自定义区块（包含 title 和 items）
   */
  function validateCustom(data: Record<string, unknown>): boolean {
    let isValid = true

    // 校验标题
    if (isEmpty(data.title)) {
      errors.value['title'] = '请输入区块标题'
      isValid = false
    }

    // 校验内容项
    const items = data.items as Array<{ name: string }> | undefined
    if (Array.isArray(items)) {
      for (let i = 0; i < items.length; i++) {
        if (isEmpty(items[i]?.name)) {
          errors.value[`items.${i}.name`] = '请输入名称'
          isValid = false
        }
      }
    }

    return isValid
  }

  /**
   * 判断值是否为空
   */
  function isEmpty(value: unknown): boolean {
    if (value === null || value === undefined) {
      return true
    }
    if (typeof value === 'string') {
      return value.trim() === ''
    }
    return false
  }

  /**
   * 检查字段是否有错误
   */
  function hasError(field: string): boolean {
    return field in errors.value
  }

  /**
   * 获取字段错误信息
   */
  function getFieldError(field: string): string {
    return errors.value[field] || ''
  }

  /**
   * 清空错误
   */
  function clearErrors(): void {
    errors.value = {}
  }

  return {
    errors,
    validate,
    hasError,
    getFieldError,
    clearErrors,
    ValidationKey
  }
}

/**
 * 用于子组件注入校验上下文的 Composable
 */
export function useValidationInject(): ValidationContext | undefined {
  return inject(ValidationKey)
}
