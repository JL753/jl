package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.service.KnowledgeGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/knowledge-graph")
@RequiredArgsConstructor
public class KnowledgeGraphController {

    private final KnowledgeGraphService kgService;

    /** 获取完整知识依赖图 */
    @GetMapping("/full")
    public ApiResponse<Map<String, Object>> getFullGraph() {
        return ApiResponse.ok(kgService.getFullGraph());
    }

    /** 获取当前用户各知识点掌握度 */
    @GetMapping("/my-progress")
    public ApiResponse<Object> getMyProgress() {
        Long userId = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(kgService.getUserProgress(userId));
    }

    /** 获取指定用户的知识点掌握度 */
    @GetMapping("/progress/{userId}")
    public ApiResponse<Object> getUserProgress(@PathVariable Long userId) {
        return ApiResponse.ok(kgService.getUserProgress(userId));
    }

    /** 推荐下一个应学习的知识点 */
    @GetMapping("/next-recommended")
    public ApiResponse<Object> getNextRecommended() {
        Long userId = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(kgService.getNextRecommended(userId));
    }

    /** 提交练习答案并更新掌握度 */
    @PostMapping("/submit-answer")
    public ApiResponse<Object> submitAnswer(@RequestBody Map<String, Object> request) {
        Long userId = LoginUserHolder.get().getUserId();
        Long kpId = Long.valueOf(request.get("knowledgePointId").toString());
        boolean correct = Boolean.parseBoolean(request.get("correct").toString());
        kgService.updateMastery(userId, kpId, correct);
        return ApiResponse.ok(Map.of(
                "updated", true,
                "knowledgePointId", kpId,
                "progress", kgService.getUserProgress(userId)
        ));
    }

    /** 获取知识点的前驱依赖链 */
    @GetMapping("/prerequisite-chain/{kpId}")
    public ApiResponse<Object> getPrerequisiteChain(@PathVariable Long kpId) {
        return ApiResponse.ok(kgService.getPrerequisiteChain(kpId));
    }

    /** 获取某知识点的练习题 */
    @GetMapping("/exercises/{kpId}")
    public ApiResponse<Object> getExercises(@PathVariable Long kpId) {
        return ApiResponse.ok(kgService.getExercisesByKnowledgePoint(kpId));
    }
}
