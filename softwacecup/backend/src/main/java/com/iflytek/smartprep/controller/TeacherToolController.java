package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.dto.PaperCreateRequest;
import com.iflytek.smartprep.dto.QuestionItemRequest;
import com.iflytek.smartprep.dto.TeachingPptGenerateRequest;
import com.iflytek.smartprep.service.TeacherToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher-tools")
@RequiredArgsConstructor
public class TeacherToolController {

    private final TeacherToolService teacherToolService;

    @GetMapping("/questions")
    public ApiResponse<List<Map<String, Object>>> questions() {
        return ApiResponse.ok(teacherToolService.questionBank(LoginUserHolder.get().getUserId()));
    }

    @PostMapping("/questions")
    public ApiResponse<Map<String, Object>> addQuestion(@RequestBody QuestionItemRequest request) {
        return ApiResponse.ok(teacherToolService.addQuestion(LoginUserHolder.get().getUserId(), request));
    }

    @PostMapping("/papers")
    public ApiResponse<Map<String, Object>> createPaper(@RequestBody PaperCreateRequest request) {
        return ApiResponse.ok(teacherToolService.createPaper(LoginUserHolder.get().getUserId(), request));
    }

    @GetMapping("/ppt-templates")
    public ApiResponse<List<Map<String, Object>>> pptTemplates() {
        return ApiResponse.ok(teacherToolService.pptTemplates());
    }

    @PostMapping("/teaching-ppt")
    public ApiResponse<Map<String, Object>> generateTeachingPpt(@RequestBody TeachingPptGenerateRequest request) {
        return ApiResponse.ok(teacherToolService.generateTeachingPpt(LoginUserHolder.get().getUserId(), request));
    }
}
