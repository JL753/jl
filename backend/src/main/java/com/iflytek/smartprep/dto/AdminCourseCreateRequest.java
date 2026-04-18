package com.iflytek.smartprep.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Data
public class AdminCourseCreateRequest {
    @NotBlank(message = "课程名称不能为空")
    private String title;
    private String category;
    private String description;
    private String coverImage;
    private String price = "免费";
    private String tag;
    private String status = "已发布";
    private Integer totalHours;
    private String targetAudience;
    private List<ChapterItem> chapters;

    @Data
    public static class ChapterItem {
        private String title;
        private String description;
    }
}
