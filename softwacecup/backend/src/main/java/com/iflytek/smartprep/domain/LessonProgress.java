package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_lesson_progress")
public class LessonProgress {
    private Long id;
    private Long userId;
    private Long subChapterId;
    private String status;
    private LocalDateTime completedAt;
}
