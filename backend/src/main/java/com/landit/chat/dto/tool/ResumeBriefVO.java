package com.landit.chat.dto.tool;

import com.landit.resume.dto.ResumeListVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简历简要信息VO
 * 用于AI工具返回简历列表的简要信息
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeBriefVO {

    /**
     * 简历ID
     */
    private String id;

    /**
     * 简历名称
     */
    private String name;

    /**
     * 目标岗位
     */
    private String targetPosition;

    /**
     * 简历状态（OPTIMIZED / DRAFT）
     */
    private String status;

    /**
     * 简历评分（0-100）
     */
    private Integer score;

    /**
     * 从 ResumeListVO 转换
     */
    public static ResumeBriefVO from(ResumeListVO vo) {
        return ResumeBriefVO.builder()
                .id(vo.getId())
                .name(vo.getName())
                .targetPosition(vo.getTargetPosition())
                .status(vo.getStatus())
                .score(vo.getScore())
                .build();
    }
}
