package com.landit.chat.dto.tool;

import com.landit.resume.dto.ResumeDetailVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 创建简历响应类
 * 返回创建成功后的简历基本信息
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CreateResumeResponse extends ToolResponse {

    /**
     * 简历数据
     */
    private ResumeData data;

    /**
     * 简历数据内部类
     */
    @Data
    public static class ResumeData {
        private String resumeId;
        private String name;
        private String targetPosition;
        private String message;
    }

    /**
     * 从ResumeDetailVO构建成功响应
     */
    public static CreateResumeResponse from(ResumeDetailVO resume) {
        CreateResumeResponse response = new CreateResumeResponse();
        response.setSuccess(true);

        ResumeData data = new ResumeData();
        data.setResumeId(resume.getId());
        data.setName(resume.getName());
        data.setTargetPosition(resume.getTargetPosition());
        data.setMessage("简历创建成功！您现在可以选择这份简历进行进一步的操作。");

        response.setData(data);
        return response;
    }
}
