# 功能详情

## 智能简历管理 {#resume}

支持多份简历管理，提供从上传、解析、编辑到导出的完整工作流。

**核心能力：**

- **上传解析**：支持 PDF / Word 格式上传，AI 自动解析为结构化数据
- **区块式编辑**：简历按模块拆分（基本信息、教育经历、工作经历、项目经验、技能、证书等），独立编辑
- **版本管理**：每次修改自动保存版本快照，支持回滚到任意历史版本
- **PDF 导出**：一键生成高质量 PDF 文件
- **简历派生**：从已优化简历派生岗位定制版本

**区块类型：**

| 区块 | 类型 | 说明 |
|------|------|------|
| BASIC_INFO | 单对象 | 基本信息（姓名、联系方式等） |
| EDUCATION | 数组 | 教育经历 |
| WORK | 数组 | 工作经历 |
| PROJECT | 数组 | 项目经验 |
| SKILLS | 数组 | 专业技能 |
| CERTIFICATE | 数组 | 证书荣誉 |
| OPEN_SOURCE | 数组 | 开源贡献 |
| CUSTOM | 数组 | 自定义区块 |

---

## AI 对话式优化 {#ai-chat}

基于 ReactAgent 构建的智能对话系统，通过悬浮球交互，支持通用聊天和简历对话两种模式。

**核心能力：**

- **双模式支持**：通用聊天模式（求职咨询）和简历对话模式（AI 自动选择简历）
- **操作卡片**：AI 生成操作建议，以卡片形式展示字段级 Diff 对比
- **批量应用**：支持一键应用或忽略单条/全部修改建议
- **图片理解**：支持上传图片（最多 10 张），AI 结合视觉信息优化简历

**技术架构：**

```
用户消息 → ReactAgent → LLM 思考 → 选择工具 → 执行 → 返回结果
                ↓
        8 个简历操作工具
        GetResume / GetSection / UpdateSection
        AddSection / DeleteSection / CreateResume
        GetResumeList / SelectResume
```

**技能系统（3 个内置技能）：**

| 技能 | 说明 |
|------|------|
| resume-diagnosis | 简历诊断 |
| resume-optimizer | 简历优化 |
| resume-suggestions | 简历建议 |

**SSE 流式事件：**

| 事件 | 说明 |
|------|------|
| `chunk` | 文本片段 |
| `suggestion` | 操作卡片（SectionChange 列表） |
| `resume_selected` | AI 选择了简历 |
| `complete` | 对话完成 |
| `error` | 错误信息 |

---

## AI 语音模拟面试 {#voice}

基于 WebSocket + 阿里云语音服务构建的实时语音对话面试系统。

**核心能力：**

- **实时语音对话**：候选人语音输入 → ASR 识别 → AI 面试官分析 → TTS 语音回复
- **问题预生成**：会话创建时基于 JD + 简历批量生成面试问题
- **智能追问**：回答不符合预期时，AI 动态生成追问
- **求助系统**：4 种求助模式（提示/解释概念/润色回答/自由提问），SSE 流式返回
- **录音回放**：面试全程录音，支持按片段回放和文字记录查看

**系统架构：**

```
候选人音频 → WebSocket → ASR(阿里云) → 文本 → 面试官 Agent → 回复
     ↓                                               ↓
  VAD 静音检测                                    TTS 合成
     ↓                                               ↓
  PCM 16kHz                                      音频推送
```

**求助类型：**

| 类型 | 说明 |
|------|------|
| GIVE_HINTS | 给出提示 |
| EXPLAIN_CONCEPT | 解释概念 |
| POLISH_ANSWER | 润色回答 |
| FREE_QUESTION | 自由提问 |

---

## 面试中心 {#interview-center}

真实面试全流程管理模块，从面试创建到复盘分析的完整工作流。

**核心能力：**

- **面试管理**：创建、编辑、删除真实面试记录，关联公司和职位
- **AI 准备工作流**：自动调研公司、分析 JD、生成面试准备清单
- **AI 复盘分析**：分析面试对话，评估表现，生成改进建议
- **准备事项管理**：跟踪待办事项，支持手动添加和 AI 生成

**准备工作流：**

```
CheckCompany → [需调研?] → CompanyResearch → CheckJobPosition → [需分析?] → JDAnalysis → GeneratePreparation
```

**复盘分析工作流：**

```
AnalyzeTranscript(0%→30%) → AnalyzeInterview(30%→70%) → GenerateAdvice(70%→100%)
```

---

## 简历优化工作流 {#optimize}

基于 Spring AI Agent Framework 的 StateGraph 状态机，三步完成简历优化。

**工作流程：**

```
START → DiagnoseQuick → GenerateSuggestions → OptimizeSection → END
         (快速诊断)        (生成建议)            (内容优化)
```

**各节点说明：**

| 节点 | 职责 |
|------|------|
| DiagnoseQuick | 快速诊断简历问题，生成评分和建议 |
| GenerateSuggestions | 基于诊断结果生成详细优化建议 |
| OptimizeSection | 按建议优化各区块内容 |

**特性：**
- SSE 实时推送每个节点的输出进度
- 支持 MemorySaver 状态持久化（断点续传）
- 优化完成后自动保存新版本快照

---

## 简历定制工作流 {#tailor}

根据目标岗位 JD，自动分析匹配度并生成定制化简历。

**工作流程：**

```
START → AnalyzeJD → MatchResume → GenerateTailored → END
         (分析JD)    (匹配简历)     (生成定制简历)
```

**各节点说明：**

| 节点 | 职责 |
|------|------|
| AnalyzeJD | 分析职位描述，提取必备技能、关键词 |
| MatchResume | 匹配简历与 JD，计算匹配度 |
| GenerateTailoredResume | 根据匹配分析生成定制简历 |

**使用限制：** 仅支持从已优化简历派生，确保定制质量。
