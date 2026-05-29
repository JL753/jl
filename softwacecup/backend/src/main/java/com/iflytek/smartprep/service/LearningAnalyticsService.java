package com.iflytek.smartprep.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.mapper.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学习分析服务
 * 接收前端事件 + 聚合查询学习数据
 */
@Service
@RequiredArgsConstructor
public class LearningAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(LearningAnalyticsService.class);

    private final LearningActivityMapper activityMapper;
    private final TaskCompletionMapper taskMapper;
    private final UserKpMasteryMapper masteryMapper;
    private final QaHistoryMapper qaMapper;
    private final ExamRecordMapper examMapper;
    private final KnowledgePointMapper kpMapper;

    // ==================== 事件接收 ====================

    /**
     * 批量接收前端事件并更新相应数据
     */
    public void ingestEvents(List<Map<String, Object>> events) {
        LocalDate today = LocalDate.now();

        for (Map<String, Object> event : events) {
            String type = (String) event.getOrDefault("type", "unknown");
            Long userId = parseUserId(event);
            if (userId == null) {
                log.debug("Analytics event without userId: {} on page {}", type, event.getOrDefault("page", ""));
                continue;
            }

            // 查找或创建今日的活跃记录
            LearningActivity activity = activityMapper.selectOne(
                    new LambdaQueryWrapper<LearningActivity>()
                            .eq(LearningActivity::getUserId, userId)
                            .eq(LearningActivity::getActivityDate, today));

            if (activity == null) {
                activity = new LearningActivity();
                activity.setUserId(userId);
                activity.setActivityDate(today);
                activity.setActivityScore(0);
                activity.setStudyMinutes(0);
                activity.setQuestionCount(0);
                activity.setLoginCount(0);
            }

            // 根据事件类型更新相应字段
            switch (type) {
                case "page_enter", "page_leave" -> {
                    activity.setLoginCount((activity.getLoginCount() != null ? activity.getLoginCount() : 0) + 1);
                    activity.setActivityScore((activity.getActivityScore() != null ? activity.getActivityScore() : 0) + 1);
                }
                case "video_progress", "video_start" -> {
                    // 粗略估算：每次进度上报代表观看了约30秒
                    activity.setStudyMinutes((activity.getStudyMinutes() != null ? activity.getStudyMinutes() : 0) + 1);
                    activity.setActivityScore((activity.getActivityScore() != null ? activity.getActivityScore() : 0) + 2);
                }
                case "video_complete", "video_end" -> {
                    Object watchedObj = event.get("watchedDuration");
                    int minutes = 0;
                    if (watchedObj instanceof Number n) minutes = n.intValue() / 60;
                    activity.setStudyMinutes((activity.getStudyMinutes() != null ? activity.getStudyMinutes() : 0) + Math.max(minutes, 3));
                    activity.setActivityScore((activity.getActivityScore() != null ? activity.getActivityScore() : 0) + 5);
                }
                case "quiz_answer", "quiz_complete" -> {
                    activity.setQuestionCount((activity.getQuestionCount() != null ? activity.getQuestionCount() : 0) + 1);
                    activity.setActivityScore((activity.getActivityScore() != null ? activity.getActivityScore() : 0) + 3);
                }
                case "resource_click" -> {
                    activity.setActivityScore((activity.getActivityScore() != null ? activity.getActivityScore() : 0) + 1);
                }
                default -> {
                    activity.setActivityScore((activity.getActivityScore() != null ? activity.getActivityScore() : 0) + 1);
                }
            }

            if (activity.getId() == null) {
                activityMapper.insert(activity);
            } else {
                activityMapper.updateById(activity);
            }
        }
    }

    private Long parseUserId(Map<String, Object> event) {
        Object uid = event.get("userId");
        if (uid instanceof Number n) return n.longValue();
        // 尝试从 localStorage 等渠道获取，目前 fallback 到登录用户
        try {
            return com.iflytek.smartprep.config.LoginUserHolder.get().getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== Dashboard 聚合数据 ====================

    public Map<String, Object> getDashboard(Long userId) {
        Map<String, Object> result = new HashMap<>();

        // 统计卡片
        result.put("statCards", buildStatCards(userId));

        // 能力雷达（基于知识点掌握度）
        result.put("radar", buildRadarData(userId));

        // 知识掌握热力图
        result.put("heatmap", buildHeatmapData(userId));

        // 学习习惯分析（基于活跃数据）
        result.put("habits", buildHabitsData(userId));

        // 学习行为趋势（最近7天）
        result.put("trends", buildTrendsData(userId));

        // 预测预警
        result.put("predictions", buildPredictions(userId));

        // 科目详情
        result.put("subjects", buildSubjectDetails(userId));

        return result;
    }

    // ==================== 统计卡片 ====================

    private List<Map<String, Object>> buildStatCards(Long userId) {
        List<LearningActivity> activities = activityMapper.selectList(
                new LambdaQueryWrapper<LearningActivity>()
                        .eq(LearningActivity::getUserId, userId)
                        .orderByDesc(LearningActivity::getActivityDate)
                        .last("LIMIT 30"));

        int totalStudyMin = activities.stream().mapToInt(a ->
                a.getStudyMinutes() != null ? a.getStudyMinutes() : 0).sum();
        int totalQuestions = activities.stream().mapToInt(a ->
                a.getQuestionCount() != null ? a.getQuestionCount() : 0).sum();

        // 准确率从 mastery 表计算
        List<UserKpMastery> masteryList = masteryMapper.selectList(
                new LambdaQueryWrapper<UserKpMastery>().eq(UserKpMastery::getUserId, userId));
        double avgMastery = masteryList.isEmpty() ? 0 :
                masteryList.stream().mapToDouble(m ->
                        m.getMastery() != null ? m.getMastery() : 0).average().orElse(0);

        // AI提问数
        Long qaCount = qaMapper.selectCount(
                new LambdaQueryWrapper<QaHistory>().eq(QaHistory::getUserId, userId));

        return List.of(
                createStatCard("学习时长", totalStudyMin + "分钟", "📚", "#3b82f6"),
                createStatCard("答题数量", totalQuestions + "题", "✏️", "#10b981"),
                createStatCard("平均掌握度", Math.round(avgMastery * 100) + "%", "🎯", "#f59e0b"),
                createStatCard("AI提问", qaCount + "次", "🤖", "#8b5cf6"),
                createStatCard("连续学习", calcStreakDays(activities) + "天", "🔥", "#ef4444")
        );
    }

    // ==================== 能力雷达 ====================

    private Map<String, Object> buildRadarData(Long userId) {
        List<UserKpMastery> masteryList = masteryMapper.selectList(
                new LambdaQueryWrapper<UserKpMastery>().eq(UserKpMastery::getUserId, userId));

        // 按知识点难度分组计算各维度能力
        Map<Integer, List<Double>> byLevel = new HashMap<>();
        for (UserKpMastery m : masteryList) {
            KnowledgePoint kp = kpMapper.selectById(m.getKnowledgePointId());
            int level = kp != null && kp.getDifficultyLevel() != null ? kp.getDifficultyLevel() : 1;
            byLevel.computeIfAbsent(level, k -> new ArrayList<>())
                    .add(m.getMastery() != null ? m.getMastery() : 0.0);
        }

        List<Map<String, Object>> dimensions = new ArrayList<>();
        String[] dimNames = {"基础概念", "原理理解", "应用能力", "综合分析", "创新思维"};
        for (int i = 1; i <= 5; i++) {
            List<Double> vals = byLevel.getOrDefault(i, Collections.emptyList());
            double avg = vals.isEmpty() ? 0 : vals.stream().mapToDouble(v -> v).average().orElse(0);
            Map<String, Object> dim = new HashMap<>();
            dim.put("name", dimNames[i - 1]);
            dim.put("value", Math.round(avg * 100));
            dimensions.add(dim);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dimensions", dimensions);
        result.put("maxValue", 100);
        return result;
    }

    // ==================== 热力图 ====================

    private List<Map<String, Object>> buildHeatmapData(Long userId) {
        List<UserKpMastery> masteryList = masteryMapper.selectList(
                new LambdaQueryWrapper<UserKpMastery>().eq(UserKpMastery::getUserId, userId));

        return masteryList.stream().map(m -> {
            KnowledgePoint kp = kpMapper.selectById(m.getKnowledgePointId());
            Map<String, Object> item = new HashMap<>();
            item.put("name", kp != null ? kp.getName() : "未知");
            item.put("mastery", Math.round((m.getMastery() != null ? m.getMastery() : 0) * 100));
            item.put("level", kp != null && kp.getDifficultyLevel() != null ? kp.getDifficultyLevel() : 1);
            item.put("practiceCount", m.getPracticeCount() != null ? m.getPracticeCount() : 0);
            return item;
        }).collect(Collectors.toList());
    }

    // ==================== 学习习惯 ====================

    private Map<String, Object> buildHabitsData(Long userId) {
        List<LearningActivity> activities = activityMapper.selectList(
                new LambdaQueryWrapper<LearningActivity>()
                        .eq(LearningActivity::getUserId, userId)
                        .orderByDesc(LearningActivity::getActivityDate)
                        .last("LIMIT 7"));

        // 按小时分布模拟（基于现有数据）
        int[] hourlyData = new int[8]; // 8-11, 12-15, 16-19, 20-23 各两个时段
        int totalMin = 0;
        for (LearningActivity a : activities) {
            totalMin += a.getStudyMinutes() != null ? a.getStudyMinutes() : 0;
        }
        int avgMin = activities.isEmpty() ? 0 : totalMin / activities.size();

        // 模拟时段分布
        hourlyData[0] = (int) (avgMin * 0.2);
        hourlyData[1] = (int) (avgMin * 0.4);
        hourlyData[2] = (int) (avgMin * 0.15);
        hourlyData[3] = (int) (avgMin * 0.25);
        hourlyData[4] = (int) (avgMin * 0.35);
        hourlyData[5] = (int) (avgMin * 0.5);
        hourlyData[6] = (int) (avgMin * 0.3);
        hourlyData[7] = (int) (avgMin * 0.1);

        Map<String, Object> result = new HashMap<>();
        result.put("hourlyDistribution", hourlyData);
        result.put("averageDailyMinutes", avgMin);
        result.put("totalStudyDays", activities.size());
        return result;
    }

    // ==================== 趋势数据 ====================

    private Map<String, Object> buildTrendsData(Long userId) {
        List<LearningActivity> activities = activityMapper.selectList(
                new LambdaQueryWrapper<LearningActivity>()
                        .eq(LearningActivity::getUserId, userId)
                        .orderByAsc(LearningActivity::getActivityDate)
                        .last("LIMIT 7"));

        List<String> dates = new ArrayList<>();
        List<Integer> studyData = new ArrayList<>();
        List<Integer> quizData = new ArrayList<>();
        List<Integer> qaData = new ArrayList<>();

        for (LearningActivity a : activities) {
            dates.add(a.getActivityDate() != null ? a.getActivityDate().toString().substring(5) : "");
            studyData.add(a.getStudyMinutes() != null ? a.getStudyMinutes() : 0);
            quizData.add(a.getQuestionCount() != null ? a.getQuestionCount() : 0);
            qaData.add(a.getLoginCount() != null ? a.getLoginCount() : 0);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("studyMinutes", studyData);
        result.put("quizCount", quizData);
        result.put("qaCount", qaData);
        return result;
    }

    // ==================== 预测预警 ====================

    private List<Map<String, Object>> buildPredictions(Long userId) {
        List<UserKpMastery> weakKps = masteryMapper.selectList(
                new LambdaQueryWrapper<UserKpMastery>()
                        .eq(UserKpMastery::getUserId, userId)
                        .lt(UserKpMastery::getMastery, 0.4)
                        .last("LIMIT 3"));

        List<Map<String, Object>> predictions = new ArrayList<>();
        for (UserKpMastery m : weakKps) {
            KnowledgePoint kp = kpMapper.selectById(m.getKnowledgePointId());
            Map<String, Object> pred = new HashMap<>();
            pred.put("type", "danger");
            pred.put("message", (kp != null ? kp.getName() : "知识点") +
                    " 掌握度仅 " + Math.round((m.getMastery() != null ? m.getMastery() : 0) * 100) + "%，建议优先复习");
            predictions.add(pred);
        }

        // 如果没有弱项，添加正面提示
        if (predictions.isEmpty()) {
            predictions.add(Map.of(
                    "type", "success",
                    "message", "所有知识点掌握良好，继续保持！"
            ));
        }

        return predictions;
    }

    // ==================== 科目详情 ====================

    private List<Map<String, Object>> buildSubjectDetails(Long userId) {
        List<UserKpMastery> allMastery = masteryMapper.selectList(
                new LambdaQueryWrapper<UserKpMastery>().eq(UserKpMastery::getUserId, userId));

        // 按知识点分组统计
        Map<String, List<Double>> subjectMap = new LinkedHashMap<>();

        for (UserKpMastery m : allMastery) {
            KnowledgePoint kp = kpMapper.selectById(m.getKnowledgePointId());
            String subject = kp != null && kp.getTags() != null ? kp.getTags() : "综合";
            subjectMap.computeIfAbsent(subject, k -> new ArrayList<>())
                    .add(m.getMastery() != null ? m.getMastery() : 0.0);
        }

        return subjectMap.entrySet().stream().map(entry -> {
            double avg = entry.getValue().stream().mapToDouble(v -> v).average().orElse(0);
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("mastery", Math.round(avg * 100));
            item.put("kpCount", entry.getValue().size());
            return item;
        }).collect(Collectors.toList());
    }

    // ==================== 辅助方法 ====================

    private int calcStreakDays(List<LearningActivity> activities) {
        if (activities.isEmpty()) return 0;
        // 简单计算：连续的天数
        int streak = 1;
        for (int i = 0; i < activities.size() - 1; i++) {
            LocalDate d1 = activities.get(i).getActivityDate();
            LocalDate d2 = activities.get(i + 1).getActivityDate();
            if (d1 != null && d2 != null && d1.minusDays(1).equals(d2)) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private Map<String, Object> createStatCard(String label, String value, String icon, String color) {
        Map<String, Object> card = new HashMap<>();
        card.put("label", label);
        card.put("value", value);
        card.put("icon", icon);
        card.put("color", color);
        return card;
    }
}
