package com.iflytek.smartprep.dto;

import lombok.Data;

@Data
public class AdminUserUpdateRequest {
    private String password;
    private String role;
    private String displayName;
    private String avatarUrl;
}
