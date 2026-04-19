package com.cnsoftbei.smartlearning.controller;

import com.cnsoftbei.smartlearning.api.*;
import com.cnsoftbei.smartlearning.service.DemoDataService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AppController {

    private final DemoDataService demoDataService;

    public AppController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @PostMapping("/auth/login")
    public ApiResponse<LoginResult> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok("登录成功", demoDataService.login(request.username(), request.role()));
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(demoDataService.dashboard());
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.ok(demoDataService.systemSummary());
    }

    @PostMapping("/profile/build")
    public ApiResponse<Map<String, Object>> buildProfile(@RequestBody ProfileBuildRequest request) {
        return ApiResponse.ok("画像构建完成", demoDataService.buildProfile(request.conversation()));
    }

    @PostMapping("/agents/generate")
    public ApiResponse<Map<String, Object>> generate(@RequestBody AgentGenerateRequest request) {
        return ApiResponse.ok("多智能体资源生产完成", demoDataService.generateResources(request));
    }

    @GetMapping("/path-plan")
    public ApiResponse<Map<String, Object>> pathPlan() {
        return ApiResponse.ok(demoDataService.pathPlan());
    }

    @PostMapping("/tutor/ask")
    public ApiResponse<Map<String, Object>> tutor(@RequestBody TutorAskRequest request) {
        return ApiResponse.ok("答疑完成", demoDataService.tutor(request.question()));
    }

    @GetMapping("/assessment")
    public ApiResponse<Map<String, Object>> assessment() {
        return ApiResponse.ok(demoDataService.assessment());
    }

    @GetMapping("/teacher/workbench")
    public ApiResponse<Map<String, Object>> teacherWorkbench() {
        return ApiResponse.ok(demoDataService.teacherWorkbench());
    }

    @GetMapping("/teacher/exam-center")
    public ApiResponse<Map<String, Object>> examCenter() {
        return ApiResponse.ok(demoDataService.examCenter());
    }

    @GetMapping("/teacher/resource-center")
    public ApiResponse<Map<String, Object>> resourceCenter() {
        return ApiResponse.ok(demoDataService.resourceCenter());
    }

    @GetMapping("/bigscreen")
    public ApiResponse<Map<String, Object>> bigscreen() {
        return ApiResponse.ok(demoDataService.bigScreen());
    }
}
