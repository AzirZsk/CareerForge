package com.landit.resume.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ChangeFieldTranslator 单元测试
 *
 * @author Azir
 */
@DisplayName("ChangeFieldTranslator 测试")
class ChangeFieldTranslatorTest {

    private List<Map<String, Object>> testSections;

    @BeforeEach
    void setUp() {
        testSections = createTestSections();
    }

    @Test
    @DisplayName("操作类型翻译")
    void testTranslateType() {
        assertEquals("新增", ChangeFieldTranslator.translateType("added"));
        assertEquals("修改", ChangeFieldTranslator.translateType("modified"));
        assertEquals("删除", ChangeFieldTranslator.translateType("removed"));
        assertEquals("未知", ChangeFieldTranslator.translateType(null));
        assertEquals("unknown", ChangeFieldTranslator.translateType("unknown"));
    }

    @Test
    @DisplayName("区块名称翻译")
    void testTranslateSectionName() {
        assertEquals("基本信息", ChangeFieldTranslator.translateField("basicInfo"));
        assertEquals("教育经历", ChangeFieldTranslator.translateField("education"));
        assertEquals("工作经历", ChangeFieldTranslator.translateField("work"));
        assertEquals("项目经历", ChangeFieldTranslator.translateField("projects"));
        assertEquals("专业技能", ChangeFieldTranslator.translateField("skills"));
        assertEquals("证书荣誉", ChangeFieldTranslator.translateField("certificates"));
        assertEquals("开源贡献", ChangeFieldTranslator.translateField("openSource"));
    }

    @Test
    @DisplayName("基本信息字段翻译")
    void testTranslateBasicInfoFields() {
        assertEquals("基本信息-姓名", ChangeFieldTranslator.translateField("basicInfo.name"));
        assertEquals("基本信息-个人简介", ChangeFieldTranslator.translateField("basicInfo.summary"));
        assertEquals("基本信息-所在地", ChangeFieldTranslator.translateField("basicInfo.location"));
        assertEquals("基本信息-电话", ChangeFieldTranslator.translateField("basicInfo.phone"));
        assertEquals("基本信息-邮箱", ChangeFieldTranslator.translateField("basicInfo.email"));
    }

    @Test
    @DisplayName("工作经历字段翻译")
    void testTranslateWorkFields() {
        assertEquals("工作经历[1]-公司名称", ChangeFieldTranslator.translateField("work[0].company"));
        assertEquals("工作经历[1]-工作描述", ChangeFieldTranslator.translateField("work[0].description"));
        assertEquals("工作经历[1]-工作成果", ChangeFieldTranslator.translateField("work[0].achievements"));
        assertEquals("工作经历[2]-时间段", ChangeFieldTranslator.translateField("work[1].period"));
        assertEquals("工作经历[3]-技术栈", ChangeFieldTranslator.translateField("work[2].technologies"));
    }

    @Test
    @DisplayName("项目经历字段翻译")
    void testTranslateProjectsFields() {
        assertEquals("项目经历[1]-项目名称", ChangeFieldTranslator.translateField("projects[0].name"));
        assertEquals("项目经历[1]-项目描述", ChangeFieldTranslator.translateField("projects[0].description"));
        assertEquals("项目经历[1]-项目成果", ChangeFieldTranslator.translateField("projects[0].achievements"));
        assertEquals("项目经历[1]-技术栈", ChangeFieldTranslator.translateField("projects[0].technologies"));
    }

    @Test
    @DisplayName("嵌套数组字段翻译")
    void testTranslateNestedArrayFields() {
        assertEquals("工作经历[1]-工作成果[1]", ChangeFieldTranslator.translateField("work[0].achievements[0]"));
        assertEquals("工作经历[1]-工作成果[2]", ChangeFieldTranslator.translateField("work[0].achievements[1]"));
        assertEquals("教育经历[1]-主修课程[1]", ChangeFieldTranslator.translateField("education[0].courses[0]"));
        assertEquals("教育经历[1]-校内荣誉[2]", ChangeFieldTranslator.translateField("education[0].honors[1]"));
    }

    @Test
    @DisplayName("技能字段翻译")
    void testTranslateSkillsFields() {
        assertEquals("专业技能[1]", ChangeFieldTranslator.translateField("skills[0]"));
        assertEquals("专业技能[2]", ChangeFieldTranslator.translateField("skills[1]"));
        assertEquals("专业技能[1]-技能名称", ChangeFieldTranslator.translateField("skills[0].name"));
        assertEquals("专业技能[1]-熟练度", ChangeFieldTranslator.translateField("skills[0].level"));
    }

    @Test
    @DisplayName("自定义区块字段翻译 - 无上下文")
    void testTranslateCustomSectionsWithoutContext() {
        // 无上下文时使用默认翻译
        assertEquals("自定义区块", ChangeFieldTranslator.translateField("customSections"));
        assertEquals("自定义区块[1]-区块标题", ChangeFieldTranslator.translateField("customSections[0].title"));
    }

    @Test
    @DisplayName("自定义区块字段翻译 - 有上下文")
    void testTranslateCustomSectionsWithContext() {
        // 有上下文时使用动态 title
        assertEquals("游戏经历[1]-区块标题",
                ChangeFieldTranslator.translateField("customSections[0].title", testSections));
        assertEquals("游戏经历[1]-内容项列表[1]-描述",
                ChangeFieldTranslator.translateField("customSections[0].items[0].description", testSections));
        assertEquals("志愿者经历[2]-内容项列表[1]-亮点",
                ChangeFieldTranslator.translateField("customSections[1].items[0].highlights", testSections));
    }

    @Test
    @DisplayName("未知字段处理")
    void testTranslateUnknownField() {
        assertEquals("未知字段", ChangeFieldTranslator.translateField(null));
        assertEquals("未知字段", ChangeFieldTranslator.translateField(""));
        assertEquals("unknownField", ChangeFieldTranslator.translateField("unknownField"));
    }

    /**
     * 创建测试用的 sections 数据
     */
    private List<Map<String, Object>> createTestSections() {
        List<Map<String, Object>> sections = new ArrayList<>();

        // 添加 CUSTOM 类型的区块
        Map<String, Object> customSection = new LinkedHashMap<>();
        customSection.put("type", "CUSTOM");

        List<Map<String, Object>> items = new ArrayList<>();

        // 第一个自定义区块：游戏经历
        Map<String, Object> item1 = new LinkedHashMap<>();
        item1.put("id", "custom_1");
        Map<String, Object> content1 = new LinkedHashMap<>();
        content1.put("title", "游戏经历");
        content1.put("items", List.of(
                Map.of("name", "LOL", "description", "王者段位")
        ));
        item1.put("content", content1);
        items.add(item1);

        // 第二个自定义区块：志愿者经历
        Map<String, Object> item2 = new LinkedHashMap<>();
        item2.put("id", "custom_2");
        Map<String, Object> content2 = new LinkedHashMap<>();
        content2.put("title", "志愿者经历");
        content2.put("items", List.of(
                Map.of("name", "社区服务", "highlights", List.of("帮助老人"))
        ));
        item2.put("content", content2);
        items.add(item2);

        customSection.put("items", items);
        sections.add(customSection);

        return sections;
    }
}
