package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.domain.LearningResource;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.dto.ResourceGenerateRequest;
import com.iflytek.smartprep.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping("/generate")
    public ApiResponse<List<LearningResource>> generate(@Valid @RequestBody ResourceGenerateRequest request) {
        Long uid = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(resourceService.generate(uid, request));
    }

    @GetMapping("/mine")
    public ApiResponse<List<LearningResource>> mine() {
        Long uid = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(resourceService.listByUser(uid));
    }

    @GetMapping("/teacher-library")
    public ApiResponse<List<Map<String, Object>>> teacherLibrary() {
        return ApiResponse.ok(resourceService.teacherLibrary());
    }

    @GetMapping("/progress")
    public ApiResponse<Map<String, Object>> progress() {
        Long uid = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(resourceService.progress(uid));
    }

    @GetMapping("/recommendation")
    public ApiResponse<Map<String, Object>> recommendation(@RequestParam(required = false) String prompt) {
        Long uid = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(resourceService.recommendation(uid, prompt));
    }
}
