package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.service.LearningAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final LearningAnalyticsService analyticsService;

    /** 批量接收前端埋点事件 */
    @PostMapping("/event")
    public ApiResponse<String> ingestEvents(@RequestBody Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) payload.getOrDefault("events", List.of());
        analyticsService.ingestEvents(events);
        return ApiResponse.ok("ok");
    }

    /** 获取学习分析 Dashboard 完整数据 */
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> getDashboard() {
        Long userId = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(analyticsService.getDashboard(userId));
    }
}
