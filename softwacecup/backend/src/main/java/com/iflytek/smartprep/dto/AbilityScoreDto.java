package com.iflytek.smartprep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbilityScoreDto {
    private int breadthScore;
    private int depthScore;
    private int problemScore;
    private int activityScore;
    private int transferScore;
    private int resilienceScore;
    private String diagnosis;
    private String evaluatedAt;
}
