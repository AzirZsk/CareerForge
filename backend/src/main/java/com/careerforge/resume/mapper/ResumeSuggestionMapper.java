package com.careerforge.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careerforge.resume.entity.ResumeSuggestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 简历优化建议 Mapper
 *
 * @author Azir
 */
@Mapper
public interface ResumeSuggestionMapper extends BaseMapper<ResumeSuggestion> {

}
