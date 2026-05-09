package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.domain.LearningAssessment;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.dto.AssessmentRequest;
import com.iflytek.smartprep.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assessment")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @PostMapping("/evaluate")
    public ApiResponse<LearningAssessment> evaluate(@RequestBody AssessmentRequest request) {
        Long uid = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(assessmentService.evaluate(uid, request));
    }

    @GetMapping("/mine")
    public ApiResponse<List<LearningAssessment>> mine() {
        Long uid = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(assessmentService.list(uid));
    }
}
