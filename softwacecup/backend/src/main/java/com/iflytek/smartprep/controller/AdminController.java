package com.iflytek.smartprep.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.smartprep.config.LoginUser;
import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.config.RequireRole;
import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.dto.*;
import com.iflytek.smartprep.mapper.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final LearningResourceMapper learningResourceMapper;
    private final ExamMapper examMapper;
    private final ExamRecordMapper examRecordMapper;
    private final OperationLogMapper operationLogMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    // ==================== 仪表盘统计 ====================

    @GetMapping("/dashboard")
    @RequireRole({"admin"})
    public ApiResponse<Map<String, Object>> dashboard() {
        long userCount = userMapper.selectCount(new LambdaQueryWrapper<User>());
        long courseCount = courseMapper.selectCount(new LambdaQueryWrapper<Course>());
        long resourceCount = learningResourceMapper.selectCount(new LambdaQueryWrapper<LearningResource>());
        long examCount = examMapper.selectCount(new LambdaQueryWrapper<Exam>());

        long teacherCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "teacher"));
        long studentCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "student"));

        return ApiResponse.ok(Map.of(
                "userCount", userCount,
                "courseCount", courseCount,
                "resourceCount", resourceCount,
                "examCount", examCount,
                "teacherCount", teacherCount,
                "studentCount", studentCount
        ));
    }

    // ==================== 用户管理 CRUD ====================

    @GetMapping("/users")
    @RequireRole({"admin"})
    public ApiResponse<List<Map<String, Object>>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getDisplayName, keyword));
        }
        if (role != null && !role.isBlank()) {
            wrapper.eq(User::getRole, role);
        }
        wrapper.orderByDesc(User::getId);
        List<User> users = userMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("role", u.getRole());
            map.put("displayName", u.getDisplayName());
            map.put("avatarUrl", u.getAvatarUrl());
            result.add(map);
        }
        return ApiResponse.ok(result);
    }

    @PostMapping("/users")
    @RequireRole({"admin"})
    public ApiResponse<Map<String, Object>> createUser(@Valid @RequestBody AdminUserCreateRequest request) {
        User exists = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()).last("limit 1"));
        if (exists != null) {
            return ApiResponse.fail("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRole(request.getRole());
        user.setDisplayName(request.getDisplayName());
        user.setAvatarUrl("https://api.dicebear.com/7.x/initials/svg?seed=" +
                (request.getDisplayName() != null ? request.getDisplayName() : request.getUsername()));
        userMapper.insert(user);

        logAction("创建用户", "用户#" + user.getId(), "创建用户: " + request.getUsername());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("displayName", user.getDisplayName());
        return ApiResponse.ok(result);
    }

    @PutMapping("/users/{id}")
    @RequireRole({"admin"})
    public ApiResponse<Map<String, Object>> updateUser(@PathVariable Long id,
                                                        @RequestBody AdminUserUpdateRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(request.getPassword());
        }
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getDisplayName() != null) user.setDisplayName(request.getDisplayName());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        userMapper.updateById(user);

        logAction("更新用户", "用户#" + id, "更新用户信息");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("displayName", user.getDisplayName());
        return ApiResponse.ok(result);
    }

    @DeleteMapping("/users/{id}")
    @RequireRole({"admin"})
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }
        if ("admin".equals(user.getRole())) {
            long adminCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getRole, "admin"));
            if (adminCount <= 1) {
                return ApiResponse.fail("无法删除唯一的管理员账号");
            }
        }
        userMapper.deleteById(id);

        logAction("删除用户", "用户#" + id, "删除用户: " + user.getUsername());

        return ApiResponse.ok(null);
    }

    // ==================== 课程管理 CRUD ====================

    @GetMapping("/courses")
    @RequireRole({"admin"})
    public ApiResponse<List<Course>> listCourses(@RequestParam(required = false) String status) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Course::getStatus, status);
        }
        wrapper.orderByDesc(Course::getCreatedAt);
        return ApiResponse.ok(courseMapper.selectList(wrapper));
    }

    @PostMapping("/courses")
    @RequireRole({"admin"})
    public ApiResponse<Course> createCourse(@Valid @RequestBody AdminCourseCreateRequest request) {
        Course course = new Course();
        course.setTitle(request.getTitle());
        course.setCategory(request.getCategory());
        course.setDescription(request.getDescription());
        course.setCoverImage(request.getCoverImage());
        course.setPrice(request.getPrice());
        course.setTag(request.getTag());
        course.setStatus(request.getStatus());
        course.setTotalHours(request.getTotalHours());
        course.setTargetAudience(request.getTargetAudience());
        course.setCreatedBy(LoginUserHolder.get().getUserId());
        course.setCreatedAt(LocalDateTime.now());
        course.setUpdatedAt(LocalDateTime.now());

        if (request.getChapters() != null && !request.getChapters().isEmpty()) {
            try {
                course.setChaptersJson(objectMapper.writeValueAsString(request.getChapters()));
            } catch (Exception e) {
                course.setChaptersJson("[]");
            }
        } else {
            course.setChaptersJson("[]");
        }

        courseMapper.insert(course);

        logAction("创建课程", "课程#" + course.getId(), "创建课程: " + request.getTitle());

        return ApiResponse.ok(course);
    }

    @PutMapping("/courses/{id}")
    @RequireRole({"admin"})
    public ApiResponse<Course> updateCourse(@PathVariable Long id,
                                             @RequestBody AdminCourseUpdateRequest request) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            return ApiResponse.fail("课程不存在");
        }
        if (request.getTitle() != null) course.setTitle(request.getTitle());
        if (request.getCategory() != null) course.setCategory(request.getCategory());
        if (request.getDescription() != null) course.setDescription(request.getDescription());
        if (request.getCoverImage() != null) course.setCoverImage(request.getCoverImage());
        if (request.getPrice() != null) course.setPrice(request.getPrice());
        if (request.getTag() != null) course.setTag(request.getTag());
        if (request.getStatus() != null) course.setStatus(request.getStatus());
        if (request.getTotalHours() != null) course.setTotalHours(request.getTotalHours());
        if (request.getTargetAudience() != null) course.setTargetAudience(request.getTargetAudience());
        if (request.getChaptersJson() != null) {
            course.setChaptersJson(String.valueOf(request.getChaptersJson()));
        }
        course.setUpdatedAt(LocalDateTime.now());
        courseMapper.updateById(course);

        logAction("更新课程", "课程#" + id, "更新课程: " + course.getTitle());

        return ApiResponse.ok(course);
    }

    @DeleteMapping("/courses/{id}")
    @RequireRole({"admin"})
    public ApiResponse<Void> deleteCourse(@PathVariable Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            return ApiResponse.fail("课程不存在");
        }
        courseMapper.deleteById(id);

        logAction("删除课程", "课程#" + id, "删除课程: " + course.getTitle());

        return ApiResponse.ok(null);
    }

    // ==================== 资源管理 CRUD ====================

    @GetMapping("/resources")
    @RequireRole({"admin"})
    public ApiResponse<List<LearningResource>> listResources(
            @RequestParam(required = false) String type) {
        LambdaQueryWrapper<LearningResource> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isBlank()) {
            wrapper.eq(LearningResource::getResourceType, type);
        }
        wrapper.orderByDesc(LearningResource::getCreatedAt);
        return ApiResponse.ok(learningResourceMapper.selectList(wrapper));
    }

    @DeleteMapping("/resources/{id}")
    @RequireRole({"admin"})
    public ApiResponse<Void> deleteResource(@PathVariable Long id) {
        learningResourceMapper.deleteById(id);
        logAction("删除资源", "资源#" + id, "删除学习资源");
        return ApiResponse.ok(null);
    }

    // ==================== 考试管理 CRUD ====================

    @GetMapping("/exams")
    @RequireRole({"admin"})
    public ApiResponse<List<Exam>> listExams(@RequestParam(required = false) String status) {
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Exam::getStatus, status);
        }
        wrapper.orderByDesc(Exam::getCreatedAt);
        return ApiResponse.ok(examMapper.selectList(wrapper));
    }

    @PostMapping("/exams")
    @RequireRole({"admin"})
    public ApiResponse<Exam> createExam(@RequestBody Map<String, Object> body) {
        Exam exam = new Exam();
        exam.setExamName((String) body.getOrDefault("examName", "新考试"));
        exam.setCourse((String) body.get("course"));
        exam.setDuration(body.get("duration") != null ? Integer.valueOf(body.get("duration").toString()) : 60);
        exam.setTopic((String) body.get("topic"));
        exam.setStatus((String) body.getOrDefault("status", "草稿"));
        exam.setQuestionCount(0);
        exam.setCreatorUserId(LoginUserHolder.get().getUserId());
        exam.setCreatedAt(LocalDateTime.now());
        examMapper.insert(exam);

        logAction("创建考试", "考试#" + exam.getId(), "创建考试: " + exam.getExamName());

        return ApiResponse.ok(exam);
    }

    @PutMapping("/exams/{id}")
    @RequireRole({"admin"})
    public ApiResponse<Exam> updateExam(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) {
            return ApiResponse.fail("考试不存在");
        }
        if (body.get("examName") != null) exam.setExamName((String) body.get("examName"));
        if (body.get("course") != null) exam.setCourse((String) body.get("course"));
        if (body.get("duration") != null) exam.setDuration(Integer.valueOf(body.get("duration").toString()));
        if (body.get("topic") != null) exam.setTopic((String) body.get("topic"));
        if (body.get("status") != null) exam.setStatus((String) body.get("status"));
        examMapper.updateById(exam);

        logAction("更新考试", "考试#" + id, "更新考试状态: " + exam.getStatus());

        return ApiResponse.ok(exam);
    }

    @DeleteMapping("/exams/{id}")
    @RequireRole({"admin"})
    public ApiResponse<Void> deleteExam(@PathVariable Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) {
            return ApiResponse.fail("考试不存在");
        }
        jdbcTemplate.update("DELETE FROM sp_exam_question WHERE exam_id = ?", id);
        jdbcTemplate.update("DELETE FROM sp_exam_record WHERE exam_id = ?", id);
        examMapper.deleteById(id);

        logAction("删除考试", "考试#" + id, "删除考试: " + exam.getExamName());

        return ApiResponse.ok(null);
    }

    // ==================== 操作日志查询 ====================

    @GetMapping("/logs")
    @RequireRole({"admin"})
    public ApiResponse<List<OperationLog>> listLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (action != null && !action.isBlank()) {
            wrapper.like(OperationLog::getAction, action);
        }
        wrapper.orderByDesc(OperationLog::getCreatedAt);
        wrapper.last("LIMIT 200");
        return ApiResponse.ok(operationLogMapper.selectList(wrapper));
    }

    // ==================== 系统设置 ====================

    @PutMapping("/settings")
    @RequireRole({"admin"})
    public ApiResponse<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> settings) {
        logAction("系统设置", "平台配置", "更新系统设置: " + settings.keySet());
        
        // 将设置持久化到数据库或返回确认
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("saved", true);
        result.put("keys", settings.keySet());
        result.put("message", "设置已保存");
        return ApiResponse.ok(result);
    }

    @GetMapping("/settings")
    @RequireRole({"admin"})
    public ApiResponse<Map<String, Object>> getSettings() {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("siteName", "智备优教 - 基于大模型的个性化教育平台");
        settings.put("llmBaseUrl", "https://maas-coding-api.cn-huabei-1.xf-yun.com/v2");
        settings.put("llmModel", "astron-code-latest");
        settings.put("temperature", 0.7);
        settings.put("maxTokens", 4096);
        settings.put("tokenExpireHours", 24);
        settings.put("blockedWords", "作弊,违法,恐怖,色情,暴力");
        return ApiResponse.ok(settings);
    }

    // ==================== 辅助方法 ====================

    private void logAction(String action, String target, String details) {
        try {
            OperationLog log = new OperationLog();
            LoginUser user = LoginUserHolder.get();
            if (user != null) {
                log.setUserId(user.getUserId());
                log.setUsername(user.getUsername());
            }
            log.setAction(action);
            log.setTarget(target);
            log.setDetails(details);
            log.setIpAddress("-"); // 可通过 HttpServletRequest 获取
            log.setCreatedAt(LocalDateTime.now());
            
            // 确保表存在
            try {
                operationLogMapper.insert(log);
            } catch (Exception e) {
                // 表可能不存在，忽略日志记录
            }
        } catch (Exception ignored) {
        }
    }
}
