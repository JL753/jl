package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.service.AdaptiveQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/adaptive-quiz")
@RequiredArgsConstructor
public class AdaptiveQuizController {

    private final AdaptiveQuizService adaptiveQuizService;

    /** 生成自适应测验 */
    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generate(@RequestBody Map<String, Object> request) {
        Long userId = LoginUserHolder.get().getUserId();
        Long kpId = Long.valueOf(request.get("knowledgePointId").toString());
        int count = request.containsKey("count") ?
                Integer.parseInt(request.get("count").toString()) : 5;
        return ApiResponse.ok(adaptiveQuizService.generateQuiz(userId, kpId, Math.min(count, 20)));
    }

    /** 提交答案并批改 */
    @PostMapping("/submit")
    public ApiResponse<Map<String, Object>> submit(@RequestBody Map<String, Object> request) {
        Long userId = LoginUserHolder.get().getUserId();
        Long kpId = Long.valueOf(request.get("knowledgePointId").toString());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) request.get("answers");
        return ApiResponse.ok(adaptiveQuizService.gradeSubmission(userId, kpId, answers));
    }

    /** 错题根因分析 */
    @PostMapping("/analyze-mistake")
    public ApiResponse<Map<String, Object>> analyzeMistake(@RequestBody Map<String, Object> request) {
        Long userId = LoginUserHolder.get().getUserId();
        Long kpId = Long.valueOf(request.get("knowledgePointId").toString());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mistakes = (List<Map<String, Object>>) request.get("mistakes");
        return ApiResponse.ok(adaptiveQuizService.analyzeMistakes(userId, kpId, mistakes));
    }
}
