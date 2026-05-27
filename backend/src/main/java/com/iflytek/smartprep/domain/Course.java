package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_course")
public class Course {
    private Long id;
    private String title;
    private String category;
    private String description;
    private String coverImage;
    private String price;
    private String tag;
    private String status;
    private Integer totalHours;
    private String targetAudience;
    private Long subjectId;
    private String chaptersJson;
    private String difficulty;
    private String background;
    private String target;
    private String principle;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
