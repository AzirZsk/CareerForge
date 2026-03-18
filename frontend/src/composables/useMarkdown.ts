// =====================================================
// Markdown 渲染 Composable
// @author Azir
// =====================================================

import { marked } from 'marked'
import { computed } from 'vue'

// 配置 marked 选项
marked.setOptions({
  breaks: true,      // 支持 GFM 换行
  gfm: true          // 启用 GitHub Flavored Markdown
})

/**
 * Markdown 渲染 Composable
 * 提供将 markdown 文本转换为 HTML 的能力
 */
export function useMarkdown() {
  /**
   * 将 markdown 文本转换为 HTML
   * @param text markdown 文本
   * @returns HTML 字符串
   */
  function renderMarkdown(text: string | undefined | null): string {
    if (!text) return ''

    try {
      return marked.parse(text, { async: false }) as string
    } catch (error) {
      console.error('Markdown 解析失败:', error)
      return text
    }
  }

  /**
   * 创建响应式的 markdown 渲染
   * @param markdownTextGetter 获取 markdown 文本的函数或 ref
   */
  function useRenderedMarkdown(markdownTextGetter: () => string | undefined | null) {
    return computed(() => renderMarkdown(markdownTextGetter()))
  }

  /**
   * 渲染行内 markdown（不包含段落标签）
   * @param text markdown 文本
   */
  function renderInlineMarkdown(text: string | undefined | null): string {
    if (!text) return ''

    try {
      return marked.parseInline(text, { async: false }) as string
    } catch (error) {
      console.error('Markdown 内联解析失败:', error)
      return text
    }
  }

  return {
    renderMarkdown,
    useRenderedMarkdown,
    renderInlineMarkdown
  }
}
