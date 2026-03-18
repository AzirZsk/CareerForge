package com.landit.resume.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 保存定制简历请求DTO
 * 用于将定制后的简历保存为新的派生简历
 *
 * @author Azir
 */
@Data
public class SaveTailoredResumeRequest {

    @NotBlank(message = "目标岗位不能为空")
    private String targetPosition;

    private String resumeName;

    @NotBlank(message = "职位描述不能为空")
    private String jobDescription;

    @NotEmpty(message = "定制内容不能为空")
    @Valid
    private List<SectionDataItem> afterSection;

    /**
     * 区块数据项
     */
    @Data
    public static class SectionDataItem {
        private String id;
        private String type;
        private String title;
        @NotBlank(message = "区块内容不能为空")
        private String content;
    }
}
