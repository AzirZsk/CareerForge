package com.careerforge.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careerforge.task.entity.AsyncTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 异步任务 Mapper
 *
 * @author Azir
 */
@Mapper
public interface AsyncTaskMapper extends BaseMapper<AsyncTask> {

}
