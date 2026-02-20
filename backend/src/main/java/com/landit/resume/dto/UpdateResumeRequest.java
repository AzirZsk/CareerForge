package com.landit.resume.dto;

import com.landit.resume.entity.ResumeSection;
import lombok.Data;

import java.util.List;

/**
 * 更新简历请求DTO
 *
 * @author Azir
 */
@Data
public class UpdateResumeRequest {

    private String name;

    private List<ResumeSection> sections;

}
