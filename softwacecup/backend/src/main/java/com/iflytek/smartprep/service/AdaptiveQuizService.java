package com.iflytek.smartprep.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.smartprep.domain.KnowledgePoint;
import com.iflytek.smartprep.domain.KpDependency;
import com.iflytek.smartprep.domain.UserKpMastery;
import com.iflytek.smartprep.mapper.KnowledgePointMapper;
import com.iflytek.smartprep.mapper.KpDependencyMapper;
import com.iflytek.smartprep.mapper.UserKpMasteryMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 自适应出题引擎
 * 根据用户掌握度 + 知识点信息，调用 LLM 生成个性化题目
 */
@Service
@RequiredArgsConstructor
public class AdaptiveQuizService {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveQuizService.class);
    private static final double MASTERY_THRESHOLD = 0.6;

    private final LLMClient llmClient;
    private final KnowledgePointMapper kpMapper;
    private final UserKpMasteryMapper masteryMapper;
    private final KpDependencyMapper depMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 生成自适应测验 ====================

    /**
     * 为用户指定知识点生成个性化测验
     */
    public Map<String, Object> generateQuiz(Long userId, Long kpId, int count) {
        KnowledgePoint kp = kpMapper.selectById(kpId);
        if (kp == null) throw new IllegalArgumentException("知识点不存在: " + kpId);

        // 1. 获取用户对该知识点的掌握度
        double mastery = getUserMastery(userId, kpId);

        // 2. 获取前驱知识点的薄弱环节
        List<Map<String, Object>> weakPrereqs = findWeakPrerequisites(userId, kpId);

        // 3. 根据掌握度确定出题难度和侧重
        String difficultyFocus = determineDifficultyFocus(mastery);

        // 4. 构建 prompt 并调用 LLM 生成题目
        String prompt = buildQuizPrompt(kp, mastery, weakPrereqs, count, difficultyFocus);
        String response = llmClient.chat(AI_TUTOR_SYSTEM_PROMPT, prompt);

        // 5. 解析 LLM 返回的题目
        List<Map<String, Object>> questions = parseQuizResponse(response);

        // 6. 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("knowledgePointId", kpId);
        result.put("knowledgePointName", kp.getName());
        result.put("userMastery", Math.round(mastery * 1000.0) / 1000.0);
        result.put("difficultyFocus", difficultyFocus);
        result.put("weakPrerequisites", weakPrereqs);
        result.put("questions", questions);
        result.put("quizBasis", buildQuizBasis(kp, mastery, weakPrereqs));
        return result;
    }

    // ==================== 批改与错题分析 ====================

    /**
     * 批改用户提交的答案
     */
    public Map<String, Object> gradeSubmission(Long userId, Long kpId, List<Map<String, Object>> answers) {
        KnowledgePoint kp = kpMapper.selectById(kpId);
        if (kp == null) throw new IllegalArgumentException("知识点不存在: " + kpId);

        int correct = 0;
        List<Map<String, Object>> graded = new ArrayList<>();

        for (Map<String, Object> ans : answers) {
            String userAnswer = String.valueOf(ans.getOrDefault("userAnswer", ""));
            String correctAnswer = String.valueOf(ans.getOrDefault("correctAnswer", ""));
            boolean isCorrect = normalizeAnswer(userAnswer).equalsIgnoreCase(normalizeAnswer(correctAnswer));

            Map<String, Object> gradedItem = new HashMap<>(ans);
            gradedItem.put("correct", isCorrect);
            graded.add(gradedItem);

            if (isCorrect) correct++;
            // 更新掌握度
            kgUpdateMastery(userId, kpId, isCorrect);
        }

        int total = answers.size();
        double accuracy = total > 0 ? (double) correct / total : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("totalQuestions", total);
        result.put("correctCount", correct);
        result.put("accuracy", Math.round(accuracy * 1000.0) / 1000.0);
        result.put("knowledgePointId", kpId);
        result.put("knowledgePointName", kp.getName());
        result.put("gradedAnswers", graded);

        // 对错题进行根因分析
        if (correct < total) {
            List<Map<String, Object>> mistakes = graded.stream()
                    .filter(g -> !Boolean.TRUE.equals(g.get("correct")))
                    .collect(Collectors.toList());
            result.put("mistakeAnalysis", analyzeMistakes(userId, kpId, mistakes));
        }

        // 获取更新后的掌握度
        result.put("updatedMastery", Math.round(getUserMastery(userId, kpId) * 1000.0) / 1000.0);

        return result;
    }

    /**
     * 错题根因分析 — 追溯到前驱知识点的薄弱环节
     */
    public Map<String, Object> analyzeMistakes(Long userId, Long kpId, List<Map<String, Object>> mistakes) {
        List<Map<String, Object>> weakPrereqs = findWeakPrerequisites(userId, kpId);

        if (mistakes.isEmpty()) {
            return Map.of("hasIssues", false);
        }

        // 调用 LLM 分析错题根因
        StringBuilder mistakesDesc = new StringBuilder();
        for (int i = 0; i < mistakes.size(); i++) {
            Map<String, Object> m = mistakes.get(i);
            mistakesDesc.append(String.format("错题%d: %s\n正确答案: %s\n用户答案: %s\n\n",
                    i + 1,
                    m.getOrDefault("question", "未知"),
                    m.getOrDefault("correctAnswer", ""),
                    m.getOrDefault("userAnswer", "")));
        }

        String prompt = String.format("""
                请分析以下错题，找出知识薄弱点并给出针对性建议。

                当前知识点：%s
                薄弱的前驱知识点：%s

                %s

                请以JSON格式返回（不要包含markdown代码块）:
                {
                  "rootCause": "根本原因分析（1-2句话）",
                  "weakPoints": ["薄弱点1", "薄弱点2"],
                  "suggestions": ["建议1", "建议2"],
                  "prerequisiteGap": "前驱知识缺口说明",
                  "remediationSteps": ["补救步骤1", "补救步骤2"]
                }
                """,
                kpMapper.selectById(kpId).getName(),
                weakPrereqs.stream().map(w -> String.valueOf(w.get("name"))).collect(Collectors.joining(", ")),
                mistakesDesc.toString());

        try {
            String resp = llmClient.chat(AI_TUTOR_SYSTEM_PROMPT, prompt);
            Map<String, Object> analysis = objectMapper.readValue(
                    LLMClient.extractJson(resp),
                    new TypeReference<Map<String, Object>>() {});
            analysis.put("hasIssues", true);
            analysis.put("weakPrerequisites", weakPrereqs);
            return analysis;
        } catch (Exception e) {
            log.error("错题分析失败: {}", e.getMessage());
            return Map.of(
                    "hasIssues", true,
                    "rootCause", "无法自动分析，建议回顾前置知识点",
                    "weakPrerequisites", weakPrereqs
            );
        }
    }

    // ==================== 辅助方法 ====================

    private double getUserMastery(Long userId, Long kpId) {
        List<UserKpMastery> list = masteryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserKpMastery>()
                        .eq(UserKpMastery::getUserId, userId)
                        .eq(UserKpMastery::getKnowledgePointId, kpId));
        return list.isEmpty() ? 0.0 : list.get(0).getMastery();
    }

    private List<Map<String, Object>> findWeakPrerequisites(Long userId, Long kpId) {
        List<KpDependency> allDeps = depMapper.selectList(null);
        Set<Long> prereqIds = allDeps.stream()
                .filter(d -> d.getSuccessorId().equals(kpId))
                .map(KpDependency::getPrerequisiteId)
                .collect(Collectors.toSet());

        List<Map<String, Object>> weakPrereqs = new ArrayList<>();
        for (Long pid : prereqIds) {
            double mastery = getUserMastery(userId, pid);
            if (mastery < MASTERY_THRESHOLD) {
                KnowledgePoint pkp = kpMapper.selectById(pid);
                Map<String, Object> item = new HashMap<>();
                item.put("knowledgePointId", pid);
                item.put("name", pkp != null ? pkp.getName() : "未知");
                item.put("mastery", Math.round(mastery * 1000.0) / 1000.0);
                weakPrereqs.add(item);
            }
        }
        return weakPrereqs;
    }

    private String determineDifficultyFocus(double mastery) {
        if (mastery < 0.2) return "基础概念与入门练习";
        if (mastery < 0.5) return "基础巩固与简单应用";
        if (mastery < 0.75) return "综合应用与中等难度";
        return "挑战题与高阶思维";
    }

    private String buildQuizPrompt(KnowledgePoint kp, double mastery,
                                   List<Map<String, Object>> weakPrereqs,
                                   int count, String difficultyFocus) {
        String weakInfo = "";
        if (!weakPrereqs.isEmpty()) {
            weakInfo = "注意：用户以下前驱知识点掌握较弱：" +
                    weakPrereqs.stream()
                            .map(w -> w.get("name") + "(掌握度:" + w.get("mastery") + ")")
                            .collect(Collectors.joining("、")) +
                    "，请在题目中适当融入这些知识点的巩固。";
        }

        return String.format("""
                请为以下知识点生成 %d 道自适应练习题。

                知识点：%s
                难度等级：%d/5
                用户当前掌握度：%.0f%%
                出题侧重：%s
                %s

                要求：
                1. 题目类型混合：选择题(3-4道)、判断题(1-2道)、填空题(1-2道)
                2. 难度递进：从简单到困难排序
                3. 每题标注考察的具体能力维度（记忆/理解/应用/分析）
                4. 提供详细解析，指出易错点

                请以JSON格式返回（不要包含markdown代码块）：
                {
                  "questions": [
                    {
                      "id": "q1",
                      "type": "choice|judge|fill",
                      "difficulty": 1-5,
                      "abilityDimension": "记忆|理解|应用|分析",
                      "question": "题目内容",
                      "options": ["A. ...", "B. ...", "C. ...", "D. ..."],
                      "correctAnswer": "A",
                      "explanation": "详细解析...",
                      "knowledgePointHint": "考察的具体知识点"
                    }
                  ]
                }
                """,
                count,
                kp.getName(),
                kp.getDifficultyLevel() != null ? kp.getDifficultyLevel() : 3,
                Math.round(mastery * 100),
                difficultyFocus,
                weakInfo);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseQuizResponse(String response) {
        if (response == null) return Collections.emptyList();
        try {
            Map<String, Object> parsed = objectMapper.readValue(
                    LLMClient.extractJson(response),
                    new TypeReference<Map<String, Object>>() {});
            Object questions = parsed.get("questions");
            if (questions instanceof List) {
                return (List<Map<String, Object>>) questions;
            }
        } catch (Exception e) {
            log.error("解析题目响应失败: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private String buildQuizBasis(KnowledgePoint kp, double mastery,
                                  List<Map<String, Object>> weakPrereqs) {
        StringBuilder sb = new StringBuilder();
        sb.append("出题依据：基于你对「").append(kp.getName()).append("」");
        sb.append("的掌握度为 ").append(Math.round(mastery * 100)).append("%");
        if (!weakPrereqs.isEmpty()) {
            sb.append("，且前驱知识点");
            for (Map<String, Object> w : weakPrereqs) {
                sb.append("「").append(w.get("name")).append("」较为薄弱");
            }
            sb.append("，本次练习将侧重巩固这些环节");
        }
        sb.append("，难度定位为「").append(determineDifficultyFocus(mastery)).append("」");
        return sb.toString();
    }

    private String normalizeAnswer(String answer) {
        return answer.trim().toLowerCase().replaceAll("[\\s\\.。，,;；:：]", "");
    }

    // 简单的本地掌握度更新（不依赖 KnowledgeGraphService）
    private void kgUpdateMastery(Long userId, Long kpId, boolean correct) {
        List<UserKpMastery> list = masteryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserKpMastery>()
                        .eq(UserKpMastery::getUserId, userId)
                        .eq(UserKpMastery::getKnowledgePointId, kpId));

        double oldMastery = 0.0;
        int practiceCount = 0;
        int correctCount = 0;

        UserKpMastery record;
        if (!list.isEmpty()) {
            record = list.get(0);
            oldMastery = record.getMastery();
            practiceCount = record.getPracticeCount();
            correctCount = record.getCorrectCount();
        } else {
            record = new UserKpMastery();
            record.setUserId(userId);
            record.setKnowledgePointId(kpId);
        }

        double K = 0.15;
        double expected = 1.0 / (1.0 + Math.exp(-(oldMastery - 0.5) * 8.0));
        double actual = correct ? 1.0 : 0.0;
        double newMastery = Math.max(0.0, Math.min(1.0, oldMastery + K * (actual - expected)));

        record.setMastery(Math.round(newMastery * 1000.0) / 1000.0);
        record.setPracticeCount(practiceCount + 1);
        if (correct) record.setCorrectCount(correctCount + 1);
        record.setLastPracticeAt(java.time.LocalDateTime.now());

        if (list.isEmpty()) {
            masteryMapper.insert(record);
        } else {
            masteryMapper.updateById(record);
        }
    }

    private static final String AI_TUTOR_SYSTEM_PROMPT = """
            你是高校智能教学平台中的AI出题专家。你的任务是：
            1. 根据知识点和用户掌握度生成个性化练习题
            2. 难度适应学习者的当前水平（维果茨基最近发展区原则）
            3. 题目清晰、选项合理、解析详细
            4. 标注每题考察的能力维度
            请仅输出要求的JSON格式，不要输出额外内容。""";
}
