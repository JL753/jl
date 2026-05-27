package com.iflytek.smartprep.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuestionItemRequest {
    private String course;
    private String title;
    private String category1;
    private String category2;
    private String category3;
    private String questionType;
    private String difficulty;
    private List<String> options;
    private String correctAnswer;
    private String analysis;
    private String imageUrl;
}
