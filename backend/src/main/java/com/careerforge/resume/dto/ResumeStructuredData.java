package com.careerforge.resume.dto;

import com.careerforge.common.annotation.SchemaField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 简历结构化数据DTO
 * 用于存储AI解析后的完整简历结构
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeStructuredData {

    /**
     * 将简历全部内容转换为Markdown格式，标题用 # ## ### 层级，列表用 - 或 1.，保留所有文字信息，不要遗漏
     */
    @SchemaField(value = "将简历全部内容转换为Markdown格式，标题用 # ## ### 层级，列表用 - 或 1.，保留所有文字信息，不要遗漏", required = true)
    private String markdownContent;

    /**
     * 基本信息（单对象）
     */
    @SchemaField("基本信息（单对象）")
    private BasicInfo basicInfo;

    /**
     * 教育经历（数组）
     */
    @SchemaField("教育经历（数组）")
    @Builder.Default
    private List<EducationExperience> education = List.of();

    /**
     * 工作经历（数组）
     */
    @SchemaField("工作经历（数组）")
    @Builder.Default
    private List<WorkExperience> work = List.of();

    /**
     * 项目经验（数组）
     */
    @SchemaField("项目经验（数组）")
    @Builder.Default
    private List<ProjectExperience> projects = List.of();

    /**
     * 技能（对象数组）
     */
    @SchemaField("技能（对象数组）")
    @Builder.Default
    private List<Skill> skills = List.of();

    /**
     * 证书/荣誉（数组）
     */
    @SchemaField("证书/荣誉（数组）")
    @Builder.Default
    private List<Certificate> certificates = List.of();

    /**
     * 开源贡献（数组）
     */
    @SchemaField("开源贡献（数组）")
    @Builder.Default
    private List<OpenSourceContribution> openSource = List.of();

    /**
     * 自定义区块（数组）- 用于存储不属于标准模块的内容，如游戏经历、志愿者经历、竞赛经历等
     */
    @SchemaField(value = "自定义区块（数组）- 用于存储不属于标准模块的内容，如游戏经历、志愿者经历、竞赛经历等")
    @Builder.Default
    private List<CustomSection> customSections = List.of();

    /**
     * 基本信息DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BasicInfo {
        /**
         * 姓名，未找到则为空字符串
         */
        @SchemaField(value = "姓名", required = true)
        private String name;

        /**
         * 性别：男/女/未知
         */
        @SchemaField(value = "性别：男/女/未知", required = true, enumValues = {"男", "女", "未知"})
        private String gender;

        /**
         * 出生日期，如 1995-03 或 1995-03-15
         */
        @SchemaField(value = "出生日期，如 1995-03 或 1995-03-15", required = true)
        private String birthday;

        /**
         * 年龄，如 28
         */
        @SchemaField(value = "年龄，如 28", required = true)
        private String age;

        /**
         * 联系电话，保持原格式
         */
        @SchemaField(value = "联系电话，保持原格式", required = true)
        private String phone;

        /**
         * 邮箱地址
         */
        @SchemaField(value = "邮箱地址", required = true)
        private String email;

        /**
         * 求职意向/目标岗位
         */
        @SchemaField(value = "求职意向/目标岗位", required = true)
        private String targetPosition;

        /**
         * 个人简介/自我评价
         */
        @SchemaField(value = "个人简介/自我评价", required = true)
        private String summary;

        /**
         * 所在地（城市）
         */
        @SchemaField(value = "所在地（城市）", required = true)
        private String location;

        /**
         * LinkedIn 主页链接
         */
        @SchemaField(value = "LinkedIn 主页链接", required = true)
        private String linkedin;

        /**
         * GitHub 主页链接
         */
        @SchemaField(value = "GitHub 主页链接", required = true)
        private String github;

        /**
         * 个人网站链接
         */
        @SchemaField(value = "个人网站链接", required = true)
        private String website;
    }

    /**
     * 教育经历DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EducationExperience {
        /**
         * 学校名称
         */
        @SchemaField("学校名称")
        private String school;

        /**
         * 学历：本科/硕士/博士/大专/高中/中专/其他
         */
        @SchemaField(value = "学历：本科/硕士/博士/大专/高中/中专/其他", enumValues = {"本科", "硕士", "博士", "大专", "高中", "中专", "其他"})
        private String degree;

        /**
         * 专业名称
         */
        @SchemaField("专业名称")
        private String major;

        /**
         * 时间段，格式：YYYY.MM-YYYY.MM
         */
        @SchemaField("时间段，格式：YYYY.MM-YYYY.MM")
        private String period;

        /**
         * 绩点（如 3.8/4.0）
         */
        @SchemaField("绩点（如 3.8/4.0）")
        private String gpa;

        /**
         * 主修课程列表
         */
        @SchemaField("主修课程列表")
        @Builder.Default
        private List<String> courses = List.of();

        /**
         * 校内荣誉列表
         */
        @SchemaField("校内荣誉列表")
        @Builder.Default
        private List<String> honors = List.of();
    }

    /**
     * 工作经历DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkExperience {
        /**
         * 公司名称
         */
        @SchemaField("公司名称")
        private String company;

        /**
         * 职位名称
         */
        @SchemaField("职位名称")
        private String position;

        /**
         * 时间段
         */
        @SchemaField("时间段")
        private String period;

        /**
         * 工作描述，多行内容用换行符连接
         */
        @SchemaField("工作描述，多行内容用换行符连接")
        private String description;

        /**
         * 工作地点（城市）
         */
        @SchemaField("工作地点（城市）")
        private String location;

        /**
         * 工作成果列表
         */
        @SchemaField("工作成果列表")
        @Builder.Default
        private List<String> achievements = List.of();

        /**
         * 使用的技术栈
         */
        @SchemaField("使用的技术栈")
        @Builder.Default
        private List<String> technologies = List.of();

        /**
         * 公司行业（如：电商、金融科技、在线教育）
         */
        @SchemaField("公司行业（如：电商、金融科技、在线教育）")
        private String industry;

        /**
         * 代表产品列表（如：["淘宝App", "支付宝"]）
         */
        @SchemaField("代表产品列表（如：[\"淘宝App\", \"支付宝\"]）")
        @Builder.Default
        private List<String> products = List.of();
    }

    /**
     * 项目经验DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectExperience {
        /**
         * 项目名称
         */
        @SchemaField("项目名称")
        private String name;

        /**
         * 项目角色
         */
        @SchemaField("项目角色")
        private String role;

        /**
         * 时间段
         */
        @SchemaField("时间段")
        private String period;

        /**
         * 项目描述
         */
        @SchemaField("项目描述")
        private String description;

        /**
         * 项目成果/亮点列表
         */
        @SchemaField("项目成果/亮点列表")
        @Builder.Default
        private List<String> achievements = List.of();

        /**
         * 使用的技术栈
         */
        @SchemaField("使用的技术栈")
        @Builder.Default
        private List<String> technologies = List.of();

        /**
         * 项目链接
         */
        @SchemaField("项目链接")
        private String url;
    }

    /**
     * 证书/荣誉DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Certificate {
        /**
         * 证书/荣誉名称
         */
        @SchemaField("证书/荣誉名称")
        private String name;

        /**
         * 获得日期
         */
        @SchemaField("获得日期")
        private String date;

        /**
         * 颁发机构
         */
        @SchemaField("颁发机构")
        private String issuer;

        /**
         * 证书编号
         */
        @SchemaField("证书编号")
        private String credentialId;

        /**
         * 证书链接
         */
        @SchemaField("证书链接")
        private String url;
    }

    /**
     * 开源贡献DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpenSourceContribution {
        /**
         * 项目名称
         */
        @SchemaField("项目名称")
        private String projectName;

        /**
         * 项目地址（GitHub/GitLab等）
         */
        @SchemaField("项目地址（GitHub/GitLab等）")
        private String url;

        /**
         * 角色：核心贡献者/文档贡献者/Issue贡献者等
         */
        @SchemaField(value = "角色：核心贡献者/文档贡献者/Issue贡献者等", enumValues = {"核心贡献者", "文档贡献者", "Issue贡献者", "项目创始人", "其他"})
        private String role;

        /**
         * 时间段
         */
        @SchemaField("时间段")
        private String period;

        /**
         * 贡献描述
         */
        @SchemaField("贡献描述")
        private String description;

        /**
         * 贡献成果列表
         */
        @SchemaField("贡献成果列表")
        @Builder.Default
        private List<String> achievements = List.of();
    }

    /**
     * 技能DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Skill {
        /**
         * 技能名称
         */
        @SchemaField("技能名称")
        private String name;

        /**
         * 技能描述（关键经验、应用场景）
         */
        @SchemaField("技能描述（关键经验、应用场景）")
        private String description;

        /**
         * 熟练度：了解/熟悉/熟练/精通
         */
        @SchemaField(value = "熟练度：了解/熟悉/熟练/精通", enumValues = {"了解", "熟悉", "熟练", "精通"})
        private String level;

        /**
         * 技能分类：编程语言/框架/工具/软技能等
         */
        @SchemaField(value = "技能分类：编程语言/框架/工具/软技能等", enumValues = {"编程语言", "框架", "工具", "软技能", "其他"})
        private String category;
    }

    /**
     * 自定义区块DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomSection {
        /**
         * 区块标题，如"游戏经历"、"志愿者经历"、"竞赛经历"
         */
        @SchemaField(value = "区块标题，如\"游戏经历\"、\"志愿者经历\"、\"竞赛经历\"", required = true)
        private String title;

        /**
         * 评分（0-100）
         * 注意：此字段不参与 AI 解析，由优化流程计算得出
         */
        private Integer score;

        /**
         * 内容项列表
         */
        @SchemaField(value = "内容项列表", required = true)
        @Builder.Default
        private List<ContentItem> items = List.of();
    }

    /**
     * 通用内容项DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentItem {
        /**
         * 内容项名称
         */
        @SchemaField(value = "内容项名称", required = true)
        private String name;

        /**
         * 角色或职位（可选）
         */
        @SchemaField("角色或职位（可选）")
        private String role;

        /**
         * 时间段（可选）
         */
        @SchemaField("时间段（可选）")
        private String period;

        /**
         * 详细描述（可选）
         */
        @SchemaField("详细描述（可选）")
        private String description;

        /**
         * 成果或要点列表（可选）
         */
        @SchemaField("成果或要点列表（可选）")
        @Builder.Default
        private List<String> highlights = List.of();
    }
}
