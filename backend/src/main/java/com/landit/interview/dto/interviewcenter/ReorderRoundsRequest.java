package com.landit.interview.dto.interviewcenter;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 重排面试轮次顺序请求 DTO
 *
 * @author Azir
 */
@Data
public class ReorderRoundsRequest {

    @NotEmpty(message = "轮次ID列表不能为空")
    private List<String> roundIds;

}
