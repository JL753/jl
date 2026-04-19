package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.dto.DashboardStats;
import com.iflytek.smartprep.dto.ExamPublishRequest;
import com.iflytek.smartprep.dto.ExamSubmitRequest;
import com.iflytek.smartprep.dto.GradeExamRequest;
import com.iflytek.smartprep.service.DashboardService;
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
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/teacher")
    public ApiResponse<DashboardStats> teacher() {
        return ApiResponse.ok(dashboardService.teacherDashboard());
    }

    @GetMapping("/student")
    public ApiResponse<DashboardStats> student() {
        return ApiResponse.ok(dashboardService.studentDashboard(LoginUserHolder.get().getUserId()));
    }

    @GetMapping("/teacher/workspace")
    public ApiResponse<Map<String, Object>> teacherWorkspace() {
        return ApiResponse.ok(dashboardService.teacherWorkspace());
    }

    @GetMapping("/student/workspace")
    public ApiResponse<Map<String, Object>> studentWorkspace() {
        return ApiResponse.ok(dashboardService.studentWorkspace(LoginUserHolder.get().getUserId()));
    }

    @GetMapping("/screen")
    public ApiResponse<Map<String, Object>> screen() {
        return ApiResponse.ok(dashboardService.datacenterScreen());
    }

    @GetMapping("/exams")
    public ApiResponse<List<Map<String, Object>>> exams(@RequestParam(defaultValue = "student") String role) {
        return ApiResponse.ok(dashboardService.examList(LoginUserHolder.get().getUserId(), role));
    }

    @GetMapping("/exam-detail")
    public ApiResponse<Map<String, Object>> examDetail(@RequestParam Long examId,
                                                       @RequestParam(defaultValue = "student") String role) {
        return ApiResponse.ok(dashboardService.examDetail(LoginUserHolder.get().getUserId(), examId, role));
    }

    @PostMapping("/publish-exam")
    public ApiResponse<Map<String, Object>> publishExam(@RequestBody ExamPublishRequest request) {
        return ApiResponse.ok(dashboardService.publishExam(LoginUserHolder.get().getUserId(), request));
    }

    @GetMapping("/exam-records")
    public ApiResponse<List<Map<String, Object>>> examRecords() {
        return ApiResponse.ok(dashboardService.examRecords(LoginUserHolder.get().getUserId()));
    }

    @GetMapping("/wrong-questions")
    public ApiResponse<List<Map<String, Object>>> wrongQuestions() {
        return ApiResponse.ok(dashboardService.wrongQuestions(LoginUserHolder.get().getUserId()));
    }

    @GetMapping("/exam-scoreboard")
    public ApiResponse<List<Map<String, Object>>> examScoreboard(@RequestParam Long examId) {
        return ApiResponse.ok(dashboardService.examScoreboard(examId));
    }

    @GetMapping("/exam-scoreboard-summary")
    public ApiResponse<Map<String, Object>> examScoreboardSummary(@RequestParam Long examId) {
        return ApiResponse.ok(dashboardService.examScoreboardSummary(examId));
    }

    @GetMapping("/exam-score-detail")
    public ApiResponse<Map<String, Object>> examScoreDetail(@RequestParam Long recordId) {
        return ApiResponse.ok(dashboardService.examScoreDetail(recordId));
    }

    @GetMapping("/wrong-question-detail")
    public ApiResponse<Map<String, Object>> wrongQuestionDetail(@RequestParam Long wrongQuestionId) {
        return ApiResponse.ok(dashboardService.wrongQuestionDetail(wrongQuestionId));
    }

    @GetMapping("/exam-dimension-stats")
    public ApiResponse<List<Map<String, Object>>> examDimensionStats() {
        return ApiResponse.ok(dashboardService.examDimensionStats());
    }

    @GetMapping("/profile-card")
    public ApiResponse<Map<String, Object>> profileCard() {
        return ApiResponse.ok(dashboardService.profileCard(LoginUserHolder.get().getUserId(), LoginUserHolder.get().getRole()));
    }

    @PostMapping("/grade-exam")
    public ApiResponse<Map<String, Object>> gradeExam(@RequestBody GradeExamRequest request) {
        return ApiResponse.ok(dashboardService.gradeExam(LoginUserHolder.get().getUserId(), request));
    }

    @PostMapping("/submit-exam")
    public ApiResponse<Map<String, Object>> submitExam(@RequestBody ExamSubmitRequest request) {
        return ApiResponse.ok(dashboardService.submitExam(LoginUserHolder.get().getUserId(), request));
    }
}
