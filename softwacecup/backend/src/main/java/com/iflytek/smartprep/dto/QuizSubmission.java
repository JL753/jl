package com.iflytek.smartprep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 学生答题提交
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmission {
    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 答案映射（题目ID -> 学生答案）
     */
    private Map<String, String> answers;
}
