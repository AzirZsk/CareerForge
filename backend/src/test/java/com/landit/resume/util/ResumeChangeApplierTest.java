package com.landit.resume.util;

import com.landit.resume.dto.OptimizeSectionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ResumeChangeApplier 单元测试
 *
 * @author Azir
 */
@DisplayName("ResumeChangeApplier 测试")
class ResumeChangeApplierTest {

    private List<Map<String, Object>> testSections;

    @BeforeEach
    void setUp() {
        testSections = createTestSections();
    }

    @Nested
    @DisplayName("基本信息变更测试")
    class BasicInfoChangeTests {

        @Test
        @DisplayName("修改基本信息字段 - 个人简介")
        void testModifyBasicInfoSummary() {
            OptimizeSectionResponse.Change change = createChange(
                    "modified", "basicInfo.summary", "string",
                    null, "5年电竞新媒体运营经验，精通从0到1账号搭建与内容矩阵运营。"
            );
            List<Map<String, Object>> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));
            Map<String, Object> basicInfo = findSectionByType(result, "BASIC_INFO");
            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) basicInfo.get("content");
            assertEquals("5年电竞新媒体运营经验，精通从0到1账号搭建与内容矩阵运营。", content.get("summary"));
        }

        @Test
        @DisplayName("修改基本信息字段 - 姓名")
        void testModifyBasicInfoName() {
            OptimizeSectionResponse.Change change = createChange(
                    "modified", "basicInfo.name", "string",
                    "陈家尧", "张三"
            );
            List<Map<String, Object>> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));
            Map<String, Object> basicInfo = findSectionByType(result, "BASIC_INFO");
            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) basicInfo.get("content");
            assertEquals("张三", content.get("name"));
        }
    }

    @Nested
    @DisplayName("工作经历变更测试")
    class WorkChangeTests {

        @Test
        @DisplayName("修改工作经历字段 - 时间段")
        void testModifyWorkPeriod() {
            OptimizeSectionResponse.Change change = createChange(
                    "modified", "work[0].period", "string",
                    "2025.03-2026.03", "2023.03-2024.12"
            );
            List<Map<String, Object>> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));
            Map<String, Object> workSection = findSectionByType(result, "WORK");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) workSection.get("items");
            Map<String, Object> content = parseContent(items.get(0).get("content"));
            assertEquals("2023.03-2024.12", content.get("period"));
        }

        @Test
        @DisplayName("修改工作经历字段 - 工作描述")
        void testModifyWorkDescription() {
            String newDescription = "负责FPX永劫无间分部全平台内容运营";
            OptimizeSectionResponse.Change change = createChange(
                    "modified", "work[0].description", "string",
                    "旧描述", newDescription
            );
            List<Map<String, Object>> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));
            Map<String, Object> workSection = findSectionByType(result, "WORK");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) workSection.get("items");
            Map<String, Object> content = parseContent(items.get(0).get("content"));
            assertTrue(content.get("description").toString().contains("全平台内容运营"));
        }

        @Test
        @DisplayName("修改工作经历字段 - 技术栈(数组)")
        void testModifyWorkTechnologies() {
            List<String> newTechnologies = List.of("抖音/小红书运营", "剪映/PR剪辑", "巨量算数数据分析");
            OptimizeSectionResponse.Change change = createArrayChange(
                    "modified", "work[0].technologies", null, newTechnologies
            );
            List<Map<String, Object>> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));
            Map<String, Object> workSection = findSectionByType(result, "WORK");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) workSection.get("items");
            Map<String, Object> content = parseContent(items.get(0).get("content"));
            @SuppressWarnings("unchecked")
            List<String> technologies = (List<String>) content.get("technologies");
            assertEquals(3, technologies.size());
            assertTrue(technologies.contains("抖音/小红书运营"));
        }

        @Test
        @DisplayName("修改第二条工作经历")
        void testModifySecondWorkDescription() {
            OptimizeSectionResponse.Change change = createChange(
                    "modified", "work[1].description", "string",
                    "旧描述", "负责FPX永劫无间分部所有内容"
            );
            List<Map<String, Object>> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));
            Map<String, Object> workSection = findSectionByType(result, "WORK");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) workSection.get("items");
            Map<String, Object> content = parseContent(items.get(1).get("content"));
            assertTrue(content.get("description").toString().contains("FPX"));
        }
    }

    @Nested
    @DisplayName("专业技能变更测试")
    class SkillsChangeTests {

        @Test
        @DisplayName("新增技能项")
        void testAddSkill() {
            // 先确保有 SKILLS 区块
            ensureSkillsSection();
            OptimizeSectionResponse.Change change = createChange(
                    "added", "skills[0]", "string",
                    null, "社交媒体运营（精通）"
            );
            List<Map<String, Object>> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));
            Map<String, Object> skillsSection = findSectionByType(result, "SKILLS");
            assertNotNull(skillsSection);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) skillsSection.get("items");
            assertFalse(items.isEmpty());
        }

        @Test
        @DisplayName("新增多个技能项")
        void testAddMultipleSkills() {
            ensureSkillsSection();
            List<OptimizeSectionResponse.Change> changes = List.of(
                    createChange("added", "skills[0]", "string", null, "社交媒体运营（精通）"),
                    createChange("added", "skills[1]", "string", null, "内容创作与视频剪辑"),
                    createChange("added", "skills[2]", "string", null, "数据分析")
            );
            List<Map<String, Object>> result = ResumeChangeApplier.applyChanges(testSections, changes);
            Map<String, Object> skillsSection = findSectionByType(result, "SKILLS");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) skillsSection.get("items");
            @SuppressWarnings("unchecked")
            Map<String, Object> content = parseContent(items.get(0).get("content"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> skills = (List<Map<String, Object>>) content.get("skills");
            assertTrue(skills.size() >= 3);
        }
    }

    @Nested
    @DisplayName("教育经历变更测试")
    class EducationChangeTests {

        @Test
        @DisplayName("修改教育经历 - 主修课程(数组)")
        void testModifyEducationCourses() {
            List<String> newCourses = List.of("市场营销", "统计学", "经济法");
            OptimizeSectionResponse.Change change = createArrayChange(
                    "modified", "education[0].courses",
                    List.of("宏观经济学", "微观经济学"), newCourses
            );
            List<Map<String, Object>> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));
            Map<String, Object> educationSection = findSectionByType(result, "EDUCATION");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) educationSection.get("items");
            Map<String, Object> content = parseContent(items.get(0).get("content"));
            @SuppressWarnings("unchecked")
            List<String> courses = (List<String>) content.get("courses");
            assertEquals(3, courses.size());
            assertTrue(courses.contains("市场营销"));
        }
    }

    @Nested
    @DisplayName("自定义区块变更测试")
    class CustomSectionChangeTests {

        @Test
        @DisplayName("修改自定义区块 - 描述")
        void testModifyCustomSectionDescription() {
            String newDescription = "游戏时长3000+小时，累计投入5W+用于产品测试";
            OptimizeSectionResponse.Change change = createChange(
                    "modified", "customSections[0].items[2].description", "string",
                    "游戏时长 3000+小时，累计充值 5W 左右。", newDescription
            );
            List<Map<String, Object>> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));
            Map<String, Object> customSection = findSectionByType(result, "CUSTOM");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) customSection.get("items");
            Map<String, Object> content = parseContent(items.get(0).get("content"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentItems = (List<Map<String, Object>>) content.get("items");
            Map<String, Object> targetItem = contentItems.get(2);
            assertTrue(targetItem.get("description").toString().contains("产品测试"));
        }
    }

    @Nested
    @DisplayName("综合变更测试")
    class ComprehensiveChangeTests {

        @Test
        @DisplayName("应用多个变更")
        void testApplyMultipleChanges() {
            List<OptimizeSectionResponse.Change> changes = List.of(
                    createChange("modified", "basicInfo.summary", "string", null, "新的个人简介"),
                    createChange("modified", "work[0].period", "string", "2025.03-2026.03", "2023.03-2024.12"),
                    createArrayChange("modified", "education[0].courses", null, List.of("市场营销", "统计学"))
            );
            List<Map<String, Object>> result = ResumeChangeApplier.applyChanges(testSections, changes);
            // 验证基本信息
            Map<String, Object> basicInfo = findSectionByType(result, "BASIC_INFO");
            @SuppressWarnings("unchecked")
            Map<String, Object> basicContent = (Map<String, Object>) basicInfo.get("content");
            assertEquals("新的个人简介", basicContent.get("summary"));
            // 验证工作经历
            Map<String, Object> workSection = findSectionByType(result, "WORK");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> workItems = (List<Map<String, Object>>) workSection.get("items");
            Map<String, Object> workContent = parseContent(workItems.get(0).get("content"));
            assertEquals("2023.03-2024.12", workContent.get("period"));
            // 验证教育经历
            Map<String, Object> educationSection = findSectionByType(result, "EDUCATION");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> eduItems = (List<Map<String, Object>>) educationSection.get("items");
            Map<String, Object> eduContent = parseContent(eduItems.get(0).get("content"));
            @SuppressWarnings("unchecked")
            List<String> courses = (List<String>) eduContent.get("courses");
            assertEquals(2, courses.size());
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("sections 为 null")
        void testNullSections() {
            List<Map<String, Object>> result = ResumeChangeApplier.applyChanges(null, List.of());
            assertNull(result);
        }

        @Test
        @DisplayName("changes 为 null")
        void testNullChanges() {
            List<Map<String, Object>> result = ResumeChangeApplier.applyChanges(testSections, null);
            assertNotNull(result);
            assertEquals(testSections.size(), result.size());
        }

        @Test
        @DisplayName("changes 为空列表")
        void testEmptyChanges() {
            List<Map<String, Object>> result = ResumeChangeApplier.applyChanges(testSections, List.of());
            assertNotNull(result);
            assertEquals(testSections.size(), result.size());
        }

        @Test
        @DisplayName("无效的字段路径")
        void testInvalidFieldPath() {
            OptimizeSectionResponse.Change change = createChange(
                    "modified", "invalidField.path", "string", null, "测试值"
            );
            // 不应该抛出异常，而是跳过无效变更
            assertDoesNotThrow(() -> ResumeChangeApplier.applyChanges(testSections, List.of(change)));
        }

        @Test
        @DisplayName("越界的数组索引")
        void testOutOfBoundArrayIndex() {
            OptimizeSectionResponse.Change change = createChange(
                    "modified", "work[999].description", "string", null, "测试值"
            );
            // 不应该抛出异常，而是跳过无效变更
            assertDoesNotThrow(() -> ResumeChangeApplier.applyChanges(testSections, List.of(change)));
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 查找指定类型的区块
     */
    private Map<String, Object> findSectionByType(List<Map<String, Object>> sections, String type) {
        return sections.stream()
                .filter(s -> type.equals(s.get("type")))
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析 content（可能是 JSON 字符串或 Map）
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseContent(Object content) {
        if (content instanceof Map) {
            return (Map<String, Object>) content;
        }
        if (content instanceof String) {
            return com.landit.common.util.JsonParseHelper.parseToMap((String) content);
        }
        return new LinkedHashMap<>();
    }

    /**
     * 创建字符串类型的变更
     */
    private OptimizeSectionResponse.Change createChange(String type, String field, String valueType,
                                                        String before, String after) {
        return OptimizeSectionResponse.Change.builder()
                .type(type)
                .field(field)
                .valueType(valueType)
                .before(before != null ? OptimizeSectionResponse.ChangeValue.ofString(before) : null)
                .after(after != null ? OptimizeSectionResponse.ChangeValue.ofString(after) : null)
                .build();
    }

    /**
     * 创建数组类型的变更
     */
    private OptimizeSectionResponse.Change createArrayChange(String type, String field,
                                                             List<String> before, List<String> after) {
        return OptimizeSectionResponse.Change.builder()
                .type(type)
                .field(field)
                .valueType("string_array")
                .before(before != null ? OptimizeSectionResponse.ChangeValue.ofArray(before) : null)
                .after(after != null ? OptimizeSectionResponse.ChangeValue.ofArray(after) : null)
                .build();
    }

    /**
     * 确保 SKILLS 区块存在
     */
    private void ensureSkillsSection() {
        if (findSectionByType(testSections, "SKILLS") == null) {
            Map<String, Object> skillsSection = new LinkedHashMap<>();
            skillsSection.put("type", "SKILLS");
            skillsSection.put("title", "专业技能");
            List<Map<String, Object>> items = new ArrayList<>();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("content", "{\"skills\":[]}");
            items.add(item);
            skillsSection.put("items", items);
            testSections.add(skillsSection);
        }
    }

    /**
     * 创建测试用的 sections 数据（模拟真实简历结构）
     */
    private List<Map<String, Object>> createTestSections() {
        List<Map<String, Object>> sections = new ArrayList<>();
        // 基本信息
        Map<String, Object> basicInfo = new LinkedHashMap<>();
        basicInfo.put("type", "BASIC_INFO");
        basicInfo.put("title", "基本信息");
        Map<String, Object> basicContent = new LinkedHashMap<>();
        basicContent.put("name", "陈家尧");
        basicContent.put("gender", "男");
        basicContent.put("phone", "15215575397");
        basicContent.put("email", "1776096741@qq.com");
        basicContent.put("summary", "");
        basicInfo.put("content", basicContent);
        sections.add(basicInfo);
        // 教育经历
        Map<String, Object> education = new LinkedHashMap<>();
        education.put("type", "EDUCATION");
        education.put("title", "教育经历");
        List<Map<String, Object>> eduItems = new ArrayList<>();
        Map<String, Object> eduItem = new LinkedHashMap<>();
        eduItem.put("id", "edu_1");
        eduItem.put("content", "{\"school\":\"宿州学院\",\"degree\":\"本科\",\"major\":\"国际经济与贸易\",\"period\":\"2017.09-2021.06\",\"courses\":[\"宏观经济学\",\"微观经济学\",\"经济法\",\"会计学\"]}");
        eduItems.add(eduItem);
        education.put("items", eduItems);
        sections.add(education);
        // 工作经历
        Map<String, Object> work = new LinkedHashMap<>();
        work.put("type", "WORK");
        work.put("title", "工作经历");
        List<Map<String, Object>> workItems = new ArrayList<>();
        // 工作1
        Map<String, Object> work1 = new LinkedHashMap<>();
        work1.put("id", "work_1");
        work1.put("content", "{\"company\":\"自媒体博主\",\"position\":\"游戏博主\",\"period\":\"2025.03-2026.03\",\"description\":\"账号从0到1搭建\",\"technologies\":[]}");
        workItems.add(work1);
        // 工作2
        Map<String, Object> work2 = new LinkedHashMap<>();
        work2.put("id", "work_2");
        work2.put("content", "{\"company\":\"FPX电子竞技俱乐部\",\"position\":\"新媒体运营\",\"period\":\"2024.05-2025.03\",\"description\":\"负责FPX永劫无间分部\"}");
        workItems.add(work2);
        // 工作3
        Map<String, Object> work3 = new LinkedHashMap<>();
        work3.put("id", "work_3");
        work3.put("content", "{\"company\":\"JDG电子竞技俱乐部\",\"position\":\"新媒体运营\",\"period\":\"2022.06-2024.01\"}");
        workItems.add(work3);
        // 工作4
        Map<String, Object> work4 = new LinkedHashMap<>();
        work4.put("id", "work_4");
        work4.put("content", "{\"company\":\"WBG电子竞技俱乐部\",\"position\":\"新媒体运营\",\"period\":\"2021.06-2022.03\"}");
        workItems.add(work4);
        work.put("items", workItems);
        sections.add(work);
        // 自定义区块
        Map<String, Object> custom = new LinkedHashMap<>();
        custom.put("type", "CUSTOM");
        custom.put("title", "自定义区块");
        List<Map<String, Object>> customItems = new ArrayList<>();
        Map<String, Object> customItem = new LinkedHashMap<>();
        customItem.put("id", "custom_1");
        customItem.put("content", "{\"title\":\"游戏经历\",\"items\":[{\"name\":\"三角洲行动\",\"description\":\"游戏时长2000+小时\"},{\"name\":\"英雄联盟\",\"description\":\"游戏时长2000+小时\"},{\"name\":\"MMO游戏\",\"description\":\"游戏时长 3000+小时，累计充值 5W 左右。\"}]}");
        customItems.add(customItem);
        custom.put("items", customItems);
        sections.add(custom);
        return sections;
    }
}
