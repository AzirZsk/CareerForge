package com.careerforge.chat.dto.tool;

import com.careerforge.resume.dto.ResumeDetailVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取简历响应类
 * 返回简历完整信息
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetResumeResponse extends ToolResponse {

    /**
     * 简历数据
     */
    private ResumeData data;

    /**
     * 简历数据内部类
     */
    @Data
    public static class ResumeData {
        private String name;
        private String targetPosition;
        private Integer overallScore;
        private List<SectionData> sections;
    }

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
     * 从ResumeDetailVO构建成功响应
     */
    public static GetResumeResponse from(ResumeDetailVO resume) {
        GetResumeResponse response = new GetResumeResponse();
        response.setSuccess(true);

        ResumeData data = new ResumeData();
        data.setName(resume.getName());
        data.setTargetPosition(resume.getTargetPosition());
        data.setOverallScore(resume.getOverallScore());

        if (resume.getSections() != null) {
            List<SectionData> sections = new ArrayList<>();
            for (ResumeDetailVO.ResumeSectionVO section : resume.getSections()) {
                SectionData sd = new SectionData();
                sd.setId(section.getId());
                sd.setType(section.getType());
                sd.setTitle(section.getTitle());
                sd.setContent(section.getContent());
                sd.setScore(section.getScore());
                sections.add(sd);
            }
            data.setSections(sections);
        }

        response.setData(data);
        return response;
    }
}
