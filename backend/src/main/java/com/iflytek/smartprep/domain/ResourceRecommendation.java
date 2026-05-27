package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_resource_recommendation")
public class ResourceRecommendation {
    private Long id;
    private Long userId;
    private Long lessonId;
    private String resourcesJson;
    private LocalDateTime createdAt;
}
