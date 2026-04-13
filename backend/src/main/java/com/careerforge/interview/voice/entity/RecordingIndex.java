package com.careerforge.interview.voice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careerforge.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 录音索引实体
 * 存储合并后的录音信息和完整文字记录
 *
 * @author Azir
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("t_recording_index")
public class RecordingIndex extends BaseEntity {

    /**
     * 关联的面试会话 ID
     */
    private String sessionId;

    /**
     * 总时长（毫秒）
     */
    private Integer totalDurationMs;

    /**
     * 合并后的完整音频路径
     */
    private String mergedAudioPath;

    /**
     * 完整文字记录（JSON 格式）
     */
    private String transcript;
}
