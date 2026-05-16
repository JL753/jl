package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sp_learning_activity")
public class LearningActivity {
    private Long id;
    private Long userId;
    private LocalDate activityDate;
    private Integer activityScore;
    private Integer loginCount;
    private Integer studyMinutes;
    private Integer questionCount;
    private Integer resourceViewCount;
    private LocalDateTime createdAt;
}
