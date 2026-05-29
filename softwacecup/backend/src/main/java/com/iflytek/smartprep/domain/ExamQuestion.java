package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sp_exam_question")
public class ExamQuestion {
    private Long id;
    private Long examId;
    private Integer questionNo;
    private String questionType;
    private String title;
    private Integer score;
    private String answerKey;
}
