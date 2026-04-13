package com.careerforge.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 简历图片解析响应 DTO
 * 用于返回简历转换后的图片列表
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeImagesResponse {

    /**
     * 图片 DataURL 列表
     * 格式：data:image/png;base64,xxx
     */
    private List<String> images;

    /**
     * 图片数量
     */
    private Integer count;

    public static ResumeImagesResponse of(List<String> images) {
        return ResumeImagesResponse.builder()
                .images(images)
                .count(images != null ? images.size() : 0)
                .build();
    }

}
