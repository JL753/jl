package com.iflytek.smartprep.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AgentResult {
    private String type;
    private String title;
    private String markdown;
    private List<String> links;
    private Integer confidence;
    private String agentName;
}
