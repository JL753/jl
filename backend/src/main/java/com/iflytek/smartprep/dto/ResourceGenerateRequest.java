package com.iflytek.smartprep.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ResourceGenerateRequest {
    @NotBlank
    private String major;
    @NotBlank
    private String course;
    @NotBlank
    private String topic;
    private String weakness;
    private String learningStage;
    private String targetScore;
    private String preferredMode;
    private List<String> requiredTypes;
}
