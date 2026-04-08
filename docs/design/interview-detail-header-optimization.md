# 面试详情页头部优化方案 A

> 创建日期：2026-04-08
> 设计师：老王
> 状态：待评审

---

## 一、当前设计问题

### 1.1 信息层次混乱

```
┌─────────────────────────────────────────────────────────┐
│ ← 公司名称 / 职位名称 [轮次标签]    [模拟面试][编辑][删除] │
├─────────────────────────────────────────────────────────┤
│ 📅 面试时间：2024/04/08 14:00                          │
│ 💻 面试类型：线上面试  🔗 会议链接...                    │
│ 📊 状态：[下拉选择]  🎯 最终结果：[下拉选择]             │
│ 📄 关联简历：xxx                                       │
│ 📝 备注：xxx                                           │
└─────────────────────────────────────────────────────────┘
```

**问题分析：**

| 问题 | 说明 |
|------|------|
| 面试时间视觉权重不足 | 时间是用户最关心的信息，但视觉上和"备注"平级 |
| 状态信息分散 | 状态在下方卡片中，头部只显示公司/职位 |
| 缺少紧迫感提示 | 明天的面试和下个月的面试视觉上没区别 |
| 操作按钮优先级不清 | "模拟面试"和"删除"平级展示 |
| 删除按钮暴露 | 危险操作直接暴露，容易误触 |

---

## 二、优化方案 A：紧凑型头部

### 2.1 设计目标

1. **一眼看清全貌**：核心信息整合到头部，无需滚动
2. **制造紧迫感**：时间提示让用户感知优先级
3. **操作分级**：主操作突出，次操作收起

### 2.2 布局设计

```
┌─────────────────────────────────────────────────────────────────────┐
│  ←  字节跳动 / 高级前端工程师                          [● 进行中]   │
│                                                                      │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐    │
│  │ 📅 时间    │  │ 💻 类型    │  │ 🎯 轮次    │  │ ⏱️ 倒计时  │    │
│  │ 明天 14:00 │  │ 线上面试   │  │ 一面       │  │ 还有 18h   │    │
│  └────────────┘  └────────────┘  └────────────┘  └────────────┘    │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ 🔗 会议链接: https://zoom.us/j/123456       [复制] [入会]  │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│                              [🎯 开始模拟面试]  [⋯ 更多]            │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.3 设计说明

#### 头部第一行：标题 + 状态标签

```
┌─────────────────────────────────────────────────────────────────────┐
│  ←  字节跳动 / 高级前端工程师                          [● 进行中]   │
│      ↑                  ↑                              ↑           │
│   返回按钮           公司/职位                       状态标签        │
│                                                       (右对齐)       │
└─────────────────────────────────────────────────────────────────────┘
```

- **状态标签**：使用彩色圆点 + 文字，右对齐
- **颜色区分**：
  - 准备中：黄色 `#fbbf24`
  - 进行中：蓝色 `#60a5fa`
  - 已完成：绿色 `#34d399`
  - 已取消：灰色 `#71717a`

#### 头部第二行：四个信息块

```
┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐
│ 📅 时间    │  │ 💻 类型    │  │ 🎯 轮次    │  │ ⏱️ 倒计时  │
│ 明天 14:00 │  │ 线上面试   │  │ 一面       │  │ 还有 18h   │
│  ↑        │  │            │  │            │  │  ↑        │
│ 人类可读  │  │            │  │            │  │ 紧迫感提示 │
└────────────┘  └────────────┘  └────────────┘  └────────────┘
```

**时间显示规则：**

| 条件 | 显示文本 | 样式 |
|------|----------|------|
| 已过期 | "已过期" | 灰色 |
| < 1 小时 | "即将开始" + 脉冲动画 | 红色 + 动画 |
| < 24 小时 | "还有 X 小时" | 橙色 |
| 明天 | "明天 HH:mm" | 蓝色 |
| 3 天内 | "X 天后" | 蓝色 |
| 更远 | "MM-DD HH:mm" | 普通灰色 |

#### 头部第三行：会议链接（仅线上面试）

```
┌─────────────────────────────────────────────────────────────┐
│ 🔗 会议链接: https://zoom.us/j/123456       [复制] [入会]  │
│                                              ↑       ↑      │
│                                           复制链接  一键入会 │
└─────────────────────────────────────────────────────────────┘
```

- **背景色**：`rgba(96, 165, 250, 0.1)` 淡蓝色
- **一键入会按钮**：直接打开会议链接

#### 头部第四行：操作按钮

```
                              [🎯 开始模拟面试]  [⋯ 更多]
                                    ↑              ↑
                                主操作(CTA)    次操作(下拉)
```

**更多菜单内容：**
- 编辑面试
- 关联简历
- 删除面试（红色）

---

## 三、组件结构设计

### 3.1 文件结构

```
components/interview-center/
├── InterviewHeader.vue        # 新建：头部组件
├── InterviewInfoBlocks.vue    # 新建：信息块组件
├── MeetingLinkBar.vue         # 新建：会议链接组件
└── ...
```

### 3.2 InterviewHeader.vue 结构

```vue
<template>
  <header class="interview-header">
    <!-- 第一行：标题 + 状态 -->
    <div class="header-row title-row">
      <div class="header-left">
        <button class="back-btn">...</button>
        <div class="header-title">
          <span class="company-name">{{ interview.companyName }}</span>
          <span class="separator">/</span>
          <span class="position-name">{{ interview.position }}</span>
        </div>
      </div>
      <div class="header-right">
        <span class="status-badge" :class="interview.status">
          <span class="status-dot"></span>
          {{ statusLabel }}
        </span>
      </div>
    </div>

    <!-- 第二行：四个信息块 -->
    <div class="header-row info-row">
      <div class="info-block">
        <span class="info-icon">📅</span>
        <div class="info-content">
          <span class="info-label">时间</span>
          <span class="info-value">{{ formattedDate }}</span>
        </div>
      </div>
      <div class="info-block">
        <span class="info-icon">💻</span>
        <div class="info-content">
          <span class="info-label">类型</span>
          <span class="info-value">{{ interviewTypeLabel }}</span>
        </div>
      </div>
      <div class="info-block">
        <span class="info-icon">🎯</span>
        <div class="info-content">
          <span class="info-label">轮次</span>
          <span class="info-value">{{ roundLabel }}</span>
        </div>
      </div>
      <div class="info-block" :class="timeUrgency.class">
        <span class="info-icon">⏱️</span>
        <div class="info-content">
          <span class="info-label">倒计时</span>
          <span class="info-value">{{ timeUrgency.text }}</span>
        </div>
      </div>
    </div>

    <!-- 第三行：会议链接（条件渲染） -->
    <MeetingLinkBar
      v-if="interview.interviewType === 'online'"
      :link="interview.onlineLink"
      :password="interview.meetingPassword"
    />

    <!-- 第四行：操作按钮 -->
    <div class="header-row actions-row">
      <div class="actions-spacer"></div>
      <div class="actions-group">
        <button class="btn btn-primary btn-mock" @click="startMockInterview">
          <font-awesome-icon icon="fa-solid fa-microphone" />
          开始模拟面试
        </button>
        <div class="more-menu">
          <button class="btn btn-icon" @click="toggleMoreMenu">
            <font-awesome-icon icon="fa-solid fa-ellipsis-vertical" />
          </button>
          <div v-if="showMoreMenu" class="dropdown">
            <button @click="showEditDialog = true">编辑面试</button>
            <button @click="showResumeDialog = true">关联简历</button>
            <button class="danger" @click="handleDelete">删除面试</button>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>
```

---

## 四、样式设计

### 4.1 SCSS 变量

```scss
// 状态颜色
$status-preparing: #fbbf24;   // 黄色
$status-in-progress: #60a5fa; // 蓝色
$status-completed: #34d399;   // 绿色
$status-cancelled: #71717a;   // 灰色

// 紧迫感颜色
$urgency-critical: #f87171;   // 红色
$urgency-urgent: #fb923c;     // 橙色
$urgency-soon: #60a5fa;       // 蓝色
$urgency-normal: #71717a;     // 灰色
```

### 4.2 组件样式

```scss
.interview-header {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  margin-bottom: $spacing-xl;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;

  & + .header-row {
    margin-top: $spacing-md;
  }
}

// 状态标签
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
  padding: 6px 12px;
  border-radius: $radius-full;
  font-size: 0.875rem;
  font-weight: 500;

  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }

  &.preparing {
    background: rgba($status-preparing, 0.15);
    color: $status-preparing;
    .status-dot { background: $status-preparing; }
  }

  &.in_progress {
    background: rgba($status-in-progress, 0.15);
    color: $status-in-progress;
    .status-dot { background: $status-in-progress; }
  }

  &.completed {
    background: rgba($status-completed, 0.15);
    color: $status-completed;
    .status-dot { background: $status-completed; }
  }

  &.cancelled {
    background: rgba($status-cancelled, 0.15);
    color: $status-cancelled;
    .status-dot { background: $status-cancelled; }
  }
}

// 信息块
.info-row {
  display: flex;
  gap: $spacing-md;
}

.info-block {
  flex: 1;
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  background: $color-bg-tertiary;
  border-radius: $radius-md;

  .info-icon {
    font-size: 1.25rem;
  }

  .info-content {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .info-label {
    font-size: 0.75rem;
    color: $color-text-tertiary;
  }

  .info-value {
    font-size: 0.875rem;
    font-weight: 500;
    color: $color-text-primary;
  }

  // 紧迫感样式
  &.critical {
    background: rgba($urgency-critical, 0.15);
    .info-value { color: $urgency-critical; }
    animation: pulse 1s infinite;
  }

  &.urgent {
    background: rgba($urgency-urgent, 0.1);
    .info-value { color: $urgency-urgent; }
  }

  &.soon {
    background: rgba($urgency-soon, 0.1);
    .info-value { color: $urgency-soon; }
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

// 操作按钮
.actions-row {
  .actions-spacer {
    flex: 1;
  }

  .actions-group {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
  }
}

.btn-mock {
  background: linear-gradient(135deg, #4ade80 0%, #22c55e 100%);
  color: #000;
  font-weight: 600;
  padding: $spacing-sm $spacing-lg;
  border-radius: $radius-md;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(74, 222, 128, 0.3);
  }
}

// 更多菜单
.more-menu {
  position: relative;

  .dropdown {
    position: absolute;
    right: 0;
    top: 100%;
    margin-top: $spacing-xs;
    background: $color-bg-elevated;
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: $radius-md;
    padding: $spacing-xs;
    min-width: 120px;
    z-index: 100;

    button {
      display: block;
      width: 100%;
      padding: $spacing-sm $spacing-md;
      text-align: left;
      background: transparent;
      border: none;
      color: $color-text-secondary;
      border-radius: $radius-sm;
      cursor: pointer;

      &:hover {
        background: $color-bg-tertiary;
        color: $color-text-primary;
      }

      &.danger {
        color: $color-error;
        &:hover {
          background: rgba($color-error, 0.1);
        }
      }
    }
  }
}
```

---

## 五、交互设计

### 5.1 时间紧迫感

| 倒计时 | 视觉效果 | 交互建议 |
|--------|----------|----------|
| < 1 小时 | 红色 + 脉冲动画 | 浏览器通知提醒 |
| < 24 小时 | 橙色高亮 | - |
| 1-3 天 | 蓝色提示 | - |
| > 3 天 | 普通灰色 | - |
| 已过期 | 灰色 + 删除线 | 建议标记为已完成或取消 |

### 5.2 操作按钮

| 按钮 | 位置 | 说明 |
|------|------|------|
| 开始模拟面试 | 主按钮 | 绿色渐变，鼠标悬停有阴影效果 |
| 更多 | 下拉菜单 | 包含编辑、关联简历、删除 |

### 5.3 会议链接

- **复制按钮**：点击后显示 "已复制" 提示，2 秒后恢复
- **一键入会**：`target="_blank"` 打开新标签页

---

## 六、响应式设计

### 6.1 平板设备 (< 768px)

```
┌─────────────────────────────────────────┐
│  ←  字节跳动 / 高级前端工程师    [进行中] │
│                                          │
│  📅 明天 14:00    💻 线上面试            │
│  🎯 一面          ⏱️ 还有 18h            │
│                                          │
│  🔗 https://zoom.us/j/123456 [复制][入会]│
│                                          │
│         [🎯 开始模拟面试]  [⋯]          │
└─────────────────────────────────────────┘
```

- 四个信息块改为 2x2 网格布局

### 6.2 手机设备 (< 480px)

```
┌─────────────────────────────┐
│  ←  字节跳动 / 高级前端工程师 │
│                        [进行中]│
│                              │
│  📅 明天 14:00               │
│  💻 线上面试                 │
│  🎯 一面                     │
│  ⏱️ 还有 18h                 │
│                              │
│  🔗 zoom.us/j/123456         │
│     [复制] [入会]            │
│                              │
│  [🎯 开始模拟面试]           │
│  [编辑] [删除]               │
└─────────────────────────────┘
```

- 信息块改为单列布局
- 操作按钮改为垂直排列

---

## 七、对比总结

| 维度 | 当前设计 | 优化方案 A |
|------|----------|------------|
| 信息密度 | 分散在多个区域 | 头部一眼看清 |
| 时间感知 | 只有日期时间 | 紧迫感提示 |
| 操作优先级 | 按钮平级 | 主次分明 |
| 危险操作 | 直接暴露 | 收起到菜单 |
| 线上面试 | 链接不突出 | 专属链接栏 |

---

## 八、实施建议

### 8.1 开发工作量估算

| 任务 | 预估工时 |
|------|----------|
| InterviewHeader 组件开发 | 2h |
| MeetingLinkBar 组件开发 | 0.5h |
| 样式调整 | 1h |
| 响应式适配 | 1h |
| 测试与调优 | 0.5h |
| **总计** | **5h** |

### 8.2 实施步骤

1. 创建 `InterviewHeader.vue` 组件
2. 创建 `MeetingLinkBar.vue` 组件
3. 在 `InterviewDetail.vue` 中引入新组件
4. 删除原有的 `page-header` 和 `interview-info-card` 相关代码
5. 测试各状态下的显示效果
6. 响应式适配

---

## 九、附录

### 9.1 状态映射表

| 状态值 | 中文标签 | 颜色 |
|--------|----------|------|
| preparing | 准备中 | 黄色 |
| in_progress | 进行中 | 蓝色 |
| completed | 已完成 | 绿色 |
| cancelled | 已取消 | 灰色 |

### 9.2 时间格式化规则

```typescript
function formatTimeUrgency(dateStr: string): { text: string; class: string } {
  const diff = new Date(dateStr).getTime() - Date.now()
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(hours / 24)

  if (diff < 0) return { text: '已过期', class: 'expired' }
  if (hours < 1) return { text: '即将开始', class: 'critical' }
  if (hours < 24) return { text: `还有 ${hours} 小时`, class: 'urgent' }
  if (days === 1) return { text: '明天', class: 'soon' }
  if (days <= 3) return { text: `${days} 天后`, class: 'soon' }
  return { text: formatDate(dateStr), class: 'normal' }
}
```

---

**评审意见：**

_请在此处记录评审反馈..._
