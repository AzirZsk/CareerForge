# AI聊天助手工具系统技术设计文档

> 版本：1.0.0
> 日期：2026-03-25
> 作者：Azir

---

## 1. 背景与问题

### 1.1 现状分析

当前AI聊天助手已具备以下能力：

| 能力 | 实现方式 | 状态 |
|------|---------|------|
| 技能系统 | 3个SKILL.md文件（提示词） | ✅ 已实现 |
| 多轮对话 | ReactAgent + MemorySaver | ✅ 已实现 |
| 简历上下文 | 首次对话注入 | ✅ 已实现 |
| 修改确认机制 | 前端弹窗 + applyChanges API | ⚠️ API未实现 |

### 1.2 核心问题

**技能只是提示词，不是工具**：

```
当前流程：
用户提问 → AI根据SKILL.md生成建议 → 用户手动复制建议到简历

期望流程：
用户提问 → AI调用工具读取简历 → AI生成修改建议 → 用户确认 → AI调用工具修改简历
```

AI只能"说"（生成建议），不能"做"（实际操作简历数据）。

### 1.3 解决方案

为ReactAgent添加**真正的可执行工具**，让AI能够直接操作简历数据。

---

## 2. 技术方案

### 2.1 技术选型

基于 **Spring AI Alibaba Agent Framework** 的 `FunctionToolCallback` 机制：

```java
// Tool实现模式
public class GetResumeTool implements BiFunction<Request, ToolContext, String> {

    // 1. 定义请求参数（使用record + Jackson注解）
    public record Request(
        @JsonProperty(required = true)
        @JsonPropertyDescription("简历ID")
        String resumeId
    ) {}

    // 2. 实现apply方法
    @Override
    public String apply(Request request, ToolContext context) {
        // 调用现有Service获取数据
        // 返回JSON格式结果
    }

    // 3. 创建ToolCallback
    public static ToolCallback createCallback(ResumeHandler handler) {
        return FunctionToolCallback.builder("get_resume", new GetResumeTool(handler))
            .description("获取简历完整信息")
            .inputType(Request.class)
            .build();
    }
}
```

### 2.2 架构设计

```
┌─────────────────────────────────────────────────────────┐
│                      ReactAgent                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │              ChatAgentConfig                    │   │
│  │  - SkillsAgentHook (已有，提供提示词技能)         │   │
│  │  + ResumeToolsHook (新增，提供可执行工具)         │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                   ResumeToolsHook                       │
│  ┌─────────────────────────────────────────────────┐   │
│  │  读取工具（直接执行）                             │   │
│  │  - get_resume: 获取简历完整信息                  │   │
│  │  - get_section: 获取指定区块内容                 │   │
│  └─────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────┐   │
│  │  修改工具（返回建议，等待确认）                    │   │
│  │  - update_section: 更新区块内容                  │   │
│  │  - add_section: 新增区块                        │   │
│  │  - delete_section: 删除区块                     │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│              现有Service层 (复用)                        │
│  - ResumeHandler.getResumeDetail()                      │
│  - ResumeHandler.updateSection()                        │
│  - ResumeHandler.addSection()                           │
│  - ResumeHandler.deleteSection()                        │
└─────────────────────────────────────────────────────────┘
```

### 2.3 确认机制设计

修改工具不会立即执行，而是返回一个**修改建议**：

```
AI调用 update_section_tool
    ↓
工具返回修改建议（JSON格式，包含before/after）
    ↓
AI将建议包装在特定格式中返回给前端
    ↓
前端收到 suggestion 事件，弹出确认框
    ↓
用户确认 → 调用 POST /ai-chat/apply → 实际执行修改
用户取消 → 丢弃建议
```

---

## 3. 工具详细设计

### 3.1 工具清单

| 工具名称 | 类型 | 功能 | 执行方式 |
|---------|------|------|---------|
| `get_resume` | 读取 | 获取简历完整信息 | 直接返回数据 |
| `get_section` | 读取 | 获取指定区块内容 | 直接返回数据 |
| `update_section` | 修改 | 更新区块内容 | 返回建议 |
| `add_section` | 修改 | 新增区块 | 返回建议 |
| `delete_section` | 修改 | 删除区块 | 返回建议 |

### 3.2 GetResumeTool

**功能**：获取简历完整信息（基本信息 + 所有区块内容）

**请求参数**：
```java
public record Request(
    @JsonProperty(required = true)
    @JsonPropertyDescription("简历ID")
    String resumeId
) {}
```

**返回格式**：
```json
{
    "name": "我的简历",
    "targetPosition": "Java后端开发",
    "overallScore": 72,
    "sections": [
        {
            "id": "1234567890",
            "type": "BASIC_INFO",
            "title": "基本信息",
            "content": "{\"name\":\"张三\",\"phone\":\"13800138000\"}",
            "score": 85
        },
        {
            "id": "1234567891",
            "type": "PROJECT",
            "title": "订单系统",
            "content": "{\"name\":\"订单系统\",\"description\":\"负责订单模块开发\"}",
            "score": 60
        }
    ]
}
```

**实现代码**：
```java
package com.landit.chat.tools;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.handler.ResumeHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.function.BiFunction;

/**
 * 获取简历完整信息工具
 */
@RequiredArgsConstructor
public class GetResumeTool implements BiFunction<GetResumeTool.Request, ToolContext, String> {

    private final ResumeHandler resumeHandler;

    public record Request(
        @JsonProperty(required = true)
        @JsonPropertyDescription("简历ID")
        String resumeId
    ) {}

    @Override
    public String apply(Request request, ToolContext context) {
        try {
            ResumeDetailVO resume = resumeHandler.getResumeDetail(request.resumeId());
            if (resume == null) {
                return "{\"error\": \"简历不存在\", \"resumeId\": \"" + request.resumeId() + "\"}";
            }

            // 构建返回结果
            StringBuilder result = new StringBuilder();
            result.append("{\n");
            result.append("  \"name\": \"").append(escapeJson(resume.getName())).append("\",\n");
            result.append("  \"targetPosition\": \"").append(escapeJson(resume.getTargetPosition())).append("\",\n");
            if (resume.getOverallScore() != null) {
                result.append("  \"overallScore\": ").append(resume.getOverallScore()).append(",\n");
            }
            result.append("  \"sections\": [\n");

            if (resume.getSections() != null) {
                for (int i = 0; i < resume.getSections().size(); i++) {
                    var section = resume.getSections().get(i);
                    result.append("    {\n");
                    result.append("      \"id\": \"").append(section.getId()).append("\",\n");
                    result.append("      \"type\": \"").append(section.getType()).append("\",\n");
                    result.append("      \"title\": \"").append(escapeJson(section.getTitle())).append("\",\n");
                    result.append("      \"content\": \"").append(escapeJson(section.getContent())).append("\"");
                    if (section.getScore() != null) {
                        result.append(",\n      \"score\": ").append(section.getScore());
                    }
                    result.append("\n    }");
                    if (i < resume.getSections().size() - 1) {
                        result.append(",");
                    }
                    result.append("\n");
                }
            }

            result.append("  ]\n");
            result.append("}");

            return result.toString();
        } catch (Exception e) {
            return "{\"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    public static ToolCallback createCallback(ResumeHandler resumeHandler) {
        return FunctionToolCallback.builder("get_resume", new GetResumeTool(resumeHandler))
            .description("获取简历完整信息，包含基本信息和所有区块内容。当需要了解简历整体情况时使用。")
            .inputType(Request.class)
            .build();
    }
}
```

### 3.3 GetSectionTool

**功能**：获取指定区块的详细内容

**请求参数**：
```java
public record Request(
    @JsonProperty(required = true)
    @JsonPropertyDescription("简历ID")
    String resumeId,

    @JsonProperty(required = true)
    @JsonPropertyDescription("区块ID")
    String sectionId
) {}
```

**返回格式**：
```json
{
    "id": "1234567891",
    "type": "PROJECT",
    "title": "订单系统",
    "content": {
        "name": "订单系统",
        "role": "核心开发",
        "techStack": "Spring Boot, MySQL, Redis",
        "description": "负责订单模块开发"
    },
    "score": 60,
    "suggestions": ["补充量化数据", "使用STAR法则"]
}
```

### 3.4 UpdateSectionTool

**功能**：更新区块内容（返回修改建议，不直接修改）

**请求参数**：
```java
public record Request(
    @JsonProperty(required = true)
    @JsonPropertyDescription("简历ID")
    String resumeId,

    @JsonProperty(required = true)
    @JsonPropertyDescription("区块ID")
    String sectionId,

    @JsonProperty(required = true)
    @JsonPropertyDescription("新的区块内容（JSON格式）")
    String content,

    @JsonProperty(required = false)
    @JsonPropertyDescription("修改原因/说明")
    String reason
) {}
```

**返回格式**（修改建议）：
```json
{
    "action": "update",
    "sectionId": "1234567891",
    "sectionType": "PROJECT",
    "sectionTitle": "订单系统",
    "beforeContent": "{\"description\":\"负责订单模块开发\"}",
    "afterContent": "{\"description\":\"主导订单核心模块开发，基于Spring Cloud微服务架构，支撑日均50万订单\"}",
    "reason": "补充量化数据和具体技术栈"
}
```

### 3.5 AddSectionTool

**功能**：新增区块（返回修改建议，不直接添加）

**请求参数**：
```java
public record Request(
    @JsonProperty(required = true)
    @JsonPropertyDescription("简历ID")
    String resumeId,

    @JsonProperty(required = true)
    @JsonPropertyDescription("区块类型：EDUCATION, WORK, PROJECT, SKILLS, CERTIFICATE, OPEN_SOURCE, CUSTOM")
    String sectionType,

    @JsonProperty(required = true)
    @JsonPropertyDescription("区块标题")
    String title,

    @JsonProperty(required = true)
    @JsonPropertyDescription("区块内容（JSON格式）")
    String content
) {}
```

### 3.6 DeleteSectionTool

**功能**：删除区块（返回修改建议，不直接删除）

**请求参数**：
```java
public record Request(
    @JsonProperty(required = true)
    @JsonPropertyDescription("简历ID")
    String resumeId,

    @JsonProperty(required = true)
    @JsonPropertyDescription("区块ID")
    String sectionId,

    @JsonProperty(required = false)
    @JsonPropertyDescription("删除原因")
    String reason
) {}
```

---

## 4. Hook实现

### 4.1 ResumeToolsHook

```java
package com.landit.chat.tools;

import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.landit.resume.handler.ResumeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 简历工具Hook
 * 注册所有简历相关工具到Agent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeToolsHook implements AgentHook {

    private final ResumeHandler resumeHandler;

    @Override
    public List<ToolCallback> getTools() {
        List<ToolCallback> tools = List.of(
            GetResumeTool.createCallback(resumeHandler),
            GetSectionTool.createCallback(resumeHandler),
            UpdateSectionTool.createCallback(resumeHandler),
            AddSectionTool.createCallback(resumeHandler),
            DeleteSectionTool.createCallback(resumeHandler)
        );

        log.info("[ResumeToolsHook] 注册 {} 个简历工具", tools.size());
        return tools;
    }
}
```

### 4.2 更新ChatAgentConfig

```java
@Bean
public ReactAgent chatAgent(
    SkillsAgentHook skillsAgentHook,
    ResumeToolsHook resumeToolsHook  // 新增
) {
    String systemPrompt = aiPromptProperties.getChat()
            .getAdvisorConfig()
            .getSystemPrompt();

    log.info("[ChatAgent] 初始化 ChatAgent, systemPrompt length={}", systemPrompt.length());

    return ReactAgent.builder()
        .name("chat_advisor")
        .model(chatModel)
        .systemPrompt(systemPrompt)
        .saver(new MemorySaver())
        .hooks(List.of(skillsAgentHook, resumeToolsHook))  // 新增resumeToolsHook
        .build();
}
```

---

## 5. 前端适配

### 5.1 现有确认机制

前端已有确认机制（`useAIChat.ts`），处理流程：

```typescript
// AI返回修改建议时触发
if (event.type === 'suggestion') {
  // 弹出确认框
  showConfirmDialog(event.data)
}

// 用户确认后调用API
async function applyChanges(changes: SectionChange[]) {
  await fetch('/landit/ai-chat/apply', {
    method: 'POST',
    body: JSON.stringify({ resumeId, changes })
  })
}
```

### 5.2 无需修改

工具返回的修改建议格式与现有`SectionChange`类型兼容，前端无需修改。

---

## 6. 实现applyChanges

**AIChatService.java**：
```java
/**
 * 应用修改建议到简历
 */
public void applyChanges(String resumeId, List<SectionChange> changes) {
    log.info("[AIChat] 应用修改: resumeId={}, changes={}", resumeId, changes.size());

    for (SectionChange change : changes) {
        try {
            switch (change.getChangeType()) {
                case "update" -> {
                    log.info("[AIChat] 更新区块: sectionId={}", change.getSectionId());
                    resumeHandler.updateSection(
                        resumeId,
                        change.getSectionId(),
                        change.getAfterContent()
                    );
                }
                case "add" -> {
                    log.info("[AIChat] 新增区块: type={}", change.getSectionType());
                    resumeHandler.addSection(
                        resumeId,
                        change.getSectionType(),
                        change.getTitle(),
                        change.getAfterContent()
                    );
                }
                case "delete" -> {
                    log.info("[AIChat] 删除区块: sectionId={}", change.getSectionId());
                    resumeHandler.deleteSection(resumeId, change.getSectionId());
                }
                default -> log.warn("[AIChat] 未知的变更类型: {}", change.getChangeType());
            }
        } catch (Exception e) {
            log.error("[AIChat] 应用修改失败: change={}", change, e);
            throw new BusinessException("应用修改失败: " + e.getMessage());
        }
    }
}
```

---

## 7. 文件清单

### 7.1 新增文件

| 文件路径 | 说明 |
|---------|------|
| `chat/tools/ResumeToolsHook.java` | 工具Hook，注册所有工具 |
| `chat/tools/GetResumeTool.java` | 获取简历完整信息 |
| `chat/tools/GetSectionTool.java` | 获取指定区块内容 |
| `chat/tools/UpdateSectionTool.java` | 更新区块（返回建议） |
| `chat/tools/AddSectionTool.java` | 新增区块（返回建议） |
| `chat/tools/DeleteSectionTool.java` | 删除区块（返回建议） |

### 7.2 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `chat/config/ChatAgentConfig.java` | 注入ResumeToolsHook |
| `chat/service/AIChatService.java` | 实现applyChanges方法 |

### 7.3 复用文件（不修改）

| 文件路径 | 说明 |
|---------|------|
| `resume/handler/ResumeHandler.java` | 提供简历CRUD能力 |
| `frontend/composables/useAIChat.ts` | 已有确认机制 |
| `frontend/api/aiChat.ts` | 已有applyChanges API调用 |

---

## 8. 验证方案

### 8.1 启动验证

```bash
# 启动后端
cd backend && mvn spring-boot:run

# 检查日志，确认工具注册成功
# 期望输出：
# [ResumeToolsHook] 注册 5 个简历工具
# [ChatAgent] 初始化 ChatAgent
```

### 8.2 功能测试

**测试1：获取简历信息**
```
用户：我的简历有哪些区块？
期望：AI调用get_resume工具，返回区块列表
```

**测试2：获取区块详情**
```
用户：我的第一个项目经历具体内容是什么？
期望：AI调用get_section工具，返回区块详情
```

**测试3：更新区块（确认流程）**
```
用户：帮我把订单系统项目的描述优化一下
期望：
1. AI调用get_section获取当前内容
2. AI调用update_section返回修改建议
3. 前端弹出确认框
4. 用户点击确认 → 调用applyChanges API → 简历更新成功
```

---

## 9. 风险与注意事项

| 风险 | 说明 | 缓解措施 |
|------|------|---------|
| 安全性 | 工具操作需要验证resumeId归属 | 当前是单用户系统，暂无需处理 |
| 幂等性 | 更新操作应该是幂等的 | 使用sectionId作为唯一标识 |
| 错误处理 | 工具执行失败时需返回清晰错误 | 返回JSON格式的错误信息 |
| JSON解析 | 区块内容是JSON格式 | 正确处理序列化/反序列化 |
| 确认机制 | 误操作风险 | 保留用户确认机制 |

---

## 10. 后续扩展

### 10.1 可选扩展工具

| 工具 | 功能 | 优先级 |
|------|------|--------|
| `get_versions` | 获取简历版本历史 | P2 |
| `rollback_version` | 回滚到指定版本 | P2 |
| `export_pdf` | 导出简历PDF | P2 |
| `match_jd` | 匹配职位描述 | P3 |

### 10.2 性能优化

1. **工具结果缓存**：对读取工具的结果进行短期缓存
2. **批量操作**：支持一次确认多个修改建议
3. **异步执行**：对耗时操作使用异步执行

---

## 附录A：完整代码示例

### GetResumeTool.java

```java
package com.landit.chat.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.handler.ResumeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.function.BiFunction;

/**
 * 获取简历完整信息工具
 * 返回简历基本信息和所有区块内容
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class GetResumeTool implements BiFunction<GetResumeTool.Request, ToolContext, String> {

    private final ResumeHandler resumeHandler;

    public record Request(
        @JsonProperty(required = true)
        @JsonPropertyDescription("简历ID")
        String resumeId
    ) {}

    @Override
    public String apply(Request request, ToolContext context) {
        log.info("[GetResumeTool] 获取简历信息: resumeId={}", request.resumeId());

        try {
            ResumeDetailVO resume = resumeHandler.getResumeDetail(request.resumeId());
            if (resume == null) {
                return errorResponse("简历不存在", request.resumeId());
            }

            return buildSuccessResponse(resume);
        } catch (Exception e) {
            log.error("[GetResumeTool] 获取简历失败", e);
            return errorResponse(e.getMessage(), request.resumeId());
        }
    }

    private String buildSuccessResponse(ResumeDetailVO resume) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true, \"data\": {");
        sb.append("\"name\": \"").append(escape(resume.getName())).append("\", ");
        sb.append("\"targetPosition\": \"").append(escape(resume.getTargetPosition())).append("\", ");

        if (resume.getOverallScore() != null) {
            sb.append("\"overallScore\": ").append(resume.getOverallScore()).append(", ");
        }

        sb.append("\"sections\": [");
        if (resume.getSections() != null) {
            for (int i = 0; i < resume.getSections().size(); i++) {
                var section = resume.getSections().get(i);
                if (i > 0) sb.append(", ");
                sb.append("{");
                sb.append("\"id\": \"").append(section.getId()).append("\", ");
                sb.append("\"type\": \"").append(section.getType()).append("\", ");
                sb.append("\"title\": \"").append(escape(section.getTitle())).append("\", ");
                sb.append("\"content\": \"").append(escape(section.getContent())).append("\"");
                if (section.getScore() != null) {
                    sb.append(", \"score\": ").append(section.getScore());
                }
                sb.append("}");
            }
        }
        sb.append("]}}");

        return sb.toString();
    }

    private String errorResponse(String message, String resumeId) {
        return "{\"success\": false, \"error\": \"" + escape(message) + "\", \"resumeId\": \"" + resumeId + "\"}";
    }

    private String escape(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    public static ToolCallback createCallback(ResumeHandler resumeHandler) {
        return FunctionToolCallback.builder("get_resume", new GetResumeTool(resumeHandler))
            .description("获取简历完整信息，包含基本信息和所有区块内容。当需要了解简历整体情况时使用此工具。")
            .inputType(Request.class)
            .build();
    }
}
```

---

## 附录B：参考文档

1. [Spring AI Alibaba Agent Framework - Tool机制](https://github.com/alibaba/spring-ai-alibaba)
2. [Spring AI - FunctionToolCallback](https://docs.spring.io/spring-ai/reference/)
3. 项目文档：`/backend/CLAUDE.md`
4. 简历Handler：`/backend/src/main/java/com/landit/resume/handler/ResumeHandler.java`
