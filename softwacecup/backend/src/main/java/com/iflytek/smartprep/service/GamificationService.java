package com.iflytek.smartprep.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 游戏化服务：XP/等级、徽章、打卡、排行榜
 */
@Service
@RequiredArgsConstructor
public class GamificationService {

    private final UserXpMapper xpMapper;
    private final BadgeDefMapper badgeDefMapper;
    private final UserBadgeMapper userBadgeMapper;
    private final UserStreakMapper streakMapper;
    private final UserMapper userMapper;
    private final UserKpMasteryMapper masteryMapper;
    private final QaHistoryMapper qaMapper;
    private final ExamRecordMapper examMapper;
    private final LearningActivityMapper activityMapper;

    // ==================== XP 与等级 ====================

    public Map<String, Object> getUserProgress(Long userId) {
        UserXp xp = getOrCreateXp(userId);
        int nextLevelXp = xpForLevel(xp.getLevel() + 1);
        int currentLevelXp = xpForLevel(xp.getLevel());

        Map<String, Object> result = new HashMap<>();
        result.put("level", xp.getLevel());
        result.put("title", xp.getTitle());
        result.put("currentXp", xp.getCurrentXp());
        result.put("nextLevelXp", nextLevelXp);
        result.put("currentLevelXp", currentLevelXp);
        result.put("progress", nextLevelXp > currentLevelXp ?
                Math.round((double)(xp.getCurrentXp() - currentLevelXp) / (nextLevelXp - currentLevelXp) * 100) : 100);
        result.put("titles", getTitlePath(xp.getLevel()));
        return result;
    }

    /** 添加 XP（教学行为触发） */
    public Map<String, Object> addXp(Long userId, int amount, String reason) {
        UserXp xp = getOrCreateXp(userId);
        int oldLevel = xp.getLevel();
        xp.setCurrentXp(xp.getCurrentXp() + amount);
        xp.setUpdatedAt(LocalDateTime.now());

        // 重新计算等级
        int newLevel = levelForXp(xp.getCurrentXp());
        xp.setLevel(newLevel);
        xp.setTitle(titleForLevel(newLevel));

        if (xp.getId() == null) {
            xpMapper.insert(xp);
        } else {
            xpMapper.updateById(xp);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("added", amount);
        result.put("reason", reason);
        result.put("newTotal", xp.getCurrentXp());
        result.put("level", xp.getLevel());
        result.put("leveledUp", newLevel > oldLevel);
        if (newLevel > oldLevel) {
            result.put("newTitle", xp.getTitle());
        }
        return result;
    }

    // ==================== 徽章 ====================

    public Map<String, Object> getBadges(Long userId) {
        List<BadgeDef> allDefs = badgeDefMapper.selectList(
                new LambdaQueryWrapper<BadgeDef>().orderByAsc(BadgeDef::getSortOrder));
        List<UserBadge> unlocked = userBadgeMapper.selectList(
                new LambdaQueryWrapper<UserBadge>().eq(UserBadge::getUserId, userId));
        Set<Long> unlockedIds = unlocked.stream()
                .map(UserBadge::getBadgeId).collect(Collectors.toSet());

        List<Map<String, Object>> badges = allDefs.stream().map(def -> {
            Map<String, Object> b = new HashMap<>();
            b.put("id", def.getId());
            b.put("name", def.getName());
            b.put("description", def.getDescription());
            b.put("icon", def.getIcon());
            b.put("color", def.getColor());
            b.put("unlocked", unlockedIds.contains(def.getId()));
            unlocked.stream()
                    .filter(ub -> ub.getBadgeId().equals(def.getId()))
                    .findFirst()
                    .ifPresent(ub -> b.put("unlockedAt", ub.getUnlockedAt()));
            return b;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("badges", badges);
        result.put("unlockedCount", unlockedIds.size());
        result.put("totalCount", allDefs.size());
        return result;
    }

    /** 检查并解锁徽章 */
    public List<Map<String, Object>> checkAndUnlockBadges(Long userId) {
        List<BadgeDef> allDefs = badgeDefMapper.selectList(null);
        List<UserBadge> unlocked = userBadgeMapper.selectList(
                new LambdaQueryWrapper<UserBadge>().eq(UserBadge::getUserId, userId));
        Set<Long> unlockedIds = unlocked.stream()
                .map(UserBadge::getBadgeId).collect(Collectors.toSet());

        List<Map<String, Object>> newlyUnlocked = new ArrayList<>();
        for (BadgeDef def : allDefs) {
            if (unlockedIds.contains(def.getId())) continue;
            // Simple rule check — full implementation would check actual user data
            boolean shouldUnlock = checkBadgeRule(userId, def.getUnlockRule());
            if (shouldUnlock) {
                UserBadge ub = new UserBadge();
                ub.setUserId(userId);
                ub.setBadgeId(def.getId());
                ub.setUnlockedAt(LocalDateTime.now());
                userBadgeMapper.insert(ub);

                Map<String, Object> info = new HashMap<>();
                info.put("badgeId", def.getId());
                info.put("name", def.getName());
                info.put("icon", def.getIcon());
                newlyUnlocked.add(info);
            }
        }
        return newlyUnlocked;
    }

    // ==================== 打卡 ====================

    public Map<String, Object> getStreak(Long userId) {
        UserStreak streak = getOrCreateStreak(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("currentStreak", streak.getCurrentStreak());
        result.put("longestStreak", streak.getLongestStreak());
        result.put("lastCheckinDate", streak.getLastCheckinDate());
        result.put("totalCheckins", streak.getTotalCheckins());
        result.put("checkedInToday", streak.getLastCheckinDate() != null &&
                streak.getLastCheckinDate().equals(LocalDate.now()));
        return result;
    }

    /** 每日签到 */
    public Map<String, Object> checkin(Long userId) {
        UserStreak streak = getOrCreateStreak(userId);
        LocalDate today = LocalDate.now();

        Map<String, Object> result = new HashMap<>();
        if (streak.getLastCheckinDate() != null && streak.getLastCheckinDate().equals(today)) {
            result.put("alreadyCheckedIn", true);
            result.put("streak", streak.getCurrentStreak());
            return result;
        }

        // 续签或重新开始
        if (streak.getLastCheckinDate() != null &&
                streak.getLastCheckinDate().equals(today.minusDays(1))) {
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        } else {
            streak.setCurrentStreak(1);
        }

        if (streak.getCurrentStreak() > streak.getLongestStreak()) {
            streak.setLongestStreak(streak.getCurrentStreak());
        }

        streak.setLastCheckinDate(today);
        streak.setTotalCheckins(streak.getTotalCheckins() + 1);
        streak.setUpdatedAt(LocalDateTime.now());

        if (streak.getId() == null) {
            streakMapper.insert(streak);
        } else {
            streakMapper.updateById(streak);
        }

        // 签到奖励XP
        int bonusXp = streak.getCurrentStreak() >= 7 ? 20 : 5;
        addXp(userId, bonusXp, "每日签到 (+" + bonusXp + "XP)");

        result.put("streak", streak.getCurrentStreak());
        result.put("bonusXp", bonusXp);
        result.put("alreadyCheckedIn", false);
        return result;
    }

    // ==================== 排行榜 ====================

    public List<Map<String, Object>> getLeaderboard(int limit) {
        List<UserXp> topUsers = xpMapper.selectList(
                new LambdaQueryWrapper<UserXp>()
                        .orderByDesc(UserXp::getCurrentXp)
                        .last("LIMIT " + limit));

        List<Map<String, Object>> board = new ArrayList<>();
        int rank = 0;
        for (UserXp ux : topUsers) {
            rank++;
            User user = userMapper.selectById(ux.getUserId());
            Map<String, Object> item = new HashMap<>();
            item.put("rank", rank);
            item.put("userId", ux.getUserId());
            item.put("level", ux.getLevel());
            item.put("xp", ux.getCurrentXp());
            item.put("title", ux.getTitle());
            item.put("displayName", user != null ? user.getDisplayName() : ("用户" + ux.getUserId()));
            item.put("avatarUrl", user != null ? user.getAvatarUrl() : "");
            board.add(item);
        }
        return board;
    }

    // ==================== 每日挑战 ====================

    public List<Map<String, Object>> getDailyChallenges(Long userId) {
        // 从数据库获取真实进度
        List<LearningActivity> todayActivities = activityMapper.selectList(
                new LambdaQueryWrapper<LearningActivity>()
                        .eq(LearningActivity::getUserId, userId)
                        .eq(LearningActivity::getActivityDate, LocalDate.now()));
        LearningActivity today = todayActivities.isEmpty() ? null : todayActivities.get(0);

        int studyMinutes = today != null && today.getStudyMinutes() != null ? today.getStudyMinutes() : 0;
        int questionCount = today != null && today.getQuestionCount() != null ? today.getQuestionCount() : 0;
        Long qaCount = qaMapper.selectCount(
                new LambdaQueryWrapper<QaHistory>()
                        .eq(QaHistory::getUserId, userId)
                        .ge(QaHistory::getCreatedAt, LocalDate.now().atStartOfDay()));

        return List.of(
                createChallenge("完成5道练习题", "quiz_5", 5, "+30 XP", questionCount),
                createChallenge("观看1个教学视频", "watch_video", 1, "+20 XP",
                        today != null && today.getActivityScore() >= 5 ? 1 : 0),
                createChallenge("向AI导师提问", "ask_ai", 1, "+15 XP", qaCount.intValue()),
                createChallenge("连续学习30分钟", "study_30min", 30, "+25 XP", studyMinutes)
        );
    }

    // ==================== 辅助方法 ====================

    private UserXp getOrCreateXp(Long userId) {
        List<UserXp> list = xpMapper.selectList(
                new LambdaQueryWrapper<UserXp>().eq(UserXp::getUserId, userId));
        if (!list.isEmpty()) return list.get(0);
        UserXp xp = new UserXp();
        xp.setUserId(userId);
        xp.setCurrentXp(0);
        xp.setLevel(1);
        xp.setTitle("初学者");
        xp.setCreatedAt(LocalDateTime.now());
        xp.setUpdatedAt(LocalDateTime.now());
        return xp;
    }

    private UserStreak getOrCreateStreak(Long userId) {
        List<UserStreak> list = streakMapper.selectList(
                new LambdaQueryWrapper<UserStreak>().eq(UserStreak::getUserId, userId));
        if (!list.isEmpty()) return list.get(0);
        UserStreak s = new UserStreak();
        s.setUserId(userId);
        s.setCurrentStreak(0);
        s.setLongestStreak(0);
        s.setTotalCheckins(0);
        return s;
    }

    /** XP → 等级映射 */
    private int levelForXp(int xp) {
        for (int lv = 1; lv <= 50; lv++) {
            if (xp < xpForLevel(lv + 1)) return lv;
        }
        return 50;
    }

    private int xpForLevel(int level) {
        return (int) (100 * Math.pow(level, 1.5));
    }

    private String titleForLevel(int level) {
        String[] titles = {"初学者", "勤奋学徒", "知识探索者", "学习达人", "进阶高手",
                "知识先锋", "学霸精英", "卓越学者", "智慧大师", "传奇导师"};
        return level <= titles.length ? titles[level - 1] : "终极学者";
    }

    private List<String> getTitlePath(int currentLevel) {
        String[] titles = {"初学者", "勤奋学徒", "知识探索者", "学习达人", "进阶高手",
                "知识先锋", "学霸精英", "卓越学者", "智慧大师", "传奇导师"};
        List<String> path = new ArrayList<>();
        for (int i = 0; i < titles.length && i < currentLevel + 3; i++) {
            path.add(titles[i]);
        }
        return path;
    }

    @SuppressWarnings("unchecked")
    private boolean checkBadgeRule(Long userId, String ruleJson) {
        if (ruleJson == null || ruleJson.isBlank()) return false;
        try {
            ObjectMapper om = new ObjectMapper();
            Map<String, Object> rule = om.readValue(ruleJson, new TypeReference<Map<String, Object>>() {});
            String type = (String) rule.get("type");
            if (type == null) return false;

            return switch (type) {
                case "first_login" -> true; // 用户已存在即已登录过
                case "streak" -> {
                    UserStreak streak = getOrCreateStreak(userId);
                    int required = ((Number) rule.getOrDefault("days", 7)).intValue();
                    yield streak.getCurrentStreak() >= required || streak.getLongestStreak() >= required;
                }
                case "quiz_count" -> {
                    int required = ((Number) rule.getOrDefault("count", 100)).intValue();
                    List<UserKpMastery> all = masteryMapper.selectList(
                            new LambdaQueryWrapper<UserKpMastery>().eq(UserKpMastery::getUserId, userId));
                    int total = all.stream().mapToInt(m -> m.getPracticeCount() != null ? m.getPracticeCount() : 0).sum();
                    yield total >= required;
                }
                case "qa_count" -> {
                    int required = ((Number) rule.getOrDefault("count", 20)).intValue();
                    Long count = qaMapper.selectCount(
                            new LambdaQueryWrapper<QaHistory>().eq(QaHistory::getUserId, userId));
                    yield count >= required;
                }
                case "all_mastery" -> {
                    double threshold = ((Number) rule.getOrDefault("threshold", 0.6)).doubleValue();
                    List<UserKpMastery> all = masteryMapper.selectList(
                            new LambdaQueryWrapper<UserKpMastery>().eq(UserKpMastery::getUserId, userId));
                    yield !all.isEmpty() && all.stream().allMatch(m ->
                            (m.getMastery() != null ? m.getMastery() : 0) >= threshold);
                }
                case "exam_avg" -> {
                    int required = ((Number) rule.getOrDefault("score", 85)).intValue();
                    List<ExamRecord> records = examMapper.selectList(
                            new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getUserId, userId));
                    if (records.isEmpty()) yield false;
                    double avg = records.stream().mapToInt(r -> r.getScore() != null ? r.getScore() : 0)
                            .average().orElse(0);
                    yield avg >= required;
                }
                case "total_days" -> {
                    int required = ((Number) rule.getOrDefault("days", 30)).intValue();
                    Long days = activityMapper.selectCount(
                            new LambdaQueryWrapper<LearningActivity>().eq(LearningActivity::getUserId, userId));
                    yield days >= required;
                }
                case "perfect_quiz" -> {
                    // 检查是否有过全对记录：有知识点练习全对
                    List<UserKpMastery> all = masteryMapper.selectList(
                            new LambdaQueryWrapper<UserKpMastery>().eq(UserKpMastery::getUserId, userId));
                    yield all.stream().anyMatch(m ->
                            m.getPracticeCount() != null && m.getPracticeCount() > 0 &&
                                    m.getCorrectCount() != null && m.getCorrectCount().equals(m.getPracticeCount()));
                }
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> createChallenge(String name, String id, int target, String reward, int current) {
        Map<String, Object> c = new HashMap<>();
        c.put("id", id);
        c.put("name", name);
        c.put("target", target);
        c.put("reward", reward);
        c.put("progress", target > 0 ? Math.min(100, Math.round((float) current / target * 100)) : 0);
        c.put("current", current);
        return c;
    }
}
