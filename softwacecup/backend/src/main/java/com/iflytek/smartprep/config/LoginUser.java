package com.iflytek.smartprep.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginUser {
    private Long userId;
    private String username;
    private String role;
}
