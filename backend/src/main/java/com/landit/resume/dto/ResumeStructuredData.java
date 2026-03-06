package com.landit.resume.dto;

import com.landit.common.annotation.SchemaField;
import com.landit.common.enums.Gender;
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
     * 姓名（根级必填字段）
     */
    @SchemaField(value = "姓名", required = true)
    private String name;

    /**
     * 性别（根级必填字段）
     */
    @SchemaField(value = "性别", enumValues = {"男", "女", "未知"}, required = true)
    private Gender gender;

    /**
     * markdown 格式的简历完整内容（根级必填字段）
     */
    @SchemaField(value = "markdown格式的简历完整内容", required = true)
    private String markdownContent;

    /**
     * 基本信息
     */
    @SchemaField("基本信息")
    private BasicInfo basicInfo;

    /**
     * 教育经历列表
     */
    @SchemaField("教育经历")
    @Builder.Default
    private List<EducationExperience> education = List.of();

    /**
     * 工作经历列表
     */
    @SchemaField("工作经历")
    @Builder.Default
    private List<WorkExperience> work = List.of();

    /**
     * 项目经验列表
     */
    @SchemaField("项目经历")
    @Builder.Default
    private List<ProjectExperience> projects = List.of();

    /**
     * 技能列表
     */
    @SchemaField("专业技能")
    @Builder.Default
    private List<Skill> skills = List.of();

    /**
     * 证书/荣誉列表
     */
    @SchemaField("证书荣誉")
    @Builder.Default
    private List<Certificate> certificates = List.of();

    /**
     * 开源贡献列表
     */
    @SchemaField("开源贡献")
    @Builder.Default
    private List<OpenSourceContribution> openSource = List.of();

    /**
     * 基本信息DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BasicInfo {
        /**
         * 姓名
         */
        @SchemaField("姓名")
        private String name;

        /**
         * 性别
         */
        @SchemaField(value = "性别", enumValues = {"男", "女", "未知"})
        private Gender gender;

        /**
         * 电话
         */
        @SchemaField("电话")
        private String phone;

        /**
         * 邮箱
         */
        @SchemaField("邮箱")
        private String email;

        /**
         * 求职意向/目标岗位
         */
        @SchemaField("求职意向")
        private String targetPosition;

        /**
         * 个人简介/自我评价
         */
        @SchemaField("个人简介")
        private String summary;

        /**
         * 所在地（城市）
         */
        @SchemaField("所在地")
        private String location;

        /**
         * LinkedIn 主页
         */
        @SchemaField("LinkedIn")
        private String linkedin;

        /**
         * GitHub 主页
         */
        @SchemaField("GitHub")
        private String github;

        /**
         * 个人网站
         */
        @SchemaField("个人网站")
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
         * 学校
         */
        @SchemaField("学校名称")
        private String school;

        /**
         * 学历
         */
        @SchemaField("学历")
        private String degree;

        /**
         * 专业
         */
        @SchemaField("专业")
        private String major;

        /**
         * 时间段
         */
        @SchemaField("时间段")
        private String period;

        /**
         * 绩点（如 3.8/4.0）
         */
        @SchemaField("绩点")
        private String gpa;

        /**
         * 主修课程
         */
        @SchemaField("主修课程")
        @Builder.Default
        private List<String> courses = List.of();

        /**
         * 校内荣誉
         */
        @SchemaField("校内荣誉")
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
         * 公司
         */
        @SchemaField("公司名称")
        private String company;

        /**
         * 职位
         */
        @SchemaField("职位")
        private String position;

        /**
         * 时间段
         */
        @SchemaField("时间段")
        private String period;

        /**
         * 工作描述
         */
        @SchemaField("工作描述")
        private String description;

        /**
         * 工作地点
         */
        @SchemaField("工作地点")
        private String location;

        /**
         * 工作成果
         */
        @SchemaField("工作成果")
        @Builder.Default
        private List<String> achievements = List.of();

        /**
         * 使用技术栈
         */
        @SchemaField("技术栈")
        @Builder.Default
        private List<String> technologies = List.of();
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
         * 角色
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
         * 项目成果
         */
        @SchemaField("项目成果")
        @Builder.Default
        private List<String> achievements = List.of();

        /**
         * 技术栈
         */
        @SchemaField("技术栈")
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
         * 证书名称
         */
        @SchemaField("证书名称")
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
         * 项目地址（如 GitHub 链接）
         */
        @SchemaField("项目地址")
        private String url;

        /**
         * 角色/贡献类型
         */
        @SchemaField("贡献角色")
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
         * 贡献成果（如 PR 数量、star 数等）
         */
        @SchemaField("贡献成果")
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
         * 技能描述（关键经验、应用场景等）
         */
        @SchemaField("技能描述")
        private String description;

        /**
         * 熟练度
         */
        @SchemaField(value = "熟练度", enumValues = {"了解", "熟悉", "熟练", "精通"})
        private String level;

        /**
         * 技能分类（编程语言、框架、工具、软技能等）
         */
        @SchemaField("技能分类")
        private String category;
    }
}
