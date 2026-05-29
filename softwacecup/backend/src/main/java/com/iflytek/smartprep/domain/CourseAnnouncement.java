package com.iflytek.smartprep.domain;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_course_announcement")
public class CourseAnnouncement {
    private Long id;
    private Long courseId;
    private String type;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
