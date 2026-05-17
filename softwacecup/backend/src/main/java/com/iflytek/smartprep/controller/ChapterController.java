package com.iflytek.smartprep.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.config.RequireRole;
import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.mapper.*;
import com.iflytek.smartprep.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;
    private final ChapterResourceMapper resourceMapper;
    private final CourseAnnouncementMapper announcementMapper;
    private final LessonProgressMapper lessonProgressMapper;

    // ==================== 章节树 ====================

    @GetMapping("/courses/{id}/chapters")
    public ApiResponse<List<Map<String, Object>>> getChapters(@PathVariable Long id) {
        return ApiResponse.ok(chapterService.getChapterTree(id));
    }

    // ==================== 子章节 ====================

    @GetMapping("/sub-chapters/{id}")
    public ApiResponse<Map<String, Object>> getSubChapter(@PathVariable Long id) {
        return ApiResponse.ok(chapterService.getSubChapterDetail(id));
    }

    // ==================== 章节CRUD ====================

    @PostMapping("/chapters")
    @RequireRole({"teacher"})
    public ApiResponse<Chapter> createChapter(@RequestBody Chapter chapter) {
        return ApiResponse.ok(chapterService.createChapter(chapter));
    }

    @PutMapping("/chapters/{id}")
    @RequireRole({"teacher"})
    public ApiResponse<Chapter> updateChapter(@PathVariable Long id, @RequestBody Chapter chapter) {
        return ApiResponse.ok(chapterService.updateChapter(id, chapter));
    }

    @DeleteMapping("/chapters/{id}")
    @RequireRole({"teacher"})
    public ApiResponse<String> deleteChapter(@PathVariable Long id) {
        chapterService.deleteChapter(id);
        return ApiResponse.ok("ok");
    }

    // ==================== 子章节CRUD ====================

    @PostMapping("/sub-chapters")
    @RequireRole({"teacher"})
    public ApiResponse<SubChapter> createSubChapter(@RequestBody SubChapter subChapter) {
        return ApiResponse.ok(chapterService.createSubChapter(subChapter));
    }

    @PutMapping("/sub-chapters/{id}")
    @RequireRole({"teacher"})
    public ApiResponse<SubChapter> updateSubChapter(@PathVariable Long id, @RequestBody SubChapter subChapter) {
        return ApiResponse.ok(chapterService.updateSubChapter(id, subChapter));
    }

    @DeleteMapping("/sub-chapters/{id}")
    @RequireRole({"teacher"})
    public ApiResponse<String> deleteSubChapter(@PathVariable Long id) {
        chapterService.deleteSubChapter(id);
        return ApiResponse.ok("ok");
    }

    // ==================== 章节资源 ====================

    @GetMapping("/chapters/{id}/resources")
    public ApiResponse<List<ChapterResource>> getChapterResources(@PathVariable Long id) {
        return ApiResponse.ok(resourceMapper.selectList(
                new LambdaQueryWrapper<ChapterResource>()
                        .eq(ChapterResource::getChapterId, id)
                        .orderByDesc(ChapterResource::getCreatedAt)));
    }

    @PostMapping("/chapters/{id}/resources")
    @RequireRole({"teacher"})
    public ApiResponse<ChapterResource> addResource(@PathVariable Long id, @RequestBody ChapterResource resource) {
        resource.setId(System.currentTimeMillis());
        resource.setChapterId(id);
        resource.setCreatedAt(LocalDateTime.now());
        resource.setUpdatedAt(LocalDateTime.now());
        resourceMapper.insert(resource);
        return ApiResponse.ok(resource);
    }

    @DeleteMapping("/resources/{id}")
    @RequireRole({"teacher"})
    public ApiResponse<String> deleteResource(@PathVariable Long id) {
        resourceMapper.deleteById(id);
        return ApiResponse.ok("ok");
    }

    @GetMapping("/courses/{id}/resources")
    public ApiResponse<List<ChapterResource>> getCourseResources(@PathVariable Long id) {
        return ApiResponse.ok(resourceMapper.selectList(
                new LambdaQueryWrapper<ChapterResource>()
                        .eq(ChapterResource::getCourseId, id)
                        .orderByDesc(ChapterResource::getCreatedAt)));
    }

    // ==================== 课程公告 ====================

    @GetMapping("/courses/{id}/announcements")
    public ApiResponse<List<CourseAnnouncement>> getAnnouncements(@PathVariable Long id) {
        return ApiResponse.ok(announcementMapper.selectList(
                new LambdaQueryWrapper<CourseAnnouncement>()
                        .eq(CourseAnnouncement::getCourseId, id)
                        .orderByDesc(CourseAnnouncement::getCreatedAt)));
    }

    @PostMapping("/courses/{id}/announcements")
    @RequireRole({"teacher"})
    public ApiResponse<CourseAnnouncement> createAnnouncement(@PathVariable Long id, @RequestBody CourseAnnouncement announcement) {
        announcement.setId(System.currentTimeMillis());
        announcement.setCourseId(id);
        announcement.setCreatedAt(LocalDateTime.now());
        announcement.setUpdatedAt(LocalDateTime.now());
        announcementMapper.insert(announcement);
        return ApiResponse.ok(announcement);
    }

    // ==================== 进度 ====================

    @GetMapping("/progress/sub-chapter/{id}")
    public ApiResponse<LessonProgress> getSubChapterProgress(@PathVariable Long id) {
        Long userId = LoginUserHolder.get().getUserId();
        LessonProgress lp = lessonProgressMapper.selectOne(
                new LambdaQueryWrapper<LessonProgress>()
                        .eq(LessonProgress::getUserId, userId)
                        .eq(LessonProgress::getLessonId, id));
        return ApiResponse.ok(lp != null ? lp : new LessonProgress());
    }

    @PostMapping("/progress/sub-chapter/{id}/complete")
    public ApiResponse<String> completeSubChapter(@PathVariable Long id) {
        Long userId = LoginUserHolder.get().getUserId();
        LessonProgress lp = lessonProgressMapper.selectOne(
                new LambdaQueryWrapper<LessonProgress>()
                        .eq(LessonProgress::getUserId, userId)
                        .eq(LessonProgress::getLessonId, id));
        if (lp == null) {
            lp = new LessonProgress();
            lp.setId(System.currentTimeMillis());
            lp.setUserId(userId);
            lp.setLessonId(id);
            lp.setStatus("completed");
            lp.setCompletedAt(LocalDateTime.now());
            lessonProgressMapper.insert(lp);
        } else {
            lp.setStatus("completed");
            lp.setCompletedAt(LocalDateTime.now());
            lessonProgressMapper.updateById(lp);
        }
        return ApiResponse.ok("ok");
    }

    @GetMapping("/progress/course/{id}")
    public ApiResponse<Map<String, Object>> getCourseProgress(@PathVariable Long id) {
        Long userId = LoginUserHolder.get().getUserId();
        List<Map<String, Object>> tree = chapterService.getChapterTree(id);
        List<Long> lessonIds = new ArrayList<>();
        for (Map<String, Object> ch : tree) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> subs = (List<Map<String, Object>>) ch.get("subChapters");
            if (subs != null) {
                for (Map<String, Object> sc : subs) {
                    lessonIds.add((Long) sc.get("id"));
                }
            }
        }
        long total = lessonIds.size();
        long completed = total > 0 ? lessonProgressMapper.selectCount(
                new LambdaQueryWrapper<LessonProgress>()
                        .eq(LessonProgress::getUserId, userId)
                        .eq(LessonProgress::getStatus, "completed")
                        .in(LessonProgress::getLessonId, lessonIds)) : 0;
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("completed", completed);
        result.put("percent", total > 0 ? (int) (completed * 100 / total) : 0);
        return ApiResponse.ok(result);
    }
}
