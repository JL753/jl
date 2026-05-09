package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sp_learning_resource")
public class LearningResource {
    private Long id;
    private Long userId;
    private String resourceType;
    private String title;
    private String content;
    private String linksJson;
    private Integer confidence;
    private LocalDateTime createdAt;
}
