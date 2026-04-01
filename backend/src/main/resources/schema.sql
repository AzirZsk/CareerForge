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
    -- 诊断评分（AI分析结果）
    overall_score INTEGER DEFAULT 0,        -- 总体评分
    content_score INTEGER DEFAULT 0,        -- 内容评分
    structure_score INTEGER DEFAULT 0,      -- 结构评分
    matching_score INTEGER DEFAULT 0,       -- 匹配度评分
    competitiveness_score INTEGER DEFAULT 0, -- 竞争力评分
    -- 职位描述（用于定制简历）
    job_description TEXT,                   -- 目标职位的JD内容
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    deleted INTEGER DEFAULT 0               -- 逻辑删除标记
);

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
    date DATETIME,                          -- 面试时间
    duration INTEGER DEFAULT 0,             -- 面试时长（分钟）
    score INTEGER DEFAULT 0,                -- 面试得分（0-100）
    status VARCHAR(20) DEFAULT 'in_progress', -- 面试状态（in_progress-进行中 completed-已完成）
    questions INTEGER DEFAULT 0,            -- 问题总数
    correct_answers INTEGER DEFAULT 0,      -- 正确回答数
    -- 生命周期管理扩展字段
    source VARCHAR(20) DEFAULT 'mock',      -- 面试来源（mock-模拟 real-真实）
    jd_content TEXT,                        -- 职位描述内容
    overall_result VARCHAR(20),             -- 面试结果（pass-通过 fail-未通过 pending-待定）
    notes TEXT,                             -- 面试备注
    company_research TEXT,                  -- 公司调研信息
    jd_analysis TEXT,                       -- JD分析结果
    job_position_id VARCHAR(64),            -- 关联职位ID
    round_type VARCHAR(30),                 -- 轮次类型（hr-人力 technical-技术 manager-经理等）
    round_name VARCHAR(100),                -- 轮次名称
    -- 面试类型与地址信息
    interview_type VARCHAR(20),              -- 面试类型（onsite-现场 / online-线上）
    location VARCHAR(500),                   -- 现场面试地址
    online_link VARCHAR(500),                -- 线上面试链接
    meeting_password VARCHAR(100),           -- 线上面试密码/会议号
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
    -- 语音模式相关字段
    voice_mode VARCHAR(20) DEFAULT 'text',  -- 语音模式（text-纯文本 half_voice-半语音 full_voice-全语音）
    assist_count INT DEFAULT 0,             -- 已使用求助次数
    assist_limit INT DEFAULT 5,             -- 求助次数上限
    freeze_at DATETIME,                     -- 冻结时间点（求助时的面试暂停时间）
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
    actions TEXT,                            -- 操作卡片列表（JSON格式）
    action_status VARCHAR(20) DEFAULT 'pending', -- 操作状态（pending-待处理 applied-已应用 discarded-已丢弃）
    segments TEXT,                           -- 内容分片列表（JSON格式，记录文字和操作卡片的穿插顺序）
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0                -- 逻辑删除标记（0-未删除 1-已删除）
);

CREATE INDEX IF NOT EXISTS idx_chat_message_resume_id ON t_chat_message(resume_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_session_id ON t_chat_message(session_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_created_at ON t_chat_message(created_at);

-- ============================================================================
-- 语音面试功能扩展（v1.4.0）
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 助手对话记录表（语音面试求助记录）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_assistant_conversation (
    id VARCHAR(64) PRIMARY KEY,              -- 主键ID（雪花ID字符串）
    session_id VARCHAR(64) NOT NULL,          -- 关联的面试会话ID
    freeze_index INT NOT NULL,                -- 冻结时的问题序号
    assist_type VARCHAR(30) NOT NULL,         -- 求助类型：GIVE_HINTS, EXPLAIN_CONCEPT, POLISH_ANSWER, FREE_QUESTION
    user_question TEXT,                       -- 用户问题（自由提问时）
    assistant_response TEXT NOT NULL,         -- 助手回复
    audio_url VARCHAR(500),                   -- 音频存储路径
    duration_ms INT,                          -- 音频时长（毫秒）
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,                -- 逻辑删除标记
    FOREIGN KEY (session_id) REFERENCES t_interview_session(id)
);

CREATE INDEX IF NOT EXISTS idx_assistant_conversation_session_id ON t_assistant_conversation(session_id);
CREATE INDEX IF NOT EXISTS idx_assistant_conversation_freeze_index ON t_assistant_conversation(session_id, freeze_index);

-- ----------------------------------------------------------------------------
-- 面试录音片段表（语音面试录音存储）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_interview_recording (
    id VARCHAR(64) PRIMARY KEY,              -- 主键ID（雪花ID字符串）
    session_id VARCHAR(64) NOT NULL,          -- 关联的面试会话ID
    segment_index INT NOT NULL,               -- 片段序号
    role VARCHAR(20) NOT NULL,                -- 角色：interviewer / candidate / assistant
    content TEXT,                             -- 对应的文字内容
    audio_path VARCHAR(500) NOT NULL,         -- 音频文件路径
    duration_ms INT NOT NULL,                 -- 时长（毫秒）
    start_time DATETIME NOT NULL,             -- 开始时间
    end_time DATETIME NOT NULL,               -- 结束时间
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,                -- 逻辑删除标记
    FOREIGN KEY (session_id) REFERENCES t_interview_session(id)
);

CREATE INDEX IF NOT EXISTS idx_interview_recording_session_id ON t_interview_recording(session_id);
CREATE INDEX IF NOT EXISTS idx_interview_recording_segment_index ON t_interview_recording(session_id, segment_index);

-- ----------------------------------------------------------------------------
-- 录音索引表（面试录音合并信息）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_recording_index (
    id VARCHAR(64) PRIMARY KEY,              -- 主键ID（雪花ID字符串）
    session_id VARCHAR(64) NOT NULL,          -- 关联的面试会话ID
    total_duration_ms INT DEFAULT 0,          -- 总时长（毫秒）
    merged_audio_path VARCHAR(500),           -- 合并后的完整音频路径
    transcript TEXT,                          -- 完整文字记录（JSON格式）
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,                -- 逻辑删除标记
    FOREIGN KEY (session_id) REFERENCES t_interview_session(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_recording_index_session_id ON t_recording_index(session_id);

-- ============================================================================
-- 面试生命周期管理功能（v1.5.0）
-- ============================================================================

-- ----------------------------------------------------------------------------
-- t_company 公司表（公司调研信息）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_company (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    research TEXT,
    research_updated_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_company_name ON t_company(name);

-- ----------------------------------------------------------------------------
-- t_job_position 职位表（JD分析信息）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_job_position (
    id VARCHAR(64) PRIMARY KEY,
    company_id VARCHAR(64) NOT NULL,
    title VARCHAR(100) NOT NULL,
    jd_content TEXT,
    jd_analysis TEXT,
    jd_analysis_updated_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_job_position_company_id ON t_job_position(company_id);
CREATE INDEX IF NOT EXISTS idx_job_position_title ON t_job_position(title);

-- ----------------------------------------------------------------------------
-- t_interview_preparation 表（面试准备事项表）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_interview_preparation (
    id VARCHAR(64) PRIMARY KEY,
    interview_id VARCHAR(64) NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    completed INTEGER DEFAULT 0,
    source VARCHAR(20) DEFAULT 'manual',
    sort_order INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_interview_preparation_interview_id ON t_interview_preparation(interview_id);
CREATE INDEX IF NOT EXISTS idx_interview_preparation_item_type ON t_interview_preparation(item_type);

-- ----------------------------------------------------------------------------
-- t_interview_review_note 表（面试复盘笔记表）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_interview_review_note (
    id VARCHAR(64) PRIMARY KEY,
    interview_id VARCHAR(64) NOT NULL,
    type VARCHAR(20) NOT NULL,
    overall_feeling TEXT,
    high_points TEXT,
    weak_points TEXT,
    lessons_learned TEXT,
    suggestions TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_interview_review_note_interview_id ON t_interview_review_note(interview_id);
CREATE INDEX IF NOT EXISTS idx_interview_review_note_type ON t_interview_review_note(type);
