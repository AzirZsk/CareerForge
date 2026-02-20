# LandIt API 接口文档

> @author Azir
> 版本: 1.0.0
> 最后更新: 2026-02-19

---

## 一、概述

### 1.1 项目简介

LandIt 是一款面向求职者的智能辅助平台，通过 AI 技术帮助用户提升求职竞争力。

### 1.2 基础信息

- **Base URL**: `http://localhost:8080`
- **API前缀**: `/landit`
- **数据格式**: JSON
- **编码**: UTF-8

---

## 二、统一响应格式

### 2.1 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1708348800000
}
```

### 2.2 错误响应

```json
{
  "code": 400,
  "message": "参数错误",
  "errors": [
    {
      "field": "email",
      "message": "邮箱格式不正确"
    }
  ],
  "timestamp": 1708348800000
}
```

### 2.3 分页响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [],
    "total": 100,
    "page": 1,
    "size": 10,
    "pages": 10
  }
}
```

---

## 三、错误码说明

| 错误码 | 说明 |
|-------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 四、接口模块

### 4.1 用户模块 (/user)

#### 4.1.1 获取当前用户信息

**GET** `/user/profile`

**响应数据**:
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
  "skills": ["Vue.js", "React", "TypeScript"],
  "createdAt": "2024-01-15"
}
```

#### 4.1.2 更新用户信息

**PUT** `/user/profile`

**请求体**:
```json
{
  "name": "Azir",
  "email": "newemail@example.com",
  "phone": "13912345678",
  "targetPosition": "前端架构师",
  "targetSalary": "40-60K",
  "skills": ["Vue.js", "React", "TypeScript", "Node.js"]
}
```

#### 4.1.3 上传头像

**POST** `/user/avatar`

**Content-Type**: `multipart/form-data`

**请求体**: FormData (file字段)

**响应数据**:
```json
{
  "avatarUrl": "https://cdn.example.com/avatars/user_001.jpg"
}
```

---

### 4.2 简历模块 (/resumes)

#### 4.2.1 获取简历列表

**GET** `/resumes`

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| status | string | 否 | 'optimized' \| 'draft' |

**响应数据**:
```json
[
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
]
```

#### 4.2.2 获取简历详情

**GET** `/resumes/:id`

**响应数据**:
```json
{
  "id": "resume_001",
  "name": "前端工程师简历_v3",
  "targetPosition": "高级前端工程师",
  "sections": [
    {
      "id": "section_001",
      "type": "basic",
      "title": "基本信息",
      "content": {
        "name": "Azir",
        "phone": "138****8888",
        "email": "zhangshukun@example.com"
      },
      "score": 100,
      "suggestions": []
    }
  ],
  "overallScore": 92,
  "keywordMatch": 87,
  "formatScore": 95,
  "contentScore": 91
}
```

#### 4.2.3 上传简历文件

**POST** `/resumes/upload`

**Content-Type**: `multipart/form-data`

**请求体**: FormData (file字段，支持PDF/Word/图片)

**响应数据**:
```json
{
  "id": "resume_new",
  "name": "上传的简历",
  "targetPosition": "高级前端工程师",
  "parseStatus": "completed",
  "sections": []
}
```

#### 4.2.4 创建空白简历

**POST** `/resumes`

**请求体**:
```json
{
  "name": "我的新简历",
  "targetPosition": "前端工程师"
}
```

#### 4.2.5 更新简历

**PUT** `/resumes/:id`

**请求体**:
```json
{
  "name": "前端工程师简历_v4",
  "sections": [
    {
      "id": "section_001",
      "type": "basic",
      "content": {}
    }
  ]
}
```

#### 4.2.6 删除简历

**DELETE** `/resumes/:id`

#### 4.2.7 设置主简历

**PUT** `/resumes/:id/primary`

#### 4.2.8 获取优化建议

**GET** `/resumes/:id/suggestions`

**响应数据**:
```json
[
  {
    "id": "sug_001",
    "type": "critical",
    "category": "关键词",
    "title": "缺少高频关键词",
    "description": "目标岗位中\"性能优化\"出现频率较高",
    "impact": "高",
    "position": "项目经历 - 创作者平台"
  }
]
```

#### 4.2.9 应用优化建议

**POST** `/resumes/:id/suggestions/:suggestionId/apply`

#### 4.2.10 AI优化简历

**POST** `/resumes/:id/optimize`

**请求体**:
```json
{
  "targetPosition": "高级前端工程师"
}
```

**响应数据**:
```json
{
  "optimizedResume": {},
  "suggestions": []
}
```

#### 4.2.11 导出简历PDF

**GET** `/resumes/:id/export`

**响应**: `application/pdf` (文件流)

---

### 4.3 面试模块 (/interviews)

#### 4.3.1 开始面试会话

**POST** `/interviews/sessions`

**请求体**:
```json
{
  "type": "technical",
  "position": "高级前端工程师",
  "difficulty": "medium",
  "questionCount": 10
}
```

**响应数据**:
```json
{
  "sessionId": "session_001",
  "firstQuestion": {
    "id": "q_001",
    "question": "请解释 Vue 3 的响应式原理",
    "keyPoints": ["Proxy", "Reflect", "依赖收集"]
  }
}
```

#### 4.3.2 提交回答

**POST** `/interviews/sessions/:sessionId/answers`

**请求体**:
```json
{
  "questionId": "q_001",
  "answer": "Vue 3 使用 Proxy 实现响应式...",
  "timestamp": "2024-02-17 14:05:30"
}
```

**响应数据**:
```json
{
  "score": 90,
  "feedback": "回答结构清晰，重点突出",
  "nextQuestion": {
    "id": "q_002",
    "question": "如何优化单页应用性能？",
    "keyPoints": ["代码分割", "懒加载", "缓存"]
  },
  "isFollowUp": false,
  "finished": false
}
```

#### 4.3.3 请求提示

**GET** `/interviews/sessions/:sessionId/hints`

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| questionId | string | 是 | 问题ID |

**响应数据**:
```json
{
  "hint": "可以从Proxy的工作原理、依赖收集机制、触发更新的流程来回答",
  "keyPoints": ["Proxy", "Reflect", "依赖收集", "触发更新"]
}
```

#### 4.3.4 结束面试

**POST** `/interviews/sessions/:sessionId/finish`

**请求体**:
```json
{
  "duration": 45
}
```

**响应数据**:
```json
{
  "interviewId": "interview_001",
  "reviewId": "review_001",
  "overallScore": 85
}
```

#### 4.3.5 获取面试历史

**GET** `/interviews/history`

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| type | string | 否 | 'technical' \| 'behavioral' |
| page | number | 否 | 页码，默认1 |
| size | number | 否 | 每页数量，默认10 |

**响应数据**:
```json
{
  "list": [
    {
      "id": "interview_001",
      "type": "technical",
      "position": "高级前端工程师",
      "company": "字节跳动",
      "date": "2024-02-17",
      "duration": 45,
      "score": 85,
      "status": "completed",
      "questions": 12,
      "correctAnswers": 10
    }
  ],
  "total": 28,
  "page": 1,
  "size": 10,
  "pages": 3
}
```

#### 4.3.6 获取面试详情

**GET** `/interviews/:id`

**响应数据**:
```json
{
  "id": "interview_001",
  "type": "technical",
  "position": "高级前端工程师",
  "company": "字节跳动",
  "date": "2024-02-17",
  "duration": 45,
  "score": 85,
  "conversation": [
    {
      "id": "c_001",
      "role": "interviewer",
      "content": "请自我介绍",
      "timestamp": "2024-02-17 14:00:00"
    },
    {
      "id": "c_002",
      "role": "candidate",
      "content": "我叫Azir...",
      "timestamp": "2024-02-17 14:00:30",
      "score": 90,
      "feedback": "回答结构清晰"
    }
  ],
  "analysis": {
    "strengths": ["表达清晰", "项目经验丰富"],
    "weaknesses": ["部分细节不够深入"],
    "overallFeedback": "整体表现良好"
  }
}
```

#### 4.3.7 获取题库

**GET** `/interviews/questions`

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| type | string | 否 | 'technical' \| 'behavioral' |

**响应数据**:
```json
{
  "technical": [
    {
      "id": "q_001",
      "category": "Vue.js",
      "difficulty": "medium",
      "question": "请解释 Vue 3 的响应式原理",
      "followUp": "Proxy 相比 Object.defineProperty 有哪些优势？",
      "keyPoints": ["Proxy", "Reflect", "依赖收集"],
      "sampleAnswer": "Vue 3 使用 Proxy 替代..."
    }
  ]
}
```

---

### 4.4 复盘模块 (/reviews)

#### 4.4.1 获取复盘列表

**GET** `/reviews`

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| page | number | 否 | 页码，默认1 |
| size | number | 否 | 每页数量，默认10 |

**响应数据**:
```json
{
  "list": [
    {
      "id": "review_001",
      "interviewId": "interview_001",
      "overallScore": 85,
      "date": "2024-02-17"
    }
  ],
  "total": 28,
  "page": 1,
  "size": 10
}
```

#### 4.4.2 获取复盘详情

**GET** `/reviews/:id`

**响应数据**:
```json
{
  "id": "review_001",
  "interviewId": "interview_001",
  "overallScore": 85,
  "analysis": {
    "strengths": ["表达清晰", "项目经验丰富"],
    "weaknesses": ["部分细节不够深入"],
    "overallFeedback": "整体表现良好"
  },
  "dimensions": [
    {
      "name": "技术深度",
      "score": 88,
      "maxScore": 100,
      "feedback": "对核心概念理解透彻"
    }
  ],
  "questionAnalysis": [
    {
      "question": "请解释 Vue 3 的响应式原理",
      "yourAnswer": "Vue 3 使用 Proxy...",
      "score": 90,
      "keyPointsCovered": ["Proxy", "依赖收集"],
      "keyPointsMissed": ["Reflect API的作用"],
      "suggestion": "建议补充 Reflect 与 Proxy 配合使用的原理"
    }
  ],
  "improvementPlan": [
    {
      "category": "技术深化",
      "items": ["深入学习 Vue 3 源码", "了解微前端沙箱原理"]
    }
  ]
}
```

#### 4.4.3 导出复盘报告

**GET** `/reviews/:id/export`

**响应**: `application/pdf` (文件流)

---

### 4.5 统计模块 (/statistics)

#### 4.5.1 获取统计数据

**GET** `/statistics`

**响应数据**:
```json
{
  "overview": {
    "totalInterviews": 28,
    "averageScore": 84,
    "improvementRate": 23,
    "studyHours": 45
  },
  "weeklyProgress": [
    {
      "week": "W1",
      "score": 72,
      "interviews": 3
    }
  ],
  "skillRadar": [
    {
      "skill": "Vue.js",
      "score": 92
    }
  ],
  "recentActivity": [
    {
      "type": "interview",
      "content": "完成字节跳动技术面试",
      "time": "2小时前",
      "score": 85
    }
  ]
}
```

---

### 4.6 职位推荐模块 (/jobs)

#### 4.6.1 获取推荐职位

**GET** `/jobs/recommendations`

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| page | number | 否 | 页码，默认1 |
| size | number | 否 | 每页数量，默认10 |

**响应数据**:
```json
{
  "list": [
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
      "description": "负责抖音创作者平台..."
    }
  ],
  "total": 50,
  "page": 1,
  "size": 10
}
```

---

## 五、枚举值定义

### ResumeStatus（简历状态）
| 值 | 说明 |
|---|------|
| optimized | 已优化 |
| draft | 草稿 |

### SuggestionType（建议类型）
| 值 | 说明 |
|---|------|
| critical | 关键问题 |
| improvement | 改进建议 |
| enhancement | 增强建议 |

### InterviewType（面试类型）
| 值 | 说明 |
|---|------|
| technical | 技术面试 |
| behavioral | 行为面试 |

### InterviewStatus（面试状态）
| 值 | 说明 |
|---|------|
| completed | 已完成 |
| in_progress | 进行中 |

### QuestionDifficulty（题目难度）
| 值 | 说明 |
|---|------|
| easy | 简单 |
| medium | 中等 |
| hard | 困难 |

### ConversationRole（对话角色）
| 值 | 说明 |
|---|------|
| interviewer | 面试官 |
| candidate | 候选人 |

### ActivityType（活动类型）
| 值 | 说明 |
|---|------|
| interview | 面试 |
| resume | 简历 |
| practice | 练习 |
| review | 复盘 |

---

*文档生成时间: 2026-02-19*
