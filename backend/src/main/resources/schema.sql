-- ============================================================================
-- LandIt SQLite 数据库初始化脚本
-- @author Azir
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 用户表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_user (
    id VARCHAR(64) PRIMARY KEY,              -- 主键ID（雪花ID字符串）
    name VARCHAR(100),                      -- 用户姓名
    gender VARCHAR(10),                     -- 性别（MALE-男 FEMALE-女）
    avatar VARCHAR(500),                    -- 头像URL
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    deleted INTEGER DEFAULT 0               -- 逻辑删除标记（0-未删除 1-已删除）
);

-- ----------------------------------------------------------------------------
-- 简历表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_resume (
    id VARCHAR(64) PRIMARY KEY,              -- 主键ID（雪花ID字符串）
    user_id VARCHAR(64) NOT NULL,            -- 所属用户ID
    name VARCHAR(200),                      -- 简历名称
    target_position VARCHAR(100),           -- 目标岗位
    markdown_content TEXT,                  -- 简历原文本（Markdown格式）
    -- 简历类型与派生关系
    resume_type VARCHAR(20) DEFAULT 'PRIMARY',  -- 简历类型（PRIMARY-主简历 DERIVED-派生简历）
    source_resume_id VARCHAR(64),            -- 派生来源简历ID（为空表示主简历）
    -- 版本控制
    version INTEGER DEFAULT 1,              -- 当前版本号
    -- 状态与评分
    status VARCHAR(20) DEFAULT 'draft',     -- 简历状态（draft-草稿 optimized-已优化）
    score INTEGER DEFAULT 0,                -- 综合评分（0-100）
    completeness INTEGER DEFAULT 0,         -- 完整度（0-100）
    is_primary INTEGER DEFAULT 0,           -- 是否主简历（0-否 1-是）
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    deleted INTEGER DEFAULT 0               -- 逻辑删除标记
);

-- ----------------------------------------------------------------------------
-- 简历诊断评分字段扩展
-- ----------------------------------------------------------------------------
ALTER TABLE t_resume ADD COLUMN overall_score INTEGER DEFAULT 0;
ALTER TABLE t_resume ADD COLUMN content_score INTEGER DEFAULT 0;
ALTER TABLE t_resume ADD COLUMN structure_score INTEGER DEFAULT 0;
ALTER TABLE t_resume ADD COLUMN matching_score INTEGER DEFAULT 0;
ALTER TABLE t_resume ADD COLUMN competitiveness_score INTEGER DEFAULT 0;

-- ----------------------------------------------------------------------------
-- 简历职位描述字段（用于定制简历）
-- ----------------------------------------------------------------------------
ALTER TABLE t_resume ADD COLUMN job_description TEXT;

-- ----------------------------------------------------------------------------
-- 简历历史版本表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_resume_version (
    id VARCHAR(64) PRIMARY KEY,              -- 主键ID（雪花ID字符串）
    resume_id VARCHAR(64) NOT NULL,          -- 关联的主简历ID
    version INTEGER NOT NULL,               -- 版本号
    -- 冗余存储完整简历数据（快照）
    name VARCHAR(200),                      -- 简历名称
    target_position VARCHAR(100),           -- 目标岗位
    status VARCHAR(20),                     -- 简历状态
    score INTEGER DEFAULT 0,                -- 综合评分
    completeness INTEGER DEFAULT 0,         -- 完整度
    -- 变更信息
    change_summary VARCHAR(500),            -- 本次变更说明
    change_type VARCHAR(20),                -- 变更类型（MANUAL/AI_OPTIMIZE/DERIVE/ROLLBACK）
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    deleted INTEGER DEFAULT 0               -- 逻辑删除标记
);

-- ----------------------------------------------------------------------------
-- 简历模块表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_resume_section (
    id VARCHAR(64) PRIMARY KEY,              -- 主键ID（雪花ID字符串）
    resume_id VARCHAR(64) NOT NULL,          -- 所属简历ID
    resume_version_id VARCHAR(64),           -- 关联版本ID（为空表示当前版本）
    type VARCHAR(50),                       -- 模块类型（basic/experience/project/skill等）
    title VARCHAR(100),                     -- 模块标题
    content TEXT,                           -- 模块内容（JSON格式）
    score INTEGER DEFAULT 0,                -- 模块评分（0-100）
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    deleted INTEGER DEFAULT 0               -- 逻辑删除标记
);

-- ----------------------------------------------------------------------------
-- 简历优化建议表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_resume_suggestion (
    id VARCHAR(64) PRIMARY KEY,              -- 主键ID（雪花ID字符串）
    resume_id VARCHAR(64),                   -- 所属简历ID
    section_id VARCHAR(64),                  -- 关联的简历模块ID
    type VARCHAR(20),                       -- 建议类型（critical-关键 improvement-改进 enhancement-增强）
    category VARCHAR(50),                   -- 建议分类
    title VARCHAR(200),                     -- 建议标题
    description TEXT,                       -- 详细描述
    impact VARCHAR(20),                     -- 影响程度（高/中/低）
    position VARCHAR(200),                  -- 建议位置
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    deleted INTEGER DEFAULT 0               -- 逻辑删除标记
);

-- 为 section_id 添加索引
-- 注意：SQLite 不支持 IF NOT EXISTS 用于 CREATE INDEX，需要使用更安全的方式
-- ALTER TABLE t_resume_suggestion ADD COLUMN section_id VARCHAR(64);

-- ----------------------------------------------------------------------------
-- 面试记录表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_interview (
    id VARCHAR(64) PRIMARY KEY,              -- 主键ID（雪花ID字符串）
    user_id VARCHAR(64),                     -- 所属用户ID
    type VARCHAR(20),                       -- 面试类型（technical-技术 behavioral-行为）
    position VARCHAR(100),                  -- 面试岗位
    company VARCHAR(100),                   -- 面试公司
    date DATE,                              -- 面试日期
    duration INTEGER DEFAULT 0,             -- 面试时长（分钟）
    score INTEGER DEFAULT 0,                -- 面试得分（0-100）
    status VARCHAR(20) DEFAULT 'in_progress', -- 面试状态（in_progress-进行中 completed-已完成）
    questions INTEGER DEFAULT 0,            -- 问题总数
    correct_answers INTEGER DEFAULT 0,      -- 正确回答数
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    deleted INTEGER DEFAULT 0               -- 逻辑删除标记
);

-- ----------------------------------------------------------------------------
-- 面试题目表（题库）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_interview_question (
    id VARCHAR(64) PRIMARY KEY,              -- 主键ID（雪花ID字符串）
    type VARCHAR(20),                       -- 题目类型（technical-技术 behavioral-行为）
    category VARCHAR(50),                   -- 题目分类（如Vue.js、React等）
    difficulty VARCHAR(20),                 -- 题目难度（easy-简单 medium-中等 hard-困难）
    question TEXT,                          -- 题目内容
    follow_up TEXT,                         -- 追问内容
    key_points TEXT,                        -- 关键要点（JSON数组）
    sample_answer TEXT,                     -- 参考答案
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    deleted INTEGER DEFAULT 0               -- 逻辑删除标记
);

-- ----------------------------------------------------------------------------
-- 面试会话表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_interview_session (
    id VARCHAR(64) PRIMARY KEY,              -- 主键ID（雪花ID字符串）
    user_id VARCHAR(64),                     -- 所属用户ID
    type VARCHAR(20),                       -- 面试类型（technical-技术 behavioral-行为）
    position VARCHAR(100),                  -- 目标岗位
    status VARCHAR(20) DEFAULT 'in_progress', -- 会话状态（in_progress-进行中 completed-已完成）
    current_question_index INTEGER DEFAULT 0, -- 当前问题序号
    total_questions INTEGER DEFAULT 10,     -- 问题总数
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    deleted INTEGER DEFAULT 0               -- 逻辑删除标记
);

-- ----------------------------------------------------------------------------
-- 面试对话记录表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_conversation (
    id VARCHAR(64) PRIMARY KEY,              -- 主键ID（雪花ID字符串）
    session_id VARCHAR(64),                  -- 所属会话ID
    role VARCHAR(20),                       -- 对话角色（interviewer-面试官 candidate-候选人）
    content TEXT,                           -- 对话内容
    timestamp DATETIME,                     -- 时间戳
    score INTEGER,                          -- 评分（仅候选人回答）
    feedback TEXT,                          -- 反馈（仅候选人回答）
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    deleted INTEGER DEFAULT 0               -- 逻辑删除标记
);

-- ----------------------------------------------------------------------------
-- 面试复盘表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_interview_review (
    id VARCHAR(64) PRIMARY KEY,              -- 主键ID（雪花ID字符串）
    interview_id VARCHAR(64),                -- 关联的面试ID
    user_id VARCHAR(64),                     -- 所属用户ID
    overall_score INTEGER DEFAULT 0,        -- 总体评分（0-100）
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    deleted INTEGER DEFAULT 0               -- 逻辑删除标记
);

-- ----------------------------------------------------------------------------
-- 复盘维度表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_review_dimension (
    id VARCHAR(64) PRIMARY KEY,              -- 主键ID（雪花ID字符串）
    review_id VARCHAR(64),                   -- 所属复盘ID
    name VARCHAR(50),                       -- 维度名称（技术深度/表达能力/项目经验等）
    score INTEGER DEFAULT 0,                -- 得分
    max_score INTEGER DEFAULT 100,          -- 满分
    feedback TEXT,                          -- 反馈说明
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    deleted INTEGER DEFAULT 0               -- 逻辑删除标记
);

-- ----------------------------------------------------------------------------
-- 问题分析表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_question_analysis (
    id VARCHAR(64) PRIMARY KEY,              -- 主键ID（雪花ID字符串）
    review_id VARCHAR(64),                   -- 所属复盘ID
    question TEXT,                          -- 问题内容
    your_answer TEXT,                       -- 用户的回答
    score INTEGER DEFAULT 0,                -- 得分（0-100）
    key_points_covered TEXT,                -- 已覆盖要点（JSON数组）
    key_points_missed TEXT,                 -- 遗漏要点（JSON数组）
    suggestion TEXT,                        -- 改进建议
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    deleted INTEGER DEFAULT 0               -- 逻辑删除标记
);

-- ----------------------------------------------------------------------------
-- 改进计划表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_improvement_plan (
    id VARCHAR(64) PRIMARY KEY,              -- 主键ID（雪花ID字符串）
    review_id VARCHAR(64),                   -- 所属复盘ID
    category VARCHAR(50),                   -- 计划分类（技术深化/项目经验等）
    items TEXT,                             -- 改进项列表（JSON数组）
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    deleted INTEGER DEFAULT 0               -- 逻辑删除标记
);

-- ----------------------------------------------------------------------------
-- 职位推荐表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_job (
    id VARCHAR(64) PRIMARY KEY,              -- 主键ID（雪花ID字符串）
    company VARCHAR(100),                   -- 公司名称
    company_logo VARCHAR(500),              -- 公司Logo URL
    position VARCHAR(100),                  -- 职位名称
    salary VARCHAR(50),                     -- 薪资范围
    location VARCHAR(100),                  -- 工作地点
    experience VARCHAR(50),                 -- 经验要求
    education VARCHAR(50),                  -- 学历要求
    tags TEXT,                              -- 技能标签（JSON数组）
    match_score INTEGER DEFAULT 0,          -- 匹配度评分（0-100）
    published_at DATETIME,                  -- 发布时间
    description TEXT,                       -- 职位描述
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    deleted INTEGER DEFAULT 0               -- 逻辑删除标记
);

-- ============================================================================
-- 索引
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_resume_user_id ON t_resume(user_id);
CREATE INDEX IF NOT EXISTS idx_resume_source_id ON t_resume(source_resume_id);
CREATE INDEX IF NOT EXISTS idx_resume_version_resume_id ON t_resume_version(resume_id);
CREATE INDEX IF NOT EXISTS idx_resume_section_version_id ON t_resume_section(resume_version_id);
CREATE INDEX IF NOT EXISTS idx_interview_user_id ON t_interview(user_id);
CREATE INDEX IF NOT EXISTS idx_review_interview_id ON t_interview_review(interview_id);
CREATE INDEX IF NOT EXISTS idx_session_user_id ON t_interview_session(user_id);
CREATE INDEX IF NOT EXISTS idx_conversation_session_id ON t_conversation(session_id);
CREATE INDEX IF NOT EXISTS idx_suggestion_section_id ON t_resume_suggestion(section_id);

-- ----------------------------------------------------------------------------
-- AI聊天消息表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_chat_message (
    id VARCHAR(64) PRIMARY KEY,              -- 主键ID（雪花ID字符串）
    resume_id VARCHAR(64),                   -- 关联的简历ID（可选，通用聊天模式为空）
    session_id VARCHAR(64),                  -- 会话ID（通用聊天模式使用UUID，简历模式使用resumeId）
    role VARCHAR(20) NOT NULL,               -- 角色（user / assistant）
    content TEXT NOT NULL,                   -- 消息内容
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0                -- 逻辑删除标记（0-未删除 1-已删除）
);

CREATE INDEX IF NOT EXISTS idx_chat_message_resume_id ON t_chat_message(resume_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_session_id ON t_chat_message(session_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_created_at ON t_chat_message(created_at);
