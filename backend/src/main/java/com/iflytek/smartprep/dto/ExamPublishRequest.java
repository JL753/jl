package com.iflytek.smartprep.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExamPublishRequest {
    @NotBlank
    private String examName;
    @NotBlank
    private String course;
    @Min(10)
    private Integer duration;
    @NotBlank
    private String topic;
}
