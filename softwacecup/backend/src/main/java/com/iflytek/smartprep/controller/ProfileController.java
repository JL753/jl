package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.domain.StudentProfile;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.dto.DialogueProfileRequest;
import com.iflytek.smartprep.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping("/dialogue")
    public ApiResponse<StudentProfile> build(@Valid @RequestBody DialogueProfileRequest request) {
        Long uid = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(profileService.buildByDialogue(uid, request));
    }

    @GetMapping("/mine")
    public ApiResponse<StudentProfile> mine() {
        Long uid = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(profileService.getByUserId(uid));
    }
}
