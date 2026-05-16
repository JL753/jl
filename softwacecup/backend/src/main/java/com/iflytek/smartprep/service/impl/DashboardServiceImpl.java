package com.iflytek.smartprep.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.dto.*;
import com.iflytek.smartprep.mapper.*;
import com.iflytek.smartprep.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
  private final UserMapper userMapper;
  private final KnowledgeDocMapper knowledgeDocMapper;
  private final ResourceService resourceService;
  private final ProfileService profileService;
  private final StudyPathService studyPathService;
  private final AssessmentService assessmentService;
  private final ExamMapper examMapper;
  private final ExamQuestionMapper examQuestionMapper;
  private final ExamRecordMapper examRecordMapper;
  private final WrongQuestionMapper wrongQuestionMapper;
  private final LearningActivityMapper learningActivityMapper;
  private final TaskCompletionMapper taskCompletionMapper;
  private final ObjectMapper objectMapper;
  private final StudentAbilityMapper studentAbilityMapper;

  @Override public DashboardStats teacherDashboard() { return stats("教师端总览", countUsers("teacher"), countUsers("student")); }
  @Override public DashboardStats studentDashboard(Long userId) {
    DashboardStats base = stats(txt(profileService.summary(userId).get("course")), examRecords(userId).size(), wrongQuestions(userId).size());
    // 从 sp_student_ability 加载六维能力数据
    StudentAbility ability = studentAbilityMapper.selectOne(
      new LambdaQueryWrapper<StudentAbility>().eq(StudentAbility::getUserId, userId).orderByDesc(StudentAbility::getEvaluatedAt).last("LIMIT 1")
    );
    if (ability != null) {
      List<Map<String,Object>> radar = List.of(
        row("name","知识广度","max",100,"value",nz(ability.getBreadthScore())),
        row("name","知识深度","max",100,"value",nz(ability.getDepthScore())),
        row("name","解题能力","max",100,"value",nz(ability.getProblemScore())),
        row("name","学习活跃度","max",100,"value",nz(ability.getActivityScore())),
        row("name","知识迁移","max",100,"value",nz(ability.getTransferScore())),
        row("name","学习韧性","max",100,"value",nz(ability.getResilienceScore()))
      );
      base.setRadar(radar);
    }
    return base;
  }
  @Override public Map<String,Object> teacherWorkspace() { return row("courseCards", List.of(row("title","智能备课助手","desc","支持 AI 问答与备课"), row("title","考试管理","desc","支持出题与批改")), "pendingTasks", List.of("教师账号数："+countUsers("teacher"), "学生账号数："+countUsers("student"), "考试数："+examMapper.selectCount(new LambdaQueryWrapper<>()))); }
  @Override public Map<String,Object> studentWorkspace(Long userId) { return row("courseSchedule", List.of(row("time","08:00-08:45","name",txt(profileService.summary(userId).get("course"))), row("time","13:55-14:40","name","在线考试训练")), "assistantTabs", List.of("智能问答","资源推荐"), "examInfo", List.of(row("title","阶段测试","count",examList(userId,"student").size()), row("title","考试记录","count",examRecords(userId).size()))); }
  @Override public Map<String,Object> datacenterScreen() { return row("schoolStats", List.of(row("label","学生人数","value",countUsers("student")+"名"), row("label","教师人数","value",countUsers("teacher")+"名"), row("label","课程文档","value",countDocs()+"份"), row("label","平台状态","value","运行中"))); }

  @Override public List<Map<String,Object>> examList(Long userId, String role) {
    List<Exam> list = examMapper.selectList(new LambdaQueryWrapper<Exam>().orderByDesc(Exam::getCreatedAt));
    return list.stream().map(this::examMap).toList();
  }

  @Override public Map<String,Object> examDetail(Long userId, Long examId, String role) {
    Exam exam = examMapper.selectById(examId);
    if (exam == null) return row("exam", Map.of(), "questions", List.of(), "rules", List.of("考试开始后系统将自动计时"));
    List<Map<String,Object>> qs = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, examId).orderByAsc(ExamQuestion::getQuestionNo)).stream().map(this::questionMap).toList();
    return row("exam", examMap(exam), "questions", qs, "rules", List.of("考试开始后系统将自动计时", "提交后自动评分"));
  }

  @Override public Map<String,Object> publishExam(Long userId, ExamPublishRequest request) {
    Exam exam = new Exam();
    exam.setCreatorUserId(userId); exam.setExamName(request.getExamName()); exam.setCourse(request.getCourse()); exam.setDuration(request.getDuration());
    exam.setTopic(request.getTopic()); exam.setStatus("已发布"); exam.setQuestionCount(0); exam.setCreatedAt(LocalDateTime.now());
    examMapper.insert(exam);
    return row("examId", exam.getId(), "message", "考试发布成功", "questionCount", 0);
  }

  @Override public Map<String,Object> submitExam(Long userId, ExamSubmitRequest request) {
    Map<String,Object> payload = new LinkedHashMap<>();
    Map<String,Object> answers = new LinkedHashMap<>();
    if (request.getAnswers() != null) {
      for (ExamSubmitRequest.AnswerItem item : request.getAnswers()) {
        answers.put(String.valueOf(item.getQuestionId()), item.getAnswer());
      }
    }
    payload.put("answers", answers);
    payload.put("annotations", List.of());
    ExamRecord r = new ExamRecord();
    r.setExamId(request.getExamId());
    r.setUserId(userId);
    r.setScore(0);
    r.setReview("等待教师批改");
    r.setAnswersJson(toJson(payload));
    r.setSubmittedAt(LocalDateTime.now());
    examRecordMapper.insert(r);
    return row("examId", request.getExamId(), "recordId", r.getId(), "score", 0, "aiReview", "已提交，等待教师逐题批改", "submitTime", LocalDate.now().toString());
  }

  @Override public List<Map<String,Object>> examRecords(Long userId) {
    return examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getUserId, userId).orderByDesc(ExamRecord::getSubmittedAt))
      .stream().map(r -> row("recordId", r.getId(), "examId", r.getExamId(), "score", nz(r.getScore()), "review", txt(r.getReview()), "submittedAt", String.valueOf(r.getSubmittedAt()))).toList();
  }

  @Override public List<Map<String,Object>> wrongQuestions(Long userId) {
    return wrongQuestionMapper.selectList(new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getUserId, userId).orderByDesc(WrongQuestion::getCreatedAt))
      .stream().map(w -> row("id", w.getId(), "questionTitle", txt(w.getQuestionTitle()), "myAnswer", txt(w.getMyAnswer()), "correctAnswer", txt(w.getCorrectAnswer()), "analysis", txt(w.getAnalysis()))).toList();
  }

  @Override public List<Map<String,Object>> examScoreboard(Long examId) {
    return examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getExamId, examId).orderByDesc(ExamRecord::getSubmittedAt))
      .stream().map(r -> {
        User u = userMapper.selectById(r.getUserId());
        return row("recordId", r.getId(), "studentName", u == null ? "-" : txt(u.getDisplayName()), "username", u == null ? "-" : txt(u.getUsername()), "score", nz(r.getScore()), "wrongCount", 0, "submittedAt", String.valueOf(r.getSubmittedAt()), "review", txt(r.getReview()));
      }).toList();
  }

  @Override public Map<String,Object> examScoreboardSummary(Long examId) {
    List<Map<String,Object>> board = examScoreboard(examId);
    IntSummaryStatistics s = board.stream().mapToInt(x -> Integer.parseInt(String.valueOf(x.get("score")))).summaryStatistics();
    long excellent = board.stream().filter(x -> Integer.parseInt(String.valueOf(x.get("score"))) >= 90).count();
    return row("participantCount", board.size(), "averageScore", board.isEmpty()?0:Math.round(s.getAverage()*10.0)/10.0, "highestScore", board.isEmpty()?0:s.getMax(), "lowestScore", board.isEmpty()?0:s.getMin(), "excellentCount", excellent);
  }

  @Override public Map<String,Object> examScoreDetail(Long recordId) {
    ExamRecord r = examRecordMapper.selectById(recordId);
    if (r == null) return row("record", Map.of(), "answers", List.of());
    Exam exam = examMapper.selectById(r.getExamId());
    User student = userMapper.selectById(r.getUserId());
    Map<String,Object> payload = parsePayload(r.getAnswersJson());
    Map<String,Object> answers = mapValue(payload.get("answers"));
    Map<String, Map<String, Object>> annotations = listValue(payload.get("annotations")).stream()
      .collect(Collectors.toMap(x -> String.valueOf(x.get("questionId")), x -> x, (a, b) -> b, LinkedHashMap::new));
    List<Map<String,Object>> details = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, r.getExamId()).orderByAsc(ExamQuestion::getQuestionNo))
      .stream().map(q -> {
        Map<String,Object> annotation = annotations.getOrDefault(String.valueOf(q.getId()), Map.of());
        String answer = txt(answers.get(String.valueOf(q.getId())));
        return row(
          "questionId", q.getId(),
          "questionNo", q.getQuestionNo(),
          "title", txt(q.getTitle()),
          "type", txt(q.getQuestionType()),
          "fullScore", nz(q.getScore()),
          "answer", "-".equals(answer) ? "" : answer,
          "correctAnswer", txt(q.getAnswerKey()),
          "comment", txt(annotation.get("comment")),
          "teacherScore", annotation.get("score") == null ? nz(q.getScore()) : annotation.get("score"),
          "isCorrect", Objects.equals(txt(q.getAnswerKey()), answer)
        );
      }).toList();
    return row(
      "record", row("recordId", r.getId(), "score", nz(r.getScore()), "review", txt(r.getReview()), "submittedAt", String.valueOf(r.getSubmittedAt())),
      "exam", exam == null ? Map.of() : examMap(exam),
      "student", row("studentName", student == null ? "-" : txt(student.getDisplayName()), "username", student == null ? "-" : txt(student.getUsername())),
      "answers", details
    );
  }

  @Override public Map<String,Object> wrongQuestionDetail(Long wrongQuestionId) {
    WrongQuestion w = wrongQuestionMapper.selectById(wrongQuestionId);
    if (w == null) return Map.of();
    return row("id", w.getId(), "questionTitle", txt(w.getQuestionTitle()), "myAnswer", txt(w.getMyAnswer()), "correctAnswer", txt(w.getCorrectAnswer()), "analysis", txt(w.getAnalysis()));
  }

  @Override public List<Map<String,Object>> examDimensionStats() { return List.of(row("name","知识理解","value",84), row("name","概念记忆","value",79), row("name","迁移应用","value",73)); }
  @Override public Map<String,Object> studentDataCenter(Long userId) { return row("profile", profileService.summary(userId), "resources", resourceService.latestOverview(userId), "assessment", assessmentService.latestOverview(userId), "records", examRecords(userId), "wrongs", wrongQuestions(userId)); }

  @Override public Map<String,Object> profileCard(Long userId, String role) {
    User user = userMapper.selectById(userId);
    Map<String,Object> p = profileService.summary(userId);
    Map<String,Object> r = resourceService.latestOverview(userId);
    Map<String,Object> a = assessmentService.latestOverview(userId);
    List<Map<String,Object>> records = examRecords(userId);
    return row(
      "userId", userId,
      "role", role,
      "displayName", user == null ? "未命名用户" : txt(user.getDisplayName()),
      "username", user == null ? "-" : txt(user.getUsername()),
      "avatarUrl", user == null ? "" : txt(user.getAvatarUrl()),
      "course", txt(p.get("course")),
      "major", txt(p.get("major")),
      "knowledgeBase", txt(p.get("knowledgeBase")),
      "cognitiveStyle", txt(p.get("cognitiveStyle")),
      "interestPreference", txt(p.get("interestPreference")),
      "pacePreference", txt(p.get("pacePreference")),
      "weakPoints", txt(p.get("weakPoints")),
      "resourceCount", num(r.get("total")),
      "examCount", records.size(),
      "latestScore", records.isEmpty() ? "暂无" : txt(records.get(0).get("score")),
      "accuracy", txt(a.get("accuracy")),
      "summary", List.of(
        row("label", "当前角色", "value", roleLabel(role)),
        row("label", "学习/教学方向", "value", txt(p.get("course"))),
        row("label", "知识基础", "value", txt(p.get("knowledgeBase"))),
        row("label", "资源总数", "value", num(r.get("total")) + "份"),
        row("label", "考试记录", "value", records.size() + "次")
      )
    );
  }

  @Override public Map<String,Object> gradeExam(Long teacherUserId, GradeExamRequest request) {
    ExamRecord r = examRecordMapper.selectById(request.getRecordId());
    if (r == null) throw new IllegalArgumentException("成绩记录不存在");
    Map<String,Object> payload = parsePayload(r.getAnswersJson());
    payload.put("annotations", request.getQuestionAnnotations() == null ? List.of() : request.getQuestionAnnotations().stream().map(x -> row("questionId", x.getQuestionId(), "score", x.getScore(), "comment", x.getComment())).toList());
    r.setAnswersJson(toJson(payload));
    if (request.getScore() != null) r.setScore(request.getScore());
    String finalReview = request.getOverallReview() != null && !request.getOverallReview().isBlank() ? request.getOverallReview() : request.getReview();
    if (finalReview != null && !finalReview.isBlank()) r.setReview(finalReview);
    examRecordMapper.updateById(r);
    return row("recordId", r.getId(), "score", nz(r.getScore()), "review", txt(r.getReview()), "message", "批改完成");
  }

  private DashboardStats stats(String title, Object a, Object b) {
    return DashboardStats.builder().summary(row("title", title, "a", a, "b", b)).bar(List.of()).line(List.of()).radar(List.of()).pie(List.of()).cards(List.of()).table(List.of()).notices(List.of()).build();
  }
  private long countUsers(String role) { return userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, role)); }
  private long countDocs() { return knowledgeDocMapper.selectCount(new LambdaQueryWrapper<KnowledgeDoc>()); }
  private int num(Object v) { try { return Integer.parseInt(String.valueOf(v == null ? 0 : v)); } catch (Exception e) { return 0; } }
  private int nz(Integer v) { return v == null ? 0 : v; }
  private String txt(Object v) { return v == null ? "-" : String.valueOf(v); }
  private Map<String,Object> row(Object... kv) { Map<String,Object> m = new LinkedHashMap<>(); for (int i = 0; i < kv.length; i += 2) m.put(String.valueOf(kv[i]), kv[i+1]); return m; }
  private Map<String,Object> examMap(Exam e) { return row("examId", e.getId(), "examName", txt(e.getExamName()), "course", txt(e.getCourse()), "duration", nz(e.getDuration()), "status", txt(e.getStatus()), "questionCount", nz(e.getQuestionCount()), "topic", txt(e.getTopic()), "createdAt", String.valueOf(e.getCreatedAt())); }
  private Map<String,Object> questionMap(ExamQuestion q) { return row("questionId", q.getId(), "questionNo", q.getQuestionNo(), "title", txt(q.getTitle()), "type", txt(q.getQuestionType()), "score", nz(q.getScore())); }
  private String roleLabel(String role) { return switch (txt(role)) { case "teacher" -> "教师端"; case "admin" -> "管理员端"; default -> "学生端"; }; }
  @SuppressWarnings("unchecked") private Map<String,Object> mapValue(Object value) { return value instanceof Map<?, ?> map ? (Map<String, Object>) map : new LinkedHashMap<>(); }
  @SuppressWarnings("unchecked") private List<Map<String,Object>> listValue(Object value) { return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of(); }
  private Map<String,Object> parsePayload(String json) {
    try {
      Map<String,Object> raw = objectMapper.readValue(json == null || json.isBlank() ? "{}" : json, Map.class);
      if (raw.containsKey("answers")) return raw;
      return row("answers", raw, "annotations", List.of());
    } catch (Exception e) {
      return row("answers", Map.of(), "annotations", List.of());
    }
  }
  private String toJson(Object value) {
    try { return objectMapper.writeValueAsString(value); }
    catch (Exception e) { return "{}"; }
  }

  @Override
  public List<Map<String, Object>> learningActivityTrend(Long userId) {
    LocalDate endDate = LocalDate.now();
    LocalDate startDate = endDate.minusDays(6);
    List<LearningActivity> activities = learningActivityMapper.selectList(
      new LambdaQueryWrapper<LearningActivity>()
        .eq(LearningActivity::getUserId, userId)
        .between(LearningActivity::getActivityDate, startDate, endDate)
        .orderByAsc(LearningActivity::getActivityDate)
    );
    Map<LocalDate, Integer> activityMap = activities.stream()
      .collect(Collectors.toMap(LearningActivity::getActivityDate, LearningActivity::getActivityScore));
    List<Map<String, Object>> result = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      LocalDate date = startDate.plusDays(i);
      result.add(row("date", date.toString(), "score", activityMap.getOrDefault(date, 0)));
    }
    return result;
  }

  @Override
  public List<Map<String, Object>> weeklyTaskCompletion(Long userId) {
    LocalDate today = LocalDate.now();
    LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
    return taskCompletionMapper.selectList(
      new LambdaQueryWrapper<TaskCompletion>()
        .eq(TaskCompletion::getUserId, userId)
        .eq(TaskCompletion::getWeekStartDate, weekStart)
        .orderByAsc(TaskCompletion::getTaskType)
    ).stream().map(t -> row(
      "taskType", txt(t.getTaskType()),
      "completionRate", nz(t.getCompletionRate()),
      "totalCount", nz(t.getTotalCount()),
      "completedCount", nz(t.getCompletedCount())
    )).toList();
  }
}
