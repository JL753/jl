package com.iflytek.smartprep.domain;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_course_answer")
public class CourseAnswer {
    private Long id;
    private Long questionId;
    private Long userId;
    private String content;
    private Boolean isAi;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
