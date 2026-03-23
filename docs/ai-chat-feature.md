# AI聊天对话优化简历功能 - 技术设计文档

> 文档版本：1.0
> 创建日期：2026-03-23
> 作者：AI Assistant

---

## 一、功能概述

### 1.1 功能目标

提供一个全局可用的AI聊天对话窗口，让用户能够通过自然对话的方式优化简历内容，比现有的工作流式交互更加灵活和直观。

### 1.2 核心场景

1. **图片上传与识别**：用户上传简历截图/绩效页面，AI使用多模态能力识别内容，作为优化证据
2. **对话式优化**：用户通过对话让AI优化特定区块（工作经历、项目描述等）
3. **从零创建**：用户从零开始，通过对话引导创建新简历
4. **修改确认**：AI给出修改建议，用户确认后应用到简历

### 1.3 技术选型

| 技术点 | 方案 | 说明 |
|--------|------|------|
| 修改建议机制 | OpenAI Function Calling | 结构化输出，解析稳定，模型优化过 |
| 流式输出 | SSE + fetch ReadableStream | 支持POST请求传递图片和历史消息 |
| 图片处理 | AI多模态识别 | 复用现有 FileToImageService |
| 前端状态 | Composable 模式 | useAIChat 管理所有聊天状态 |

---

## 二、功能需求

### 2.1 聊天对话窗口

| 需求项 | 说明 |
|--------|------|
| 位置 | 全局悬浮窗口，右下角悬浮按钮，点击弹出 |
| 流式输出 | AI回复采用打字机效果，逐字显示 |
| 历史保存 | 会话内保存，刷新/关闭窗口后清空 |
| 图片上传 | 支持上传简历截图、绩效页面等图片 |
| 图片识别 | 使用AI多模态能力识别图片内容 |

### 2.2 简历上下文管理

| 需求项 | 说明 |
|--------|------|
| 简历选择 | 在对话窗口内通过下拉框选择简历 |
| 支持类型 | 现有简历优化 + 从零创建新简历 |
| 新建时机 | 选择"新建简历"时，立即创建空白简历 |

### 2.3 快捷指令

| 类型 | 示例 |
|------|------|
| 区块优化类 | 优化工作经历、润色项目描述、精简技能列表 |
| 内容调整类 | 改为量化描述、增加技术关键词 |
| 诊断分析类 | 分析优势和不足、给出改进建议 |
| 创建生成类 | 从零写工作经历、创建个人简介 |

### 2.4 修改确认机制

| 需求项 | 说明 |
|--------|------|
| 建议展示 | AI返回修改建议时，显示修改前后对比 |
| 用户确认 | 用户确认后才应用到简历 |
| 部分应用 | 支持选择性应用部分修改 |

---

## 三、系统架构

### 3.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                              前端                                    │
├─────────────────────────────────────────────────────────────────────┤
│  App.vue                                                            │
│    └── AIChatFloat.vue (悬浮按钮)                                    │
│          └── AIChatWindow.vue (聊天窗口容器)                          │
│                ├── ChatHeader.vue (头部: 简历选择器)                   │
│                ├── ChatMessageList.vue (消息列表)                      │
│                ├── ChatInputArea.vue (输入区: 图片上传 + 发送)          │
│                ├── QuickCommands.vue (快捷指令面板)                     │
│                └── ApplyChangesDialog.vue (修改确认对话框)              │
├─────────────────────────────────────────────────────────────────────┤
│  useAIChat Composable (状态管理 + SSE连接)                           │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ SSE (POST /ai-chat/stream)
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                              后端                                    │
├─────────────────────────────────────────────────────────────────────┤
│  AIChatController.java                                              │
│    └── AIChatHandler.java (SSE流式输出处理)                          │
│          └── AIChatService.java (AI对话核心逻辑)                      │
│                ├── ResumeTools.java (Function Calling 工具定义)      │
│                └── FileToImageService.java (图片处理，复用)           │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.2 数据流图

```
用户输入消息（可选附带图片）
    │
    ▼
前端创建 SSE 连接 (POST /ai-chat/stream)
    │
    ▼
后端构建系统提示词（注入简历上下文）
    │
    ▼
AI 流式生成回复
    │
    ├─► 文本块 → SSE chunk 事件 → 前端打字机效果显示
    │
    └─► Tool Call → SSE suggestion 事件 → 前端显示修改建议卡片
    │
    ▼
用户确认应用修改
    │
    ▼
POST /ai-chat/apply → 后端更新简历
```

---

## 四、前端设计

### 4.1 组件架构

```
frontend/src/components/chat/
├── AIChatFloat.vue          # 悬浮按钮组件
├── AIChatWindow.vue         # 聊天窗口主容器
├── ChatHeader.vue           # 头部（简历选择器）
├── ChatMessageList.vue      # 消息列表
├── ChatMessageItem.vue      # 单条消息
├── ChatInputArea.vue        # 输入区域（图片上传+发送）
├── QuickCommands.vue        # 快捷指令面板
└── ApplyChangesDialog.vue   # 修改确认对话框
```

### 4.2 组件布局

```
┌─────────────────────────────────────────────────────────┐
│                    AIChatWindow.vue                      │
│  ┌────────────────────────────────────────────────────┐ │
│  │ ChatHeader.vue                                     │ │
│  │ [简历选择下拉框 ▼] [新建简历] [关闭 ×]               │ │
│  └────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────┐ │
│  │ ChatMessageList.vue                                │ │
│  │  ┌──────────────────────────────────────────────┐  │ │
│  │  │ ChatMessageItem.vue (用户消息)               │  │ │
│  │  └──────────────────────────────────────────────┘  │ │
│  │  ┌──────────────────────────────────────────────┐  │ │
│  │  │ ChatMessageItem.vue (AI消息，打字机效果)     │  │ │
│  │  └──────────────────────────────────────────────┘  │ │
│  │  ┌──────────────────────────────────────────────┐  │ │
│  │  │ 修改建议卡片                                  │  │ │
│  │  └──────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────┐ │
│  │ QuickCommands.vue                                  │ │
│  │ [优化工作经历] [润色项目] [量化描述] [更多...]      │ │
│  └────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────┐ │
│  │ ChatInputArea.vue                                  │ │
│  │ [图片 📎] [输入框....................] [发送 ➤]    │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

### 4.3 状态管理（useAIChat Composable）

```typescript
// composables/useAIChat.ts
interface AIChatState {
  // 窗口状态
  visible: boolean

  // 简历上下文
  resumeId: string | null
  resumeList: ResumeListItem[]

  // 消息
  messages: ChatMessage[]
  isStreaming: boolean

  // 修改建议
  pendingSuggestions: SectionChange[]

  // 错误
  error: string | null
}

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  type: 'text' | 'image' | 'suggestion'
  content: string
  images?: ChatImage[]
  suggestions?: SectionChange[]
  timestamp: number
  isStreaming?: boolean
}

interface SectionChange {
  id: string                    // 前端生成的唯一ID
  sectionId?: string            // 区块ID（update/delete时需要）
  sectionType?: SectionType     // 区块类型（add时需要）
  sectionTitle?: string         // 区块标题（显示用）
  changeType: 'update' | 'add' | 'delete'
  beforeContent?: Record<string, any>   // 修改前内容（前端填充）
  afterContent: Record<string, any>     // 修改后内容
  description: string           // 修改说明
}
```

### 4.4 SSE 连接实现

```typescript
// 使用 fetch + ReadableStream 处理 POST 请求的 SSE
async function startSSEConnection(text: string, images: File[], aiMsgId: string) {
  const formData = new FormData()
  formData.append('resumeId', state.resumeId || '')
  formData.append('message', text)
  formData.append('history', JSON.stringify(state.messages.slice(0, -1)))
  images?.forEach((img, i) => formData.append(`image${i}`, img))

  const response = await fetch('/landit/ai-chat/stream', {
    method: 'POST',
    body: formData
  })

  const reader = response.body?.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (reader) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })

    // 按行解析 SSE 事件
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''

    for (const line of lines) {
      if (line.startsWith('data:')) {
        const data = JSON.parse(line.slice(5))
        handleSSEEvent(data, aiMsgId)
      }
    }
  }
}
```

### 4.5 SSE 事件处理

```typescript
function handleSSEEvent(event: ChatSSEEvent, aiMsgId: string) {
  const aiMsg = state.messages.find(m => m.id === aiMsgId)

  switch (event.event) {
    case 'chunk':
      // 追加内容（打字机效果）
      aiMsg.content += event.content
      break

    case 'suggestion':
      // 添加修改建议
      state.pendingSuggestions.push(event.data)
      break

    case 'complete':
      aiMsg.isStreaming = false
      state.isStreaming = false
      break

    case 'error':
      aiMsg.content = '发生错误：' + event.content
      aiMsg.isStreaming = false
      state.isStreaming = false
      break
  }
}
```

---

## 五、后端设计

### 5.1 模块结构

```
backend/src/main/java/com/landit/chat/
├── controller/
│   └── AIChatController.java       # REST API 控制器
├── handler/
│   └── AIChatHandler.java          # 业务处理器
├── service/
│   └── AIChatService.java          # AI对话服务
├── tools/
│   └── ResumeTools.java            # Function Calling 工具定义
└── dto/
    ├── ChatRequest.java            # 聊天请求
    ├── ChatEvent.java              # SSE 事件
    ├── ApplyChangesRequest.java    # 应用修改请求
    ├── SectionChange.java          # 区块修改
    └── ImageRecognitionResult.java # 图片识别结果
```

### 5.2 API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/ai-chat/stream` | SSE流式聊天对话 |
| POST | `/ai-chat/apply` | 应用修改到简历 |
| POST | `/ai-chat/recognize-image` | 上传图片并识别内容 |

### 5.3 SSE 事件类型

| 事件类型 | 说明 | 数据结构 |
|----------|------|----------|
| `chunk` | AI回复内容片段 | `{ event: "chunk", content: "..." }` |
| `suggestion` | 修改建议 | `{ event: "suggestion", data: SectionChange }` |
| `complete` | 对话完成 | `{ event: "complete" }` |
| `error` | 发生错误 | `{ event: "error", content: "错误信息" }` |

### 5.4 Function Calling 工具定义

```java
// ResumeTools.java
@Component
public class ResumeTools {

    @Tool(description = "更新简历区块内容，用于修改已有区块")
    public SectionChange updateSection(
            @ToolParam(description = "区块ID") String sectionId,
            @ToolParam(description = "区块标题") String sectionTitle,
            @ToolParam(description = "修改后的完整内容(JSON字符串)") String afterContent,
            @ToolParam(description = "修改说明") String description) {
        return SectionChange.update(sectionId, sectionTitle, afterContent, description);
    }

    @Tool(description = "新增简历区块，用于添加新的经历或技能")
    public SectionChange addSection(
            @ToolParam(description = "区块类型: WORK/PROJECT/EDUCATION/SKILLS/CERTIFICATE") String sectionType,
            @ToolParam(description = "区块内容(JSON字符串)") String content,
            @ToolParam(description = "添加说明") String description) {
        return SectionChange.add(sectionType, content, description);
    }

    @Tool(description = "删除简历区块")
    public SectionChange deleteSection(
            @ToolParam(description = "区块ID") String sectionId,
            @ToolParam(description = "删除原因") String reason) {
        return SectionChange.delete(sectionId, reason);
    }
}
```

### 5.5 流式处理实现

```java
// AIChatService.java
public Flux<ChatEvent> streamChat(String resumeId, ChatRequest request, String threadId) {
    // 状态：累积 tool call 参数
    StringBuilder toolCallBuffer = new StringBuilder();
    String[] currentToolCall = {null, null};  // {id, name}

    return chatClient.prompt()
        .system(buildSystemPrompt(resumeId))
        .messages(buildMessages(request))
        .functions(resumeTools)  // 注册工具
        .stream()
        .chatResponse()
        .map(response -> {
            // 1. 处理文本块（直接流式返回）
            String text = response.getResult().getOutput().getText();
            if (text != null && !text.isEmpty()) {
                return ChatEvent.chunk(threadId, text);
            }

            // 2. 处理 tool call（累积参数）
            if (response.getResult().getOutput().getToolCalls() != null) {
                for (ToolCall tc : response.getResult().getOutput().getToolCalls()) {
                    if (tc.id() != null) currentToolCall[0] = tc.id();
                    if (tc.name() != null) currentToolCall[1] = tc.name();
                    if (tc.arguments() != null) toolCallBuffer.append(tc.arguments());
                }
            }

            // 3. tool call 完成，解析并返回建议
            if (response.getMetadata().getFinishReason() == FinishReason.TOOL_CALLS) {
                String args = toolCallBuffer.toString();
                SectionChange change = parseToolCall(currentToolCall[1], args);

                // 清空状态
                toolCallBuffer.setLength(0);
                currentToolCall[0] = null;
                currentToolCall[1] = null;

                return ChatEvent.suggestion(threadId, change);
            }

            return null;
        })
        .filter(Objects::nonNull)
        .concatWith(Flux.just(ChatEvent.complete(threadId)))
        .onErrorResume(e -> Flux.just(ChatEvent.error(threadId, e.getMessage())));
}
```

### 5.6 系统提示词设计

```
# 角色定义
你是一位专业的简历优化顾问，帮助用户优化简历内容。

# 当前简历数据
以下是用户的简历内容（JSON格式）：

{
  "sections": [
    {
      "id": "work-001",
      "type": "WORK",
      "title": "阿里云",
      "content": {
        "company": "阿里云",
        "position": "高级开发工程师",
        "description": "负责云平台的开发工作..."
      }
    },
    ...
  ]
}

# 修改规则
1. 根据用户描述，准确定位要修改的区块（使用 sectionId）
2. 当需要修改简历时，调用相应的工具函数
3. 在调用工具前，先简要说明你的修改思路
4. 保持专业友好的沟通风格
5. 如果用户需求不明确，主动询问
```

---

## 六、交互流程

### 6.1 用户修改指令的完整流程

**场景示例**：用户输入 "帮我把阿里云的工作经历改得更专业一点，加上量化数据"

```
1. 用户发送指令
       ↓
2. 后端接收请求（带简历上下文）
       ↓
3. AI 分析用户意图 + 匹配简历区块
       ↓
4. AI 流式输出分析文本："好的，我来帮你优化这段工作经历..."
       ↓
5. AI 调用工具：update_section(sectionId="work-001", ...)
       ↓
6. 前端接收 SSE 事件：chunk（打字机显示）+ suggestion（建议卡片）
       ↓
7. 用户查看修改对比，确认应用
       ↓
8. 调用 POST /ai-chat/apply
       ↓
9. 后端更新简历 → 返回成功
       ↓
10. 前端显示"已应用"提示
```

### 6.2 不同修改类型的处理

| 场景 | 用户指令示例 | 调用工具 | 参数说明 |
|------|-------------|----------|----------|
| 更新内容 | "把工作描述改得更专业" | update_section | sectionId + afterContent |
| 新增区块 | "帮我加一段开源贡献" | add_section | sectionType + content |
| 删除区块 | "删掉这个不相关的项目" | delete_section | sectionId + reason |
| 批量修改 | "优化所有工作经历" | 多次 update_section | 各自的参数 |

### 6.3 用户不满意时的处理

```
用户: 再改得更简洁一点，控制在100字以内
    ↓
AI: 好的，我重新调整一下...（流式输出）
    ↓
AI 再次调用工具：update_section(sectionId="work-001", content={精简后的内容})
    ↓
前端显示新的修改建议（替换之前的待确认建议）
```

---

## 七、文件清单

### 7.1 需要创建的文件

**前端（11个文件）**

```
frontend/src/
├── components/chat/
│   ├── AIChatFloat.vue          # 悬浮按钮
│   ├── AIChatWindow.vue         # 聊天窗口主容器
│   ├── ChatHeader.vue           # 头部
│   ├── ChatMessageList.vue      # 消息列表
│   ├── ChatMessageItem.vue      # 单条消息
│   ├── ChatInputArea.vue        # 输入区域
│   ├── QuickCommands.vue        # 快捷指令
│   └── ApplyChangesDialog.vue   # 修改确认对话框
├── composables/
│   └── useAIChat.ts             # AI聊天 Composable
├── types/
│   └── ai-chat.ts               # 类型定义
└── api/
    └── aiChat.ts                # API 调用
```

**后端（9个文件）**

```
backend/src/main/java/com/landit/chat/
├── controller/
│   └── AIChatController.java
├── handler/
│   └── AIChatHandler.java
├── service/
│   └── AIChatService.java
├── tools/
│   └── ResumeTools.java
└── dto/
    ├── ChatRequest.java
    ├── ChatEvent.java
    ├── ApplyChangesRequest.java
    ├── SectionChange.java
    └── ImageRecognitionResult.java
```

### 7.2 需要修改的文件

| 文件 | 修改内容 |
|------|----------|
| `frontend/src/App.vue` | 添加 AIChatFloat 组件 |
| `frontend/src/assets/styles/variables.scss` | 添加聊天窗口相关 z-index |
| `backend/.../common/config/AIPromptProperties.java` | 添加聊天提示词配置 |

---

## 八、可复用的现有代码

| 模块 | 文件 | 复用内容 |
|------|------|----------|
| SSE流式输出 | `ResumeOptimizeGraphHandler.java` | SseEmitter 封装模式 |
| 图片处理 | `FileToImageService.java` | convertToMedia() 方法 |
| 弹窗样式 | `ConfirmModal.vue` | 弹窗基础结构和动画 |
| 样式系统 | `variables.scss` | SCSS 变量和 z-index |

---

## 九、验证方案

### 9.1 功能测试

| 测试项 | 预期结果 |
|--------|----------|
| 点击悬浮按钮 | 聊天窗口正常弹出 |
| 选择已有简历 | 能正常加载简历上下文 |
| 选择新建简历 | 自动创建空白简历 |
| 发送文本消息 | AI流式回复正常显示（打字机效果） |
| 上传图片 | AI能识别图片内容 |
| 点击快捷指令 | 能正常发送预设内容 |
| AI返回修改建议 | 能正常显示确认对话框 |
| 确认修改后 | 简历内容正确更新 |

### 9.2 边界测试

| 测试项 | 处理方式 |
|--------|----------|
| 未选择简历时发送消息 | 提示用户先选择简历 |
| 网络断开 | 显示错误提示，支持重连 |
| 大图片上传 | 压缩后上传，显示加载状态 |
| 长对话性能 | 限制历史消息数量 |
| AI返回格式错误 | 降级显示原始内容 |

---

## 十、附录

### 10.1 快捷指令预设

```typescript
const QUICK_COMMANDS = [
  // 区块优化类
  { id: 'optimize-work', category: 'optimize', label: '优化工作经历', prompt: '请帮我优化工作经历部分的描述，使其更加专业和有说服力' },
  { id: 'optimize-project', category: 'optimize', label: '润色项目描述', prompt: '请润色我的项目经验描述，突出技术亮点和成果' },
  { id: 'optimize-skills', category: 'optimize', label: '精简技能列表', prompt: '请帮我精简和优化技能列表，去除冗余并突出核心技能' },

  // 内容调整类
  { id: 'quantify-content', category: 'adjust', label: '改为量化描述', prompt: '请将这段内容改为量化描述，使用具体的数字和成果' },
  { id: 'add-keywords', category: 'adjust', label: '增加技术关键词', prompt: '请为这段内容增加相关的技术关键词，提高 ATS 匹配度' },

  // 诊断分析类
  { id: 'analyze-strengths', category: 'diagnose', label: '分析优势和不足', prompt: '请分析我简历的优势和不足之处，并给出改进建议' },
  { id: 'improvement-suggestions', category: 'diagnose', label: '给出改进建议', prompt: '请针对我的简历给出具体的改进建议' },

  // 创建生成类
  { id: 'create-work', category: 'create', label: '从零写工作经历', prompt: '请根据我的背景帮我写一段工作经历' },
  { id: 'create-summary', category: 'create', label: '创建个人简介', prompt: '请帮我写一段专业的个人简介' }
]
```

### 10.2 z-index 层级规划

```scss
// 聊天窗口相关层级
$z-chat-float: 900;           // 悬浮按钮
$z-chat-window: 1000;         // 聊天窗口
$z-chat-overlay: 1010;        // 聊天窗口遮罩
$z-confirm-dialog: 1020;      // 确认对话框
```
