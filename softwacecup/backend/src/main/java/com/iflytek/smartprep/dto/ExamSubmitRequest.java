package com.iflytek.smartprep.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExamSubmitRequest {
    private Long examId;
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private Long questionId;
        private String answer;
    }
}
