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
    private final SubChapterMapper subChapterMapper;
    private final ChapterMapper chapterMapper;
    private final CourseMapper courseMapper;
    private final SubjectMapper subjectMapper;

    private static final double MASTERY_THRESHOLD = 0.6;
    private static final double K_FACTOR = 0.15; // ELO update factor

    @Override
    public Map<String, Object> getFullGraph() {
        List<KnowledgePoint> allKps = kpMapper.selectList(null);
        List<KpDependency> allDeps = depMapper.selectList(null);

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        // Track unique entities to avoid duplicates
        java.util.Set<Long> addedSubChapterIds = new java.util.HashSet<>();
        java.util.Set<Long> addedChapterIds = new java.util.HashSet<>();
        java.util.Set<Long> addedCourseIds = new java.util.HashSet<>();
        java.util.Set<Long> addedSubjectIds = new java.util.HashSet<>();

        // KnowledgePoint nodes + walk up to SubChapter → Chapter → Course → Subject
        for (KnowledgePoint kp : allKps) {
            Long subChapterId = kp.getLessonId();
            Long courseId = null;

            // Walk up the hierarchy to find courseId and build nodes
            if (subChapterId != null) {
                SubChapter sc = subChapterMapper.selectById(subChapterId);
                if (sc != null) {
                    // KP → SubChapter edge
                    if (addedSubChapterIds.add(sc.getId())) {
                        Map<String, Object> scNode = new HashMap<>();
                        scNode.put("id", "sc_" + sc.getId());
                        scNode.put("type", "sub_chapter");
                        scNode.put("name", sc.getTitle());
                        scNode.put("subChapterId", sc.getId());
                        nodes.add(scNode);
                    }
                    edges.add(edge("kp_" + kp.getId(), "sc_" + sc.getId(), "belongs_to"));

                    if (sc.getChapterId() != null) {
                        Chapter chapter = chapterMapper.selectById(sc.getChapterId());
                        if (chapter != null) {
                            courseId = chapter.getCourseId();
                            // Add courseId to SubChapter node
                            updateNodeField(nodes, "sc_" + sc.getId(), "courseId", courseId);

                            // SubChapter → Chapter edge
                            if (addedChapterIds.add(chapter.getId())) {
                                Map<String, Object> chNode = new HashMap<>();
                                chNode.put("id", "ch_" + chapter.getId());
                                chNode.put("type", "chapter");
                                chNode.put("name", chapter.getTitle());
                                chNode.put("chapterId", chapter.getId());
                                chNode.put("courseId", courseId);
                                nodes.add(chNode);
                            }
                            edges.add(edge("sc_" + sc.getId(), "ch_" + chapter.getId(), "contains"));

                            if (courseId != null) {
                                Course course = courseMapper.selectById(courseId);
                                if (course != null) {
                                    // Chapter → Course edge
                                    if (addedCourseIds.add(course.getId())) {
                                        Map<String, Object> cNode = new HashMap<>();
                                        cNode.put("id", "c_" + course.getId());
                                        cNode.put("type", "course");
                                        cNode.put("name", course.getTitle());
                                        cNode.put("courseId", course.getId());
                                        nodes.add(cNode);
                                    }
                                    edges.add(edge("ch_" + chapter.getId(), "c_" + course.getId(), "contains"));

                                    if (course.getSubjectId() != null) {
                                        Subject subject = subjectMapper.selectById(course.getSubjectId());
                                        if (subject != null) {
                                            // Course → Subject edge
                                            if (addedSubjectIds.add(subject.getId())) {
                                                Map<String, Object> sNode = new HashMap<>();
                                                sNode.put("id", "s_" + subject.getId());
                                                sNode.put("type", "subject");
                                                sNode.put("name", subject.getName());
                                                sNode.put("subjectId", subject.getId());
                                                nodes.add(sNode);
                                            }
                                            edges.add(edge("c_" + course.getId(), "s_" + subject.getId(), "contains"));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Map<String, Object> kpNode = new HashMap<>();
            kpNode.put("id", "kp_" + kp.getId());
            kpNode.put("type", "knowledge_point");
            kpNode.put("name", kp.getName());
            kpNode.put("difficultyLevel", kp.getDifficultyLevel());
            kpNode.put("tags", kp.getTags());
            kpNode.put("subChapterId", subChapterId);
            kpNode.put("lessonId", subChapterId);
            kpNode.put("courseId", courseId);
            nodes.add(kpNode);
        }

        // DEPENDS_ON edges (between knowledge points)
        for (KpDependency dep : allDeps) {
            edges.add(edge("kp_" + dep.getDependsOnKpId(), "kp_" + dep.getKpId(), "depends_on"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("edges", edges);
        return result;
    }

    private Map<String, Object> edge(String from, String to, String relationType) {
        Map<String, Object> edge = new HashMap<>();
        edge.put("from", from);
        edge.put("to", to);
        edge.put("relationType", relationType);
        return edge;
    }

    private void updateNodeField(List<Map<String, Object>> nodes, String nodeId, String key, Object value) {
        for (Map<String, Object> node : nodes) {
            if (nodeId.equals(node.get("id"))) {
                node.put(key, value);
                return;
            }
        }
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
