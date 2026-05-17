package com.iflytek.smartprep.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.config.RequireRole;
import com.iflytek.smartprep.domain.Course;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.mapper.CourseMapper;
import com.iflytek.smartprep.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseController {

    private final CourseMapper courseMapper;
    private final ChapterService chapterService;

    @GetMapping("/courses/public")
    public ApiResponse<List<Course>> listPublicCourses() {
        return ApiResponse.ok(courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getStatus, "已发布")
                        .or().eq(Course::getStatus, "published")
                        .orderByDesc(Course::getCreatedAt)));
    }

    @GetMapping("/subjects/{id}/courses")
    public ApiResponse<List<Course>> listCoursesBySubject(@PathVariable Long id) {
        return ApiResponse.ok(courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getSubjectId, id)
                        .and(w -> w.eq(Course::getStatus, "已发布").or().eq(Course::getStatus, "published"))
                        .orderByDesc(Course::getCreatedAt)));
    }

    @GetMapping("/courses/{id}")
    public ApiResponse<Course> getCourse(@PathVariable Long id) {
        Course c = courseMapper.selectById(id);
        return c != null ? ApiResponse.ok(c) : ApiResponse.fail("课程不存在");
    }

    @PostMapping("/courses")
    @RequireRole({"teacher"})
    public ApiResponse<Course> createCourse(@RequestBody Course course) {
        course.setId(System.currentTimeMillis());
        course.setCreatedAt(LocalDateTime.now());
        course.setUpdatedAt(LocalDateTime.now());
        courseMapper.insert(course);
        return ApiResponse.ok(course);
    }

    @PutMapping("/courses/{id}")
    @RequireRole({"teacher"})
    public ApiResponse<Course> updateCourse(@PathVariable Long id, @RequestBody Course course) {
        course.setId(id);
        course.setUpdatedAt(LocalDateTime.now());
        courseMapper.updateById(course);
        return ApiResponse.ok(courseMapper.selectById(id));
    }

    @DeleteMapping("/courses/{id}")
    @RequireRole({"teacher"})
    public ApiResponse<String> deleteCourse(@PathVariable Long id) {
        courseMapper.deleteById(id);
        return ApiResponse.ok("ok");
    }

    /** Backward-compatible course tree — returns all published courses with their chapter trees */
    @GetMapping("/course/tree")
    public ApiResponse<List<Map<String, Object>>> getCourseTree(@RequestParam(required = false) Long courseId) {
        if (courseId != null) {
            Course c = courseMapper.selectById(courseId);
            if (c == null) return ApiResponse.fail("课程不存在");
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", c.getId());
            node.put("name", c.getTitle());
            node.put("chapters", chapterService.getChapterTree(courseId));
            return ApiResponse.ok(List.of(node));
        }
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getStatus, "已发布")
                        .or().eq(Course::getStatus, "published"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Course c : courses) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", c.getId());
            node.put("name", c.getTitle());
            node.put("chapters", chapterService.getChapterTree(c.getId()));
            result.add(node);
        }
        return ApiResponse.ok(result);
    }
}
