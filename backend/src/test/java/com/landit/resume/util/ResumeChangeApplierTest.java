package com.landit.resume.util;

import com.landit.common.util.JsonParseHelper;
import com.landit.resume.dto.OptimizeSectionResponse;
import com.landit.resume.dto.ResumeDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ResumeChangeApplier 单元测试
 *
 * @author Azir
 */
@DisplayName("ResumeChangeApplier 测试")
class ResumeChangeApplierTest {

    private List<ResumeDetailVO.ResumeSectionVO> testSections;

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
            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));
            ResumeDetailVO.ResumeSectionVO basicInfo = findSectionByType(result, "BASIC_INFO");
            Map<String, Object> content = parseContent(basicInfo.getContent());
            assertEquals("5年电竞新媒体运营经验，精通从0到1账号搭建与内容矩阵运营。", content.get("summary"));
        }

        @Test
        @DisplayName("修改基本信息字段 - 姓名")
        void testModifyBasicInfoName() {
            OptimizeSectionResponse.Change change = createChange(
                    "modified", "basicInfo.name", "string",
                    "陈家尧", "张三"
            );
            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));
            ResumeDetailVO.ResumeSectionVO basicInfo = findSectionByType(result, "BASIC_INFO");
            Map<String, Object> content = parseContent(basicInfo.getContent());
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
            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));
            ResumeDetailVO.ResumeSectionVO workSection = findSectionByType(result, "WORK");
            List<Map<String, Object>> items = parseContentArray(workSection.getContent());
            assertEquals("2023.03-2024.12", items.get(0).get("period"));
        }

        @Test
        @DisplayName("修改工作经历字段 - 工作描述")
        void testModifyWorkDescription() {
            String newDescription = "负责FPX永劫无间分部全平台内容运营";
            OptimizeSectionResponse.Change change = createChange(
                    "modified", "work[0].description", "string",
                    "旧描述", newDescription
            );
            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));
            ResumeDetailVO.ResumeSectionVO workSection = findSectionByType(result, "WORK");
            List<Map<String, Object>> items = parseContentArray(workSection.getContent());
            assertTrue(items.get(0).get("description").toString().contains("全平台内容运营"));
        }

        @Test
        @DisplayName("修改工作经历字段 - 技术栈(数组)")
        void testModifyWorkTechnologies() {
            List<String> newTechnologies = List.of("抖音/小红书运营", "剪映/PR剪辑", "巨量算数数据分析");
            OptimizeSectionResponse.Change change = createArrayChange(
                    "modified", "work[0].technologies", null, newTechnologies
            );
            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));
            ResumeDetailVO.ResumeSectionVO workSection = findSectionByType(result, "WORK");
            List<Map<String, Object>> items = parseContentArray(workSection.getContent());
            @SuppressWarnings("unchecked")
            List<String> technologies = (List<String>) items.get(0).get("technologies");
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
            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));
            ResumeDetailVO.ResumeSectionVO workSection = findSectionByType(result, "WORK");
            List<Map<String, Object>> items = parseContentArray(workSection.getContent());
            assertTrue(items.get(1).get("description").toString().contains("FPX"));
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
            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));
            ResumeDetailVO.ResumeSectionVO skillsSection = findSectionByType(result, "SKILLS");
            assertNotNull(skillsSection);
            // content 是数组 JSON，直接解析为数组
            List<Map<String, Object>> skills = parseContentArray(skillsSection.getContent());
            assertFalse(skills.isEmpty());
            assertEquals("社交媒体运营（精通）", skills.get(0).get("name"));
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
            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, changes);
            ResumeDetailVO.ResumeSectionVO skillsSection = findSectionByType(result, "SKILLS");
            // content 是数组 JSON，直接解析为数组
            List<Map<String, Object>> skills = parseContentArray(skillsSection.getContent());
            assertTrue(skills.size() >= 3);
        }
    }

    @Nested
    @DisplayName("JSON字符串格式变更测试")
    class JsonStringChangeTests {

        @Test
        @DisplayName("修改聚合类型 - 整体替换元素（JSON字符串格式）")
        void testModifyAggregateSection_ReplaceEntireItem_JsonString() {
            // 准备数据：创建一个 SKILLS 区块
            List<Map<String, Object>> skillsItems = new ArrayList<>();
            Map<String, Object> skill1 = new LinkedHashMap<>();
            skill1.put("name", "测试技术");
            skill1.put("description", "自动化测试");
            skill1.put("level", "熟练");
            skillsItems.add(skill1);

            ResumeDetailVO.ResumeSectionVO skillsSection = ResumeDetailVO.ResumeSectionVO.builder()
                    .type("SKILLS")
                    .title("专业技能")
                    .content(JsonParseHelper.toJsonString(skillsItems))
                    .build();

            // 确保 SKILLS 区块存在
            testSections.removeIf(s -> "SKILLS".equals(s.getType()));
            testSections.add(skillsSection);

            // 变更：整体替换 skills[0]，值为 JSON 字符串格式
            String afterValue = "{\"name\":\"测试技术\",\"description\":\"自动化测试框架、兼容性测试、性能测试\",\"level\":\"熟练\",\"category\":\"工具\"}";
            OptimizeSectionResponse.Change change = createChange(
                    "modified", "skills[0]", "string",
                    null, afterValue
            );

            // 执行
            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));

            // 验证
            ResumeDetailVO.ResumeSectionVO resultSection = findSectionByType(result, "SKILLS");
            List<Map<String, Object>> skills = parseContentArray(resultSection.getContent());
            assertEquals(1, skills.size());
            assertEquals("测试技术", skills.get(0).get("name"));
            assertEquals("自动化测试框架、兼容性测试、性能测试", skills.get(0).get("description"));
            assertEquals("熟练", skills.get(0).get("level"));
            assertEquals("工具", skills.get(0).get("category"));
        }

        @Test
        @DisplayName("新增聚合类型 - 添加完整对象（JSON字符串格式）")
        void testAddAggregateSection_AddFullObject_JsonString() {
            // 准备数据：创建一个 SKILLS 区块
            List<Map<String, Object>> skillsItems = new ArrayList<>();
            Map<String, Object> skill1 = new LinkedHashMap<>();
            skill1.put("name", "Java");
            skill1.put("description", "后端开发");
            skill1.put("level", "精通");
            skillsItems.add(skill1);

            ResumeDetailVO.ResumeSectionVO skillsSection = ResumeDetailVO.ResumeSectionVO.builder()
                    .type("SKILLS")
                    .title("专业技能")
                    .content(JsonParseHelper.toJsonString(skillsItems))
                    .build();

            // 确保 SKILLS 区块存在
            testSections.removeIf(s -> "SKILLS".equals(s.getType()));
            testSections.add(skillsSection);

            // 变更：新增 skills[1]，值为 JSON 字符串格式
            String afterValue = "{\"name\":\"AutoGen框架\",\"description\":\"Microsoft AutoGen多智能体协作框架\",\"level\":\"了解\",\"category\":\"技术方向\"}";
            OptimizeSectionResponse.Change change = createChange(
                    "added", "skills[1]", "string",
                    null, afterValue
            );

            // 执行
            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));

            // 验证
            ResumeDetailVO.ResumeSectionVO resultSection = findSectionByType(result, "SKILLS");
            List<Map<String, Object>> skills = parseContentArray(resultSection.getContent());
            assertEquals(2, skills.size());
            assertEquals("AutoGen框架", skills.get(1).get("name"));
            assertEquals("Microsoft AutoGen多智能体协作框架", skills.get(1).get("description"));
            assertEquals("了解", skills.get(1).get("level"));
            assertEquals("技术方向", skills.get(1).get("category"));
        }

        @Test
        @DisplayName("修改聚合类型 - 普通字段修改不受影响")
        void testModifyAggregateSection_PropertyChange_StillWorks() {
            // 准备数据
            List<Map<String, Object>> skillsItems = new ArrayList<>();
            Map<String, Object> skill1 = new LinkedHashMap<>();
            skill1.put("name", "Java");
            skill1.put("description", "后端开发");
            skill1.put("level", "了解");
            skillsItems.add(skill1);

            ResumeDetailVO.ResumeSectionVO skillsSection = ResumeDetailVO.ResumeSectionVO.builder()
                    .type("SKILLS")
                    .title("专业技能")
                    .content(JsonParseHelper.toJsonString(skillsItems))
                    .build();

            testSections.removeIf(s -> "SKILLS".equals(s.getType()));
            testSections.add(skillsSection);

            // 变更：只修改 level 属性
            OptimizeSectionResponse.Change change = createChange(
                    "modified", "skills[0].level", "string",
                    "了解", "精通"
            );

            // 执行
            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));

            // 验证
            ResumeDetailVO.ResumeSectionVO resultSection = findSectionByType(result, "SKILLS");
            List<Map<String, Object>> skills = parseContentArray(resultSection.getContent());
            assertEquals(1, skills.size());
            assertEquals("Java", skills.get(0).get("name"));
            assertEquals("精通", skills.get(0).get("level"));
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
            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));
            ResumeDetailVO.ResumeSectionVO educationSection = findSectionByType(result, "EDUCATION");
            List<Map<String, Object>> items = parseContentArray(educationSection.getContent());
            @SuppressWarnings("unchecked")
            List<String> courses = (List<String>) items.get(0).get("courses");
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
            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));
            ResumeDetailVO.ResumeSectionVO customSection = findSectionByType(result, "CUSTOM");
            List<Map<String, Object>> items = parseContentArray(customSection.getContent());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentItems = (List<Map<String, Object>>) items.get(0).get("items");
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
            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, changes);
            // 验证基本信息
            ResumeDetailVO.ResumeSectionVO basicInfo = findSectionByType(result, "BASIC_INFO");
            Map<String, Object> basicContent = parseContent(basicInfo.getContent());
            assertEquals("新的个人简介", basicContent.get("summary"));
            // 验证工作经历
            ResumeDetailVO.ResumeSectionVO workSection = findSectionByType(result, "WORK");
            List<Map<String, Object>> workItems = parseContentArray(workSection.getContent());
            assertEquals("2023.03-2024.12", workItems.get(0).get("period"));
            // 验证教育经历
            ResumeDetailVO.ResumeSectionVO educationSection = findSectionByType(result, "EDUCATION");
            List<Map<String, Object>> eduItems = parseContentArray(educationSection.getContent());
            @SuppressWarnings("unchecked")
            List<String> courses = (List<String>) eduItems.get(0).get("courses");
            assertEquals(2, courses.size());
        }
    }

    @Nested
    @DisplayName("删除操作测试")
    class RemovalTests {

        @Test
        @DisplayName("删除聚合类型的属性 - 只删除指定属性，不删除整个元素")
        void testRemovePropertyFromAggregateSection_ShouldOnlyRemoveProperty() {
            // 原始数据：work[0] 有 company, position, description
            ResumeDetailVO.ResumeSectionVO workSection = findSectionByType(testSections, "WORK");
            List<Map<String, Object>> originalItems = parseContentArray(workSection.getContent());
            assertEquals(4, originalItems.size());
            assertNotNull(originalItems.get(0).get("description"));

            // 删除 work[0].description（只删除属性，不删除整个 work[0]）
            OptimizeSectionResponse.Change change = createChange(
                    "removed", "work[0].description", "string",
                    "账号从0到1搭建", null
            );

            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));
            ResumeDetailVO.ResumeSectionVO resultWorkSection = findSectionByType(result, "WORK");
            List<Map<String, Object>> items = parseContentArray(resultWorkSection.getContent());

            // 验证：元素数量不变，但 description 属性被删除
            assertEquals(4, items.size());
            assertNull(items.get(0).get("description"));
            assertEquals("自媒体博主", items.get(0).get("company"));  // 其他属性保留
            assertEquals("游戏博主", items.get(0).get("position"));
        }

        @Test
        @DisplayName("删除整个数组元素 - work[0]")
        void testRemoveEntireArrayElement() {
            ResumeDetailVO.ResumeSectionVO workSection = findSectionByType(testSections, "WORK");
            List<Map<String, Object>> originalItems = parseContentArray(workSection.getContent());
            String originalFirstCompany = (String) originalItems.get(0).get("company");
            String originalSecondCompany = (String) originalItems.get(1).get("company");

            // 删除 work[0]（删除整个元素）
            OptimizeSectionResponse.Change change = createChange(
                    "removed", "work[0]", "string",
                    null, null
            );

            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));
            ResumeDetailVO.ResumeSectionVO resultWorkSection = findSectionByType(result, "WORK");
            List<Map<String, Object>> items = parseContentArray(resultWorkSection.getContent());

            // 验证：元素数量减少1，原来的 work[1] 变成 work[0]
            assertEquals(3, items.size());
            assertEquals(originalSecondCompany, items.get(0).get("company"));
        }

        @Test
        @DisplayName("连续删除多个数组元素 - 按索引升序删除 work[0], work[1]")
        void testRemoveMultipleArrayElements_SequentialDeletion() {
            // 原始数据：[A(自媒体), B(FPX), C(JDG), D(WBG)]
            ResumeDetailVO.ResumeSectionVO workSection = findSectionByType(testSections, "WORK");
            List<Map<String, Object>> originalItems = parseContentArray(workSection.getContent());
            assertEquals(4, originalItems.size());

            String companyA = (String) originalItems.get(0).get("company"); // 自媒体博主
            String companyB = (String) originalItems.get(1).get("company"); // FPX
            String companyC = (String) originalItems.get(2).get("company"); // JDG
            String companyD = (String) originalItems.get(3).get("company"); // WBG

            // 按升序删除 work[0] 和 work[1]（期望删除 A 和 B）
            List<OptimizeSectionResponse.Change> changes = List.of(
                    createChange("removed", "work[0]", "string", null, null),
                    createChange("removed", "work[1]", "string", null, null)
            );

            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, changes);
            ResumeDetailVO.ResumeSectionVO resultWorkSection = findSectionByType(result, "WORK");
            List<Map<String, Object>> items = parseContentArray(resultWorkSection.getContent());

            // 验证：应该只剩下 C(JDG) 和 D(WBG)
            assertEquals(2, items.size());
            assertEquals(companyC, items.get(0).get("company")); // JDG
            assertEquals(companyD, items.get(1).get("company")); // WBG
        }

        @Test
        @DisplayName("连续删除三个数组元素 - work[0], work[1], work[2]")
        void testRemoveThreeArrayElements() {
            // 原始数据：[A, B, C, D]
            ResumeDetailVO.ResumeSectionVO workSection = findSectionByType(testSections, "WORK");
            List<Map<String, Object>> originalItems = parseContentArray(workSection.getContent());
            String companyD = (String) originalItems.get(3).get("company"); // WBG

            // 按升序删除 work[0], work[1], work[2]（期望只保留 D）
            List<OptimizeSectionResponse.Change> changes = List.of(
                    createChange("removed", "work[0]", "string", null, null),
                    createChange("removed", "work[1]", "string", null, null),
                    createChange("removed", "work[2]", "string", null, null)
            );

            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, changes);
            ResumeDetailVO.ResumeSectionVO resultWorkSection = findSectionByType(result, "WORK");
            List<Map<String, Object>> items = parseContentArray(resultWorkSection.getContent());

            // 验证：应该只剩下 D
            assertEquals(1, items.size());
            assertEquals(companyD, items.get(0).get("company"));
        }

        @Test
        @DisplayName("混合操作 - 删除元素后修改属性")
        void testMixedOperations_DeleteThenModify() {
            // 删除 work[0]，然后修改 work[1].description
            // 原始：[A(自媒体), B(FPX), C(JDG), D(WBG)]
            // 期望：[B(FPX-修改后), C(JDG), D(WBG)]
            ResumeDetailVO.ResumeSectionVO workSection = findSectionByType(testSections, "WORK");
            List<Map<String, Object>> originalItems = parseContentArray(workSection.getContent());
            String companyB = (String) originalItems.get(1).get("company"); // FPX

            List<OptimizeSectionResponse.Change> changes = List.of(
                    createChange("removed", "work[0]", "string", null, null),
                    createChange("modified", "work[1].description", "string", null, "修改后的描述")
            );

            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, changes);
            ResumeDetailVO.ResumeSectionVO resultWorkSection = findSectionByType(result, "WORK");
            List<Map<String, Object>> items = parseContentArray(resultWorkSection.getContent());

            // 验证：3个元素，原来的 work[1](FPX) 现在是 work[0]
            assertEquals(3, items.size());
            assertEquals(companyB, items.get(0).get("company"));
            // work[1] 在删除后指向原来的 work[2](JDG)，其 description 被修改
            assertEquals("修改后的描述", items.get(1).get("description"));
        }

        @Test
        @DisplayName("删除 BASIC_INFO 的属性")
        void testRemoveBasicInfoProperty() {
            // 确保基本信息有 summary 字段
            ResumeDetailVO.ResumeSectionVO basicSection = findSectionByType(testSections, "BASIC_INFO");
            Map<String, Object> content = parseContent(basicSection.getContent());
            content.put("summary", "原有的个人简介");
            basicSection.setContent(JsonParseHelper.toJsonString(content));

            // 删除 summary 属性
            OptimizeSectionResponse.Change change = createChange(
                    "removed", "basicInfo.summary", "string",
                    "原有的个人简介", null
            );

            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, List.of(change));
            ResumeDetailVO.ResumeSectionVO resultBasic = findSectionByType(result, "BASIC_INFO");
            Map<String, Object> resultContent = parseContent(resultBasic.getContent());

            // 验证：summary 被删除，其他属性保留
            assertNull(resultContent.get("summary"));
            assertEquals("陈家尧", resultContent.get("name"));
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("sections 为 null")
        void testNullSections() {
            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(null, List.of());
            assertNull(result);
        }

        @Test
        @DisplayName("changes 为 null")
        void testNullChanges() {
            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, null);
            assertNotNull(result);
            assertEquals(testSections.size(), result.size());
        }

        @Test
        @DisplayName("changes 为空列表")
        void testEmptyChanges() {
            List<ResumeDetailVO.ResumeSectionVO> result = ResumeChangeApplier.applyChanges(testSections, List.of());
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
    private ResumeDetailVO.ResumeSectionVO findSectionByType(List<ResumeDetailVO.ResumeSectionVO> sections, String type) {
        return sections.stream()
                .filter(s -> type.equals(s.getType()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析 content 为 Map（用于 BASIC_INFO 等单对象类型）
     */
    private Map<String, Object> parseContent(String content) {
        if (content == null) {
            return new LinkedHashMap<>();
        }
        return JsonParseHelper.parseToMap(content);
    }

    /**
     * 解析 content 为 List<Map>（用于 WORK、EDUCATION 等聚合类型）
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseContentArray(String content) {
        if (content == null) {
            return new ArrayList<>();
        }
        return JsonParseHelper.parseToEntity(content, ArrayList.class);
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
            ResumeDetailVO.ResumeSectionVO skillsSection = ResumeDetailVO.ResumeSectionVO.builder()
                    .type("SKILLS")
                    .title("专业技能")
                    .content("[]")  // 聚合类型的 content 是数组 JSON
                    .build();
            testSections.add(skillsSection);
        }
    }

    /**
     * 创建测试用的 sections 数据（模拟真实简历结构）
     * 使用新的数据结构：content 为 JSON 字符串
     */
    private List<ResumeDetailVO.ResumeSectionVO> createTestSections() {
        List<ResumeDetailVO.ResumeSectionVO> sections = new ArrayList<>();

        // 基本信息 - content 是单个对象 JSON
        Map<String, Object> basicContent = new LinkedHashMap<>();
        basicContent.put("name", "陈家尧");
        basicContent.put("gender", "男");
        basicContent.put("phone", "15215575397");
        basicContent.put("email", "1776096741@qq.com");
        basicContent.put("summary", "");
        sections.add(ResumeDetailVO.ResumeSectionVO.builder()
                .type("BASIC_INFO")
                .title("基本信息")
                .content(JsonParseHelper.toJsonString(basicContent))
                .build());

        // 教育经历 - content 是数组 JSON
        List<Map<String, Object>> eduItems = new ArrayList<>();
        Map<String, Object> edu1 = new LinkedHashMap<>();
        edu1.put("id", "edu_1");
        edu1.put("school", "宿州学院");
        edu1.put("degree", "本科");
        edu1.put("major", "国际经济与贸易");
        edu1.put("period", "2017.09-2021.06");
        edu1.put("courses", List.of("宏观经济学", "微观经济学", "经济法", "会计学"));
        eduItems.add(edu1);
        sections.add(ResumeDetailVO.ResumeSectionVO.builder()
                .type("EDUCATION")
                .title("教育经历")
                .content(JsonParseHelper.toJsonString(eduItems))
                .build());

        // 工作经历 - content 是数组 JSON
        List<Map<String, Object>> workItems = new ArrayList<>();
        Map<String, Object> work1 = new LinkedHashMap<>();
        work1.put("id", "work_1");
        work1.put("company", "自媒体博主");
        work1.put("position", "游戏博主");
        work1.put("period", "2025.03-2026.03");
        work1.put("description", "账号从0到1搭建");
        work1.put("technologies", new ArrayList<>());
        workItems.add(work1);

        Map<String, Object> work2 = new LinkedHashMap<>();
        work2.put("id", "work_2");
        work2.put("company", "FPX电子竞技俱乐部");
        work2.put("position", "新媒体运营");
        work2.put("period", "2024.05-2025.03");
        work2.put("description", "负责FPX永劫无间分部");
        workItems.add(work2);

        Map<String, Object> work3 = new LinkedHashMap<>();
        work3.put("id", "work_3");
        work3.put("company", "JDG电子竞技俱乐部");
        work3.put("position", "新媒体运营");
        work3.put("period", "2022.06-2024.01");
        workItems.add(work3);

        Map<String, Object> work4 = new LinkedHashMap<>();
        work4.put("id", "work_4");
        work4.put("company", "WBG电子竞技俱乐部");
        work4.put("position", "新媒体运营");
        work4.put("period", "2021.06-2022.03");
        workItems.add(work4);

        sections.add(ResumeDetailVO.ResumeSectionVO.builder()
                .type("WORK")
                .title("工作经历")
                .content(JsonParseHelper.toJsonString(workItems))
                .build());

        // 自定义区块 - content 是数组 JSON
        List<Map<String, Object>> customItems = new ArrayList<>();
        Map<String, Object> custom1 = new LinkedHashMap<>();
        custom1.put("id", "custom_1");
        custom1.put("title", "游戏经历");
        List<Map<String, Object>> gameItems = new ArrayList<>();
        gameItems.add(Map.of("name", "三角洲行动", "description", "游戏时长2000+小时"));
        gameItems.add(Map.of("name", "英雄联盟", "description", "游戏时长2000+小时"));
        gameItems.add(Map.of("name", "MMO游戏", "description", "游戏时长 3000+小时，累计充值 5W 左右。"));
        custom1.put("items", gameItems);
        customItems.add(custom1);
        sections.add(ResumeDetailVO.ResumeSectionVO.builder()
                .type("CUSTOM")
                .title("自定义区块")
                .content(JsonParseHelper.toJsonString(customItems))
                .build());

        return sections;
    }
}
