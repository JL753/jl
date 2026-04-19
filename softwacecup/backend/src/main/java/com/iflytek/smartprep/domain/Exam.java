package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sp_exam")
public class Exam {
    private Long id;
    private Long creatorUserId;
    private String examName;
    private String course;
    private Integer duration;
    private String topic;
    private String status;
    private Integer questionCount;
    private LocalDateTime createdAt;
}
