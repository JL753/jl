package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sp_wrong_question")
public class WrongQuestion {
    private Long id;
    private Long userId;
    private Long examRecordId;
    private Long examQuestionId;
    private String questionTitle;
    private String myAnswer;
    private String correctAnswer;
    private String analysis;
    private LocalDateTime createdAt;
}
