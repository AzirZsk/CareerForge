package com.landit.resume.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 应用优化变更请求
 * 用于批量更新优化后的简历区块内容
 *
 * @author Azir
 */
@Data
public class ApplyOptimizeRequest {

    @NotNull(message = "优化前数据不能为空")
    @Valid
    private List<SectionDataItem> beforeSection;

    @NotNull(message = "优化后数据不能为空")
    @NotEmpty(message = "优化后数据不能为空")
    @Valid
    private List<SectionDataItem> afterSection;

    /**
     * 区块数据项
     */
    @Data
    public static class SectionDataItem {
        /**
         * 区块ID（更新时必填，新增时为空）
         */
        private String id;

        /**
         * 区块类型（如 WORK, PROJECT 等）
         */
        private String type;

        /**
         * 区块标题
         */
        private String title;

        /**
         * 区块内容（JSON字符串）
         */
        @NotBlank(message = "区块内容不能为空")
        private String content;
    }
}
