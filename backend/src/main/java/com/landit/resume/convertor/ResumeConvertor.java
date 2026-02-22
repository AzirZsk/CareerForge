package com.landit.resume.convertor;

import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.dto.ResumeVersionVO;
import com.landit.resume.entity.Resume;
import com.landit.resume.entity.ResumeSection;
import com.landit.resume.entity.ResumeVersion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 简历对象转换器
 *
 * @author Azir
 */
@Mapper(componentModel = "spring")
public interface ResumeConvertor {

    /**
     * ResumeVersion -> ResumeVersionVO
     */
    ResumeVersionVO toVersionVO(ResumeVersion version);

    /**
     * List<ResumeVersion> -> List<ResumeVersionVO>
     */
    List<ResumeVersionVO> toVersionVOList(List<ResumeVersion> versions);

    /**
     * Resume -> ResumeVersion（用于创建版本快照）
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "resumeId", source = "id")
    @Mapping(target = "changeSummary", ignore = true)
    @Mapping(target = "changeType", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    ResumeVersion toResumeVersion(Resume resume);

    /**
     * Resume -> ResumeDetailVO（基础映射，不含sections）
     */
    @Mapping(target = "sections", ignore = true)
    @Mapping(target = "overallScore", source = "score")
    @Mapping(target = "formatScore", constant = "0")
    @Mapping(target = "contentScore", constant = "0")
    @Mapping(target = "analyzed", constant = "false")
    ResumeDetailVO toDetailVO(Resume resume);

    /**
     * ResumeSection -> ResumeSectionVO
     */
    @Mapping(target = "suggestions", ignore = true)
    ResumeDetailVO.ResumeSectionVO toSectionVO(ResumeSection section);

    /**
     * List<ResumeSection> -> List<ResumeSectionVO>
     */
    List<ResumeDetailVO.ResumeSectionVO> toSectionVOList(List<ResumeSection> sections);

}
