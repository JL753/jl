package com.iflytek.smartprep.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.config.RequireRole;
import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.mapper.*;
import com.iflytek.smartprep.service.agent.WebSearchAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubjectCourseController {

    private final SubjectMapper subjectMapper;
    private final CourseMapper courseMapper;
    private final UnitMapper unitMapper;
    private final LessonMapper lessonMapper;
    private final ExerciseMapper exerciseMapper;

    // ==================== 学科 ====================

    @GetMapping("/subjects")
    public ApiResponse<List<Subject>> listSubjects() {
        return ApiResponse.ok(subjectMapper.selectList(
                new LambdaQueryWrapper<Subject>().orderByAsc(Subject::getSortOrder)));
    }

    /** GET /api/subjects/{id}/courses — 获取学科下课程（公开） */
    @GetMapping("/subjects/{id}/courses")
    public ApiResponse<List<Course>> listCoursesBySubject(@PathVariable Long id) {
        return ApiResponse.ok(courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getStatus, "已发布")
                        .or().eq(Course::getStatus, "published")
                        .orderByDesc(Course::getCreatedAt)));
    }

    /** GET /api/courses/public — 获取所有已发布课程（公开） */
    @GetMapping("/courses/public")
    public ApiResponse<List<Course>> listPublicCourses() {
        return ApiResponse.ok(courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getStatus, "已发布")
                        .or().eq(Course::getStatus, "published")
                        .orderByDesc(Course::getCreatedAt)));
    }

    /** GET /api/courses/{id}/units — 获取课程下单元（公开） */
    @GetMapping("/courses/{id}/units")
    public ApiResponse<List<Unit>> listUnitsByCourse(@PathVariable Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) return ApiResponse.fail("课程不存在");
        return ApiResponse.ok(unitMapper.selectList(
                new LambdaQueryWrapper<Unit>()
                        .orderByAsc(Unit::getSortOrder)));
    }

    @GetMapping("/subjects/{id}")
    public ApiResponse<Subject> getSubject(@PathVariable Long id) {
        Subject s = subjectMapper.selectById(id);
        return s != null ? ApiResponse.ok(s) : ApiResponse.fail("学科不存在");
    }

    @GetMapping("/subjects/{id}/units")
    public ApiResponse<List<Unit>> getSubjectUnits(@PathVariable Long id) {
        return ApiResponse.ok(unitMapper.selectList(
                new LambdaQueryWrapper<Unit>()
                        .eq(Unit::getSubjectId, id)
                        .orderByAsc(Unit::getSortOrder)));
    }

    // ==================== 单元 ====================

    @GetMapping("/units/{id}")
    public ApiResponse<Unit> getUnit(@PathVariable Long id) {
        Unit u = unitMapper.selectById(id);
        return u != null ? ApiResponse.ok(u) : ApiResponse.fail("单元不存在");
    }

    @GetMapping("/units/{id}/lessons")
    public ApiResponse<List<Lesson>> getUnitLessons(@PathVariable Long id) {
        return ApiResponse.ok(lessonMapper.selectList(
                new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getUnitId, id)
                        .orderByAsc(Lesson::getSortOrder)));
    }

    // ==================== 课时 ====================

    @GetMapping("/lessons/{id}")
    public ApiResponse<Lesson> getLesson(@PathVariable Long id) {
        Lesson l = lessonMapper.selectById(id);
        return l != null ? ApiResponse.ok(l) : ApiResponse.fail("课时不存在");
    }

    @GetMapping("/lessons/{id}/exercises")
    public ApiResponse<List<Exercise>> getLessonExercises(@PathVariable Long id) {
        List<Exercise> byLesson = exerciseMapper.selectList(
                new LambdaQueryWrapper<Exercise>().eq(Exercise::getLessonId, id));
        if (!byLesson.isEmpty()) return ApiResponse.ok(byLesson);
        return ApiResponse.ok(List.of());
    }

    @PostMapping("/lessons")
    @RequireRole({"teacher"})
    public ApiResponse<Lesson> createLesson(@RequestBody Lesson lesson) {
        lesson.setId(System.currentTimeMillis());
        lessonMapper.insert(lesson);
        return ApiResponse.ok(lesson);
    }

    @PutMapping("/lessons/{id}")
    @RequireRole({"teacher"})
    public ApiResponse<Lesson> updateLesson(@PathVariable Long id, @RequestBody Lesson lesson) {
        lesson.setId(id);
        lessonMapper.updateById(lesson);
        return ApiResponse.ok(lessonMapper.selectById(id));
    }

    // ==================== 个人导入视频 ====================

    /** GET /api/lessons/my-imports — 学生导入的视频列表（按BVID分组） */
    @GetMapping("/lessons/my-imports")
    public ApiResponse<List<Map<String, Object>>> getMyImports() {
        Long userId = LoginUserHolder.get().getUserId();
        List<Lesson> myLessons = lessonMapper.selectList(
                new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getUserId, userId)
                        .orderByDesc(Lesson::getId));

        // 按 videoUrl (BVID) 去重分组
        Map<String, List<Lesson>> grouped = new java.util.LinkedHashMap<>();
        for (Lesson l : myLessons) {
            String key = l.getVideoUrl() != null ? l.getVideoUrl() : "";
            if (!grouped.containsKey(key)) {
                grouped.put(key, new ArrayList<>());
            }
            grouped.get(key).add(l);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            List<Lesson> lessons = entry.getValue();
            Lesson first = lessons.get(0);

            // 获取单元和学科名
            String subjectName = "";
            String unitName = "";
            if (first.getUnitId() != null) {
                Unit unit = unitMapper.selectById(first.getUnitId());
                if (unit != null) {
                    unitName = unit.getName();
                    if (unit.getSubjectId() != null) {
                        Subject subject = subjectMapper.selectById(unit.getSubjectId());
                        if (subject != null) subjectName = subject.getName();
                    }
                }
            }

            // 提取 BVID
            String bvid = "";
            String url = first.getVideoUrl();
            if (url != null && url.contains("BV")) {
                bvid = url.substring(url.indexOf("BV"));
                if (bvid.length() > 12) bvid = bvid.substring(0, 12);
            }

            Map<String, Object> item = new HashMap<>();
            item.put("bvid", bvid);
            item.put("title", first.getName() != null ? first.getName() : "");
            item.put("subjectName", subjectName);
            item.put("unitName", unitName);
            item.put("lessonCount", lessons.size());
            item.put("firstLessonId", first.getId());
            item.put("coverUrl", first.getCoverUrl() != null ? first.getCoverUrl() : "");
            result.add(item);
        }
        return ApiResponse.ok(result);
    }

    // ==================== 进度 ====================

    private final LessonProgressMapper lessonProgressMapper;

    @GetMapping("/progress/lesson/{lessonId}")
    public ApiResponse<LessonProgress> getLessonProgress(@PathVariable Long lessonId) {
        Long userId = LoginUserHolder.get().getUserId();
        LessonProgress lp = lessonProgressMapper.selectOne(
                new LambdaQueryWrapper<LessonProgress>()
                        .eq(LessonProgress::getUserId, userId)
                        .eq(LessonProgress::getLessonId, lessonId));
        return ApiResponse.ok(lp != null ? lp : new LessonProgress());
    }

    @PostMapping("/progress/lesson/{lessonId}/complete")
    public ApiResponse<String> completeLesson(@PathVariable Long lessonId) {
        Long userId = LoginUserHolder.get().getUserId();
        LessonProgress lp = lessonProgressMapper.selectOne(
                new LambdaQueryWrapper<LessonProgress>()
                        .eq(LessonProgress::getUserId, userId)
                        .eq(LessonProgress::getLessonId, lessonId));
        if (lp == null) {
            lp = new LessonProgress();
            lp.setId(System.currentTimeMillis());
            lp.setUserId(userId);
            lp.setLessonId(lessonId);
            lp.setStatus("completed");
            lp.setCompletedAt(java.time.LocalDateTime.now());
            lessonProgressMapper.insert(lp);
        } else {
            lp.setStatus("completed");
            lp.setCompletedAt(java.time.LocalDateTime.now());
            lessonProgressMapper.updateById(lp);
        }
        return ApiResponse.ok("完成");
    }

    @GetMapping("/progress/course/{subjectId}")
    public ApiResponse<Map<String, Object>> getCourseProgress(@PathVariable Long subjectId) {
        Long userId = LoginUserHolder.get().getUserId();
        List<Lesson> allLessons = lessonMapper.selectList(
                new LambdaQueryWrapper<Lesson>()
                        .inSql(Lesson::getUnitId,
                                "SELECT id FROM sp_unit WHERE subject_id = " + subjectId));
        long total = allLessons.size();
        long completed = lessonProgressMapper.selectCount(
                new LambdaQueryWrapper<LessonProgress>()
                        .eq(LessonProgress::getUserId, userId)
                        .eq(LessonProgress::getStatus, "completed"));
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("completed", completed);
        result.put("percent", total > 0 ? (int) (completed * 100 / total) : 0);
        return ApiResponse.ok(result);
    }

    // ==================== AI 资源推荐 ====================

    private final WebSearchAgent webSearchAgent;

    @PostMapping("/lessons/{id}/recommend-resources")
    public ApiResponse<List<Map<String, Object>>> recommendResources(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) return ApiResponse.fail("课时不存在");

        String weakDimension = body != null ? body.getOrDefault("weakDimension", "综合") : "综合";
        String result = webSearchAgent.recommend(lesson.getName(), weakDimension,
                LoginUserHolder.get().getUserId(), id);

        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> resources = om.readValue(result, List.class);
            return ApiResponse.ok(resources);
        } catch (Exception e) {
            return ApiResponse.fail("推荐解析失败");
        }
    }
}
