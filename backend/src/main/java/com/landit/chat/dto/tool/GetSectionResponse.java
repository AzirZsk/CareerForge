package com.landit.chat.dto.tool;

import com.landit.resume.dto.ResumeDetailVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 获取区块响应类
 * 返回单个区块的详细信息
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetSectionResponse extends ToolResponse {

    /**
     * 区块数据
     */
    private SectionData data;

    /**
     * 区块数据内部类
     */
    @Data
    public static class SectionData {
        private String id;
        private String type;
        private String title;
        private String content;
        private Integer score;
    }

    /**
     * 从ResumeSectionVO构建成功响应
     */
    public static GetSectionResponse from(ResumeDetailVO.ResumeSectionVO section) {
        GetSectionResponse response = new GetSectionResponse();
        response.setSuccess(true);

        SectionData data = new SectionData();
        data.setId(section.getId());
        data.setType(section.getType());
        data.setTitle(section.getTitle());
        data.setContent(section.getContent());
        data.setScore(section.getScore());

        response.setData(data);
        return response;
    }
}
