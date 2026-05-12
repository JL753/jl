package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sp_task_completion")
public class TaskCompletion {
    private Long id;
    private Long userId;
    private LocalDate weekStartDate;
    private String taskType;
    private Integer completionRate;
    private Integer totalCount;
    private Integer completedCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
