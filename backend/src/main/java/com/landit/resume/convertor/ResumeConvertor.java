package com.landit.resume.convertor;

import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.entity.Resume;
import com.landit.resume.entity.ResumeSection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 简历转换器
 *
 * @author Azir
 */
@Mapper(componentModel = "spring")
public interface ResumeConvertor {

    /**
     * Resume -> ResumeDetailVO
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "targetPosition", source = "targetPosition")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "score", source = "score")
    @Mapping(target = "completeness", source = "completeness")
    @Mapping(target = "isPrimary", source = "isPrimary")
    @Mapping(target = "overallScore", source = "overallScore")
    @Mapping(target = "contentScore", source = "contentScore")
    @Mapping(target = "structureScore", source = "structureScore")
    @Mapping(target = "matchingScore", source = "matchingScore")
    @Mapping(target = "competitivenessScore", source = "competitivenessScore")
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
