package com.iflytek.smartprep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 测验题目
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestion {
    /**
     * 题目ID
     */
    private String id;

    /**
     * 题目类型：choice（单选）、essay（简答）
     */
    private String type;

    /**
     * 题目内容
     */
    private String question;

    /**
     * 选项（仅单选题有）
     */
    private List<String> options;

    /**
     * 正确答案（单选题为选项索引，简答题为参考答案）
     */
    private String correctAnswer;

    /**
     * 知识点标签
     */
    private String knowledgePoint;

    /**
     * 难度（1-5）
     */
    private Integer difficulty;

    /**
     * 分值
     */
    private Integer score;
}
