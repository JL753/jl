package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI 智能体路由控制器
 * 接收前端意图分析结果，路由到 AgentService 生成真实 LLM 响应
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /** 课程推荐智能体 */
    @PostMapping("/course-recommend")
    public Map<String, Object> courseRecommend(@RequestBody Map<String, Object> request) {
        String query = (String) request.getOrDefault("query", "");
        return agentService.courseRecommend(query);
    }

    /** 知识讲解智能体 */
    @PostMapping("/knowledge")
    public Map<String, Object> knowledge(@RequestBody Map<String, Object> request) {
        String query = (String) request.getOrDefault("query", "");
        Long userId = getUserIdOrNull();
        return agentService.knowledgeExplain(query, userId);
    }

    /** 学情诊断智能体 */
    @PostMapping("/diagnosis")
    public Map<String, Object> diagnosis(@RequestBody Map<String, Object> request) {
        String query = (String) request.getOrDefault("query", "");
        Long userId = getUserIdOrNull();
        return agentService.diagnosis(query, userId);
    }

    /** 路径规划智能体 */
    @PostMapping("/path-planning")
    public Map<String, Object> pathPlanning(@RequestBody Map<String, Object> request) {
        String query = (String) request.getOrDefault("query", "");
        Long userId = getUserIdOrNull();
        return agentService.pathPlanning(query, userId);
    }

    private Long getUserIdOrNull() {
        try {
            return LoginUserHolder.get().getUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
