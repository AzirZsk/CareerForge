import { defineConfig } from 'vitepress'

export default defineConfig({
  lang: 'zh-CN',
  title: 'CareerForge',
  description: 'AI 驱动的全流程智能求职助手',

  base: '/Landit/',
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

    nav: [
      { text: '首页', link: '/' },
      { text: '功能', link: '/features' },
      { text: '部署', link: '/deployment' },
      { text: 'GitHub', link: 'https://github.com/AzirZsk/Landit' },
    ],

    sidebar: {
      '/features': [
        {
          text: '功能详情',
          items: [
            { text: '智能简历管理', link: '/features#resume' },
            { text: 'AI 对话式优化', link: '/features#ai-chat' },
            { text: 'AI 语音模拟面试', link: '/features#voice' },
            { text: '面试中心', link: '/features#interview-center' },
            { text: '简历优化工作流', link: '/features#optimize' },
            { text: '简历定制工作流', link: '/features#tailor' },
          ]
        }
      ],
      '/deployment': [
        {
          text: '部署指南',
          items: [
            { text: 'Docker 部署', link: '/deployment#docker' },
            { text: '环境变量', link: '/deployment#env' },
            { text: '传统部署', link: '/deployment#manual' },
            { text: 'Nginx 配置', link: '/deployment#nginx' },
            { text: '故障排除', link: '/deployment#troubleshooting' },
          ]
        }
      ],
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/AzirZsk/Landit' },
    ],

    footer: {
      message: '基于 AGPL-3.0 许可证开源',
      copyright: 'Copyright 2024-present CareerForge'
    },
  },

  ignoreDeadLinks: true,
})
