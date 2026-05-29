package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.domain.CourseSchedule;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.service.CourseScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class CourseScheduleController {

    private final CourseScheduleService courseScheduleService;

    @GetMapping("/my")
    public ApiResponse<List<CourseSchedule>> getMyCourses() {
        Long userId = LoginUserHolder.get().getUserId();
        List<CourseSchedule> schedules = courseScheduleService.getByUserId(userId);
        return ApiResponse.ok(schedules);
    }

    @GetMapping("/day/{dayOfWeek}")
    public ApiResponse<List<CourseSchedule>> getCoursesByDay(@PathVariable Integer dayOfWeek) {
        Long userId = LoginUserHolder.get().getUserId();
        List<CourseSchedule> schedules = courseScheduleService.getByUserIdAndDay(userId, dayOfWeek);
        return ApiResponse.ok(schedules);
    }

    @PostMapping("/add")
    public ApiResponse<CourseSchedule> addCourse(@RequestBody CourseSchedule schedule) {
        Long userId = LoginUserHolder.get().getUserId();
        schedule.setUserId(userId);
        CourseSchedule saved = courseScheduleService.save(schedule);
        return ApiResponse.ok(saved);
    }

    @PutMapping("/update")
    public ApiResponse<String> updateCourse(@RequestBody CourseSchedule schedule) {
        Long userId = LoginUserHolder.get().getUserId();
        schedule.setUserId(userId);
        courseScheduleService.update(schedule);
        return ApiResponse.ok("更新成功");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteCourse(@PathVariable Long id) {
        courseScheduleService.delete(id);
        return ApiResponse.ok("删除成功");
    }

    @DeleteMapping("/clear")
    public ApiResponse<String> clearAll() {
        Long userId = LoginUserHolder.get().getUserId();
        courseScheduleService.deleteByUserId(userId);
        return ApiResponse.ok("清空成功");
    }
}
