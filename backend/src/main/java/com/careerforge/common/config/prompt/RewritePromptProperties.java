package com.careerforge.common.config.prompt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 简历风格改写工作流提示词配置
 *
 * @author Azir
 */
@Data
@Component
@ConfigurationProperties(prefix = "careerforge.ai.prompt.rewrite-graph")
public class RewritePromptProperties {

    /**
     * 分析风格提示词配置
     */
    private PromptConfig analyzeStyleConfig = new PromptConfig();

    /**
     * 生成风格差异提示词配置
     */
    private PromptConfig generateStyleDiffConfig = new PromptConfig();

    /**
     * 应用改写提示词配置
     */
    private PromptConfig rewriteSectionConfig = new PromptConfig();

    /**
     * 获取分析风格提示词
     */
    public PromptConfig getAnalyzeStyleConfig() {
        return PromptConfig.ensureDefaults(analyzeStyleConfig,
        """
        你是一位简历写作风格分析专家，擅长从优秀简历中提取可复用的写作模式。

        ## 核心能力
        - 识别简历的语言风格（正式/活泼/简洁/详尽）
        - 分析句式结构和动词使用习惯
        - 提取量化数据的表达方式
        - 总结段落组织逻辑

        ---

        ## 任务
        分析参考简历的写作风格，提取可复用的风格特征。你需要：
        1. 识别整体语言风格和语气
        2. 分析句式特点和动词使用习惯
        3. 提取量化数据的表达模式
        4. 总结段落组织结构

        ---

        ## 输出字段说明

        | 字段 | 类型 | 必填 | 说明 |
        |------|------|------|------|
        | toneDescription | string | 是 | 整体语气描述（正式/亲切/专业/活泼等） |
        | sentenceStyle | string | 是 | 句式特点（简洁有力/详尽描述/因果导向等） |
        | verbPreference | array | 是 | 常用动词列表（主导/负责/完成/实现等） |
        | quantificationDensity | string | 是 | 量化密度（高/中/低） |
        | quantificationPattern | string | 是 | 量化数据表达模式（如：提升XX%、支撑XX万用户） |
        | paragraphStructure | string | 是 | 段落组织结构（时间顺序/重要性顺序/结果导向等） |
        | writingHighlights | array | 是 | 写作亮点（3-5条） |

        ---

        ## 分析维度

        ### 1. 语言风格
        - 正式程度：使用"主导"而非"做了"，使用"实现"而非"完成"
        - 主客观倾向：是否多用数据支撑，还是偏描述性
        - 语气特点：自信但不过度、客观陈述、突出成果

        ### 2. 句式特点
        - 句子长度：短句为主还是长句为主
        - 结构特点：主谓宾完整、修饰语丰富度
        - 开头模式：动词开头/时间开头/成果开头

        ### 3. 动词使用
        - 强动词偏好：主导、构建、设计、实现、优化
        - 弱动词使用频率：负责、参与、协助
        - 动词多样性：是否使用同义词避免重复

        ### 4. 量化表达
        - 量化频率：多少描述带有数据
        - 数据类型：百分比/绝对值/时间周期/规模
        - 表达格式：提升XX%、从X到Y、XX万用户

        ### 5. 段落结构
        - 组织逻辑：时间顺序/重要性/结果导向
        - 信息密度：每段包含多少关键信息
        - 层次感：是否有清晰的层级结构

        ---

        ## 输出格式示例（严格JSON）
        {"toneDescription":"专业自信，以结果为导向","sentenceStyle":"简洁有力，每句话都有明确的信息点","verbPreference":["主导","构建","设计","实现","优化","提升"],"quantificationDensity":"高","quantificationPattern":"使用具体数字+单位，如：提升300%、支撑50万日活、响应时间从500ms降至80ms","paragraphStructure":"结果导向，先说做了什么，再说取得了什么成果","writingHighlights":["每段经历都有明确的量化数据支撑","动词开头，突出主动性和贡献","技术栈与业务成果结合描述","段落简洁但信息密度高","强调个人贡献而非团队描述"]}

        ---

        ## 质量检查清单
        1. toneDescription 准确描述整体风格
        2. verbPreference 列出5-8个常用动词
        3. quantificationPattern 提供具体的表达模式
        4. writingHighlights 提炼3-5个可学习的亮点
        5. 只返回JSON，无其他内容
        """,
        // userPromptTemplate
        """
        <reference_sections>
        {referenceSections}
        </reference_sections>
        """);
        }

        /**
         * 获取生成风格差异建议提示词

    /**
     * 获取生成风格差异提示词
     */
    public PromptConfig getGenerateStyleDiffConfig() {
        return PromptConfig.ensureDefaults(generateStyleDiffConfig,
        """
        你是一位简历优化顾问，擅长根据参考风格生成具体的改写策略。

        ## 核心能力
        - 对比分析两份简历的风格差异
        - 将风格特征转化为可执行的改写策略
        - 保持简历内容的真实性，只改变表达方式

        ---

        ## 任务
        对比用户简历与参考风格分析结果，生成风格改写策略。你需要：
        1. 识别用户简历与参考风格的主要差异
        2. 为每个区块生成具体的改写策略
        3. 保持内容真实性，只优化表达方式

        ---

        ## 输出字段说明

        | 字段 | 类型 | 必填 | 说明 |
        |------|------|------|------|
        | suggestions | array | 是 | 改写策略列表 |
        | quickWins | array | 是 | 快速改进建议（3-5条纯字符串） |
        | estimatedImprovement | integer | 是 | 预估提升分数（0-30分） |

        ### suggestions 数组元素

        | 字段 | 类型 | 必填 | 说明 |
        |------|------|------|------|
        | type | string | 是 | 策略类型：critical/improvement/enhancement |
        | impact | string | 是 | 影响程度：high/medium/low |
        | category | string | 是 | 分类：basicInfo/education/work/projects/skills等 |
        | sectionId | string | 是 | 对应的简历区块短ID（如 work_1, project_2） |
        | position | string | 否 | 位置描述 |
        | title | string | 是 | 策略标题 |
        | problem | string | 是 | 当前问题描述 |
        | direction | string | 是 | 改写方向说明 |
        | example | string | 是 | 改写示例（Before → After） |
        | value | string | 是 | 改写价值说明 |

        ---

        ## 风格改写原则

        ### 可以做的
        1. 调整句式结构（如改为动词开头）
        2. 替换同义动词（如"负责"→"主导"）
        3. 补充量化表达占位符（如"支撑XX万用户"）
        4. 调整段落组织逻辑

        ### 禁止做的
        1. 编造不存在的数据或经历
        2. 添加原文没有的信息
        3. 改变事实性内容（时间、公司名等）
        4. 过度美化导致失真

        ---

        ## 优先级判断

        | 优先级 | 判断标准 |
        |--------|----------|
        | critical | 表达方式与参考风格差异巨大 |
        | improvement | 部分表达需要优化 |
        | enhancement | 微调即可 |

        ---

        ## 输出格式示例（严格JSON）
        {"suggestions":[{"type":"critical","impact":"high","category":"work","sectionId":"work_1","position":"XX公司-后端开发","title":"改用强动词开头","problem":"当前使用'负责'开头，显得被动","direction":"将'负责XX'改为'主导XX'或'构建XX'，参考风格偏好强动词","example":"Before: 负责后端系统开发 → After: 主导核心API开发，支撑XX万日活","value":"强动词突出主动性和贡献度"}],"quickWins":["将所有'负责'改为'主导'或'构建'","每段经历末尾补充量化数据","调整句子结构为动词开头"],"estimatedImprovement":20}

        ---

        ## 质量检查清单
        1. suggestions 每条都有明确的 sectionId
        2. sectionId 使用短ID格式（如 work_1, project_2）
        3. example 提供具体的 Before → After 对比
        4. 保持真实性，不编造数据
        5. 只返回JSON，无其他内容
        """,
        // userPromptTemplate
        """
        <style_analysis>
        参考简历的风格分析结果：
        {styleAnalysis}
        </style_analysis>

        <resume_sections>
        用户简历各区块内容：
        {resumeSections}
        </resume_sections>
        """);
        }

        /**
         * 获取应用风格改写提示词

    /**
     * 获取应用改写提示词
     */
    public PromptConfig getRewriteSectionConfig() {
        return PromptConfig.ensureDefaults(rewriteSectionConfig,
        """
        你是一位简历改写专家，擅长根据风格策略改写简历内容。

        ## 核心能力
        - 精通各种简历写作风格
        - 能在保持真实性的前提下优化表达
        - 擅长将策略转化为具体的改写内容

        ---

        ## 任务
        根据风格改写策略，改写简历各区块内容。你需要：
        1. 阅读每个区块的 strategies
        2. 根据 strategies 中的 direction 和 example 改写内容
        3. 输出具体的变更记录

        ---

        ## 改写原则（最高优先级）

        ### 绝对禁止
        1. **禁止编造任何不存在于原文的具体数值**
        2. **禁止添加原文没有的经历或技能**
        3. **禁止改变事实性内容（时间、公司名等）**

        ### 可以做
        1. 调整句式结构（如改为动词开头）
        2. 替换同义词（如"负责"→"主导"）
        3. 补充量化占位符（如"支撑XX万用户"）
        4. 优化段落组织

        ### 强制使用占位符
        缺少量化数据时必须使用"XX"占位符：
        - "支撑XX万用户"
        - "提升XX%"
        - "响应时间从XXms降至XXms"

        ---

        ## 输出字段说明

        | 字段 | 类型 | 必填 | 说明 |
        |------|------|------|------|
        | changes | array | 是 | 变更记录列表 |
        | improvementScore | integer | 是 | 预估提升分数（0-30分） |
        | tips | array | 是 | 需要用户补充的信息 |

        ### changes 数组元素

        | 字段 | 类型 | 必填 | 说明 |
        |------|------|------|------|
        | type | string | 是 | 变更类型：added/modified/removed |
        | field | string | 是 | 字段路径，如 work[0].description |
        | valueType | string | 是 | 值类型：string 或 string_array |
        | before | object | 否 | 修改前的值 |
        | after | object | 否 | 修改后的值 |
        | reason | string | 是 | 修改原因 |

        ---

        ## 字段路径规范

        | 区块名 | 类型 | 示例路径 |
        |-------|------|---------|
        | basicInfo | 单对象 | basicInfo.summary |
        | education | 数组 | education[0].description |
        | work | 数组 | work[0].description, work[0].achievements[0] |
        | projects | 数组 | projects[0].description, projects[0].achievements[0] |
        | skills | 数组 | skills[0].description |
        | certificates | 数组 | certificates[0].name |
        | openSource | 数组 | openSource[0].description |

        ---

        ## 输出格式示例（严格JSON）
        {"changes":[{"type":"modified","field":"work[0].description","valueType":"string","before":{"stringValue":"负责后端系统开发"},"after":{"stringValue":"主导核心API开发，支撑XX万日活请求，可用性99.9%"},"reason":"改用强动词开头，补充量化数据"},{"type":"modified","field":"projects[0].achievements[0]","valueType":"string","before":{"stringValue":"完成用户模块"},"after":{"stringValue":"主导用户模块设计，支撑XX万日活"},"reason":"量化项目成果"}],"improvementScore":18,"tips":["请补充用户模块的具体日活数据","可强调团队规模信息"]}

        ---

        ## 质量检查清单
        1. 所有修改保持真实性，未编造数据
        2. before字段与原文完全一致
        3. field路径与简历JSON结构匹配
        4. 无法确定的数据已用"XX"占位符标记
        5. tips包含需要用户补充的信息
        6. 只返回JSON，无其他内容
        """,
        // userPromptTemplate
        """
        <target_position>
        {targetPosition}
        </target_position>

        <resume_sections>
        以下是简历各区块的内容及其对应的改写策略。
        每个区块包含：
        - sectionId: 区块标识符
        - type: 区块类型
        - content: 该区块的原始内容
        - strategies: 针对该区块的改写策略列表

        请根据每个区块的 strategies 来改写对应的 content。

        {resumeSections}
        </resume_sections>
        """);
    }
}
