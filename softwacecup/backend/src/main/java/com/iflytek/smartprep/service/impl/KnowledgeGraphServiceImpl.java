package com.iflytek.smartprep.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.mapper.*;
import com.iflytek.smartprep.service.KnowledgeGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private final KnowledgePointMapper kpMapper;
    private final KpDependencyMapper depMapper;
    private final UserKpMasteryMapper masteryMapper;
    private final ExerciseMapper exerciseMapper;

    private static final double MASTERY_THRESHOLD = 0.6;
    private static final double K_FACTOR = 0.15; // ELO update factor

    @Override
    public Map<String, Object> getFullGraph() {
        List<KnowledgePoint> allKps = kpMapper.selectList(null);
        List<KpDependency> allDeps = depMapper.selectList(null);

        List<Map<String, Object>> nodes = allKps.stream().map(kp -> {
            Map<String, Object> node = new HashMap<>();
            node.put("id", kp.getId());
            node.put("name", kp.getName());
            node.put("difficultyLevel", kp.getDifficultyLevel());
            node.put("tags", kp.getTags());
            node.put("lessonId", kp.getSubChapterId());
            return node;
        }).collect(Collectors.toList());

        List<Map<String, Object>> edges = allDeps.stream().map(dep -> {
            Map<String, Object> edge = new HashMap<>();
            edge.put("from", dep.getDependsOnKpId());
            edge.put("to", dep.getKpId());
            edge.put("relationType", "depends_on");
            return edge;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("edges", edges);
        return result;
    }

    @Override
    public List<Map<String, Object>> getUserProgress(Long userId) {
        List<UserKpMastery> masteryList = masteryMapper.selectList(
                new LambdaQueryWrapper<UserKpMastery>().eq(UserKpMastery::getUserId, userId));
        Set<Long> knownKpIds = masteryList.stream()
                .map(UserKpMastery::getKnowledgePointId).collect(Collectors.toSet());

        List<KnowledgePoint> allKps = kpMapper.selectList(null);
        return allKps.stream().map(kp -> {
            Map<String, Object> item = new HashMap<>();
            item.put("knowledgePointId", kp.getId());
            item.put("name", kp.getName());
            item.put("difficultyLevel", kp.getDifficultyLevel());
            item.put("tags", kp.getTags());
            Optional<UserKpMastery> um = masteryList.stream()
                    .filter(m -> m.getKnowledgePointId().equals(kp.getId())).findFirst();
            item.put("mastery", um.map(UserKpMastery::getMastery).orElse(0.0));
            item.put("practiceCount", um.map(UserKpMastery::getPracticeCount).orElse(0));
            item.put("status", um.isPresent() ? (um.get().getMastery() >= MASTERY_THRESHOLD ? "mastered" : "learning") : "locked");
            return item;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getNextRecommended(Long userId) {
        List<UserKpMastery> masteryList = masteryMapper.selectList(
                new LambdaQueryWrapper<UserKpMastery>().eq(UserKpMastery::getUserId, userId));
        Map<Long, Double> mastered = new HashMap<>();
        for (UserKpMastery m : masteryList) {
            if (m.getMastery() >= MASTERY_THRESHOLD) {
                mastered.put(m.getKnowledgePointId(), m.getMastery());
            }
        }

        List<KpDependency> allDeps = depMapper.selectList(null);
        Map<Long, List<Long>> prereqMap = new HashMap<>(); // successor -> [prerequisites]
        for (KpDependency dep : allDeps) {
            prereqMap.computeIfAbsent(dep.getKpId(), k -> new ArrayList<>())
                    .add(dep.getDependsOnKpId());
        }

        List<KnowledgePoint> allKps = kpMapper.selectList(null);
        List<Map<String, Object>> recommended = new ArrayList<>();
        for (KnowledgePoint kp : allKps) {
            List<Long> prereqs = prereqMap.getOrDefault(kp.getId(), Collections.emptyList());
            if (prereqs.isEmpty()) continue; // skip root nodes (already recommended or accessible)

            boolean allPrereqsMastered = prereqs.stream().allMatch(mastered::containsKey);
            boolean selfNotMastered = !mastered.containsKey(kp.getId());

            if (allPrereqsMastered && selfNotMastered) {
                Map<String, Object> item = new HashMap<>();
                item.put("knowledgePointId", kp.getId());
                item.put("name", kp.getName());
                item.put("difficultyLevel", kp.getDifficultyLevel());
                item.put("tags", kp.getTags());
                // Find current user's mastery level
                masteryList.stream()
                        .filter(m -> m.getKnowledgePointId().equals(kp.getId())).findFirst()
                        .ifPresentOrElse(
                                m -> item.put("mastery", m.getMastery()),
                                () -> item.put("mastery", 0.0));
                recommended.add(item);
            }
        }
        return recommended;
    }

    @Override
    public void updateMastery(Long userId, Long knowledgePointId, boolean correct) {
        UserKpMastery existing = masteryMapper.selectOne(
                new LambdaQueryWrapper<UserKpMastery>()
                        .eq(UserKpMastery::getUserId, userId)
                        .eq(UserKpMastery::getKnowledgePointId, knowledgePointId));

        double oldMastery = 0.0;
        int practiceCount = 0;
        int correctCount = 0;

        if (existing != null) {
            oldMastery = existing.getMastery();
            practiceCount = existing.getPracticeCount();
            correctCount = existing.getCorrectCount();
        }

        // ELO-like update: expected = sigmoid-like, actual = 1 or 0
        double expected = 1.0 / (1.0 + Math.exp(-(oldMastery - 0.5) * 8.0));
        double actual = correct ? 1.0 : 0.0;
        double newMastery = Math.max(0.0, Math.min(1.0, oldMastery + K_FACTOR * (actual - expected)));

        practiceCount++;
        if (correct) correctCount++;

        if (existing != null) {
            existing.setMastery(Math.round(newMastery * 1000.0) / 1000.0);
            existing.setPracticeCount(practiceCount);
            existing.setCorrectCount(correctCount);
            existing.setLastPracticeAt(LocalDateTime.now());
            masteryMapper.updateById(existing);
        } else {
            UserKpMastery newRecord = new UserKpMastery();
            newRecord.setUserId(userId);
            newRecord.setKnowledgePointId(knowledgePointId);
            newRecord.setMastery(Math.round(newMastery * 1000.0) / 1000.0);
            newRecord.setPracticeCount(1);
            newRecord.setCorrectCount(correct ? 1 : 0);
            newRecord.setLastPracticeAt(LocalDateTime.now());
            masteryMapper.insert(newRecord);
        }
    }

    @Override
    public List<Map<String, Object>> getPrerequisiteChain(Long knowledgePointId) {
        List<KpDependency> allDeps = depMapper.selectList(null);
        Map<Long, Long> prereqMap = new HashMap<>(); // successor -> prerequisite (single parent for chain)
        for (KpDependency dep : allDeps) {
            prereqMap.putIfAbsent(dep.getKpId(), dep.getDependsOnKpId());
        }

        List<KnowledgePoint> allKps = kpMapper.selectList(null);
        Map<Long, KnowledgePoint> kpMap = allKps.stream()
                .collect(Collectors.toMap(KnowledgePoint::getId, k -> k));

        List<Map<String, Object>> chain = new ArrayList<>();
        Long current = knowledgePointId;
        while (current != null) {
            KnowledgePoint kp = kpMap.get(current);
            if (kp != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", kp.getId());
                item.put("name", kp.getName());
                item.put("difficultyLevel", kp.getDifficultyLevel());
                chain.add(0, item); // prepend to get root→leaf order
            }
            current = prereqMap.get(current);
        }
        return chain;
    }

    @Override
    public List<Map<String, Object>> getExercisesByKnowledgePoint(Long knowledgePointId) {
        List<Exercise> exercises = exerciseMapper.selectList(
                new LambdaQueryWrapper<Exercise>().eq(Exercise::getKnowledgePointId, knowledgePointId));
        return exercises.stream().map(ex -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", ex.getId());
            item.put("type", ex.getType());
            item.put("difficulty", ex.getDifficulty());
            item.put("contentJson", ex.getContentJson());
            item.put("answer", ex.getAnswer());
            item.put("explanation", ex.getExplanation());
            return item;
        }).collect(Collectors.toList());
    }
}
