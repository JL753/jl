package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /** 获取学科→单元→课时树 */
    @GetMapping("/tree")
    public ApiResponse<Object> getSubjectTree() {
        Long userId = null;
        try { userId = LoginUserHolder.get().getUserId(); } catch (Exception ignored) {}
        return ApiResponse.ok(courseService.getSubjectTree(userId));
    }

    /** 获取课时详情（含视频、内容、知识点、练习） */
    @GetMapping("/lesson/{lessonId}")
    public ApiResponse<Map<String, Object>> getLessonDetail(@PathVariable Long lessonId) {
        return ApiResponse.ok(courseService.getLessonDetail(lessonId));
    }

    /** 获取某单元下的课时列表 */
    @GetMapping("/unit/{unitId}/lessons")
    public ApiResponse<Object> getLessonsByUnit(@PathVariable Long unitId) {
        return ApiResponse.ok(courseService.getLessonsByUnit(unitId));
    }

    /** 获取某课时关联的知识点 */
    @GetMapping("/lesson/{lessonId}/knowledge-points")
    public ApiResponse<Object> getKnowledgePoints(@PathVariable Long lessonId) {
        return ApiResponse.ok(courseService.getKnowledgePointsByLesson(lessonId));
    }
}
