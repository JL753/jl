package com.iflytek.smartprep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识图谱生成请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeMapRequest {
    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 最大深度（默认3层）
     */
    private Integer maxDepth;

    /**
     * 是否包含概念详情
     */
    private Boolean includeDetails;
}
