package com.iflytek.smartprep.dto;

import lombok.Data;

@Data
public class StudyPathRequest {
    private String currentStage;
    private String availableHoursPerWeek;
    private String target;
}
