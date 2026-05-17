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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/ability")
@RequiredArgsConstructor
public class StudentAbilityController {

    private final StudentAbilityMapper abilityMapper;
    private final LessonProgressMapper progressMapper;
    private final UserKpMasteryMapper kpMasteryMapper;
    private final UserStreakMapper streakMapper;
    private final UnitMapper unitMapper;
    private final LessonMapper lessonMapper;
    private final SubjectMapper subjectMapper;
    private final ExerciseAttemptMapper exerciseAttemptMapper;
    private final StudyDurationMapper studyDurationMapper;
    private final LLMClient llmClient;

    @PostMapping("/evaluate")
    public ApiResponse<AbilityScoreDto> evaluate() {
        Long userId = LoginUserHolder.get().getUserId();
        LocalDate today = LocalDate.now();

        // === 1. 知识广度：覆盖学科比例 × 覆盖单元比例 ===
        long totalSubjects = subjectMapper.selectCount(null);
        List<Unit> allUnits = unitMapper.selectList(null);
        long totalUnits = allUnits.size();

        java.util.Set<Long> coveredUnits = new java.util.HashSet<>();
        java.util.Set<Long> coveredSubjects = new java.util.HashSet<>();
        List<LessonProgress> allCompleted = progressMapper.selectList(
                new LambdaQueryWrapper<LessonProgress>()
                        .eq(LessonProgress::getUserId, userId)
                        .eq(LessonProgress::getStatus, "completed"));
        for (LessonProgress lp : allCompleted) {
            Lesson lesson = lessonMapper.selectById(lp.getSubChapterId());
            if (lesson != null && lesson.getUnitId() != null) {
                coveredUnits.add(lesson.getUnitId());
                Unit unit = unitMapper.selectById(lesson.getUnitId());
                if (unit != null && unit.getSubjectId() != null) {
                    coveredSubjects.add(unit.getSubjectId());
                }
            }
        }
        double subjectRatio = totalSubjects > 0 ? (double) coveredSubjects.size() / totalSubjects : 0;
        double unitRatio = totalUnits > 0 ? (double) coveredUnits.size() / totalUnits : 0;
        int breadth = (int) (subjectRatio * unitRatio * 100);

        // === 2. 知识深度：avg(UserKpMastery.mastery) ===
        List<UserKpMastery> masteries = kpMasteryMapper.selectList(
                new LambdaQueryWrapper<UserKpMastery>().eq(UserKpMastery::getUserId, userId));
        int depth = masteries.isEmpty() ? 0 :
                (int) (masteries.stream().mapToDouble(m -> m.getMastery() != null ? m.getMastery() : 0).average().orElse(0) * 100);

        // === 3. 解题能力：sum(correct × difficulty) / sum(difficulty) ===
        List<ExerciseAttempt> attempts = exerciseAttemptMapper.selectList(
                new LambdaQueryWrapper<ExerciseAttempt>().eq(ExerciseAttempt::getUserId, userId));
        int totalWeight = 0;
        int weightedCorrect = 0;
        for (ExerciseAttempt a : attempts) {
            int diff = a.getDifficulty() != null ? a.getDifficulty() : 1;
            int corr = a.getCorrect() != null && a.getCorrect() > 0 ? 1 : 0;
            totalWeight += diff;
            weightedCorrect += corr * diff;
        }
        int problem = totalWeight > 0 ? (int) (weightedCorrect * 100.0 / totalWeight) : 20;

        // === 4. 学习活跃度：连续天数×10 + min(日均分钟/5, 50) ===
        UserStreak streak = streakMapper.selectOne(
                new LambdaQueryWrapper<UserStreak>().eq(UserStreak::getUserId, userId));
        int streakDays = streak != null ? Math.min(streak.getCurrentStreak() != null ? streak.getCurrentStreak() : 0, 30) : 0;
        // 近7天日均学习时长（分钟）
        LocalDate weekAgo = today.minusDays(7);
        List<StudyDuration> recentDurations = studyDurationMapper.selectList(
                new LambdaQueryWrapper<StudyDuration>()
                        .eq(StudyDuration::getUserId, userId)
                        .ge(StudyDuration::getStudyDate, weekAgo));
        int totalSeconds = recentDurations.stream().mapToInt(d -> d.getDurationSeconds() != null ? d.getDurationSeconds() : 0).sum();
        int avgMinutesPerDay = totalSeconds / 60 / Math.max(1, 7);
        int activity = Math.min(100, streakDays * 10 + Math.min(avgMinutesPerDay / 5, 50));

        // === 5. 知识迁移：跨lesson练习题正确率 ===
        java.util.Set<Long> attemptLessons = new java.util.HashSet<>();
        int crossCorrect = 0, crossTotal = 0;
        for (ExerciseAttempt a : attempts) {
            if (a.getLessonId() != null) attemptLessons.add(a.getLessonId());
            if (a.getLessonId() != null) { crossTotal++; if (a.getCorrect() != null && a.getCorrect() > 0) crossCorrect++; }
        }
        int transfer = crossTotal > 0 ? (int) (crossCorrect * 100.0 / crossTotal) : 20;
        // 跨章节加分：多lesson
        if (attemptLessons.size() > 2) {
            transfer = Math.min(95, transfer + (attemptLessons.size() - 2) * 3);
        }

        // === 6. 学习韧性：错题复习率×50 + 重试正确率×50 ===
        // 错题复习率 = 有重试记录的题数 / 总错题数
        java.util.Map<Long, java.util.List<ExerciseAttempt>> byExercise = new java.util.HashMap<>();
        for (ExerciseAttempt a : attempts) {
            if (a.getExerciseId() == null) continue;
            byExercise.computeIfAbsent(a.getExerciseId(), k -> new ArrayList<>()).add(a);
        }
        int wrongReviewed = 0, totalWrong = 0, retryCorrect = 0, retryTotal = 0;
        for (var entry : byExercise.entrySet()) {
            List<ExerciseAttempt> list = entry.getValue();
            boolean hasWrong = list.stream().anyMatch(a -> a.getCorrect() == null || a.getCorrect() == 0);
            if (hasWrong) {
                totalWrong++;
                if (list.size() > 1) wrongReviewed++; // 重试过
                // 重试正确率：第一次错、后来对了
                for (int i = 1; i < list.size(); i++) {
                    if ((list.get(i - 1).getCorrect() == null || list.get(i - 1).getCorrect() == 0)
                            && list.get(i).getCorrect() != null && list.get(i).getCorrect() > 0) {
                        retryCorrect++;
                    }
                    retryTotal++;
                }
            }
        }
        int reviewRate = totalWrong > 0 ? (wrongReviewed * 100 / totalWrong) : 50;
        int retryRate = retryTotal > 0 ? (retryCorrect * 100 / retryTotal) : 50;
        int resilience = (reviewRate + retryRate) / 2;

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
