package com.landit.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.landit.resume.entity.Resume;
import org.apache.ibatis.annotations.Mapper;

/**
 * 简历Mapper接口
 *
 * @author Azir
 */
@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {

}
