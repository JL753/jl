package com.iflytek.smartprep.dto;

import lombok.Data;

@Data
public class AdminCourseUpdateRequest {
    private String title;
    private String category;
    private String description;
    private String coverImage;
    private String price;
    private String tag;
    private String status;
    private Integer totalHours;
    private String targetAudience;
    private Object chaptersJson;
}
