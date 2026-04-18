package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.dto.*;
import com.iflytek.smartprep.service.QuizService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能测验控制器
 */
@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private static final Logger log = LoggerFactory.getLogger(QuizController.class);

    private final QuizService quizService;

    // 临时存储生成的题目（实际项目应该存到Redis或数据库）
    private final Map<String, List<QuizQuestion>> quizCache = new HashMap<>();

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    /**
     * 生成测验题目
     *
     * @param request 生成请求
     * @return 题目列表
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateQuiz(@RequestBody QuizGenerateRequest request) {
        log.info("收到测验生成请求: {}", request);

        try {
            // 设置默认值
            if (request.getChoiceCount() == null) {
                request.setChoiceCount(5);
            }
            if (request.getEssayCount() == null) {
                request.setEssayCount(1);
            }
            if (request.getDifficulty() == null) {
                request.setDifficulty(3);
            }

            // 生成题目
            List<QuizQuestion> questions = quizService.generateQuiz(request);

            // 生成测验ID并缓存题目
            String quizId = "quiz_" + System.currentTimeMillis();
            quizCache.put(quizId, questions);

            // 返回题目（不包含正确答案）
            List<QuizQuestion> questionsForStudent = questions.stream()
                    .map(q -> {
                        QuizQuestion copy = QuizQuestion.builder()
                                .id(q.getId())
                                .type(q.getType())
                                .question(q.getQuestion())
                                .options(q.getOptions())
                                .knowledgePoint(q.getKnowledgePoint())
                                .difficulty(q.getDifficulty())
                                .score(q.getScore())
                                .build();
                        return copy;
                    })
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("quizId", quizId);
            response.put("questions", questionsForStudent);
            response.put("totalScore", questions.stream().mapToInt(QuizQuestion::getScore).sum());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("生成测验失败", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 提交测验并批改
     *
     * @param quizId 测验ID
     * @param submission 学生提交
     * @return 批改结果
     */
    @PostMapping("/submit/{quizId}")
    public ResponseEntity<QuizResult> submitQuiz(
            @PathVariable String quizId,
            @RequestBody QuizSubmission submission) {
        log.info("收到测验提交，quizId: {}, studentId: {}", quizId, submission.getStudentId());

        try {
            // 获取原始题目
            List<QuizQuestion> questions = quizCache.get(quizId);
            if (questions == null) {
                return ResponseEntity.badRequest().build();
            }

            // 批改测验
            QuizResult result = quizService.gradeQuiz(submission, questions);

            // 清除缓存
            quizCache.remove(quizId);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("批改测验失败", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取学生能力画像
     *
     * @param studentId 学生ID
     * @return 能力画像数据
     */
    @GetMapping("/ability/{studentId}")
    public ResponseEntity<Map<String, Integer>> getStudentAbility(@PathVariable Long studentId) {
        log.info("获取学生能力画像: {}", studentId);

        try {
            Map<String, Integer> ability = quizService.getStudentAbility(studentId);
            return ResponseEntity.ok(ability);
        } catch (Exception e) {
            log.error("获取能力画像失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
