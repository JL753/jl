package com.iflytek.smartprep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 测验生成请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizGenerateRequest {
    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 单选题数量
     */
    private Integer choiceCount;

    /**
     * 简答题数量
     */
    private Integer essayCount;

    /**
     * 难度级别（1-5）
     */
    private Integer difficulty;
}
