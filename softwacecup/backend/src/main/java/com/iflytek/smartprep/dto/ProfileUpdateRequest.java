package com.iflytek.smartprep.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String displayName;
    private String password;
    private String avatarUrl;
    private String major;
    private String course;
    private String knowledgeBase;
    private String cognitiveStyle;
    private String weakPoints;
    private String interestPreference;
    private String pacePreference;
    private String examGoal;
}
