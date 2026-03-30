# 面试生命周期管理 - 技术实现文档

> **版本**：1.0
> **创建日期**：2026-03-30
> **基于需求文档**：`docs/interview-lifecycle.md`
> **作者**：Azir

---

## 一、上下文

### 1.1 背景与目标

LandIt 目前已有简历管理、AI模拟面试、面试复盘等功能，但缺少**真实面试的全流程管理能力**。本项目旨在：

- 将现有分散的面试功能（模拟面试、复盘）整合为统一的"面试中心"
- 新增真实面试生命周期管理：**准备 → 参加面试 → 结果跟踪 → 复盘总结**
- 实现 AI 辅助的公司调研、JD分析、准备清单生成、复盘分析

### 1.2 核心改造点

| 改造项 | 现状 | 目标 |
|--------|------|------|
| Interview 实体 | 仅支持模拟面试（type, position, company, date, score, status） | 扩展支持真实面试（interviewSource, jdContent, overallResult, companyResearch, jdAnalysis） |
| 导航结构 | 分离的"面试试演"和"面试复盘" | 合并为"面试中心"单一入口 |
| 工作流 | 简历优化/定制 | 新增面试相关 Graph（公司调研、JD分析、复盘分析） |

### 1.3 面试生命周期状态机

```
创建面试 → 面试准备 → 参加面试 → 结果跟踪 → 复盘总结
   |           |           |           |           |
   |      公司调研      记录实况    多轮状态    手动记录
   |      JD分析       录音/笔记    流转管理    AI辅助分析
   |      模拟训练
   |      准备清单
   v
(模拟面试走现有流程：开始 → 答题 → 结束 → 自动复盘)
```

---

## 二、数据库设计

### 2.1 扩展 t_interview 表

```sql
-- 面试记录表（改造）
-- 新增字段
ALTER TABLE t_interview ADD COLUMN interview_source VARCHAR(20) DEFAULT 'mock';
-- interview_source: mock(模拟面试), real(真实面试)

ALTER TABLE t_interview ADD COLUMN jd_content TEXT;
ALTER TABLE t_interview ADD COLUMN status VARCHAR(20) DEFAULT 'preparing';
-- status: preparing(准备中), in_progress(进行中), completed(已完成), cancelled(已取消)

ALTER TABLE t_interview ADD COLUMN overall_result VARCHAR(20) DEFAULT 'pending';
-- overall_result: passed(通过), failed(未通过), pending(待定)

ALTER TABLE t_interview ADD COLUMN notes TEXT;
ALTER TABLE t_interview ADD COLUMN company_research TEXT;  -- JSON格式存储公司调研结果
ALTER TABLE t_interview ADD COLUMN jd_analysis TEXT;       -- JSON格式存储JD分析结果
```

### 2.2 新增 t_interview_round 表

面试轮次表，管理多轮面试：

```sql
CREATE TABLE IF NOT EXISTS t_interview_round (
    id VARCHAR(64) PRIMARY KEY,
    interview_id VARCHAR(64) NOT NULL,           -- 关联的面试ID
    round_number INTEGER NOT NULL,               -- 轮次序号（1, 2, 3...）
    round_type VARCHAR(50),                      -- 轮次类型（technical_1/technical_2/hr/director/cto/final/custom）
    interviewer_name VARCHAR(100),               -- 面试官姓名
    scheduled_at DATETIME,                       -- 预定时间
    actual_start_at DATETIME,                    -- 实际开始时间
    actual_end_at DATETIME,                      -- 实际结束时间
    duration INTEGER DEFAULT 0,                  -- 时长（分钟）
    status VARCHAR(20) DEFAULT 'scheduled',      -- scheduled/in_progress/completed/cancelled
    result VARCHAR(20),                          -- passed/failed/pending
    notes TEXT,                                  -- 本轮笔记
    transcript TEXT,                             -- 对话记录（JSON）
    score INTEGER,                               -- 本轮评分
    feedback TEXT,                               -- 面试官反馈
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_interview_round_interview_id ON t_interview_round(interview_id);
```

### 2.3 新增 t_interview_preparation 表

面试准备清单表：

```sql
CREATE TABLE IF NOT EXISTS t_interview_preparation (
    id VARCHAR(64) PRIMARY KEY,
    interview_id VARCHAR(64) NOT NULL,           -- 关联的面试ID
    category VARCHAR(50) NOT NULL,               -- 分类（company/jd/skill/question/other）
    title VARCHAR(200) NOT NULL,                 -- 准备事项标题
    description TEXT,                            -- 详细描述
    priority VARCHAR(20) DEFAULT 'medium',       -- high/medium/low
    status VARCHAR(20) DEFAULT 'pending',        -- pending/in_progress/completed
    source VARCHAR(50),                          -- 来源（manual/ai_generated）
    due_date DATE,                               -- 截止日期
    completed_at DATETIME,                       -- 完成时间
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_interview_preparation_interview_id ON t_interview_preparation(interview_id);
```

### 2.4 新增 t_interview_review_note 表

面试复盘笔记表：

```sql
CREATE TABLE IF NOT EXISTS t_interview_review_note (
    id VARCHAR(64) PRIMARY KEY,
    interview_id VARCHAR(64) NOT NULL,           -- 关联的面试ID
    round_id VARCHAR(64),                        -- 关联的轮次ID（可选）
    note_type VARCHAR(50) NOT NULL,              -- 笔记类型（question_reflection/behavior_tip/tech_gap/improvement/other）
    title VARCHAR(200),                          -- 标题
    content TEXT NOT NULL,                       -- 内容
    tags TEXT,                                   -- 标签（JSON数组）
    is_ai_generated INTEGER DEFAULT 0,           -- 是否AI生成
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_interview_review_note_interview_id ON t_interview_review_note(interview_id);
```

---

## 三、后端模块设计

### 3.1 目录结构

```
backend/src/main/java/com/landit/interview/
├── entity/
│   ├── Interview.java                 # 扩展字段
│   ├── InterviewRound.java            # 新增
│   ├── InterviewPreparation.java      # 新增
│   └── InterviewReviewNote.java       # 新增
├── mapper/
│   ├── InterviewRoundMapper.java      # 新增
│   ├── InterviewPreparationMapper.java# 新增
│   └── InterviewReviewNoteMapper.java # 新增
├── service/
│   ├── InterviewRoundService.java     # 新增
│   ├── InterviewPreparationService.java# 新增
│   └── InterviewReviewNoteService.java# 新增
├── handler/
│   └── InterviewCenterHandler.java    # 新增
├── controller/
│   └── InterviewCenterController.java # 新增
├── dto/
│   ├── CreateRealInterviewRequest.java# 新增
│   ├── UpdateInterviewRequest.java    # 新增
│   ├── AddRoundRequest.java           # 新增
│   ├── UpdateRoundRequest.java        # 新增
│   ├── AddPreparationRequest.java     # 新增
│   ├── AddReviewNoteRequest.java      # 新增
│   ├── InterviewCenterVO.java         # 新增
│   ├── InterviewDetailVO.java         # 新增
│   ├── InterviewRoundVO.java          # 新增
│   ├── InterviewPreparationVO.java    # 新增
│   ├── InterviewReviewNoteVO.java     # 新增
│   ├── CompanyResearchResult.java     # 新增
│   └── JDAnalysisResult.java          # 新增
└── graph/
    ├── InterviewGraphConstants.java        # 新增（继承 BaseGraphConstants）
    ├── InterviewResearchGraphConfig.java   # 新增
    ├── InterviewResearchGraphService.java  # 新增
    ├── ResearchCompanyNode.java            # 新增
    ├── AnalyzeJDForInterviewNode.java      # 新增
    ├── GeneratePreparationNode.java        # 新增
    ├── InterviewReviewGraphConfig.java     # 新增
    ├── InterviewReviewGraphService.java    # 新增
    └── AnalyzeInterviewNode.java           # 新增
```

### 3.2 关键实体设计

#### Interview.java（扩展）

```java
package com.landit.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 面试记录实体类
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_interview", autoResultMap = true)
public class Interview extends BaseEntity {

    private String userId;

    private String type;

    // 新增字段
    private String interviewSource;  // mock/real

    private String position;

    private String company;

    private LocalDate date;

    private Integer duration;

    private Integer score;

    private String status;           // preparing/in_progress/completed/cancelled

    private Integer questions;

    private Integer correctAnswers;

    // 新增字段
    private String jdContent;

    private String overallResult;    // passed/failed/pending

    private String notes;

    private String companyResearch;  // JSON

    private String jdAnalysis;       // JSON
}
```

#### InterviewRound.java（新增）

```java
package com.landit.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 面试轮次实体类
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_interview_round", autoResultMap = true)
public class InterviewRound extends BaseEntity {

    private String interviewId;

    private Integer roundNumber;

    private String roundType;

    private String interviewerName;

    private LocalDateTime scheduledAt;

    private LocalDateTime actualStartAt;

    private LocalDateTime actualEndAt;

    private Integer duration;

    private String status;

    private String result;

    private String notes;

    private String transcript;

    private Integer score;

    private String feedback;
}
```

#### InterviewPreparation.java（新增）

```java
package com.landit.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 面试准备清单实体类
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_interview_preparation", autoResultMap = true)
public class InterviewPreparation extends BaseEntity {

    private String interviewId;

    private String category;

    private String title;

    private String description;

    private String priority;

    private String status;

    private String source;

    private LocalDate dueDate;

    private LocalDateTime completedAt;
}
```

#### InterviewReviewNote.java（新增）

```java
package com.landit.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 面试复盘笔记实体类
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_interview_review_note", autoResultMap = true)
public class InterviewReviewNote extends BaseEntity {

    private String interviewId;

    private String roundId;

    private String noteType;

    private String title;

    private String content;

    private String tags;

    private Integer isAiGenerated;
}
```

### 3.3 工作流设计

#### InterviewGraphConstants.java

继承 `BaseGraphConstants`，定义面试工作流常量：

```java
package com.landit.interview.graph;

import com.landit.resume.graph.BaseGraphConstants;

/**
 * 面试工作流常量定义
 *
 * @author Azir
 */
public class InterviewGraphConstants extends BaseGraphConstants {

    // ==================== Graph 名称 ====================

    public static final String GRAPH_INTERVIEW_RESEARCH = "interview_research";
    public static final String GRAPH_INTERVIEW_REVIEW = "interview_review";

    // ==================== 节点名称 ====================

    public static final String NODE_RESEARCH_COMPANY = "research_company";
    public static final String NODE_ANALYZE_JD = "analyze_jd";
    public static final String NODE_GENERATE_PREPARATION = "generate_preparation";
    public static final String NODE_ANALYZE_INTERVIEW = "analyze_interview";

    // ==================== 状态键 - 输入 ====================

    public static final String STATE_COMPANY_NAME = "company_name";
    public static final String STATE_JD_CONTENT = "jd_content";
    public static final String STATE_INTERVIEW_ID = "interview_id";
    public static final String STATE_TRANSCRIPT = "transcript";
    public static final String STATE_NOTES = "notes";
    public static final String STATE_ROUNDS_INFO = "rounds_info";

    // ==================== 状态键 - 输出 ====================

    public static final String STATE_COMPANY_RESEARCH = "company_research";
    public static final String STATE_JD_ANALYSIS = "jd_analysis";
    public static final String STATE_PREPARATION_ITEMS = "preparation_items";
    public static final String STATE_REVIEW_ANALYSIS = "review_analysis";
}
```

#### InterviewResearchGraph（公司调研+JD分析+准备清单）

```
START → ResearchCompany → AnalyzeJD → GeneratePreparation → END
         (公司调研)       (JD分析)    (生成准备清单)
```

**InterviewResearchGraphConfig.java** 参考 `TailorResumeGraphConfig` 结构：

```java
@Slf4j
@Configuration
@RequiredArgsConstructor
public class InterviewResearchGraphConfig {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;

    @Bean
    public KeyStrategyFactory interviewResearchKeyStrategyFactory() {
        return () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            // 输入
            strategies.put(STATE_COMPANY_NAME, new ReplaceStrategy());
            strategies.put(STATE_JD_CONTENT, new ReplaceStrategy());
            strategies.put(STATE_INTERVIEW_ID, new ReplaceStrategy());
            // 输出
            strategies.put(STATE_COMPANY_RESEARCH, new ReplaceStrategy());
            strategies.put(STATE_JD_ANALYSIS, new ReplaceStrategy());
            strategies.put(STATE_PREPARATION_ITEMS, new ReplaceStrategy());
            // 流程控制
            strategies.put(STATE_CURRENT_STEP, new ReplaceStrategy());
            strategies.put(STATE_MESSAGES, new AppendStrategy());
            strategies.put(STATE_NODE_OUTPUT, new ReplaceStrategy());
            return strategies;
        };
    }

    @Bean
    public CompiledGraph interviewResearchGraph(KeyStrategyFactory interviewResearchKeyStrategyFactory)
            throws GraphStateException {

        ResearchCompanyNode researchCompanyNode = new ResearchCompanyNode(chatClient, aiPromptProperties);
        AnalyzeJDForInterviewNode analyzeJDNode = new AnalyzeJDForInterviewNode(chatClient, aiPromptProperties);
        GeneratePreparationNode generatePreparationNode = new GeneratePreparationNode(chatClient, aiPromptProperties);

        StateGraph workflow = new StateGraph(GRAPH_INTERVIEW_RESEARCH, interviewResearchKeyStrategyFactory)
                .addNode(NODE_RESEARCH_COMPANY, AsyncNodeAction.node_async(researchCompanyNode))
                .addNode(NODE_ANALYZE_JD, AsyncNodeAction.node_async(analyzeJDNode))
                .addNode(NODE_GENERATE_PREPARATION, AsyncNodeAction.node_async(generatePreparationNode))
                .addEdge(START, NODE_RESEARCH_COMPANY)
                .addEdge(NODE_RESEARCH_COMPANY, NODE_ANALYZE_JD)
                .addEdge(NODE_ANALYZE_JD, NODE_GENERATE_PREPARATION)
                .addEdge(NODE_GENERATE_PREPARATION, END);

        MemorySaver memory = new MemorySaver();
        CompileConfig compileConfig = CompileConfig.builder()
                .saverConfig(SaverConfig.builder().register(memory).build())
                .build();

        return workflow.compile(compileConfig);
    }
}
```

#### InterviewReviewGraph（复盘AI分析）

```
START → AnalyzeInterview → END
         (AI分析面试记录)
```

### 3.4 Service 设计

#### InterviewRoundService.java

```java
package com.landit.interview.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.interview.entity.InterviewRound;
import com.landit.interview.mapper.InterviewRoundMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 面试轮次服务类
 *
 * @author Azir
 */
@Service
public class InterviewRoundService extends ServiceImpl<InterviewRoundMapper, InterviewRound> {

    public List<InterviewRound> getRoundsByInterviewId(String interviewId) {
        return lambdaQuery()
                .eq(InterviewRound::getInterviewId, interviewId)
                .orderByAsc(InterviewRound::getRoundNumber)
                .list();
    }

    public InterviewRound createRound(InterviewRound round) {
        Long count = lambdaQuery()
                .eq(InterviewRound::getInterviewId, round.getInterviewId())
                .count();
        round.setRoundNumber(count.intValue() + 1);
        save(round);
        return round;
    }

    public void updateRoundResult(String roundId, String result, String feedback) {
        lambdaUpdate()
                .eq(InterviewRound::getId, roundId)
                .set(InterviewRound::getResult, result)
                .set(feedback != null, InterviewRound::getFeedback, feedback)
                .update();
    }
}
```

#### InterviewPreparationService.java

```java
package com.landit.interview.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.interview.entity.InterviewPreparation;
import com.landit.interview.mapper.InterviewPreparationMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 面试准备清单服务类
 *
 * @author Azir
 */
@Service
public class InterviewPreparationService extends ServiceImpl<InterviewPreparationMapper, InterviewPreparation> {

    public List<InterviewPreparation> getPreparationsByInterviewId(String interviewId) {
        return lambdaQuery()
                .eq(InterviewPreparation::getInterviewId, interviewId)
                .orderByDesc(InterviewPreparation::getPriority)
                .orderByAsc(InterviewPreparation::getCreatedAt)
                .list();
    }

    public void updateStatus(String id, String status) {
        lambdaUpdate()
                .eq(InterviewPreparation::getId, id)
                .set(InterviewPreparation::getStatus, status)
                .set("completed".equals(status), InterviewPreparation::getCompletedAt, java.time.LocalDateTime.now())
                .update();
    }

    public void batchSave(List<InterviewPreparation> preparations) {
        saveBatch(preparations);
    }
}
```

### 3.5 Handler 设计

#### InterviewCenterHandler.java

```java
package com.landit.interview.handler;

import com.landit.interview.dto.CreateRealInterviewRequest;
import com.landit.interview.entity.Interview;
import com.landit.interview.entity.InterviewRound;
import com.landit.interview.graph.InterviewResearchGraphService;
import com.landit.interview.service.InterviewPreparationService;
import com.landit.interview.service.InterviewRoundService;
import com.landit.interview.service.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

import static com.landit.interview.graph.InterviewGraphConstants.*;

/**
 * 面试中心业务处理器
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewCenterHandler {

    private final InterviewService interviewService;
    private final InterviewRoundService roundService;
    private final InterviewPreparationService preparationService;
    private final InterviewResearchGraphService researchGraphService;

    /**
     * 创建真实面试
     */
    @Transactional(rollbackFor = Exception.class)
    public Interview createRealInterview(CreateRealInterviewRequest request) {
        Interview interview = new Interview();
        interview.setUserId(request.getUserId());
        interview.setInterviewSource("real");
        interview.setType(request.getType());
        interview.setPosition(request.getPosition());
        interview.setCompany(request.getCompany());
        interview.setDate(request.getDate());
        interview.setJdContent(request.getJdContent());
        interview.setStatus("preparing");
        interview.setOverallResult("pending");
        interviewService.save(interview);
        return interview;
    }

    /**
     * 触发公司调研（SSE）
     */
    public Flux<Map<String, Object>> triggerCompanyResearch(String interviewId, String companyName, String jdContent) {
        Map<String, Object> initialState = new HashMap<>();
        initialState.put(STATE_COMPANY_NAME, companyName);
        initialState.put(STATE_JD_CONTENT, jdContent != null ? jdContent : "");
        initialState.put(STATE_INTERVIEW_ID, interviewId);
        return researchGraphService.streamResearch(initialState, interviewId);
    }

    /**
     * 添加面试轮次
     */
    @Transactional(rollbackFor = Exception.class)
    public InterviewRound addInterviewRound(String interviewId, String roundType, String interviewerName) {
        InterviewRound round = new InterviewRound();
        round.setInterviewId(interviewId);
        round.setRoundType(roundType);
        round.setInterviewerName(interviewerName);
        round.setStatus("scheduled");
        return roundService.createRound(round);
    }

    /**
     * 更新面试状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateInterviewStatus(String interviewId, String status, String result) {
        interviewService.lambdaUpdate()
                .eq(Interview::getId, interviewId)
                .set(Interview::getStatus, status)
                .set(result != null, Interview::getOverallResult, result)
                .update();
    }
}
```

### 3.6 Controller 设计

#### InterviewCenterController.java

```java
package com.landit.interview.controller;

import com.landit.common.response.ApiResponse;
import com.landit.interview.dto.*;
import com.landit.interview.entity.Interview;
import com.landit.interview.entity.InterviewRound;
import com.landit.interview.handler.InterviewCenterHandler;
import com.landit.interview.service.InterviewPreparationService;
import com.landit.interview.service.InterviewRoundService;
import com.landit.interview.service.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 面试中心控制器
 *
 * @author Azir
 */
@Slf4j
@RestController
@RequestMapping("/interview-center")
@RequiredArgsConstructor
public class InterviewCenterController {

    private final InterviewCenterHandler interviewCenterHandler;
    private final InterviewService interviewService;
    private final InterviewRoundService roundService;
    private final InterviewPreparationService preparationService;

    /**
     * 获取面试列表
     */
    @GetMapping
    public ApiResponse<List<Interview>> getList(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String status) {
        List<Interview> list = interviewService.lambdaQuery()
                .eq(source != null, Interview::getInterviewSource, source)
                .eq(status != null, Interview::getStatus, status)
                .orderByDesc(Interview::getCreatedAt)
                .list();
        return ApiResponse.success(list);
    }

    /**
     * 创建真实面试
     */
    @PostMapping("/real")
    public ApiResponse<Interview> createRealInterview(@RequestBody @Valid CreateRealInterviewRequest request) {
        Interview interview = interviewCenterHandler.createRealInterview(request);
        return ApiResponse.success(interview);
    }

    /**
     * 获取面试详情
     */
    @GetMapping("/{id}")
    public ApiResponse<InterviewDetailVO> getDetail(@PathVariable String id) {
        Interview interview = interviewService.getById(id);
        List<InterviewRound> rounds = roundService.getRoundsByInterviewId(id);
        List<InterviewPreparationVO> preparations = preparationService.getPreparationsByInterviewId(id)
                .stream()
                .map(this::toPreparationVO)
                .toList();

        InterviewDetailVO vo = new InterviewDetailVO();
        vo.setInterview(interview);
        vo.setRounds(rounds);
        vo.setPreparations(preparations);
        return ApiResponse.success(vo);
    }

    /**
     * 触发公司调研（SSE）
     */
    @GetMapping(value = "/{id}/research/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamCompanyResearch(@PathVariable String id) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        Interview interview = interviewService.getById(id);

        interviewCenterHandler.triggerCompanyResearch(id, interview.getCompany(), interview.getJdContent())
                .subscribe(
                        output -> emitter.send(SseEmitter.event().name("message").data(output)),
                        emitter::completeWithError,
                        emitter::complete
                );

        return emitter;
    }

    /**
     * 添加面试轮次
     */
    @PostMapping("/{id}/rounds")
    public ApiResponse<InterviewRound> addRound(
            @PathVariable String id,
            @RequestBody @Valid AddRoundRequest request) {
        InterviewRound round = interviewCenterHandler.addInterviewRound(
                id, request.getRoundType(), request.getInterviewerName()
        );
        return ApiResponse.success(round);
    }

    /**
     * 更新准备事项状态
     */
    @PutMapping("/{id}/preparations/{prepId}")
    public ApiResponse<Void> updatePreparationStatus(
            @PathVariable String id,
            @PathVariable String prepId,
            @RequestBody UpdatePreparationStatusRequest request) {
        preparationService.updateStatus(prepId, request.getStatus());
        return ApiResponse.success();
    }

    /**
     * 更新面试状态
     */
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(
            @PathVariable String id,
            @RequestBody @Valid UpdateStatusRequest request) {
        interviewCenterHandler.updateInterviewStatus(id, request.getStatus(), request.getResult());
        return ApiResponse.success();
    }

    private InterviewPreparationVO toPreparationVO(com.landit.interview.entity.InterviewPreparation prep) {
        InterviewPreparationVO vo = new InterviewPreparationVO();
        vo.setId(prep.getId());
        vo.setInterviewId(prep.getInterviewId());
        vo.setCategory(prep.getCategory());
        vo.setTitle(prep.getTitle());
        vo.setDescription(prep.getDescription());
        vo.setPriority(prep.getPriority());
        vo.setStatus(prep.getStatus());
        vo.setSource(prep.getSource());
        return vo;
    }
}
```

---

## 四、前端设计

### 4.1 路由改造

```typescript
// router/index.ts 改造
const routes: RouteRecordRaw[] = [
  // ... 其他路由

  // 面试中心（合并后）
  {
    path: '/interview-center',
    name: 'InterviewCenter',
    component: () => import('@/views/interview-center/InterviewCenter.vue'),
    meta: { title: '面试中心' },
    children: [
      {
        path: '',
        name: 'InterviewList',
        component: () => import('@/views/interview-center/InterviewList.vue'),
        meta: { title: '面试列表' }
      },
      {
        path: 'mock',
        name: 'MockInterview',
        component: () => import('@/views/interview-center/MockInterview.vue'),
        meta: { title: '模拟面试' }
      },
      {
        path: ':id',
        name: 'InterviewDetail',
        component: () => import('@/views/interview-center/InterviewDetail.vue'),
        meta: { title: '面试详情' }
      },
      {
        path: ':id/session',
        name: 'InterviewSession',
        component: () => import('@/views/InterviewSession.vue'),
        meta: { title: '面试进行中' }
      }
    ]
  },

  // 保留复盘独立路由（兼容）
  {
    path: '/review',
    redirect: '/interview-center?tab=reviews'
  },
  {
    path: '/review/:id',
    redirect: '/interview-center/:id?tab=review'
  }
]
```

### 4.2 导航改造

```vue
<!-- components/common/AppNavbar.vue -->
<script setup lang="ts">
const navItems: NavItem[] = [
  {
    key: 'home',
    label: '工作台',
    path: '/'
  },
  {
    key: 'resume',
    label: '简历管理',
    path: '/resume'
  },
  {
    key: 'interview-center',  // 合并后的入口
    label: '面试中心',
    path: '/interview-center',
    badge: 'NEW'
  }
  // 移除原来的 'interview' 和 'review'
]
</script>
```

### 4.3 目录结构

```
frontend/src/views/interview-center/
├── InterviewCenter.vue              # 主容器
├── InterviewList.vue                # 面试列表（含 Tab: 全部/真实/模拟）
├── MockInterview.vue                # 模拟面试入口
├── InterviewDetail.vue              # 面试详情页（核心，5个Tab）
└── components/
    ├── InterviewCard.vue            # 面试卡片
    ├── StatusBadge.vue              # 状态徽章
    ├── PreparationSection.vue       # 准备区
    │   ├── CompanyResearchCard.vue  # 公司调研卡片
    │   ├── JDAnalysisCard.vue       # JD分析卡片
    │   └── PreparationList.vue      # 准备清单
    ├── RoundsSection.vue            # 轮次区
    │   ├── RoundTimeline.vue        # 轮次时间线
    │   └── RoundCard.vue            # 轮次卡片
    ├── RecordSection.vue            # 记录区
    │   ├── NoteEditor.vue           # 笔记编辑器
    │   └── VoiceRecorder.vue        # 语音记录
    ├── ResultSection.vue            # 结果跟踪区
    │   └── ResultForm.vue           # 结果表单
    └── ReviewSection.vue            # 复盘总结区
        ├── AIAnalysisCard.vue       # AI分析卡片
        ├── ReviewNotes.vue          # 复盘笔记
        └── ImprovementPlan.vue      # 改进计划
```

### 4.4 Composables

```
frontend/src/composables/
├── useInterviewCenter.ts         # 面试中心状态管理
├── usePreparation.ts             # 准备清单管理
├── useInterviewRounds.ts         # 轮次管理
└── useInterviewReview.ts         # 复盘管理
```

#### useInterviewCenter.ts

```typescript
// composables/useInterviewCenter.ts
import { ref, computed } from 'vue'
import { interviewCenterApi } from '@/api/interview-center'
import type {
  Interview,
  InterviewRound,
  InterviewPreparation,
  InterviewReviewNote,
  InterviewReview,
  CreateRealInterviewRequest,
  AddRoundRequest,
  UpdateRoundRequest,
  UpdateStatusRequest
} from '@/types/interview-center'

export function useInterviewCenter(interviewId?: Ref<string>) {
  const interview = ref<Interview | null>(null)
  const rounds = ref<InterviewRound[]>([])
  const preparations = ref<InterviewPreparation[]>([])
  const reviewNotes = ref<InterviewReviewNote[]>([])
  const review = ref<InterviewReview | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 获取面试列表
  async function fetchList(source?: string, status?: string) {
    loading.value = true
    try {
      return await interviewCenterApi.getList({ source, status })
    } catch (e: any) {
      error.value = e.message
      return []
    } finally {
      loading.value = false
    }
  }

  // 获取面试详情
  async function fetchDetail(id: string) {
    loading.value = true
    try {
      const data = await interviewCenterApi.getDetail(id)
      interview.value = data.interview
      rounds.value = data.rounds
      preparations.value = data.preparations
      reviewNotes.value = data.reviewNotes
      review.value = data.review
    } catch (e: any) {
      error.value = e.message
    } finally {
      loading.value = false
    }
  }

  // 创建真实面试
  async function createRealInterview(data: CreateRealInterviewRequest) {
    return await interviewCenterApi.createRealInterview(data)
  }

  // 触发公司调研（SSE）
  function triggerResearch(id: string): EventSource {
    return new EventSource(`/landit/interview-center/${id}/research/stream`)
  }

  // 更新准备事项状态
  async function updatePreparation(interviewId: string, prepId: string, status: string) {
    await interviewCenterApi.updatePreparationStatus(interviewId, prepId, { status })
    const index = preparations.value.findIndex(p => p.id === prepId)
    if (index !== -1) {
      preparations.value[index].status = status
    }
  }

  // 添加轮次
  async function addRound(id: string, data: AddRoundRequest) {
    const round = await interviewCenterApi.addRound(id, data)
    rounds.value.push(round)
    return round
  }

  // 更新轮次
  async function updateRound(interviewId: string, roundId: string, data: UpdateRoundRequest) {
    await interviewCenterApi.updateRound(interviewId, roundId, data)
    const index = rounds.value.findIndex(r => r.id === roundId)
    if (index !== -1) {
      Object.assign(rounds.value[index], data)
    }
  }

  // 更新面试状态
  async function updateStatus(id: string, data: UpdateStatusRequest) {
    await interviewCenterApi.updateStatus(id, data)
    if (interview.value) {
      interview.value.status = data.status
      if (data.result) {
        interview.value.overallResult = data.result
      }
    }
  }

  return {
    interview,
    rounds,
    preparations,
    reviewNotes,
    review,
    loading,
    error,
    fetchList,
    fetchDetail,
    createRealInterview,
    triggerResearch,
    updatePreparation,
    addRound,
    updateRound,
    updateStatus
  }
}
```

### 4.5 Types

```typescript
// types/interview-center.ts

export type InterviewSource = 'mock' | 'real'
export type InterviewStatus = 'preparing' | 'in_progress' | 'completed' | 'cancelled'
export type OverallResult = 'passed' | 'failed' | 'pending'
export type RoundType = 'technical_1' | 'technical_2' | 'hr' | 'director' | 'cto' | 'final' | 'custom'
export type RoundStatus = 'scheduled' | 'in_progress' | 'completed' | 'cancelled'
export type PreparationCategory = 'company' | 'jd' | 'skill' | 'question' | 'other'
export type Priority = 'high' | 'medium' | 'low'
export type PreparationStatus = 'pending' | 'in_progress' | 'completed'
export type NoteType = 'question_reflection' | 'behavior_tip' | 'tech_gap' | 'improvement' | 'other'

export interface Interview {
  id: string
  userId: string
  type: string
  interviewSource: InterviewSource
  position: string
  company: string
  date: string
  duration: number
  score: number
  status: InterviewStatus
  questions: number
  correctAnswers: number
  jdContent: string
  overallResult: OverallResult
  notes: string
  companyResearch: CompanyResearchResult | null
  jdAnalysis: JDAnalysisResult | null
  createdAt: string
  updatedAt: string
}

export interface InterviewRound {
  id: string
  interviewId: string
  roundNumber: number
  roundType: RoundType
  interviewerName: string
  scheduledAt: string
  actualStartAt: string
  actualEndAt: string
  duration: number
  status: RoundStatus
  result: OverallResult
  notes: string
  transcript: Conversation[]
  score: number
  feedback: string
}

export interface InterviewPreparation {
  id: string
  interviewId: string
  category: PreparationCategory
  title: string
  description: string
  priority: Priority
  status: PreparationStatus
  source: 'manual' | 'ai_generated'
  dueDate: string
  completedAt: string
}

export interface InterviewReviewNote {
  id: string
  interviewId: string
  roundId: string
  noteType: NoteType
  title: string
  content: string
  tags: string[]
  isAiGenerated: boolean
}

export interface CompanyResearchResult {
  introduction: string
  mainBusiness: string[]
  interviewStyle: string
  commonQuestions: string[]
  culture: string
  tips: string[]
}

export interface JDAnalysisResult {
  requiredSkills: string[]
  preferredSkills: string[]
  responsibilities: string[]
  keywords: string[]
  experienceLevel: string
  education: string
  suggestedPreparations: string[]
}

export interface InterviewDetailVO {
  interview: Interview
  rounds: InterviewRound[]
  preparations: InterviewPreparation[]
  reviewNotes: InterviewReviewNote[]
  review: InterviewReview | null
}

// Request DTOs
export interface CreateRealInterviewRequest {
  userId: string
  type: string
  position: string
  company: string
  date: string
  jdContent?: string
}

export interface AddRoundRequest {
  roundType: RoundType
  interviewerName?: string
  scheduledAt?: string
}

export interface UpdateRoundRequest {
  actualStartAt?: string
  actualEndAt?: string
  duration?: number
  status?: RoundStatus
  result?: OverallResult
  notes?: string
  score?: number
  feedback?: string
}

export interface UpdateStatusRequest {
  status: InterviewStatus
  result?: OverallResult
}

export interface UpdatePreparationStatusRequest {
  status: PreparationStatus
}
```

### 4.6 API

```typescript
// api/interview-center.ts
import type { ApiResponse } from '@/types'
import type {
  Interview,
  InterviewDetailVO,
  InterviewRound,
  InterviewPreparation,
  CreateRealInterviewRequest,
  AddRoundRequest,
  UpdateRoundRequest,
  UpdateStatusRequest,
  UpdatePreparationStatusRequest
} from '@/types/interview-center'

const API_BASE = '/landit/interview-center'

export const interviewCenterApi = {
  // 列表
  async getList(params: { source?: string; status?: string }): Promise<Interview[]> {
    const query = new URLSearchParams()
    if (params.source) query.append('source', params.source)
    if (params.status) query.append('status', params.status)
    const response = await fetch(`${API_BASE}?${query}`)
    const result: ApiResponse<Interview[]> = await response.json()
    if (result.code !== 200) throw new Error(result.message)
    return result.data
  },

  // 创建真实面试
  async createRealInterview(data: CreateRealInterviewRequest): Promise<Interview> {
    const response = await fetch(`${API_BASE}/real`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    const result: ApiResponse<Interview> = await response.json()
    if (result.code !== 200) throw new Error(result.message)
    return result.data
  },

  // 获取详情
  async getDetail(id: string): Promise<InterviewDetailVO> {
    const response = await fetch(`${API_BASE}/${id}`)
    const result: ApiResponse<InterviewDetailVO> = await response.json()
    if (result.code !== 200) throw new Error(result.message)
    return result.data
  },

  // 更新准备事项状态
  async updatePreparationStatus(
    interviewId: string,
    prepId: string,
    data: UpdatePreparationStatusRequest
  ): Promise<void> {
    const response = await fetch(`${API_BASE}/${interviewId}/preparations/${prepId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    const result: ApiResponse<void> = await response.json()
    if (result.code !== 200) throw new Error(result.message)
  },

  // 添加轮次
  async addRound(interviewId: string, data: AddRoundRequest): Promise<InterviewRound> {
    const response = await fetch(`${API_BASE}/${interviewId}/rounds`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    const result: ApiResponse<InterviewRound> = await response.json()
    if (result.code !== 200) throw new Error(result.message)
    return result.data
  },

  // 更新轮次
  async updateRound(
    interviewId: string,
    roundId: string,
    data: UpdateRoundRequest
  ): Promise<void> {
    const response = await fetch(`${API_BASE}/${interviewId}/rounds/${roundId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    const result: ApiResponse<void> = await response.json()
    if (result.code !== 200) throw new Error(result.message)
  },

  // 删除轮次
  async deleteRound(interviewId: string, roundId: string): Promise<void> {
    const response = await fetch(`${API_BASE}/${interviewId}/rounds/${roundId}`, {
      method: 'DELETE'
    })
    const result: ApiResponse<void> = await response.json()
    if (result.code !== 200) throw new Error(result.message)
  },

  // 更新面试状态
  async updateStatus(interviewId: string, data: UpdateStatusRequest): Promise<void> {
    const response = await fetch(`${API_BASE}/${interviewId}/status`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    const result: ApiResponse<void> = await response.json()
    if (result.code !== 200) throw new Error(result.message)
  }
}
```

---

## 五、API 接口汇总

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/interview-center` | 面试列表（支持 source/status 筛选） |
| POST | `/interview-center/real` | 创建真实面试 |
| GET | `/interview-center/{id}` | 面试详情（含轮次、准备清单） |
| PUT | `/interview-center/{id}` | 更新面试基本信息 |
| DELETE | `/interview-center/{id}` | 删除面试 |
| GET | `/interview-center/{id}/research/stream` | SSE 公司调研 |
| GET | `/interview-center/{id}/jd-analysis/stream` | SSE JD分析 |
| GET | `/interview-center/{id}/preparations` | 获取准备清单 |
| PUT | `/interview-center/{id}/preparations/{prepId}` | 更新准备事项状态 |
| POST | `/interview-center/{id}/preparations` | 手动添加准备事项 |
| POST | `/interview-center/{id}/rounds` | 添加面试轮次 |
| GET | `/interview-center/{id}/rounds` | 获取轮次列表 |
| PUT | `/interview-center/{id}/rounds/{roundId}` | 更新轮次信息 |
| DELETE | `/interview-center/{id}/rounds/{roundId}` | 删除轮次 |
| PUT | `/interview-center/{id}/status` | 更新面试状态/结果 |
| POST | `/interview-center/{id}/notes` | 添加面试笔记 |
| GET | `/interview-center/{id}/review-analysis/stream` | SSE AI复盘分析 |
| POST | `/interview-center/{id}/review-notes` | 添加复盘笔记 |

---

## 六、实现分期

### Phase 1 - 核心框架（MVP）- 2周

**目标**：完成数据模型改造和基础CRUD

1. **数据库改造**
   - 扩展 t_interview 表字段
   - 创建 t_interview_round 表
   - 创建 t_interview_preparation 表
   - 创建 t_interview_review_note 表

2. **后端基础**
   - Entity 扩展/新增
   - Mapper 新增
   - Service 基础CRUD
   - InterviewCenterController 基础API

3. **前端基础**
   - 路由改造
   - 导航合并
   - InterviewList 页面
   - InterviewDetail 基础结构

### Phase 2 - AI工作流集成 - 2周

**目标**：完成AI分析和准备清单生成

1. **后端工作流**
   - InterviewResearchGraph（公司调研）
   - JDAnalysisGraph（JD分析）
   - PreparationTodoGraph（准备事项生成）
   - SSE 流式接口

2. **前端集成**
   - PreparationSection 组件
   - CompanyResearchCard 组件
   - JDAnalysisCard 组件
   - PreparationList 组件
   - SSE 事件处理

### Phase 3 - 轮次管理和结果跟踪 - 1.5周

**目标**：支持多轮面试和状态流转

1. **后端**
   - InterviewRoundService 完善
   - 状态流转逻辑
   - 结果统计

2. **前端**
   - RoundsSection 组件
   - RoundTimeline 组件
   - ResultSection 组件
   - StatusBadge 组件

### Phase 4 - 复盘增强 - 1.5周

**目标**：完成AI复盘分析和笔记管理

1. **后端**
   - InterviewReviewGraph（复盘AI分析）
   - InterviewReviewNoteService
   - 扩展 InterviewReview 实体

2. **前端**
   - ReviewSection 组件
   - AIAnalysisCard 组件
   - ReviewNotes 组件
   - ImprovementPlan 组件

### Phase 5 - 优化和测试 - 1周

**目标**：完善用户体验和稳定性

1. **优化**
   - 前端性能优化
   - 错误处理完善
   - 加载状态优化

2. **测试**
   - 单元测试补充
   - 集成测试
   - E2E 测试

---

## 七、关键设计决策

### 7.1 数据模型设计

| 决策 | 理由 |
|------|------|
| 保留 t_interview 作为主表 | 兼容现有模拟面试功能，最小化改动 |
| 新增 t_interview_round | 支持真实面试的多轮次管理 |
| 使用 JSON 存储复杂结构 | 灵活扩展，避免过度规范化 |
| interview_source 区分来源 | 清晰区分模拟面试和真实面试 |

### 7.2 工作流设计

| 决策 | 理由 |
|------|------|
| 独立 Graph 而非复用现有 | 业务逻辑独立，避免耦合 |
| SSE 流式输出 | 实时反馈，提升用户体验 |
| MemorySaver 持久化 | 支持断点续传 |
| 继承 BaseGraphConstants | 保持架构一致性 |

### 7.3 前端架构

| 决策 | 理由 |
|------|------|
| 合并为单一入口 | 简化用户心智模型 |
| Tab 切换而非多页面 | 减少页面跳转，提升流畅度 |
| Composables 封装 | 逻辑复用，状态管理清晰 |
| 保留旧路由兼容 | 平滑迁移，不影响现有用户 |

---

## 八、关键文件清单

### 后端关键文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/src/main/java/com/landit/interview/entity/Interview.java` | 修改 | 扩展字段 |
| `backend/src/main/java/com/landit/interview/entity/InterviewRound.java` | 新增 | 面试轮次实体 |
| `backend/src/main/java/com/landit/interview/entity/InterviewPreparation.java` | 新增 | 准备清单实体 |
| `backend/src/main/java/com/landit/interview/entity/InterviewReviewNote.java` | 新增 | 复盘笔记实体 |
| `backend/src/main/java/com/landit/interview/graph/InterviewGraphConstants.java` | 新增 | 继承 BaseGraphConstants |
| `backend/src/main/java/com/landit/interview/graph/InterviewResearchGraphConfig.java` | 新增 | 参考 TailorResumeGraphConfig |
| `backend/src/main/resources/schema.sql` | 修改 | 新增表结构 |

### 前端关键文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `frontend/src/router/index.ts` | 修改 | 路由改造 |
| `frontend/src/components/common/AppNavbar.vue` | 修改 | 导航合并 |
| `frontend/src/views/interview-center/InterviewDetail.vue` | 新增 | 核心页面 |
| `frontend/src/composables/useInterviewCenter.ts` | 新增 | 状态管理 |
| `frontend/src/types/interview-center.ts` | 新增 | 类型定义 |
| `frontend/src/api/interview-center.ts` | 新增 | API 封装 |

---

## 九、验证方案

### 功能验证

1. 创建真实面试，验证表单校验和数据持久化
2. 多轮面试的 CRUD 操作和状态流转
3. 准备清单的增删改查和完成状态
4. AI 工作流的触发和结果展示（SSE）
5. 面试列表的筛选和排序
6. 复盘记录的保存和 AI 分析

### 技术验证

```bash
# 后端编译
cd backend && mvn clean compile

# 前端类型检查
cd frontend && npm run type-check

# 前端构建
cd frontend && npm run build

# 手动走通完整面试生命周期流程
```

---

## 十、风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 现有面试功能兼容性 | 模拟面试数据可能受影响 | interview_source 默认值 'mock'，旧数据自动归类 |
| AI 工作流延迟 | SSE 连接超时 | 5分钟超时 + 重试机制 |
| 前端路由迁移 | 用户收藏夹失效 | 保留旧路由重定向 |
| 数据库迁移 | SQLite ALTER TABLE 限制 | 使用 CREATE TABLE + 数据迁移 |
