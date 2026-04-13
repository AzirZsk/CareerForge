package com.careerforge.interview.dto.interviewcenter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 准备事项关联资源 DTO
 *
 * @author Azir
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreparationResource {

    /**
     * 资源类型：link/note/code/video
     */
    private String type;

    /**
     * 资源标题
     */
    private String title;

    /**
     * 链接地址（type=link时必填）
     */
    private String url;

    /**
     * 内容（type=note时必填）
     */
    private String content;

}
