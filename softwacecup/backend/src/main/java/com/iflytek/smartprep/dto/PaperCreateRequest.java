package com.iflytek.smartprep.dto;

import lombok.Data;

import java.util.List;

@Data
public class PaperCreateRequest {
    private String examName;
    private String course;
    private Integer duration;
    private String topic;
    private Integer singleCount;
    private Integer multipleCount;
    private Integer judgeCount;
    private Integer shortAnswerCount;
    private List<Long> studentIds;
}
