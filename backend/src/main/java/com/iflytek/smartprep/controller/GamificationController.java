package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    /** 用户 XP/等级信息 */
    @GetMapping("/progress")
    public ApiResponse<Map<String, Object>> getProgress() {
        Long userId = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(gamificationService.getUserProgress(userId));
    }

    /** 徽章列表 */
    @GetMapping("/badges")
    public ApiResponse<Map<String, Object>> getBadges() {
        Long userId = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(gamificationService.getBadges(userId));
    }

    /** 打卡状态 */
    @GetMapping("/streak")
    public ApiResponse<Map<String, Object>> getStreak() {
        Long userId = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(gamificationService.getStreak(userId));
    }

    /** 每日签到 */
    @PostMapping("/checkin")
    public ApiResponse<Map<String, Object>> checkin() {
        Long userId = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(gamificationService.checkin(userId));
    }

    /** 添加 XP（教学行为触发） */
    @PostMapping("/add-xp")
    public ApiResponse<Map<String, Object>> addXp(@RequestBody Map<String, Object> request) {
        Long userId = LoginUserHolder.get().getUserId();
        int amount = Integer.parseInt(request.getOrDefault("amount", "5").toString());
        String reason = (String) request.getOrDefault("reason", "学习行为");
        return ApiResponse.ok(gamificationService.addXp(userId, amount, reason));
    }

    /** 排行榜 */
    @GetMapping("/leaderboard")
    public ApiResponse<Object> getLeaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(gamificationService.getLeaderboard(limit));
    }

    /** 每日挑战 */
    @GetMapping("/daily-challenges")
    public ApiResponse<Object> getDailyChallenges() {
        Long userId = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(gamificationService.getDailyChallenges(userId));
    }
}
