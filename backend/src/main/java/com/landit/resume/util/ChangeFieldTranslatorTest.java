package com.landit.resume.util;

/**
 * ChangeFieldTranslator 测试类
 *
 * @author Azir
 */
public class ChangeFieldTranslatorTest {

    public static void main(String[] args) {
        System.out.println("========== ChangeFieldTranslator 测试 ==========\n");

        // 测试区块名称翻译
        System.out.println("--- 区块名称翻译 ---");
        testTranslate("basicInfo", "基本信息");
        testTranslate("education", "教育经历");
        testTranslate("work", "工作经历");
        testTranslate("projects", "项目经历");
        testTranslate("skills", "专业技能");
        testTranslate("certificates", "证书荣誉");
        testTranslate("openSource", "开源贡献");
        testTranslate("customSections", "自定义区块");

        // 测试基本信息字段翻译
        System.out.println("\n--- 基本信息字段翻译 ---");
        testTranslate("basicInfo.name", "基本信息-姓名");
        testTranslate("basicInfo.summary", "基本信息-个人简介");
        testTranslate("basicInfo.location", "基本信息-所在地");
        testTranslate("basicInfo.website", "基本信息-个人网站");
        testTranslate("basicInfo.github", "基本信息-GitHub");

        // 测试工作经历字段翻译
        System.out.println("\n--- 工作经历字段翻译 ---");
        testTranslate("work[0].company", "工作经历[1]-公司名称");
        testTranslate("work[0].description", "工作经历[1]-工作描述");
        testTranslate("work[0].achievements", "工作经历[1]-工作成果");
        testTranslate("work[1].period", "工作经历[2]-时间段");
        testTranslate("work[2].technologies", "工作经历[3]-技术栈");

        // 测试项目经历字段翻译
        System.out.println("\n--- 项目经历字段翻译 ---");
        testTranslate("projects[0].name", "项目经历[1]-项目名称");
        testTranslate("projects[0].description", "项目经历[1]-项目描述");
        testTranslate("projects[0].achievements", "项目经历[1]-项目成果");
        testTranslate("projects[0].technologies", "项目经历[1]-技术栈");

        // 测试嵌套数组字段翻译
        System.out.println("\n--- 嵌套数组字段翻译 ---");
        testTranslate("work[0].achievements[0]", "工作经历[1]-工作成果[1]");
        testTranslate("projects[0].achievements[1]", "项目经历[1]-项目成果[2]");
        testTranslate("education[0].courses[0]", "教育经历[1]-主修课程[1]");
        testTranslate("customSections[0].items[0]", "自定义区块[1]-内容项列表[1]");

        // 测试自定义区块字段翻译
        System.out.println("\n--- 自定义区块字段翻译 ---");
        testTranslate("customSections[0].title", "自定义区块[1]-区块标题");
        testTranslate("customSections[0].items[0].description", "自定义区块[1]-内容项列表[1]-描述");
        testTranslate("customSections[1].items[2].highlights", "自定义区块[2]-内容项列表[3]-亮点");

        // 测试教育经历字段翻译
        System.out.println("\n--- 教育经历字段翻译 ---");
        testTranslate("education[0].school", "教育经历[1]-学校名称");
        testTranslate("education[0].major", "教育经历[1]-专业");
        testTranslate("education[0].courses", "教育经历[1]-主修课程");
        testTranslate("education[0].honors", "教育经历[1]-校内荣誉");

        // 测试技能字段翻译
        System.out.println("\n--- 技能字段翻译 ---");
        testTranslate("skills[0]", "专业技能[1]");
        testTranslate("skills[1]", "专业技能[2]");
        testTranslate("skills[0].name", "专业技能[1]-技能名称");
        testTranslate("skills[0].level", "专业技能[1]-熟练度");

        // 测试操作类型翻译
        System.out.println("\n--- 操作类型翻译 ---");
        testTranslateType("added", "新增");
        testTranslateType("modified", "修改");
        testTranslateType("removed", "删除");

        System.out.println("\n========== 测试完成 ==========");
    }

    private static void testTranslate(String field, String expected) {
        String result = ChangeFieldTranslator.translateField(field);
        String status = result.equals(expected) ? "✓" : "✗";
        System.out.printf("%s %-45s => %-30s (期望: %s)%n", status, field, result, expected);
    }

    private static void testTranslateType(String type, String expected) {
        String result = ChangeFieldTranslator.translateType(type);
        String status = result.equals(expected) ? "✓" : "✗";
        System.out.printf("%s %-15s => %-10s (期望: %s)%n", status, type, result, expected);
    }
}
