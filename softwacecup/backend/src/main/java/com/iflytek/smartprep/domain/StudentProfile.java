package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sp_student_profile")
public class StudentProfile {
    private Long id;
    private Long userId;
    private String major;
    private String course;
    private String knowledgeBase;
    private String cognitiveStyle;
    private String weakPoints;
    private String interestPreference;
    private String pacePreference;
    private String examGoal;
    private String profileJson;
    private LocalDateTime updatedAt;
}
