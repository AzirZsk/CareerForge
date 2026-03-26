package com.landit.chat.dto.tool;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 区块操作建议响应类
 * 用于返回区块的新增、更新、删除建议
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SectionSuggestionResponse extends ToolResponse {

    /**
     * 操作类型：add / update / delete
     */
    private String action;

    /**
     * 简历ID
     */
    private String resumeId;

    /**
     * 区块ID（更新和删除时使用）
     */
    private String sectionId;

    /**
     * 区块类型
     */
    private String sectionType;

    /**
     * 区块标题
     */
    private String sectionTitle;

    /**
     * 修改前内容（更新和删除时使用）
     */
    private String beforeContent;

    /**
     * 修改后内容（新增和更新时使用）
     */
    private String afterContent;

    /**
     * 操作描述
     */
    private String description;

    /**
     * 创建新增建议
     */
    public static SectionSuggestionResponse forAdd(
            String resumeId,
            String sectionType,
            String sectionTitle,
            String content) {
        SectionSuggestionResponse response = new SectionSuggestionResponse();
        response.setSuccess(true);
        response.setAction("add");
        response.setResumeId(resumeId);
        response.setSectionType(sectionType);
        response.setSectionTitle(sectionTitle);
        response.setAfterContent(content);
        response.setDescription("新增区块: " + sectionTitle);
        return response;
    }

    /**
     * 创建更新建议
     */
    public static SectionSuggestionResponse forUpdate(
            String sectionId,
            String sectionType,
            String sectionTitle,
            String beforeContent,
            String afterContent,
            String reason) {
        SectionSuggestionResponse response = new SectionSuggestionResponse();
        response.setSuccess(true);
        response.setAction("update");
        response.setSectionId(sectionId);
        response.setSectionType(sectionType);
        response.setSectionTitle(sectionTitle);
        response.setBeforeContent(beforeContent);
        response.setAfterContent(afterContent);
        response.setDescription(reason != null ? reason : "更新区块内容");
        return response;
    }

    /**
     * 创建删除建议
     */
    public static SectionSuggestionResponse forDelete(
            String sectionId,
            String sectionType,
            String sectionTitle,
            String beforeContent,
            String reason) {
        SectionSuggestionResponse response = new SectionSuggestionResponse();
        response.setSuccess(true);
        response.setAction("delete");
        response.setSectionId(sectionId);
        response.setSectionType(sectionType);
        response.setSectionTitle(sectionTitle);
        response.setBeforeContent(beforeContent);
        response.setDescription(reason != null ? reason : "删除区块");
        return response;
    }
}
