package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sp_question_bank")
public class QuestionBankItem {
    private Long id;
    private Long creatorUserId;
    private String course;
    private String title;
    private String category1;
    private String category2;
    private String category3;
    private String questionType;
    private String difficulty;
    private String optionsJson;
    private String correctAnswer;
    private String analysis;
    private String imageUrl;
    private LocalDateTime createdAt;
}
