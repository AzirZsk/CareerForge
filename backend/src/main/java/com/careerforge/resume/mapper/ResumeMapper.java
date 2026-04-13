package com.careerforge.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careerforge.resume.entity.Resume;
import org.apache.ibatis.annotations.Mapper;

/**
 * 简历Mapper接口
 *
 * @author Azir
 */
@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {

}
