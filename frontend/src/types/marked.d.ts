declare module 'marked' {
  interface MarkedOptions {
    async?: boolean
    breaks?: boolean
    gfm?: boolean
    silent?: boolean
    pedantic?: boolean
  }

  // marked 是一个函数，同时也有 parse 和 parseInline 方法
  interface MarkedFunction {
    (src: string, options?: MarkedOptions): string
    parse: (src: string, options?: MarkedOptions) => string
    parseInline: (src: string, options?: MarkedOptions) => string
    setOptions: (options: MarkedOptions) => void
  }

  const marked: MarkedFunction
  export { marked }
}
