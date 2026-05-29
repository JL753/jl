package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sp_exam_record")
public class ExamRecord {
    private Long id;
    private Long examId;
    private Long userId;
    private Integer score;
    private String review;
    private String answersJson;
    private LocalDateTime submittedAt;
}
