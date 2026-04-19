package com.iflytek.smartprep.dto;

import lombok.Data;

@Data
public class AssessmentRequest {
    private Integer finishedTasks;
    private Integer practiceCount;
    private Integer wrongCount;
    private Integer studyMinutes;
    private Integer resourceUseCount;
}
