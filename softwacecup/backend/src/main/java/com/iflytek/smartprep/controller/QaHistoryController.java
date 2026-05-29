package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.domain.QaHistory;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.service.QaHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
public class QaHistoryController {

    private final QaHistoryService qaHistoryService;

    @GetMapping("/recent")
    public ApiResponse<List<QaHistory>> getRecent(@RequestParam(defaultValue = "10") int limit) {
        Long userId = LoginUserHolder.get().getUserId();
        List<QaHistory> history = qaHistoryService.getRecentByUserId(userId, limit);
        return ApiResponse.ok(history);
    }

    @PostMapping("/save")
    public ApiResponse<QaHistory> saveQa(@RequestBody QaHistory qaHistory) {
        Long userId = LoginUserHolder.get().getUserId();
        qaHistory.setUserId(userId);
        QaHistory saved = qaHistoryService.save(qaHistory);
        return ApiResponse.ok(saved);
    }

    @GetMapping("/session/{sessionId}")
    public ApiResponse<List<QaHistory>> getBySession(@PathVariable String sessionId) {
        List<QaHistory> history = qaHistoryService.getBySessionId(sessionId);
        return ApiResponse.ok(history);
    }
}
