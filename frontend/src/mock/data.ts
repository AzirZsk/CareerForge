// =====================================================
// LandIt Mock 数据
// @author Azir
// 模拟真实业务数据用于前端展示
// =====================================================

import type {
  User,
  Resume,
  ResumeDetail,
  ResumeSuggestion,
  Interview,
  InterviewQuestions,
  InterviewDetail,
  InterviewReview,
  Statistics,
  Job
} from '@/types'

// 当前用户信息
export const currentUser: User = {
  id: 'user_001',
  name: 'Azir',
  gender: 'MALE',
  avatar: null,
  createdAt: '2024-01-15'
}

// 简历列表
export const resumes: Resume[] = [
  {
    id: 'resume_001',
    name: '前端工程师简历_v3',
    targetPosition: '高级前端工程师',
    updatedAt: '2024-02-18 14:30',
    status: 'OPTIMIZED',
    score: 92,
    completeness: 95,
    isPrimary: true
  },
  {
    id: 'resume_002',
    name: '全栈开发简历',
    targetPosition: '全栈开发工程师',
    updatedAt: '2024-02-15 09:20',
    status: 'DRAFT',
    score: 78,
    completeness: 72,
    isPrimary: false
  },
  {
    id: 'resume_003',
    name: '技术经理岗位简历',
    targetPosition: '技术经理',
    updatedAt: '2024-02-10 16:45',
    status: 'OPTIMIZED',
    score: 88,
    completeness: 90,
    isPrimary: false
  }
]

// 简历详情
export const resumeDetail: ResumeDetail = {
  id: 'resume_001',
  name: '前端工程师简历_v3',
  targetPosition: '高级前端工程师',
  sections: [
    {
      id: 'section_001',
      type: 'basic',
      title: '基本信息',
      content: {
        name: 'Azir',
        phone: '138****8888',
        email: 'zhangshukun@example.com',
        location: '北京市朝阳区',
        age: 28,
        workYears: 5
      },
      score: 100,
      suggestions: []
    },
    {
      id: 'section_002',
      type: 'experience',
      title: '工作经历',
      content: [
        {
          company: '字节跳动',
          position: '高级前端工程师',
          period: '2022.03 - 至今',
          description: '负责抖音创作者平台的前端架构设计与核心功能开发，主导了微前端架构的落地，性能优化提升40%。'
        },
        {
          company: '美团',
          position: '前端工程师',
          period: '2019.07 - 2022.02',
          description: '参与美团外卖商家端管理系统开发，负责订单管理、数据看板等核心模块。'
        }
      ],
      score: 95,
      suggestions: [
        {
          type: 'improvement',
          content: '建议在字节跳动经历中增加具体的项目成果数据，如：DAU提升、用户满意度等量化指标'
        }
      ]
    },
    {
      id: 'section_003',
      type: 'project',
      title: '项目经历',
      content: [
        {
          name: '创作者数据分析平台',
          role: '前端负责人',
          period: '2023.01 - 2023.08',
          description: '从0到1搭建创作者数据分析平台，支持百万级数据实时可视化展示。',
          achievements: ['页面加载速度提升60%', '日活用户突破50万', '获得团队Q3最佳项目奖']
        }
      ],
      score: 88,
      suggestions: [
        {
          type: 'enhancement',
          content: '可以补充技术栈详情，突出技术选型的合理性'
        }
      ]
    },
    {
      id: 'section_004',
      type: 'skill',
      title: '专业技能',
      content: ['精通 Vue.js / React 框架，熟悉源码实现', '熟练使用 TypeScript 进行大型项目开发', '具有丰富的性能优化和工程化经验', '熟悉 Node.js，能够开发服务端应用'],
      score: 85,
      suggestions: [
        {
          type: 'enhancement',
          content: '建议按技能熟练度分级展示，并补充具体应用场景'
        }
      ]
    }
  ],
  overallScore: 92,
  formatScore: 95,
  contentScore: 91,
  analyzed: true
}

// 简历优化建议
export const resumeSuggestions: ResumeSuggestion[] = [
  {
    id: 'sug_001',
    type: 'critical',
    category: '关键词',
    title: '缺少高频关键词',
    description: '目标岗位中"性能优化"出现频率较高，建议在项目经历中突出相关经验',
    impact: '高',
    position: '项目经历 - 创作者平台'
  },
  {
    id: 'sug_002',
    type: 'improvement',
    category: '量化',
    title: '增加数据支撑',
    description: '工作经历中的描述较为笼统，建议补充具体数据和成果',
    impact: '中',
    position: '工作经历 - 美团'
  },
  {
    id: 'sug_003',
    type: 'enhancement',
    category: '格式',
    title: '技能标签优化',
    description: '建议将技能按照熟练程度分类，并标注使用年限',
    impact: '低',
    position: '专业技能'
  }
]

// 面试历史
export const interviewHistory: Interview[] = [
  {
    id: 'interview_001',
    type: 'technical',
    position: '高级前端工程师',
    company: '字节跳动',
    date: '2024-02-17',
    duration: 45,
    score: 85,
    status: 'completed',
    questions: 12,
    correctAnswers: 10
  },
  {
    id: 'interview_002',
    type: 'behavioral',
    position: '前端技术专家',
    company: '阿里巴巴',
    date: '2024-02-15',
    duration: 30,
    score: 78,
    status: 'completed',
    questions: 8,
    correctAnswers: 6
  },
  {
    id: 'interview_003',
    type: 'technical',
    position: '高级前端工程师',
    company: '腾讯',
    date: '2024-02-12',
    duration: 50,
    score: 92,
    status: 'completed',
    questions: 15,
    correctAnswers: 14
  }
]

// 面试题目库
export const interviewQuestions: InterviewQuestions = {
  technical: [
    {
      id: 'q_001',
      category: 'Vue.js',
      difficulty: 'medium',
      question: '请解释 Vue 3 的响应式原理，与 Vue 2 有什么不同？',
      followUp: 'Proxy 相比 Object.defineProperty 有哪些优势？',
      keyPoints: ['Proxy', 'Reflect', '依赖收集', '触发更新', '性能提升'],
      sampleAnswer: 'Vue 3 使用 Proxy 替代了 Vue 2 的 Object.defineProperty 实现响应式...'
    },
    {
      id: 'q_002',
      category: '性能优化',
      difficulty: 'hard',
      question: '如何优化一个大型单页应用的首次加载性能？',
      followUp: '在实际项目中你应用过哪些优化策略？效果如何？',
      keyPoints: ['代码分割', '懒加载', '预加载', '缓存策略', 'CDN', 'SSR'],
      sampleAnswer: '可以从代码层面、网络层面、渲染层面三个维度进行优化...'
    },
    {
      id: 'q_003',
      category: 'JavaScript',
      difficulty: 'medium',
      question: '请解释事件循环（Event Loop）的机制',
      followUp: '宏任务和微任务的执行顺序是怎样的？',
      keyPoints: ['调用栈', '任务队列', '宏任务', '微任务', '执行顺序'],
      sampleAnswer: 'JavaScript 是单线程语言，事件循环是其处理异步任务的核心机制...'
    },
    {
      id: 'q_004',
      category: 'TypeScript',
      difficulty: 'hard',
      question: 'TypeScript 中的 infer 关键字有什么作用？请举例说明',
      followUp: '如何用 infer 实现一个提取 Promise 返回类型的工具类型？',
      keyPoints: ['条件类型', '类型推断', '递归类型', '工具类型'],
      sampleAnswer: 'infer 用于条件类型中推断类型，可以在 extends 子句中使用...'
    }
  ],
  behavioral: [
    {
      id: 'q_005',
      category: '项目经验',
      difficulty: 'easy',
      question: '请介绍一个你觉得最有成就感的项目',
      followUp: '在这个项目中你遇到的最大挑战是什么？如何解决的？',
      keyPoints: ['项目背景', '个人贡献', '遇到的困难', '解决方案', '成果量化'],
      sampleAnswer: '我最有成就感的项目是创作者数据分析平台...'
    },
    {
      id: 'q_006',
      category: '团队协作',
      difficulty: 'easy',
      question: '描述一次你与团队成员产生分歧的经历，你是如何处理的？',
      followUp: '从这次经历中你学到了什么？',
      keyPoints: ['冲突描述', '沟通方式', '解决方案', '结果反思'],
      sampleAnswer: '在技术选型时，团队成员对于是否使用微前端架构有不同意见...'
    }
  ]
}

// 面试详情（带回答记录）
export const interviewDetail: InterviewDetail = {
  id: 'interview_001',
  type: 'technical',
  position: '高级前端工程师',
  company: '字节跳动',
  date: '2024-02-17',
  duration: 45,
  score: 85,
  conversation: [
    {
      id: 'c_001',
      role: 'interviewer',
      content: '你好，我是字节跳动前端团队的面试官。今天我们来进行一场技术面试，主要考察前端基础和项目经验。准备好了吗？',
      timestamp: '2024-02-17 14:00:00'
    },
    {
      id: 'c_002',
      role: 'candidate',
      content: '准备好了，请开始吧！',
      timestamp: '2024-02-17 14:00:05'
    },
    {
      id: 'c_003',
      role: 'interviewer',
      content: '好的，首先请你自我介绍一下，重点说说你最近的项目经验。',
      timestamp: '2024-02-17 14:00:10'
    },
    {
      id: 'c_004',
      role: 'candidate',
      content: '我叫Azir，有5年前端开发经验。目前在字节跳动负责抖音创作者平台的前端架构...',
      timestamp: '2024-02-17 14:00:30',
      score: 90,
      feedback: '回答结构清晰，重点突出，表达了核心价值和贡献'
    },
    {
      id: 'c_005',
      role: 'interviewer',
      content: '听起来很有意思。你提到主导了微前端架构的落地，能详细说说你们是怎么做的吗？遇到了哪些挑战？',
      timestamp: '2024-02-17 14:02:00'
    },
    {
      id: 'c_006',
      role: 'candidate',
      content: '我们使用 qiankun 框架进行微前端改造。主要挑战包括：样式隔离、应用间通信、公共依赖处理...',
      timestamp: '2024-02-17 14:02:30',
      score: 85,
      feedback: '技术方案描述清楚，但可以更深入讲解具体实现细节'
    }
  ],
  analysis: {
    strengths: ['表达清晰有条理', '项目经验丰富', '技术深度较好'],
    weaknesses: ['部分细节不够深入', '可以多用数据支撑'],
    overallFeedback: '整体表现良好，展现了扎实的技术功底和项目经验。建议在技术细节上可以更深入，并注意用数据量化成果。'
  }
}

// 面试复盘数据
export const interviewReview: InterviewReview = {
  id: 'review_001',
  interviewId: 'interview_001',
  overallScore: 85,
  analysis: {
    strengths: ['表达清晰有条理', '项目经验丰富', '技术深度较好', '对核心概念理解透彻'],
    weaknesses: ['部分细节不够深入', '可以多用数据支撑', '追问时的应变能力可加强'],
    overallFeedback: '整体表现良好，展现了扎实的技术功底和项目经验。建议在技术细节上可以更深入，并注意用数据量化成果。'
  },
  dimensions: [
    {
      name: '技术深度',
      score: 88,
      maxScore: 100,
      feedback: '对核心概念理解透彻，能够举一反三'
    },
    {
      name: '表达能力',
      score: 90,
      maxScore: 100,
      feedback: '回答结构清晰，逻辑性强'
    },
    {
      name: '项目经验',
      score: 85,
      maxScore: 100,
      feedback: '项目经验丰富，但可以更多量化数据'
    },
    {
      name: '问题解决',
      score: 82,
      maxScore: 100,
      feedback: '思路正确，但解决方案可以更全面'
    },
    {
      name: '应变能力',
      score: 80,
      maxScore: 100,
      feedback: '对追问反应及时，但深度可以加强'
    }
  ],
  questionAnalysis: [
    {
      question: '请解释 Vue 3 的响应式原理',
      yourAnswer: 'Vue 3 使用 Proxy 实现响应式...',
      score: 90,
      keyPointsCovered: ['Proxy', '依赖收集', '触发更新'],
      keyPointsMissed: ['Reflect API的作用'],
      suggestion: '建议补充 Reflect 与 Proxy 配合使用的原理'
    },
    {
      question: '微前端架构的落地挑战',
      yourAnswer: '主要挑战包括样式隔离、应用间通信...',
      score: 85,
      keyPointsCovered: ['样式隔离', '应用通信', '公共依赖'],
      keyPointsMissed: ['沙箱隔离机制'],
      suggestion: '可以补充 JS 沙箱的实现原理'
    }
  ],
  improvementPlan: [
    {
      category: '技术深化',
      items: ['深入学习 Vue 3 源码', '了解微前端沙箱实现原理', '研究性能优化最佳实践']
    },
    {
      category: '表达优化',
      items: ['回答时多用 STAR 法则', '准备更多量化数据', '加强技术方案对比分析']
    }
  ]
}

// 统计数据
export const statistics: Statistics = {
  overview: {
    totalInterviews: 28,
    averageScore: 84,
    improvementRate: 23,
    studyHours: 45
  },
  weeklyProgress: [
    { week: 'W1', score: 72, interviews: 3 },
    { week: 'W2', score: 78, interviews: 5 },
    { week: 'W3', score: 82, interviews: 4 },
    { week: 'W4', score: 85, interviews: 6 },
    { week: 'W5', score: 88, interviews: 5 },
    { week: 'W6', score: 90, interviews: 5 }
  ],
  skillRadar: [
    { skill: 'Vue.js', score: 92 },
    { skill: 'React', score: 85 },
    { skill: 'TypeScript', score: 88 },
    { skill: '性能优化', score: 82 },
    { skill: '工程化', score: 86 },
    { skill: '算法', score: 75 }
  ],
  recentActivity: [
    { type: 'interview', content: '完成字节跳动技术面试', time: '2小时前', score: 85 },
    { type: 'resume', content: '简历优化评分提升至92分', time: '昨天', score: null },
    { type: 'practice', content: '完成Vue3专项练习', time: '2天前', score: 90 },
    { type: 'review', content: '查看面试复盘报告', time: '3天前', score: null }
  ]
}

// 岗位推荐
export const jobRecommendations: Job[] = [
  {
    id: 'job_001',
    company: '字节跳动',
    companyLogo: null,
    position: '高级前端工程师',
    salary: '35-60K',
    location: '北京',
    experience: '3-5年',
    education: '本科',
    tags: ['Vue', 'React', '性能优化'],
    matchScore: 95,
    publishedAt: '1天前',
    description: '负责抖音创作者平台核心功能开发，参与技术架构设计...'
  },
  {
    id: 'job_002',
    company: '阿里巴巴',
    companyLogo: null,
    position: '前端技术专家',
    salary: '40-70K',
    location: '杭州',
    experience: '5-10年',
    education: '本科',
    tags: ['架构', '团队管理', '大型项目'],
    matchScore: 88,
    publishedAt: '2天前',
    description: '负责淘宝商家端架构升级，带领团队攻克技术难题...'
  },
  {
    id: 'job_003',
    company: '腾讯',
    companyLogo: null,
    position: 'Web前端开发工程师',
    salary: '30-50K',
    location: '深圳',
    experience: '3-5年',
    education: '本科',
    tags: ['小程序', 'Node.js', 'TypeScript'],
    matchScore: 85,
    publishedAt: '3天前',
    description: '负责微信小程序生态建设，参与开发者工具研发...'
  }
]
