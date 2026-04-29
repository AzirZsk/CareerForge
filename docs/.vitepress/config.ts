import { defineConfig } from 'vitepress'

export default defineConfig({
  lang: 'zh-CN',
  title: 'CareerForge',
  description: 'AI 驱动的全流程智能求职助手',

  base: '/CareerForge/',
  appearance: 'force-dark',
  cleanUrls: false,

  head: [
    ['link', { rel: 'icon', href: '/favicon.svg', type: 'image/svg+xml' }],
    ['link', { rel: 'preconnect', href: 'https://fonts.googleapis.com' }],
    ['link', { rel: 'preconnect', href: 'https://fonts.gstatic.com', crossorigin: '' }],
    ['link', {
      href: 'https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Crimson+Pro:wght@400;500;600;700&display=swap',
      rel: 'stylesheet'
    }],
    ['meta', { name: 'theme-color', content: '#0a0a0b' }],
  ],

  themeConfig: {
    logo: '/logo.svg',

    search: {
      provider: 'local'
    },

    nav: [
      { text: '首页', link: '/' },
      { text: '文档', link: '/guide/introduction' },
      { text: 'GitHub', link: 'https://github.com/AzirZsk/CareerForge' },
      { text: '立即试用', link: 'https://careerforge.cc/' },
    ],

    sidebar: {
      '/guide/': [
        {
          text: '功能',
          items: [
            { text: '项目介绍', link: '/guide/introduction' },
            { text: '简历管理', link: '/guide/resume' },
            { text: '面试中心', link: '/guide/interview' },
          ]
        },
        {
          text: '部署指南',
          items: [
            { text: 'AI 辅助部署（推荐）', link: '/guide/claude-code' },
            { text: 'Docker 部署', link: '/guide/docker' },
            { text: '传统部署', link: '/guide/manual' },
          ]
        }
      ],
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/AzirZsk/CareerForge' },
    ],

    footer: {
      message: '基于 AGPL-3.0 许可证开源',
      copyright: 'Copyright 2024-present CareerForge'
    },
  },

  vite: {},

  ignoreDeadLinks: true,
})
