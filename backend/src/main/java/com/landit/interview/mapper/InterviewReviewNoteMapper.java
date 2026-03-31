package com.landit.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.landit.interview.entity.InterviewReviewNote;
import org.apache.ibatis.annotations.Mapper;

/**
 * 面试复盘笔记 Mapper 接口
 *
 * @author Azir
 */
@Mapper
public interface InterviewReviewNoteMapper extends BaseMapper<InterviewReviewNote> {

}
