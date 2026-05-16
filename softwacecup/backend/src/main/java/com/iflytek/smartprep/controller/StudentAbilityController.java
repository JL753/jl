package com.iflytek.smartprep.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.dto.AbilityScoreDto;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.mapper.*;
import com.iflytek.smartprep.service.LLMClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/ability")
@RequiredArgsConstructor
public class StudentAbilityController {

    private final StudentAbilityMapper abilityMapper;
    private final LessonProgressMapper progressMapper;
    private final UserKpMasteryMapper kpMasteryMapper;
    private final UserStreakMapper streakMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final LessonMapper lessonMapper;
    private final LLMClient llmClient;

    @PostMapping("/evaluate")
    public ApiResponse<AbilityScoreDto> evaluate() {
        Long userId = LoginUserHolder.get().getUserId();

        // 全部可用课时数（公开课 + 该用户的个人课）
        long allAvailableLessons = lessonMapper.selectCount(
                new LambdaQueryWrapper<Lesson>()
                        .and(w -> w.isNull(Lesson::getUserId).or().eq(Lesson::getUserId, userId)));
        long completedLessons = progressMapper.selectCount(
                new LambdaQueryWrapper<LessonProgress>()
                        .eq(LessonProgress::getUserId, userId)
                        .eq(LessonProgress::getStatus, "completed"));

        int breadth = allAvailableLessons > 0 ? (int) (completedLessons * 100 / allAvailableLessons) : 0;

        List<UserKpMastery> masteries = kpMasteryMapper.selectList(
                new LambdaQueryWrapper<UserKpMastery>().eq(UserKpMastery::getUserId, userId));
        int depth = masteries.isEmpty() ? 0 :
                (int) (masteries.stream().mapToDouble(UserKpMastery::getMastery).average().orElse(0) * 100);

        int problem = allAvailableLessons > 0
                ? Math.min(95, 20 + (int) (completedLessons * 75.0 / allAvailableLessons))
                : 20;

        UserStreak streak = streakMapper.selectOne(
                new LambdaQueryWrapper<UserStreak>().eq(UserStreak::getUserId, userId));
        int activity = streak != null ? Math.min(streak.getCurrentStreak() * 10, 100) : 0;

        int transfer = masteries.size() > 2
                ? Math.min(90, 30 + masteries.size() * 6) : 30;

        long wrongCount = wrongQuestionMapper.selectCount(
                new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getUserId, userId));
        int resilience = wrongCount > 0 ? Math.max(30, 70 - (int) (wrongCount * 2)) : 80;

        AbilityScoreDto score = AbilityScoreDto.builder()
                .breadthScore(breadth)
                .depthScore(depth)
                .problemScore(problem)
                .activityScore(activity)
                .transferScore(transfer)
                .resilienceScore(resilience)
                .build();

        String diagnosisPrompt = String.format(
                "根据学生六维能力数据：知识广度=%d, 知识深度=%d, 解题能力=%d, 学习活跃度=%d, 知识迁移=%d, 学习韧性=%d。" +
                "请生成100字以内的学习诊断和改进建议。",
                breadth, depth, problem, activity, transfer, resilience);
        try {
            String diagnosis = llmClient.chat(diagnosisPrompt);
            score.setDiagnosis(diagnosis);
        } catch (Exception e) {
            score.setDiagnosis("学习状态良好，建议保持持续学习节奏。");
        }

        StudentAbility record = new StudentAbility();
        record.setId(System.currentTimeMillis());
        record.setUserId(userId);
        record.setBreadthScore(breadth);
        record.setDepthScore(depth);
        record.setProblemScore(problem);
        record.setActivityScore(activity);
        record.setTransferScore(transfer);
        record.setResilienceScore(resilience);
        record.setDiagnosis(score.getDiagnosis());
        record.setEvaluatedAt(LocalDateTime.now());
        abilityMapper.insert(record);

        score.setEvaluatedAt(LocalDateTime.now().toString());
        return ApiResponse.ok(score);
    }

    @GetMapping("/latest")
    public ApiResponse<AbilityScoreDto> latest() {
        Long userId = LoginUserHolder.get().getUserId();
        StudentAbility latest = abilityMapper.selectOne(
                new LambdaQueryWrapper<StudentAbility>()
                        .eq(StudentAbility::getUserId, userId)
                        .orderByDesc(StudentAbility::getEvaluatedAt)
                        .last("LIMIT 1"));
        if (latest == null) return ApiResponse.ok(AbilityScoreDto.builder().build());

        return ApiResponse.ok(AbilityScoreDto.builder()
                .breadthScore(latest.getBreadthScore())
                .depthScore(latest.getDepthScore())
                .problemScore(latest.getProblemScore())
                .activityScore(latest.getActivityScore())
                .transferScore(latest.getTransferScore())
                .resilienceScore(latest.getResilienceScore())
                .diagnosis(latest.getDiagnosis())
                .evaluatedAt(latest.getEvaluatedAt().toString())
                .build());
    }

    @GetMapping("/history")
    public ApiResponse<List<StudentAbility>> history() {
        Long userId = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(abilityMapper.selectList(
                new LambdaQueryWrapper<StudentAbility>()
                        .eq(StudentAbility::getUserId, userId)
                        .orderByDesc(StudentAbility::getEvaluatedAt)));
    }
}
