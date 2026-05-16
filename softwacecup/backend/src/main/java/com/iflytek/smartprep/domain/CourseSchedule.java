package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sp_course_schedule")
public class CourseSchedule {
    private Long id;
    private Long userId;
    private Integer dayOfWeek;  // 1-7 (周一到周日)
    private String startTime;   // HH:mm
    private String endTime;     // HH:mm
    private String courseName;
    private String location;
    private String teacher;
    private String weeks;       // 如: "1-16"
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
