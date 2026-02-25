package com.landit.resume.dto;

import lombok.Data;

import java.util.Map;

/**
 * 人工审核请求
 *
 * @author Azir
 */
@Data
public class ReviewGraphRequest {
    /**
     * 会话线程ID
     */
    private String threadId;

    /**
     * 是否通过审核
     */
    private boolean approved;

    /**
     * 修改内容（可选）
     */
    private Map<String, Object> modifications;
}
