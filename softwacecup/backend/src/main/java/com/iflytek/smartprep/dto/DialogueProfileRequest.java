package com.iflytek.smartprep.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DialogueProfileRequest {
    @NotBlank
    private String message;
    private String sessionId;
}
