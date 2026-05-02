import type MarkdownIt from 'markdown-it'

export default function imageCaption(md: MarkdownIt) {
  const defaultRender = md.renderer.rules.image!

  md.renderer.rules.image = (tokens, idx, options, env, self) => {
    const src = tokens[idx].attrGet('src') || ''
    const filename = decodeURIComponent(src.split('/').pop()?.replace(/\.[^.]+$/, '') || '')

    if (!filename) return defaultRender(tokens, idx, options, env, self)

    const imgHtml = defaultRender(tokens, idx, options, env, self)
    return `<figure class="img-caption">${imgHtml}<figcaption>${filename}</figcaption></figure>`
  }
}
