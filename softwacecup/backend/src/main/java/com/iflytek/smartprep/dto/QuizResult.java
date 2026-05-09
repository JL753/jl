package com.iflytek.smartprep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 测验批改结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResult {
    /**
     * 总分
     */
    private Integer totalScore;

    /**
     * 得分
     */
    private Integer earnedScore;

    /**
     * 正确率
     */
    private Double accuracy;

    /**
     * 每道题的批改详情（题目ID -> 批改详情）
     */
    private Map<String, QuestionGrading> gradingDetails;

    /**
     * 能力维度变化
     */
    private Map<String, AbilityChange> abilityChanges;

    /**
     * 总体评价
     */
    private String overallComment;

    /**
     * 单题批改详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionGrading {
        /**
         * 是否正确
         */
        private Boolean correct;

        /**
         * 得分
         */
        private Integer score;

        /**
         * 学生答案
         */
        private String studentAnswer;

        /**
         * 正确答案
         */
        private String correctAnswer;

        /**
         * AI详细解析
         */
        private String explanation;

        /**
         * 知识点
         */
        private String knowledgePoint;
    }

    /**
     * 能力维度变化
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbilityChange {
        /**
         * 维度名称
         */
        private String dimension;

        /**
         * 变化前分数
         */
        private Integer before;

        /**
         * 变化后分数
         */
        private Integer after;

        /**
         * 变化值
         */
        private Integer delta;

        /**
         * 变化原因
         */
        private String reason;
    }
}
