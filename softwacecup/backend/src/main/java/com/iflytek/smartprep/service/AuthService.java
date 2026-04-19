package com.iflytek.smartprep.service;

import com.iflytek.smartprep.domain.User;
import com.iflytek.smartprep.dto.LoginRequest;
import com.iflytek.smartprep.dto.LoginResponse;
import com.iflytek.smartprep.dto.ProfileUpdateRequest;
import com.iflytek.smartprep.dto.RegisterRequest;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    LoginResponse register(RegisterRequest request);
    User currentUser();
    User updateCurrentUser(ProfileUpdateRequest request);
}
