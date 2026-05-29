package com.iflytek.smartprep.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.smartprep.domain.StudyPath;
import com.iflytek.smartprep.dto.StudyPathRequest;
import com.iflytek.smartprep.mapper.StudyPathMapper;
import com.iflytek.smartprep.service.LLMClient;
import com.iflytek.smartprep.service.KnowledgeGraphService;
import com.iflytek.smartprep.service.StudyPathService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyPathServiceImpl implements StudyPathService {

    private static final Logger log = LoggerFactory.getLogger(StudyPathServiceImpl.class);

    private final StudyPathMapper studyPathMapper;
    private final LLMClient llmClient;
    private final KnowledgeGraphService knowledgeGraphService;
    private final ObjectMapper objectMapper;

    @Override
    public StudyPath generate(Long userId, StudyPathRequest request) {
        // 1. 获取用户知识点掌握度
        String userProgressSummary = buildUserProgressSummary(userId);

        // 2. 构建 LLM prompt
        String prompt = String.format("""
                请为以下学习者生成个性化学习路径。

                学习者信息：
                - 当前阶段：%s
                - 每周可用时间：%s 小时
                - 学习目标：%s

                已掌握/学习中知识点：
                %s

                请生成5-7个阶段的学习步骤，以JSON格式返回（不要markdown代码块）：
                {
                  "title": "路径标题（10字以内）",
                  "steps": [
                    {"step": 1, "name": "阶段名", "goal": "目标描述", "duration": "预计天数（如第1-3天）", "agent": "对应智能体名"}
                  ],
                  "resources": [
                    {"type": "document|mindmap|question|media|coding", "title": "资源名", "reason": "推荐理由"}
                  ]
                }

                要求：
                - 阶段递进合理，基于用户当前水平
                - 每阶段目标明确、可执行
                - 针对薄弱知识点安排更多练习
                - 资源多样化，匹配学习风格""",
                request.getCurrentStage() != null ? request.getCurrentStage() : "入门",
                request.getAvailableHoursPerWeek() != null ? request.getAvailableHoursPerWeek() : "5",
                request.getTarget() != null ? request.getTarget() : "系统掌握该领域知识",
                userProgressSummary);

        StudyPath p = new StudyPath();
        p.setUserId(userId);

        try {
            String resp = llmClient.chat(PATH_PROMPT, prompt);
            Map<String, Object> parsed = objectMapper.readValue(
                    LLMClient.extractJson(resp),
                    new TypeReference<Map<String, Object>>() {});

            p.setTitle((String) parsed.getOrDefault("title", "个性化学习路径"));
            p.setStepsJson(objectMapper.writeValueAsString(
                    parsed.getOrDefault("steps", Collections.emptyList())));
            p.setPushJson(objectMapper.writeValueAsString(
                    parsed.getOrDefault("resources", Collections.emptyList())));
        } catch (Exception e) {
            log.error("LLM 路径生成失败，使用默认路径: {}", e.getMessage());
            fallbackToDefault(p, request);
        }

        p.setCreatedAt(LocalDateTime.now());
        studyPathMapper.insert(p);
        return p;
    }

    @Override
    public List<StudyPath> list(Long userId) {
        return studyPathMapper.selectList(new LambdaQueryWrapper<StudyPath>()
                .eq(StudyPath::getUserId, userId)
                .orderByDesc(StudyPath::getCreatedAt));
    }

    @Override
    public Map<String, Object> latestOverview(Long userId) {
        List<StudyPath> paths = list(userId);
        if (paths.isEmpty()) {
            return Map.of(
                    "title", "暂无学习路径",
                    "steps", Collections.emptyList(),
                    "pushResources", Collections.emptyList()
            );
        }
        StudyPath latest = paths.get(0);
        return Map.of(
                "title", latest.getTitle(),
                "steps", parseList(latest.getStepsJson()),
                "pushResources", parseList(latest.getPushJson())
        );
    }

    // ==================== 辅助方法 ====================

    private String buildUserProgressSummary(Long userId) {
        try {
            List<Map<String, Object>> progress = knowledgeGraphService.getUserProgress(userId);
            if (progress == null || progress.isEmpty()) return "暂无学习记录";

            return progress.stream()
                    .map(p -> String.format("- %s：掌握度 %.0f%% (%s)",
                            p.get("name"),
                            Double.parseDouble(p.get("mastery").toString()) * 100,
                            p.get("status")))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "暂时无法获取学习记录";
        }
    }

    private void fallbackToDefault(StudyPath p, StudyPathRequest request) {
        p.setTitle("个性化学习路径");
        try {
            p.setStepsJson(objectMapper.writeValueAsString(buildDefaultSteps(request)));
            p.setPushJson(objectMapper.writeValueAsString(buildDefaultResources()));
        } catch (Exception ex) {
            p.setStepsJson("[]");
            p.setPushJson("[]");
        }
    }

    private List<Map<String, Object>> buildDefaultSteps(StudyPathRequest request) {
        return List.of(
                Map.of("step", 1, "name", "基础知识学习", "goal", "掌握核心概念与原理", "duration", "第1-3天", "agent", "课程讲解智能体"),
                Map.of("step", 2, "name", "知识框架构建", "goal", "建立结构化知识体系", "duration", "第4-5天", "agent", "思维导图智能体"),
                Map.of("step", 3, "name", "针对性练习", "goal", "通过练习巩固薄弱环节", "duration", "第6-9天", "agent", "题库智能体"),
                Map.of("step", 4, "name", "综合应用实践", "goal", "完成综合项目/案例", "duration", "第10-13天", "agent", "实操项目智能体"),
                Map.of("step", 5, "name", "评估与调整", "goal", "检验学习效果并调整方向", "duration", "第14天", "agent", "评估智能体")
        );
    }

    private List<Map<String, Object>> buildDefaultResources() {
        return List.of(
                Map.of("type", "document", "title", "核心概念讲义", "reason", "夯实理论基础"),
                Map.of("type", "mindmap", "title", "知识脉络图", "reason", "结构化理解"),
                Map.of("type", "question", "title", "分层练习题", "reason", "针对性突破薄弱点"),
                Map.of("type", "media", "title", "视频讲解合集", "reason", "多模态加深理解"),
                Map.of("type", "coding", "title", "实践项目任务", "reason", "提升应用能力")
        );
    }

    private List<Map<String, Object>> parseList(String json) {
        try {
            if (json == null || json.isBlank()) return Collections.emptyList();
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static final String PATH_PROMPT = """
            你是高校智能学习平台的路径规划AI专家。你的任务是为学习者设计个性化学习路径。
            路径应该：循序渐进、针对薄弱环节、匹配学习者的时间投入、目标导向。
            每阶段包含：名称、目标、时间预估、对应的智能体。
            同时推荐配套学习资源。
            输出JSON格式。""";
}
