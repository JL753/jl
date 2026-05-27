package com.iflytek.smartprep.dto;

import lombok.Data;

@Data
public class TeachingPptGenerateRequest {
    private String course;
    private String chapter;
    private String teachingGoal;
    private String studentLevel;
    private String templateCode;
}
