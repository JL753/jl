package com.iflytek.smartprep.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String role;
    private String displayName;
}
