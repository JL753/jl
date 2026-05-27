package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.config.RequireRole;
import com.iflytek.smartprep.domain.CourseAnswer;
import com.iflytek.smartprep.domain.CourseQuestion;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.dto.TutorAskRequest;
import com.iflytek.smartprep.dto.TutorAnswer;
import com.iflytek.smartprep.service.CourseQaService;
import com.iflytek.smartprep.service.TutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseQaController {

    private final CourseQaService courseQaService;
    private final TutorService tutorService;

    @GetMapping("/courses/{id}/questions")
    public ApiResponse<List<Map<String, Object>>> getQuestions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(courseQaService.getQuestions(id, page, pageSize));
    }

    @PostMapping("/courses/{id}/questions")
    @RequireRole({"student"})
    public ApiResponse<CourseQuestion> askQuestion(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(courseQaService.askQuestion(
                id, userId, body.get("title"), body.get("content")));
    }

    @GetMapping("/questions/{id}/answers")
    public ApiResponse<List<Map<String, Object>>> getAnswers(@PathVariable Long id) {
        return ApiResponse.ok(courseQaService.getAnswers(id));
    }

    @PostMapping("/questions/{id}/answers")
    @RequireRole({"teacher", "student"})
    public ApiResponse<CourseAnswer> postAnswer(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(courseQaService.postAnswer(id, userId, body.get("content")));
    }

    @PostMapping("/questions/{id}/ai-answer")
    @RequireRole({"teacher", "student"})
    public ApiResponse<CourseAnswer> aiAnswer(@PathVariable Long id) {
        Long userId = LoginUserHolder.get().getUserId();

        // Build prompt and call LLM via TutorService
        String prompt = "请用中文回答以下课程问题，回答应专业、准确、有帮助。";
        TutorAskRequest request = new TutorAskRequest();
        request.setQuestion(prompt);
        TutorAnswer tutorAnswer = tutorService.ask(userId, request);

        // Extract the markdown answer from the tutor response
        String aiContent = tutorAnswer.getMarkdown();
        CourseAnswer answer = courseQaService.postAiAnswer(id, aiContent);
        return ApiResponse.ok(answer);
    }
}
