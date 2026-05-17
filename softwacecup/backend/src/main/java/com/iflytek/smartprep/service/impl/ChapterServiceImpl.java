package com.iflytek.smartprep.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.mapper.*;
import com.iflytek.smartprep.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChapterServiceImpl implements ChapterService {

    private final ChapterMapper chapterMapper;
    private final SubChapterMapper subChapterMapper;
    private final KnowledgePointMapper kpMapper;
    private final ExerciseMapper exerciseMapper;

    @Override
    public List<Map<String, Object>> getChapterTree(Long courseId) {
        List<Chapter> chapters = chapterMapper.selectList(
                new LambdaQueryWrapper<Chapter>()
                        .eq(Chapter::getCourseId, courseId)
                        .orderByAsc(Chapter::getSortOrder));

        if (chapters.isEmpty()) return List.of();

        List<Long> chapterIds = chapters.stream().map(Chapter::getId).collect(Collectors.toList());
        List<SubChapter> allSubChapters = subChapterMapper.selectList(
                new LambdaQueryWrapper<SubChapter>()
                        .in(SubChapter::getChapterId, chapterIds)
                        .orderByAsc(SubChapter::getSortOrder));

        return chapters.stream().map(ch -> {
            Map<String, Object> chNode = new LinkedHashMap<>();
            chNode.put("id", ch.getId());
            chNode.put("title", ch.getTitle());
            chNode.put("description", ch.getDescription());
            chNode.put("sortOrder", ch.getSortOrder());

            List<SubChapter> chSubs = allSubChapters.stream()
                    .filter(sc -> sc.getChapterId().equals(ch.getId()))
                    .collect(Collectors.toList());

            List<Map<String, Object>> subNodes = chSubs.stream().map(sc -> {
                Map<String, Object> scNode = new LinkedHashMap<>();
                scNode.put("id", sc.getId());
                scNode.put("title", sc.getTitle());
                scNode.put("description", sc.getDescription());
                scNode.put("type", sc.getType());
                scNode.put("videoUrl", sc.getVideoUrl());
                scNode.put("duration", sc.getDuration());
                scNode.put("sortOrder", sc.getSortOrder());
                return scNode;
            }).collect(Collectors.toList());

            chNode.put("subChapters", subNodes);
            return chNode;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getSubChapterDetail(Long subChapterId) {
        SubChapter sc = subChapterMapper.selectById(subChapterId);
        if (sc == null) return Collections.emptyMap();

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", sc.getId());
        detail.put("chapterId", sc.getChapterId());
        detail.put("title", sc.getTitle());
        detail.put("description", sc.getDescription());
        detail.put("type", sc.getType());
        detail.put("videoUrl", sc.getVideoUrl());
        detail.put("duration", sc.getDuration());
        detail.put("content", sc.getContent());
        detail.put("coverUrl", sc.getCoverUrl());

        List<KnowledgePoint> kps = kpMapper.selectList(
                new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(KnowledgePoint::getLessonId, subChapterId));
        detail.put("knowledgePoints", kps.stream().map(kp -> {
            Map<String, Object> kpNode = new LinkedHashMap<>();
            kpNode.put("id", kp.getId());
            kpNode.put("name", kp.getName());
            kpNode.put("difficultyLevel", kp.getDifficultyLevel());
            kpNode.put("tags", kp.getTags());
            return kpNode;
        }).collect(Collectors.toList()));

        if (!kps.isEmpty()) {
            List<Long> kpIds = kps.stream().map(KnowledgePoint::getId).collect(Collectors.toList());
            List<Exercise> exercises = exerciseMapper.selectList(
                    new LambdaQueryWrapper<Exercise>()
                            .in(Exercise::getKnowledgePointId, kpIds));
            detail.put("exercises", exercises.stream().map(ex -> {
                Map<String, Object> exNode = new LinkedHashMap<>();
                exNode.put("id", ex.getId());
                exNode.put("knowledgePointId", ex.getKnowledgePointId());
                exNode.put("type", ex.getType());
                exNode.put("difficulty", ex.getDifficulty());
                exNode.put("contentJson", ex.getContentJson());
                exNode.put("answer", ex.getAnswer());
                exNode.put("explanation", ex.getExplanation());
                return exNode;
            }).collect(Collectors.toList()));
        } else {
            detail.put("exercises", List.of());
        }

        return detail;
    }

    @Override
    public Chapter createChapter(Chapter chapter) {
        chapter.setId(System.currentTimeMillis());
        chapter.setCreatedAt(java.time.LocalDateTime.now());
        chapter.setUpdatedAt(java.time.LocalDateTime.now());
        chapterMapper.insert(chapter);
        return chapter;
    }

    @Override
    public Chapter updateChapter(Long id, Chapter chapter) {
        chapter.setId(id);
        chapter.setUpdatedAt(java.time.LocalDateTime.now());
        chapterMapper.updateById(chapter);
        return chapterMapper.selectById(id);
    }

    @Override
    public void deleteChapter(Long id) {
        subChapterMapper.delete(new LambdaQueryWrapper<SubChapter>().eq(SubChapter::getChapterId, id));
        chapterMapper.deleteById(id);
    }

    @Override
    public SubChapter createSubChapter(SubChapter subChapter) {
        subChapter.setId(System.currentTimeMillis());
        subChapterMapper.insert(subChapter);
        return subChapter;
    }

    @Override
    public SubChapter updateSubChapter(Long id, SubChapter subChapter) {
        subChapter.setId(id);
        subChapterMapper.updateById(subChapter);
        return subChapterMapper.selectById(id);
    }

    @Override
    public void deleteSubChapter(Long id) {
        subChapterMapper.deleteById(id);
    }
}
