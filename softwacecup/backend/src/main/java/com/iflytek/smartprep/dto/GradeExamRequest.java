package com.iflytek.smartprep.dto;

import lombok.Data;

import java.util.List;

@Data
public class GradeExamRequest {
    private Long recordId;
    private Integer score;
    private String review;
    private String overallReview;
    private List<QuestionAnnotation> questionAnnotations;

    @Data
    public static class QuestionAnnotation {
        private Long questionId;
        private Integer score;
        private String comment;
    }
}
