package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_exercise_attempt")
public class ExerciseAttempt {
    private Long id;
    private Long userId;
    private Long lessonId;
    private Long exerciseId;
    private Integer difficulty;
    private Integer correct; // 0=wrong, 1=correct
    private LocalDateTime createdAt;
}
