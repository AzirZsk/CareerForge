# LandIt 数据模型文档

> @author Azir
> 版本: 1.0.0
> 最后更新: 2026-02-19

---

## 一、概述

本文档定义了 LandIt 智能求职助手项目中使用的所有数据模型，与前端 TypeScript 类型定义保持一致。

---

## 二、枚举类型

### 2.1 ResumeStatus（简历状态）

| 值 | 说明 |
|---|------|
| `optimized` | 已优化 |
| `draft` | 草稿 |

### 2.2 SuggestionType（建议类型）

| 值 | 说明 |
|---|------|
| `critical` | 关键问题 - 必须修复的问题 |
| `improvement` | 改进建议 - 建议优化的内容 |
| `enhancement` | 增强建议 - 可选的增强项 |

### 2.3 InterviewType（面试类型）

| 值 | 说明 |
|---|------|
| `technical` | 技术面试 |
| `behavioral` | 行为面试 |

### 2.4 InterviewStatus（面试状态）

| 值 | 说明 |
|---|------|
| `completed` | 已完成 |
| `in_progress` | 进行中 |

### 2.5 QuestionDifficulty（题目难度）

| 值 | 说明 |
|---|------|
| `easy` | 简单 |
| `medium` | 中等 |
| `hard` | 困难 |

### 2.6 ConversationRole（对话角色）

| 值 | 说明 |
|---|------|
| `interviewer` | 面试官 |
| `candidate` | 候选人 |

### 2.7 ActivityType（活动类型）

| 值 | 说明 |
|---|------|
| `interview` | 面试活动 |
| `resume` | 简历活动 |
| `practice` | 练习活动 |
| `review` | 复盘活动 |

---

## 三、用户相关模型

### 3.1 User（用户信息）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| id | string | 是 | 用户唯一标识 |
| name | string | 是 | 用户姓名 |
| avatar | string \| null | 否 | 头像URL |
| email | string | 是 | 邮箱地址 |
| phone | string | 是 | 手机号码（脱敏显示） |
| targetPosition | string | 是 | 目标岗位 |
| targetSalary | string | 是 | 期望薪资 |
| experience | string | 是 | 工作经验 |
| education | string | 是 | 学历信息 |
| skills | string[] | 是 | 技能标签列表 |
| createdAt | string | 是 | 创建时间 |

**示例**:
```json
{
  "id": "user_001",
  "name": "Azir",
  "avatar": null,
  "email": "zhangshukun@example.com",
  "phone": "138****8888",
  "targetPosition": "高级前端工程师",
  "targetSalary": "30-45K",
  "experience": "5年",
  "education": "本科 · 计算机科学与技术",
  "skills": ["Vue.js", "React", "TypeScript", "Node.js"],
  "createdAt": "2024-01-15"
}
```

### 3.2 UserUpdateInfo（用户更新信息）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| name | string | 否 | 用户姓名 |
| email | string | 否 | 邮箱地址 |
| phone | string | 否 | 手机号码 |
| experience | string | 否 | 工作经验 |
| education | string | 否 | 学历信息 |
| targetPosition | string | 否 | 目标岗位 |
| targetSalary | string | 否 | 期望薪资 |
| skills | string[] | 否 | 技能标签列表 |

---

## 四、简历相关模型

### 4.1 Resume（简历列表项）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| id | string | 是 | 简历唯一标识 |
| name | string | 是 | 简历名称 |
| targetPosition | string | 是 | 目标岗位 |
| updatedAt | string | 是 | 更新时间 |
| status | ResumeStatus | 是 | 简历状态 |
| score | number | 是 | 综合评分（0-100） |
| completeness | number | 是 | 完整度（0-100） |
| isPrimary | boolean | 是 | 是否为主简历 |

**示例**:
```json
{
  "id": "resume_001",
  "name": "前端工程师简历_v3",
  "targetPosition": "高级前端工程师",
  "updatedAt": "2024-02-18 14:30",
  "status": "optimized",
  "score": 92,
  "completeness": 95,
  "isPrimary": true
}
```

### 4.2 ResumeDetail（简历详情）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| id | string | 是 | 简历唯一标识 |
| name | string | 是 | 简历名称 |
| targetPosition | string | 是 | 目标岗位 |
| sections | ResumeSection[] | 是 | 简历模块列表 |
| overallScore | number | 是 | 综合评分 |
| keywordMatch | number | 是 | 关键词匹配度 |
| formatScore | number | 是 | 格式评分 |
| contentScore | number | 是 | 内容评分 |

### 4.3 ResumeSection（简历模块）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| id | string | 是 | 模块唯一标识 |
| type | string | 是 | 模块类型（basic/experience/project/skill等） |
| title | string | 是 | 模块标题 |
| content | ResumeSectionContent | 是 | 模块内容 |
| score | number | 是 | 模块评分 |
| suggestions | ResumeSuggestionItem[] | 是 | 模块建议列表 |

### 4.4 ResumeSectionContent（模块内容类型）

模块内容根据 type 不同，数据结构不同：

| type | content类型 | 说明 |
|------|------------|------|
| basic | Record<string, unknown> | 基本信息对象 |
| experience | WorkExperience[] | 工作经历数组 |
| project | ProjectExperience[] | 项目经历数组 |
| skill | string[] | 技能列表 |

### 4.5 WorkExperience（工作经历）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| company | string | 是 | 公司名称 |
| position | string | 是 | 职位名称 |
| period | string | 是 | 工作时间段 |
| description | string | 是 | 工作描述 |

**示例**:
```json
{
  "company": "字节跳动",
  "position": "高级前端工程师",
  "period": "2022.03 - 至今",
  "description": "负责抖音创作者平台的前端架构设计与核心功能开发"
}
```

### 4.6 ProjectExperience（项目经历）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| name | string | 是 | 项目名称 |
| role | string | 是 | 担任角色 |
| period | string | 是 | 项目周期 |
| description | string | 是 | 项目描述 |
| achievements | string[] | 是 | 项目成果列表 |

**示例**:
```json
{
  "name": "创作者数据分析平台",
  "role": "前端负责人",
  "period": "2023.01 - 2023.08",
  "description": "从0到1搭建创作者数据分析平台",
  "achievements": ["页面加载速度提升60%", "日活用户突破50万"]
}
```

### 4.7 ResumeSuggestionItem（简历建议项）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| type | SuggestionType | 是 | 建议类型 |
| content | string | 是 | 建议内容 |

### 4.8 ResumeSuggestion（简历优化建议）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| id | string | 是 | 建议唯一标识 |
| type | SuggestionType | 是 | 建议类型 |
| category | string | 是 | 建议分类 |
| title | string | 是 | 建议标题 |
| description | string | 是 | 详细描述 |
| impact | string | 是 | 影响程度（高/中/低） |
| position | string | 是 | 建议位置 |

**示例**:
```json
{
  "id": "sug_001",
  "type": "critical",
  "category": "关键词",
  "title": "缺少高频关键词",
  "description": "目标岗位中\"性能优化\"出现频率较高",
  "impact": "高",
  "position": "项目经历 - 创作者平台"
}
```

---

## 五、面试相关模型

### 5.1 Interview（面试记录）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| id | string | 是 | 面试唯一标识 |
| type | InterviewType | 是 | 面试类型 |
| position | string | 是 | 面试岗位 |
| company | string | 是 | 面试公司 |
| date | string | 是 | 面试日期 |
| duration | number | 是 | 面试时长（分钟） |
| score | number | 是 | 面试得分 |
| status | InterviewStatus | 是 | 面试状态 |
| questions | number | 是 | 问题总数 |
| correctAnswers | number | 是 | 正确回答数 |

### 5.2 InterviewQuestion（面试题目）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| id | string | 是 | 题目唯一标识 |
| category | string | 是 | 题目分类 |
| difficulty | QuestionDifficulty | 是 | 题目难度 |
| question | string | 是 | 题目内容 |
| followUp | string | 是 | 追问内容 |
| keyPoints | string[] | 是 | 关键要点 |
| sampleAnswer | string | 是 | 参考答案 |

**示例**:
```json
{
  "id": "q_001",
  "category": "Vue.js",
  "difficulty": "medium",
  "question": "请解释 Vue 3 的响应式原理",
  "followUp": "Proxy 相比 Object.defineProperty 有哪些优势？",
  "keyPoints": ["Proxy", "Reflect", "依赖收集", "触发更新"],
  "sampleAnswer": "Vue 3 使用 Proxy 替代..."
}
```

### 5.3 InterviewQuestions（题库分类）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| technical | InterviewQuestion[] | 是 | 技术面试题列表 |
| behavioral | InterviewQuestion[] | 是 | 行为面试题列表 |

### 5.4 Conversation（面试对话）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| id | string | 是 | 对话唯一标识 |
| role | ConversationRole | 是 | 对话角色 |
| content | string | 是 | 对话内容 |
| timestamp | string | 是 | 时间戳 |
| score | number | 否 | 评分（仅候选人回答） |
| feedback | string | 否 | 反馈（仅候选人回答） |

### 5.5 InterviewAnalysis（面试分析）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| strengths | string[] | 是 | 优势列表 |
| weaknesses | string[] | 是 | 不足列表 |
| overallFeedback | string | 是 | 整体反馈 |

### 5.6 InterviewDetail（面试详情）

继承 Interview 所有字段，额外包含：

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| conversation | Conversation[] | 是 | 对话记录列表 |
| analysis | InterviewAnalysis | 是 | 面试分析 |

### 5.7 InterviewSettings（面试设置）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| position | string | 是 | 目标岗位 |
| difficulty | QuestionDifficulty | 是 | 题目难度 |
| questionCount | number | 是 | 题目数量 |

### 5.8 SessionQuestion（会话问题）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| id | number | 是 | 问题ID |
| category | string | 是 | 问题分类 |
| question | string | 是 | 问题内容 |
| keyPoints | string[] | 是 | 关键要点 |

### 5.9 SessionMessage（会话消息）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| id | number | 是 | 消息ID |
| role | ConversationRole | 是 | 消息角色 |
| content | string | 是 | 消息内容 |
| timestamp | Date | 是 | 时间戳 |
| score | number | 否 | 评分 |
| feedback | string | 否 | 反馈 |

---

## 六、复盘相关模型

### 6.1 InterviewReview（面试复盘）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| id | string | 是 | 复盘唯一标识 |
| interviewId | string | 是 | 关联的面试ID |
| overallScore | number | 是 | 总体评分 |
| analysis | InterviewAnalysis | 是 | 面试分析 |
| dimensions | ReviewDimension[] | 是 | 维度分析列表 |
| questionAnalysis | QuestionAnalysis[] | 是 | 问题分析列表 |
| improvementPlan | ImprovementPlan[] | 是 | 改进计划列表 |

### 6.2 ReviewDimension（复盘维度）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| name | string | 是 | 维度名称 |
| score | number | 是 | 得分 |
| maxScore | number | 是 | 满分 |
| feedback | string | 是 | 反馈说明 |

**维度类型**:
- 技术深度
- 表达能力
- 项目经验
- 问题解决
- 应变能力

**示例**:
```json
{
  "name": "技术深度",
  "score": 88,
  "maxScore": 100,
  "feedback": "对核心概念理解透彻，能够举一反三"
}
```

### 6.3 QuestionAnalysis（问题分析）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| question | string | 是 | 问题内容 |
| yourAnswer | string | 是 | 你的回答 |
| score | number | 是 | 得分 |
| keyPointsCovered | string[] | 是 | 已覆盖要点 |
| keyPointsMissed | string[] | 是 | 遗漏要点 |
| suggestion | string | 是 | 改进建议 |

**示例**:
```json
{
  "question": "请解释 Vue 3 的响应式原理",
  "yourAnswer": "Vue 3 使用 Proxy 实现响应式...",
  "score": 90,
  "keyPointsCovered": ["Proxy", "依赖收集"],
  "keyPointsMissed": ["Reflect API的作用"],
  "suggestion": "建议补充 Reflect 与 Proxy 配合使用的原理"
}
```

### 6.4 ImprovementPlan（改进计划）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| category | string | 是 | 计划分类 |
| items | string[] | 是 | 改进项列表 |

**示例**:
```json
{
  "category": "技术深化",
  "items": ["深入学习 Vue 3 源码", "了解微前端沙箱原理"]
}
```

---

## 七、统计相关模型

### 7.1 Statistics（统计数据）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| overview | StatisticsOverview | 是 | 概览数据 |
| weeklyProgress | WeeklyProgress[] | 是 | 周进度数据 |
| skillRadar | SkillRadar[] | 是 | 技能雷达数据 |
| recentActivity | RecentActivity[] | 是 | 最近活动数据 |

### 7.2 StatisticsOverview（统计概览）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| totalInterviews | number | 是 | 总面试次数 |
| averageScore | number | 是 | 平均分数 |
| improvementRate | number | 是 | 提升率（百分比） |
| studyHours | number | 是 | 学习时长（小时） |

### 7.3 WeeklyProgress（周进度）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| week | string | 是 | 周标识（W1, W2...） |
| score | number | 是 | 本周得分 |
| interviews | number | 是 | 本周面试次数 |

**示例**:
```json
{
  "week": "W1",
  "score": 72,
  "interviews": 3
}
```

### 7.4 SkillRadar（技能雷达）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| skill | string | 是 | 技能名称 |
| score | number | 是 | 技能得分 |

**示例**:
```json
{
  "skill": "Vue.js",
  "score": 92
}
```

### 7.5 RecentActivity（最近活动）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| type | ActivityType | 是 | 活动类型 |
| content | string | 是 | 活动内容 |
| time | string | 是 | 相对时间 |
| score | number \| null | 否 | 关联分数 |

**示例**:
```json
{
  "type": "interview",
  "content": "完成字节跳动技术面试",
  "time": "2小时前",
  "score": 85
}
```

---

## 八、职位相关模型

### 8.1 Job（职位推荐）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| id | string | 是 | 职位唯一标识 |
| company | string | 是 | 公司名称 |
| companyLogo | string \| null | 否 | 公司Logo URL |
| position | string | 是 | 职位名称 |
| salary | string | 是 | 薪资范围 |
| location | string | 是 | 工作地点 |
| experience | string | 是 | 经验要求 |
| education | string | 是 | 学历要求 |
| tags | string[] | 是 | 技能标签 |
| matchScore | number | 是 | 匹配度评分 |
| publishedAt | string | 是 | 发布时间 |
| description | string | 是 | 职位描述 |

**示例**:
```json
{
  "id": "job_001",
  "company": "字节跳动",
  "companyLogo": null,
  "position": "高级前端工程师",
  "salary": "35-60K",
  "location": "北京",
  "experience": "3-5年",
  "education": "本科",
  "tags": ["Vue", "React", "性能优化"],
  "matchScore": 95,
  "publishedAt": "1天前",
  "description": "负责抖音创作者平台核心功能开发..."
}
```

---

## 九、通用模型

### 9.1 NavItem（导航项）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| key | string | 是 | 导航键 |
| label | string | 是 | 显示标签 |
| path | string | 是 | 路由路径 |
| icon | string | 是 | 图标名称 |
| badge | string | 否 | 徽章内容 |

---

## 十、数据关系图

```
User
  │
  ├── Resume (1:N)
  │     ├── ResumeSection (1:N)
  │     │     └── ResumeSuggestionItem (1:N)
  │     └── ResumeSuggestion (1:N)
  │
  ├── Interview (1:N)
  │     ├── Conversation (1:N)
  │     └── InterviewReview (1:1)
  │           ├── ReviewDimension (1:N)
  │           ├── QuestionAnalysis (1:N)
  │           └── ImprovementPlan (1:N)
  │
  ├── Statistics (1:1)
  │     ├── WeeklyProgress (1:N)
  │     ├── SkillRadar (1:N)
  │     └── RecentActivity (1:N)
  │
  └── Job (推荐列表)
```

---

*文档生成时间: 2026-02-19*
