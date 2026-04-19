package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sp_learning_assessment")
public class LearningAssessment {
    private Long id;
    private Long userId;
    private String scoreDimensionJson;
    private String diagnosis;
    private String optimizeSuggestion;
    private LocalDateTime createdAt;
}
