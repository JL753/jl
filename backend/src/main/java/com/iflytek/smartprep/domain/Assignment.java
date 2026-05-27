package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_assignment")
public class Assignment {
    private Long id;
    private Long classId;
    private String title;
    private String lessonIdsJson;
    private String description;
    private LocalDateTime dueAt;
    private LocalDateTime createdAt;
}
