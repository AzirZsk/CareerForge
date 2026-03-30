package com.landit.chat.dto.tool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Tool响应基础抽象类
 * 所有Tool响应的基类，提供统一的响应结构
 *
 * @author Azir
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class ToolResponse {

    /**
     * 操作是否成功
     */
    private Boolean success;

    /**
     * 错误信息（仅在失败时返回）
     */
    private String error;
}
