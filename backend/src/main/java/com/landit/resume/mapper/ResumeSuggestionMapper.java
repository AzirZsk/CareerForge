package com.landit.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.landit.resume.entity.ResumeSuggestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 简历优化建议 Mapper
 *
 * @author Azir
 */
@Mapper
public interface ResumeSuggestionMapper extends BaseMapper<ResumeSuggestion> {

}
