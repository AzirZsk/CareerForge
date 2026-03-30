package com.landit.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 应用修改请求
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyChangesRequest {

    /**
     * 简历ID
     */
    private String resumeId;

    /**
     * 区块变更列表
     */
    private List<SectionChange> changes;
}
