package com.iflytek.smartprep.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.config.RequireRole;
import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentMapper assignmentMapper;
    private final AssignmentSubmissionMapper submissionMapper;
    private final ClassMemberMapper memberMapper;

    @PostMapping
    @RequireRole({"teacher"})
    public ApiResponse<Assignment> createAssignment(@RequestBody Map<String, Object> body) {
        Assignment a = new Assignment();
        a.setId(System.currentTimeMillis());
        a.setClassId(Long.valueOf(body.get("classId").toString()));
        a.setTitle(body.get("title").toString());
        a.setDescription((String) body.getOrDefault("description", ""));
        a.setLessonIdsJson(body.get("lessonIds").toString());
        a.setCreatedAt(LocalDateTime.now());
        if (body.containsKey("dueAt")) {
            a.setDueAt(LocalDateTime.parse(body.get("dueAt").toString()));
        }
        assignmentMapper.insert(a);
        return ApiResponse.ok(a);
    }

    @GetMapping("/class/{classId}")
    public ApiResponse<List<Assignment>> getClassAssignments(@PathVariable Long classId) {
        return ApiResponse.ok(assignmentMapper.selectList(
                new LambdaQueryWrapper<Assignment>()
                        .eq(Assignment::getClassId, classId)
                        .orderByDesc(Assignment::getCreatedAt)));
    }

    @GetMapping("/my")
    public ApiResponse<List<Map<String, Object>>> myAssignments() {
        Long userId = LoginUserHolder.get().getUserId();
        List<ClassMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<ClassMember>().eq(ClassMember::getStudentId, userId));
        if (members.isEmpty()) return ApiResponse.ok(List.of());

        List<Long> classIds = members.stream().map(ClassMember::getClassId).toList();
        List<Assignment> assignments = assignmentMapper.selectList(
                new LambdaQueryWrapper<Assignment>().in(Assignment::getClassId, classIds));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Assignment a : assignments) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("classId", a.getClassId());
            m.put("dueAt", a.getDueAt());

            AssignmentSubmission sub = submissionMapper.selectOne(
                    new LambdaQueryWrapper<AssignmentSubmission>()
                            .eq(AssignmentSubmission::getAssignmentId, a.getId())
                            .eq(AssignmentSubmission::getStudentId, userId));
            m.put("status", sub != null ? sub.getStatus() : "pending");
            m.put("score", sub != null ? sub.getScore() : null);
            result.add(m);
        }
        return ApiResponse.ok(result);
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<AssignmentSubmission> submitAssignment(@PathVariable Long id) {
        Long userId = LoginUserHolder.get().getUserId();
        AssignmentSubmission sub = submissionMapper.selectOne(
                new LambdaQueryWrapper<AssignmentSubmission>()
                        .eq(AssignmentSubmission::getAssignmentId, id)
                        .eq(AssignmentSubmission::getStudentId, userId));
        if (sub == null) {
            sub = new AssignmentSubmission();
            sub.setId(System.currentTimeMillis());
            sub.setAssignmentId(id);
            sub.setStudentId(userId);
            sub.setStatus("submitted");
            sub.setSubmittedAt(LocalDateTime.now());
            submissionMapper.insert(sub);
        } else {
            sub.setStatus("submitted");
            sub.setSubmittedAt(LocalDateTime.now());
            submissionMapper.updateById(sub);
        }
        return ApiResponse.ok(sub);
    }

    @GetMapping("/{id}/submissions")
    @RequireRole({"teacher"})
    public ApiResponse<List<AssignmentSubmission>> getSubmissions(@PathVariable Long id) {
        return ApiResponse.ok(submissionMapper.selectList(
                new LambdaQueryWrapper<AssignmentSubmission>()
                        .eq(AssignmentSubmission::getAssignmentId, id)));
    }
}
