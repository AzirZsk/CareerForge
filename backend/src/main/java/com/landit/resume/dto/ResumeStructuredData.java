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
     * 基本信息
     */
    private BasicInfo basicInfo;

    /**
     * 教育经历列表
     */
    @Builder.Default
    private List<EducationExperience> education = List.of();

    /**
     * 工作经历列表
     */
    @Builder.Default
    private List<WorkExperience> work = List.of();

    /**
     * 项目经验列表
     */
    @Builder.Default
    private List<ProjectExperience> projects = List.of();

    /**
     * 技能列表
     */
    @Builder.Default
    private List<String> skills = List.of();

    /**
     * 证书/荣誉列表
     */
    @Builder.Default
    private List<Certificate> certificates = List.of();

    /**
     * 开源贡献列表
     */
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
}
