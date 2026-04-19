package com.iflytek.smartprep.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.iflytek.smartprep.domain.KnowledgeDoc;
import com.iflytek.smartprep.domain.StudentProfile;
import com.iflytek.smartprep.dto.*;
import com.iflytek.smartprep.mapper.KnowledgeDocMapper;
import com.iflytek.smartprep.mapper.StudentProfileMapper;
import com.iflytek.smartprep.rag.model.DocumentChunk;
import com.iflytek.smartprep.rag.service.RAGService;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智能测验服务
 * 实现AI出题、批改、能力画像更新
 */
@Service
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    @Value("${smartprep.llm.base-url:}")
    private String llmApiUrl;

    @Value("${smartprep.llm.api-key:}")
    private String llmApiKey;

    @Value("${smartprep.llm.model:glm-4}")
    private String llmModel;

    private final KnowledgeDocMapper knowledgeDocMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final RAGService ragService;
    private final Gson gson;
    private final OkHttpClient httpClient;

    public QuizService(KnowledgeDocMapper knowledgeDocMapper,
                      StudentProfileMapper studentProfileMapper,
                      RAGService ragService) {
        this.knowledgeDocMapper = knowledgeDocMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.ragService = ragService;
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 生成测验题目
     *
     * @param request 生成请求
     * @return 题目列表
     */
    public List<QuizQuestion> generateQuiz(QuizGenerateRequest request) {
        log.info("开始生成测验，文档ID: {}", request.getDocumentId());

        // 1. 获取文档内容
        KnowledgeDoc doc = knowledgeDocMapper.selectById(request.getDocumentId());
        if (doc == null) {
            throw new RuntimeException("文档不存在: " + request.getDocumentId());
        }

        // 2. 获取文档分块（取前5个作为出题依据）
        List<DocumentChunk> chunks = ragService.getDocumentChunks(request.getDocumentId());
        if (chunks.isEmpty()) {
            throw new RuntimeException("文档未进行向量化处理");
        }

        StringBuilder contentSummary = new StringBuilder();
        int chunkLimit = Math.min(5, chunks.size());
        for (int i = 0; i < chunkLimit; i++) {
            contentSummary.append(chunks.get(i).getContent()).append("\n\n");
        }

        // 3. 构建出题提示词
        String prompt = buildQuizGenerationPrompt(
                doc.getTitle(),
                contentSummary.toString(),
                request.getChoiceCount() != null ? request.getChoiceCount() : 5,
                request.getEssayCount() != null ? request.getEssayCount() : 1,
                request.getDifficulty() != null ? request.getDifficulty() : 3
        );

        // 4. 调用大模型生成题目
        String llmResponse = callLLM(prompt);

        // 5. 解析题目JSON
        List<QuizQuestion> questions = parseQuizQuestions(llmResponse);

        log.info("测验生成完成，共 {} 道题", questions.size());
        return questions;
    }

    /**
     * 批改测验
     *
     * @param submission 学生提交
     * @param questions 原始题目
     * @return 批改结果
     */
    public QuizResult gradeQuiz(QuizSubmission submission, List<QuizQuestion> questions) {
        log.info("开始批改测验，学生ID: {}", submission.getStudentId());

        Map<String, QuizResult.QuestionGrading> gradingDetails = new HashMap<>();
        int totalScore = 0;
        int earnedScore = 0;
        int correctCount = 0;

        // 1. 批改每道题
        for (QuizQuestion question : questions) {
            totalScore += question.getScore();
            String studentAnswer = submission.getAnswers().get(question.getId());

            QuizResult.QuestionGrading grading;
            if ("choice".equals(question.getType())) {
                // 单选题：直接比对答案
                boolean correct = question.getCorrectAnswer().equals(studentAnswer);
                int score = correct ? question.getScore() : 0;
                earnedScore += score;
                if (correct) correctCount++;

                grading = QuizResult.QuestionGrading.builder()
                        .correct(correct)
                        .score(score)
                        .studentAnswer(studentAnswer)
                        .correctAnswer(question.getCorrectAnswer())
                        .explanation(generateExplanation(question, studentAnswer, correct))
                        .knowledgePoint(question.getKnowledgePoint())
                        .build();
            } else {
                // 简答题：调用AI批改
                grading = gradeEssayQuestion(question, studentAnswer);
                earnedScore += grading.getScore();
                if (grading.getCorrect()) correctCount++;
            }

            gradingDetails.put(question.getId(), grading);
        }

        double accuracy = (double) correctCount / questions.size() * 100;

        // 2. 更新学生能力画像
        Map<String, QuizResult.AbilityChange> abilityChanges = updateStudentAbility(
                submission.getStudentId(),
                gradingDetails,
                accuracy
        );

        // 3. 生成总体评价
        String overallComment = generateOverallComment(accuracy, earnedScore, totalScore);

        QuizResult result = QuizResult.builder()
                .totalScore(totalScore)
                .earnedScore(earnedScore)
                .accuracy(accuracy)
                .gradingDetails(gradingDetails)
                .abilityChanges(abilityChanges)
                .overallComment(overallComment)
                .build();

        log.info("批改完成，得分: {}/{}, 正确率: {}%", earnedScore, totalScore, String.format("%.1f", accuracy));
        return result;
    }

    /**
     * 构建出题提示词
     */
    private String buildQuizGenerationPrompt(String title, String content, int choiceCount, int essayCount, int difficulty) {
        return String.format("""
                你是一位专业的教育测评专家。请根据以下文档内容，生成一套测验题目。

                文档标题：%s

                文档内容：
                %s

                要求：
                1. 生成 %d 道单选题（每题4个选项，标记为A/B/C/D）
                2. 生成 %d 道简答题
                3. 难度级别：%d/5
                4. 每道题必须包含知识点标签
                5. 必须严格按照以下JSON格式输出，不要添加任何其他文字：

                [
                  {
                    "id": "q1",
                    "type": "choice",
                    "question": "题目内容",
                    "options": ["A. 选项1", "B. 选项2", "C. 选项3", "D. 选项4"],
                    "correctAnswer": "A",
                    "knowledgePoint": "知识点名称",
                    "difficulty": 3,
                    "score": 10
                  },
                  {
                    "id": "q6",
                    "type": "essay",
                    "question": "简答题内容",
                    "correctAnswer": "参考答案",
                    "knowledgePoint": "知识点名称",
                    "difficulty": 4,
                    "score": 30
                  }
                ]

                请直接输出JSON数组，不要包含任何markdown标记或其他说明文字。
                """, title, content, choiceCount, essayCount, difficulty);
    }

    /**
     * 调用大模型API
     */
    private String callLLM(String prompt) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", llmModel);

            JsonArray messages = new JsonArray();
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);
            messages.add(message);
            requestBody.add("messages", messages);

            requestBody.addProperty("temperature", 0.7);
            requestBody.addProperty("max_tokens", 4000);

            RequestBody body = RequestBody.create(
                    gson.toJson(requestBody),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(llmApiUrl)
                    .addHeader("Authorization", "Bearer " + llmApiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("LLM API调用失败: " + response.code());
                }

                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                // 解析响应（适配智谱GLM格式）
                if (jsonResponse.has("choices")) {
                    JsonArray choices = jsonResponse.getAsJsonArray("choices");
                    if (choices.size() > 0) {
                        JsonObject firstChoice = choices.get(0).getAsJsonObject();
                        JsonObject messageObj = firstChoice.getAsJsonObject("message");
                        return messageObj.get("content").getAsString();
                    }
                }

                throw new IOException("无法解析LLM响应");
            }
        } catch (Exception e) {
            log.error("调用LLM失败", e);
            throw new RuntimeException("调用LLM失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析题目JSON
     */
    private List<QuizQuestion> parseQuizQuestions(String llmResponse) {
        try {
            // 提取JSON部分（去除可能的markdown标记）
            String jsonStr = llmResponse.trim();
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.substring(7);
            }
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.substring(3);
            }
            if (jsonStr.endsWith("```")) {
                jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
            }
            jsonStr = jsonStr.trim();

            return gson.fromJson(jsonStr, new TypeToken<List<QuizQuestion>>(){}.getType());
        } catch (Exception e) {
            log.error("解析题目JSON失败: {}", llmResponse, e);
            throw new RuntimeException("解析题目失败", e);
        }
    }

    /**
     * 生成单选题解析
     */
    private String generateExplanation(QuizQuestion question, String studentAnswer, boolean correct) {
        if (correct) {
            return "回答正确！" + question.getKnowledgePoint() + "掌握良好。";
        } else {
            return String.format("回答错误。正确答案是 %s。建议复习 %s 相关内容。",
                    question.getCorrectAnswer(), question.getKnowledgePoint());
        }
    }

    /**
     * 批改简答题（调用AI）
     */
    private QuizResult.QuestionGrading gradeEssayQuestion(QuizQuestion question, String studentAnswer) {
        String prompt = String.format("""
                请批改以下简答题：

                题目：%s
                参考答案：%s
                学生答案：%s

                请给出：
                1. 得分（满分%d分）
                2. 详细解析

                请以JSON格式输出：
                {
                  "score": 分数,
                  "explanation": "详细解析"
                }
                """, question.getQuestion(), question.getCorrectAnswer(), studentAnswer, question.getScore());

        String llmResponse = callLLM(prompt);

        try {
            String jsonStr = llmResponse.trim();
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.substring(7);
            }
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.substring(3);
            }
            if (jsonStr.endsWith("```")) {
                jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
            }
            jsonStr = jsonStr.trim();

            JsonObject result = gson.fromJson(jsonStr, JsonObject.class);
            int score = result.get("score").getAsInt();
            String explanation = result.get("explanation").getAsString();

            return QuizResult.QuestionGrading.builder()
                    .correct(score >= question.getScore() * 0.6) // 60%以上算正确
                    .score(score)
                    .studentAnswer(studentAnswer)
                    .correctAnswer(question.getCorrectAnswer())
                    .explanation(explanation)
                    .knowledgePoint(question.getKnowledgePoint())
                    .build();
        } catch (Exception e) {
            log.error("解析简答题批改结果失败", e);
            // 降级处理
            return QuizResult.QuestionGrading.builder()
                    .correct(false)
                    .score(0)
                    .studentAnswer(studentAnswer)
                    .correctAnswer(question.getCorrectAnswer())
                    .explanation("批改失败，请联系老师人工批改")
                    .knowledgePoint(question.getKnowledgePoint())
                    .build();
        }
    }

    /**
     * 更新学生能力画像
     */
    private Map<String, QuizResult.AbilityChange> updateStudentAbility(Long studentId,
                                                                        Map<String, QuizResult.QuestionGrading> gradingDetails,
                                                                        double accuracy) {
        Map<String, QuizResult.AbilityChange> changes = new HashMap<>();

        try {
            // 1. 获取学生画像
            StudentProfile profile = studentProfileMapper.selectById(studentId);
            if (profile == null) {
                log.warn("学生画像不存在: {}", studentId);
                return changes;
            }

            // 2. 解析现有能力数据
            Map<String, Integer> abilities = new HashMap<>();
            if (profile.getProfileJson() != null && !profile.getProfileJson().isEmpty()) {
                JsonObject profileJson = gson.fromJson(profile.getProfileJson(), JsonObject.class);
                if (profileJson.has("abilities")) {
                    JsonObject abilitiesJson = profileJson.getAsJsonObject("abilities");
                    for (String key : abilitiesJson.keySet()) {
                        abilities.put(key, abilitiesJson.get(key).getAsInt());
                    }
                }
            }

            // 初始化默认能力值
            if (abilities.isEmpty()) {
                abilities.put("理解能力", 60);
                abilities.put("记忆能力", 60);
                abilities.put("逻辑能力", 60);
                abilities.put("应用能力", 60);
                abilities.put("分析能力", 60);
            }

            // 3. 根据答题情况更新能力值
            Map<String, Integer> oldAbilities = new HashMap<>(abilities);

            // 根据正确率调整理解能力
            int understandingDelta = (int) ((accuracy - 60) / 10);
            abilities.put("理解能力", Math.max(0, Math.min(100, abilities.get("理解能力") + understandingDelta)));

            // 根据简答题表现调整应用能力和分析能力
            long essayCorrect = gradingDetails.values().stream()
                    .filter(g -> g.getKnowledgePoint() != null && g.getCorrect())
                    .count();
            if (essayCorrect > 0) {
                abilities.put("应用能力", Math.max(0, Math.min(100, abilities.get("应用能力") + 5)));
                abilities.put("分析能力", Math.max(0, Math.min(100, abilities.get("分析能力") + 5)));
            }

            // 4. 记录变化
            for (String dimension : abilities.keySet()) {
                int before = oldAbilities.get(dimension);
                int after = abilities.get(dimension);
                if (before != after) {
                    changes.put(dimension, QuizResult.AbilityChange.builder()
                            .dimension(dimension)
                            .before(before)
                            .after(after)
                            .delta(after - before)
                            .reason(generateChangeReason(dimension, after - before, accuracy))
                            .build());
                }
            }

            // 5. 保存更新后的画像
            JsonObject newProfileJson = new JsonObject();
            JsonObject abilitiesJson = new JsonObject();
            for (Map.Entry<String, Integer> entry : abilities.entrySet()) {
                abilitiesJson.addProperty(entry.getKey(), entry.getValue());
            }
            newProfileJson.add("abilities", abilitiesJson);
            profile.setProfileJson(gson.toJson(newProfileJson));
            studentProfileMapper.updateById(profile);

            log.info("学生能力画像已更新，学生ID: {}, 变化: {}", studentId, changes.size());
        } catch (Exception e) {
            log.error("更新学生能力画像失败", e);
        }

        return changes;
    }

    /**
     * 生成能力变化原因
     */
    private String generateChangeReason(String dimension, int delta, double accuracy) {
        if (delta > 0) {
            return String.format("本次测验表现优异（正确率%.1f%%），%s有所提升", accuracy, dimension);
        } else {
            return String.format("本次测验表现欠佳（正确率%.1f%%），%s需要加强", accuracy, dimension);
        }
    }

    /**
     * 生成总体评价
     */
    private String generateOverallComment(double accuracy, int earnedScore, int totalScore) {
        if (accuracy >= 90) {
            return String.format("优秀！得分 %d/%d，正确率 %.1f%%。知识掌握扎实，继续保持！", earnedScore, totalScore, accuracy);
        } else if (accuracy >= 75) {
            return String.format("良好！得分 %d/%d，正确率 %.1f%%。大部分知识点掌握较好，继续努力！", earnedScore, totalScore, accuracy);
        } else if (accuracy >= 60) {
            return String.format("及格。得分 %d/%d，正确率 %.1f%%。部分知识点需要加强复习。", earnedScore, totalScore, accuracy);
        } else {
            return String.format("需要努力。得分 %d/%d，正确率 %.1f%%。建议系统复习相关知识点。", earnedScore, totalScore, accuracy);
        }
    }

    /**
     * 获取学生能力画像
     */
    public Map<String, Integer> getStudentAbility(Long studentId) {
        try {
            StudentProfile profile = studentProfileMapper.selectById(studentId);
            if (profile == null || profile.getProfileJson() == null) {
                // 返回默认能力值
                Map<String, Integer> defaultAbilities = new HashMap<>();
                defaultAbilities.put("理解能力", 50);
                defaultAbilities.put("记忆能力", 50);
                defaultAbilities.put("应用能力", 50);
                defaultAbilities.put("分析能力", 50);
                defaultAbilities.put("综合能力", 50);
                return defaultAbilities;
            }

            JsonObject profileJson = gson.fromJson(profile.getProfileJson(), JsonObject.class);
            JsonObject abilitiesJson = profileJson.getAsJsonObject("abilities");

            Map<String, Integer> abilities = new HashMap<>();
            if (abilitiesJson != null) {
                for (String key : abilitiesJson.keySet()) {
                    abilities.put(key, abilitiesJson.get(key).getAsInt());
                }
            }

            return abilities;
        } catch (Exception e) {
            log.error("获取学生能力画像失败", e);
            return new HashMap<>();
        }
    }
}
