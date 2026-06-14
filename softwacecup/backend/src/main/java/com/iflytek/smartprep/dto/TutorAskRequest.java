package com.iflytek.smartprep.dto;

import lombok.Data;

@Data
public class TutorAskRequest {
    private String question;
    private String context;
    private String answerMode;
    private String sessionId;
}
