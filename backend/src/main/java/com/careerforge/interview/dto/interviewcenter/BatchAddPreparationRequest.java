package com.careerforge.interview.dto.interviewcenter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量添加准备事项请求
 *
 * @author Azir
 */
@Data
public class BatchAddPreparationRequest {

    @NotEmpty(message = "准备事项列表不能为空")
    @Valid
    private List<AddPreparationRequest> items;

}
