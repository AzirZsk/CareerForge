package com.careerforge.resume.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.careerforge.common.enums.SectionType;
import com.careerforge.common.util.JsonParseHelper;
import com.careerforge.resume.dto.ResumeDetailVO;
import com.careerforge.resume.dto.ResumeStructuredData;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历区块ID简短化工具类
 * 将简历区块的 sectionId 简短化为 {type}_{index} 格式，避免 AI 幻觉
 *
 * <p>简短标识符格式示例：work_1, project_2, skills_1, custom_0</p>
 *
 * @author Azir
 */
public final class ResumeSectionShortener {

    private ResumeSectionShortener() {
        // 工具类禁止实例化
    }

    /**
     * 将简历区块ID简短化
     *
     * @param resumeDetail 简历详情
     * @return ShortenResult 包含简短化后的内容和ID映射
     */
    public static ShortenResult shorten(ResumeDetailVO resumeDetail) {
        if (resumeDetail == null) {
            return new ShortenResult("", Map.of());
        }

        Map<String, String> shortIdToRealIdMap = new HashMap<>();
        String shortenedContent = buildStructuredResumeContent(resumeDetail, shortIdToRealIdMap);

        return new ShortenResult(shortenedContent, shortIdToRealIdMap);
    }

    /**
     * 构建结构化的简历内容，使用简短标识符替代雪花ID
     * 简短标识符格式：{type}_{index}，如 work_1, project_2, skills_1
     * 对于 CUSTOM 类型，展开每个 item 作为独立评分单元：custom_0, custom_1, ...
     *
     * @param resumeDetail      简历详情
     * @param shortIdToRealIdMap 用于存储简短标识符到真实ID的映射
     * @return 结构化的内容字符串
     */
    private static String buildStructuredResumeContent(
            ResumeDetailVO resumeDetail, Map<String, String> shortIdToRealIdMap) {
        StringBuilder sb = new StringBuilder();
        List<ResumeDetailVO.ResumeSectionVO> sections = resumeDetail.getSections();

        if (sections == null || sections.isEmpty()) {
            return "";
        }

        // 用于统计每种类型的序号
        Map<String, Integer> typeCounters = new HashMap<>();
        // 用于统计 custom 的序号
        int customIndex = 0;

        for (ResumeDetailVO.ResumeSectionVO section : sections) {
            String type = section.getType() != null ? section.getType() : "section";

            // CUSTOM 类型特殊处理：每个 CUSTOM 区块独立一条记录，content 直接是 items 数组
            if (SectionType.CUSTOM.getCode().equals(type)) {
                // content 直接是 items 数组
                List<ResumeStructuredData.ContentItem> items = JsonParseHelper.parseToEntity(
                        section.getContent(),
                        new TypeReference<List<ResumeStructuredData.ContentItem>>() {}
                );
                if (items != null && !items.isEmpty()) {
                    // 每个 CUSTOM 区块独立一条记录，直接使用 section.id
                    String shortId = "custom_" + customIndex++;
                    // 映射格式：sectionId（当前区块独立，无需 itemIndex）
                    shortIdToRealIdMap.put(shortId, section.getId());
                    // title 从 section.getTitle() 获取
                    String title = section.getTitle();
                    // 构建单个 custom item 的 JSON
                    Map<String, Object> jsonMap = new LinkedHashMap<>();
                    jsonMap.put("id", shortId);
                    jsonMap.put("type", "CUSTOM");
                    jsonMap.put("title", title);
                    jsonMap.put("content", items);
                    sb.append(JsonParseHelper.toJsonString(jsonMap)).append("\n\n");
                }
            } else {
                // 其他类型按 section 级别处理
                int index = typeCounters.getOrDefault(type.toLowerCase(), 0) + 1;
                typeCounters.put(type.toLowerCase(), index);

                // 生成简短标识符：type_index（如 work_1, project_2）
                String shortId = type.toLowerCase() + "_" + index;

                // 记录映射关系
                if (section.getId() != null) {
                    shortIdToRealIdMap.put(shortId, section.getId());
                }

                // 构建使用简短标识符的 JSON
                sb.append(buildSectionJsonWithShortId(section, shortId)).append("\n\n");
            }
        }
        return sb.toString();
    }

    /**
     * 构建单个 section 的 JSON，仅替换 section 级别的 id 为简短标识符
     * 统一使用 content 字段存储内容
     */
    private static String buildSectionJsonWithShortId(
            ResumeDetailVO.ResumeSectionVO section, String shortId) {
        // 使用 Map 构建 JSON，避免手动转义
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        jsonMap.put("id", shortId);
        jsonMap.put("type", section.getType());
        if (section.getTitle() != null) {
            jsonMap.put("title", section.getTitle());
        }
        // content 统一存储数据（JSON 字符串），需要先解析为对象再序列化
        if (section.getContent() != null) {
            Object contentObj = JsonParseHelper.parseToEntity(
                    section.getContent(), new TypeReference<Object>() {});
            jsonMap.put("content", contentObj);
        }
        return JsonParseHelper.toJsonString(jsonMap);
    }

    /**
     * 简短化结果
     */
    @Data
    @AllArgsConstructor
    public static class ShortenResult {
        /**
         * 简短化后的内容（JSON 字符串）
         */
        private String shortenedContent;

        /**
         * 短ID -> 真实ID 映射
         * key: 简短标识符（如 work_1, custom_0）
         * value: 原始雪花ID
         */
        private Map<String, String> shortIdToRealIdMap;
    }
}
