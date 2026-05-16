package com.iflytek.smartprep.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.mapper.*;
import com.iflytek.smartprep.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final SubjectMapper subjectMapper;
    private final UnitMapper unitMapper;
    private final LessonMapper lessonMapper;
    private final KnowledgePointMapper kpMapper;
    private final ExerciseMapper exerciseMapper;

    @Override
    public List<Map<String, Object>> getSubjectTree() {
        List<Subject> subjects = subjectMapper.selectList(
                new LambdaQueryWrapper<Subject>().orderByAsc(Subject::getSortOrder));
        List<Unit> allUnits = unitMapper.selectList(
                new LambdaQueryWrapper<Unit>().orderByAsc(Unit::getSortOrder));
        List<Lesson> allLessons = lessonMapper.selectList(
                new LambdaQueryWrapper<Lesson>().orderByAsc(Lesson::getSortOrder));

        return subjects.stream().map(subject -> {
            Map<String, Object> subjNode = new HashMap<>();
            subjNode.put("id", subject.getId());
            subjNode.put("name", subject.getName());
            subjNode.put("icon", subject.getIcon());
            subjNode.put("color", subject.getColor());
            subjNode.put("description", subject.getDescription());

            List<Unit> subjUnits = allUnits.stream()
                    .filter(u -> u.getSubjectId().equals(subject.getId()))
                    .collect(Collectors.toList());

            List<Map<String, Object>> unitNodes = subjUnits.stream().map(unit -> {
                Map<String, Object> unitNode = new HashMap<>();
                unitNode.put("id", unit.getId());
                unitNode.put("name", unit.getName());
                unitNode.put("description", unit.getDescription());
                unitNode.put("sortOrder", unit.getSortOrder());
                unitNode.put("prerequisiteUnitId", unit.getPrerequisiteUnitId());

                List<Lesson> unitLessons = allLessons.stream()
                        .filter(l -> l.getUnitId().equals(unit.getId()))
                        .collect(Collectors.toList());

                List<Map<String, Object>> lessonNodes = unitLessons.stream().map(lesson -> {
                    Map<String, Object> lessonNode = new HashMap<>();
                    lessonNode.put("id", lesson.getId());
                    lessonNode.put("name", lesson.getName());
                    lessonNode.put("type", lesson.getType());
                    lessonNode.put("videoUrl", lesson.getVideoUrl());
                    lessonNode.put("duration", lesson.getDuration());
                    lessonNode.put("sortOrder", lesson.getSortOrder());
                    return lessonNode;
                }).collect(Collectors.toList());

                unitNode.put("lessons", lessonNodes);
                return unitNode;
            }).collect(Collectors.toList());

            subjNode.put("units", unitNodes);
            return subjNode;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getLessonDetail(Long lessonId) {
        Lesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null) return Collections.emptyMap();

        Map<String, Object> detail = new HashMap<>();
        detail.put("id", lesson.getId());
        detail.put("unitId", lesson.getUnitId());
        detail.put("name", lesson.getName());
        detail.put("type", lesson.getType());
        detail.put("videoUrl", lesson.getVideoUrl());
        detail.put("duration", lesson.getDuration());
        detail.put("content", lesson.getContent());

        // 关联知识点
        List<KnowledgePoint> kps = kpMapper.selectList(
                new LambdaQueryWrapper<KnowledgePoint>().eq(KnowledgePoint::getLessonId, lessonId));
        detail.put("knowledgePoints", kps.stream().map(kp -> {
            Map<String, Object> kpNode = new HashMap<>();
            kpNode.put("id", kp.getId());
            kpNode.put("name", kp.getName());
            kpNode.put("difficultyLevel", kp.getDifficultyLevel());
            kpNode.put("tags", kp.getTags());
            return kpNode;
        }).collect(Collectors.toList()));

        // 关联练习
        List<Exercise> exercises = exerciseMapper.selectList(
                new LambdaQueryWrapper<Exercise>().in(Exercise::getKnowledgePointId,
                        kps.stream().map(KnowledgePoint::getId).collect(Collectors.toList())));
        detail.put("exercises", exercises.stream().map(ex -> {
            Map<String, Object> exNode = new HashMap<>();
            exNode.put("id", ex.getId());
            exNode.put("knowledgePointId", ex.getKnowledgePointId());
            exNode.put("type", ex.getType());
            exNode.put("difficulty", ex.getDifficulty());
            exNode.put("contentJson", ex.getContentJson());
            exNode.put("answer", ex.getAnswer());
            exNode.put("explanation", ex.getExplanation());
            return exNode;
        }).collect(Collectors.toList()));

        return detail;
    }

    @Override
    public List<Map<String, Object>> getLessonsByUnit(Long unitId) {
        List<Lesson> lessons = lessonMapper.selectList(
                new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getUnitId, unitId)
                        .orderByAsc(Lesson::getSortOrder));
        return lessons.stream().map(l -> {
            Map<String, Object> node = new HashMap<>();
            node.put("id", l.getId());
            node.put("name", l.getName());
            node.put("type", l.getType());
            node.put("videoUrl", l.getVideoUrl());
            node.put("duration", l.getDuration());
            node.put("sortOrder", l.getSortOrder());
            return node;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getKnowledgePointsByLesson(Long lessonId) {
        List<KnowledgePoint> kps = kpMapper.selectList(
                new LambdaQueryWrapper<KnowledgePoint>().eq(KnowledgePoint::getLessonId, lessonId));
        return kps.stream().map(kp -> {
            Map<String, Object> node = new HashMap<>();
            node.put("id", kp.getId());
            node.put("name", kp.getName());
            node.put("difficultyLevel", kp.getDifficultyLevel());
            node.put("tags", kp.getTags());
            return node;
        }).collect(Collectors.toList());
    }
}
