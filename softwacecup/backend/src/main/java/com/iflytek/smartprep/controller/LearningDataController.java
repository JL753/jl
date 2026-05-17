package com.iflytek.smartprep.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LearningDataController {

    private final ExerciseAttemptMapper exerciseAttemptMapper;
    private final StudyDurationMapper studyDurationMapper;
    private final UserKpMasteryMapper kpMasteryMapper;
    private final LessonProgressMapper progressMapper;
    private final UserStreakMapper streakMapper;
    private final KnowledgePointMapper kpMapper;
    private final LessonMapper lessonMapper;

    /**
     * 练习题作答提交
     * body: { lessonId, exerciseId, difficulty, correct }
     */
    @PostMapping("/exercise/submit")
    public ApiResponse<String> submitExercise(@RequestBody Map<String, Object> body) {
        Long userId = LoginUserHolder.get().getUserId();
        Long lessonId = toLong(body.get("lessonId"));
        Long exerciseId = toLong(body.get("exerciseId"));
        int difficulty = toInt(body.get("difficulty"), 1);
        int correct = toInt(body.get("correct"), 0);

        ExerciseAttempt attempt = new ExerciseAttempt();
        attempt.setId(System.currentTimeMillis());
        attempt.setUserId(userId);
        attempt.setLessonId(lessonId);
        attempt.setExerciseId(exerciseId);
        attempt.setDifficulty(Math.max(1, Math.min(5, difficulty)));
        attempt.setCorrect(correct);
        attempt.setCreatedAt(LocalDateTime.now());
        exerciseAttemptMapper.insert(attempt);

        // 更新知识点掌握度
        if (lessonId != null) {
            Lesson lesson = lessonMapper.selectById(lessonId);
            if (lesson != null) {
                updateKpMastery(userId, lesson, correct);
            }
        }

        return ApiResponse.ok("ok");
    }

    /**
     * 学习时长心跳（前端每30秒发送）
     * body: { lessonId, seconds }
     */
    @PostMapping("/study/heartbeat")
    public ApiResponse<String> heartbeat(@RequestBody Map<String, Object> body) {
        Long userId = LoginUserHolder.get().getUserId();
        Long lessonId = toLong(body.get("lessonId"));
        int seconds = toInt(body.get("seconds"), 30);
        LocalDate today = LocalDate.now();

        // 更新或插入今日学习时长
        StudyDuration existing = studyDurationMapper.selectOne(
                new LambdaQueryWrapper<StudyDuration>()
                        .eq(StudyDuration::getUserId, userId)
                        .eq(StudyDuration::getStudyDate, today)
                        .eq(StudyDuration::getLessonId, lessonId));
        if (existing != null) {
            existing.setDurationSeconds(existing.getDurationSeconds() + seconds);
            studyDurationMapper.updateById(existing);
        } else {
            StudyDuration sd = new StudyDuration();
            sd.setId(System.currentTimeMillis());
            sd.setUserId(userId);
            sd.setLessonId(lessonId);
            sd.setStudyDate(today);
            sd.setDurationSeconds(seconds);
            studyDurationMapper.insert(sd);
        }

        return ApiResponse.ok("ok");
    }

    private void updateKpMastery(Long userId, Lesson lesson, int correct) {
        try {
            // 找到课时关联的知识点
            List<KnowledgePoint> kps = kpMapper.selectList(
                    new LambdaQueryWrapper<KnowledgePoint>().eq(KnowledgePoint::getSubChapterId, lesson.getId()));
            double delta = correct > 0 ? 0.15 : 0.05;
            for (KnowledgePoint kp : kps) {
                UserKpMastery existing = kpMasteryMapper.selectOne(
                        new LambdaQueryWrapper<UserKpMastery>()
                                .eq(UserKpMastery::getUserId, userId)
                                .eq(UserKpMastery::getKnowledgePointId, kp.getId()));
                if (existing != null) {
                    double newMastery = Math.min(1.0, existing.getMastery() + delta);
                    existing.setMastery(newMastery);
                    existing.setPracticeCount((existing.getPracticeCount() != null ? existing.getPracticeCount() : 0) + 1);
                    if (correct > 0) existing.setCorrectCount((existing.getCorrectCount() != null ? existing.getCorrectCount() : 0) + 1);
                    existing.setLastPracticeAt(LocalDateTime.now());
                    kpMasteryMapper.updateById(existing);
                } else {
                    UserKpMastery m = new UserKpMastery();
                    m.setId(System.currentTimeMillis());
                    m.setUserId(userId);
                    m.setKnowledgePointId(kp.getId());
                    m.setMastery(correct > 0 ? 0.15 : 0.05);
                    m.setPracticeCount(1);
                    m.setCorrectCount(correct > 0 ? 1 : 0);
                    m.setLastPracticeAt(LocalDateTime.now());
                    kpMasteryMapper.insert(m);
                }
            }
        } catch (Exception ignored) {}
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private int toInt(Object v, int def) {
        if (v == null) return def;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return def; }
    }
}
