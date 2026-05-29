package com.iflytek.smartprep.service;

import com.iflytek.smartprep.dto.DashboardStats;
import com.iflytek.smartprep.dto.ExamPublishRequest;
import com.iflytek.smartprep.dto.ExamSubmitRequest;
import com.iflytek.smartprep.dto.GradeExamRequest;

import java.util.List;
import java.util.Map;

public interface DashboardService {
    DashboardStats teacherDashboard();
    DashboardStats studentDashboard(Long userId);
    Map<String, Object> teacherWorkspace();
    Map<String, Object> studentWorkspace(Long userId);
    Map<String, Object> datacenterScreen();
    List<Map<String, Object>> examList(Long userId, String role);
    Map<String, Object> examDetail(Long userId, Long examId, String role);
    Map<String, Object> publishExam(Long userId, ExamPublishRequest request);
    Map<String, Object> submitExam(Long userId, ExamSubmitRequest request);
    List<Map<String, Object>> examRecords(Long userId);
    List<Map<String, Object>> wrongQuestions(Long userId);
    List<Map<String, Object>> examScoreboard(Long examId);
    Map<String, Object> examScoreboardSummary(Long examId);
    Map<String, Object> examScoreDetail(Long recordId);
    Map<String, Object> wrongQuestionDetail(Long wrongQuestionId);
    List<Map<String, Object>> examDimensionStats();
    Map<String, Object> studentDataCenter(Long userId);
    Map<String, Object> profileCard(Long userId, String role);
    Map<String, Object> gradeExam(Long teacherUserId, GradeExamRequest request);
    List<Map<String, Object>> learningActivityTrend(Long userId);
    List<Map<String, Object>> weeklyTaskCompletion(Long userId);
}
