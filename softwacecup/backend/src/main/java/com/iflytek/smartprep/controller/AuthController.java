package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.RequireRole;
import com.iflytek.smartprep.domain.User;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.dto.LoginRequest;
import com.iflytek.smartprep.dto.LoginResponse;
import com.iflytek.smartprep.dto.ProfileUpdateRequest;
import com.iflytek.smartprep.dto.RegisterRequest;
import com.iflytek.smartprep.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @GetMapping("/me")
    public ApiResponse<User> me() {
        return ApiResponse.ok(authService.currentUser());
    }

    @PutMapping("/me")
    public ApiResponse<User> updateMe(@RequestBody ProfileUpdateRequest request) {
        return ApiResponse.ok(authService.updateCurrentUser(request));
    }

    @GetMapping("/teacher/ping")
    @RequireRole({"teacher", "admin"})
    public ApiResponse<String> teacherPing() {
        return ApiResponse.ok("teacher ok");
    }
}
