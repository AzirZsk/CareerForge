package com.landit.interview.voice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.landit.interview.voice.entity.InterviewRecording;
import org.apache.ibatis.annotations.Mapper;

/**
 * 面试录音片段 Mapper
 *
 * @author Azir
 */
@Mapper
public interface InterviewRecordingMapper extends BaseMapper<InterviewRecording> {
}
