from pathlib import Path
p = Path(r'c:\Users\ZWC\Desktop\软件杯大赛\backend\src\main\java\com\iflytek\smartprep\service\impl\DashboardServiceImpl.java')
text = p.read_text(encoding='utf-8')
anchor = '''    @Override
    public List<Map<String, Object>> wrongQuestions(Long userId) {
        List<WrongQuestion> wrongs = wrongQuestionMapper.selectList(new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getUserId, userId).orderByDesc(WrongQuestion::getCreatedAt));
        Map<Long, ExamQuestion> questionMap = loadQuestionMap(wrongs.stream().map(WrongQuestion::getExamQuestionId).toList());
        Map<Long, Exam> examMap = loadExamMap(questionMap.values().stream().map(ExamQuestion::getExamId).toList());
        return wrongs.stream().map(w -> {
            ExamQuestion q = questionMap.get(w.getExamQuestionId());
            Exam e = q == null ? null : examMap.get(q.getExamId());
            return row("id", w.getId(), "examId", e == null ? null : e.getId(), "examName", e == null ? "未知考试" : e.getExamName(), "questionTitle", w.getQuestionTitle(), "myAnswer", w.getMyAnswer(), "correctAnswer", w.getCorrectAnswer(), "analysis", w.getAnalysis(), "createdAt", String.valueOf(w.getCreatedAt()));
        }).toList();
    }
'''
insert = '''

    @Override
    public List<Map<String, Object>> examScoreboard(Long examId) {
        List<ExamRecord> records = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .orderByDesc(ExamRecord::getSubmittedAt)
                .orderByDesc(ExamRecord::getScore));
        Map<Long, User> userMap = loadUserMap(records.stream().map(ExamRecord::getUserId).toList());
        Map<Long, Long> wrongCountMap = loadWrongCountMap(records.stream().map(ExamRecord::getId).toList());
        return records.stream().map(record -> {
            User student = userMap.get(record.getUserId());
            return row(
                    "recordId", record.getId(),
                    "studentId", record.getUserId(),
                    "studentName", displayName(student),
                    "username", student == null ? "-" : student.getUsername(),
                    "score", nz(record.getScore()),
                    "wrongCount", wrongCountMap.getOrDefault(record.getId(), 0L),
                    "submittedAt", String.valueOf(record.getSubmittedAt()),
                    "review", txt(record.getReview())
            );
        }).toList();
    }

    @Override
    public Map<String, Object> examScoreboardSummary(Long examId) {
        List<ExamRecord> records = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getExamId, examId));
        IntSummaryStatistics stats = records.stream().mapToInt(r -> nz(r.getScore())).summaryStatistics();
        long excellentCount = records.stream().filter(r -> nz(r.getScore()) >= 90).count();
        return row(
                "participantCount", records.size(),
                "averageScore", records.isEmpty() ? "0.0" : String.format("%.1f", stats.getAverage()),
                "highestScore", stats.getCount() == 0 ? 0 : stats.getMax(),
                "lowestScore", stats.getCount() == 0 ? 0 : stats.getMin(),
                "excellentCount", excellentCount,
                "excellentRate", records.isEmpty() ? "0.0%" : String.format("%.1f%%", excellentCount * 100.0 / records.size())
        );
    }
'''
if 'examScoreboard(Long examId)' not in text:
    text = text.replace(anchor, anchor + insert)
helpers_anchor = '    private long countUsers(String role) {'
helpers = '''    private Map<Long, User> loadUserMap(List<Long> ids) {
        List<Long> cleanIds = cleanIds(ids);
        if (cleanIds.isEmpty()) return Map.of();
        return userMapper.selectBatchIds(cleanIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    }

    private Map<Long, Long> loadWrongCountMap(List<Long> recordIds) {
        List<Long> cleanIds = cleanIds(recordIds);
        if (cleanIds.isEmpty()) return Map.of();
        List<WrongQuestion> wrongs = wrongQuestionMapper.selectList(new LambdaQueryWrapper<WrongQuestion>().in(WrongQuestion::getExamRecordId, cleanIds));
        return wrongs.stream().filter(w -> w.getExamRecordId() != null).collect(Collectors.groupingBy(WrongQuestion::getExamRecordId, Collectors.counting()));
    }

    private String displayName(User user) {
        if (user == null) return "未知学生";
        return user.getDisplayName() == null || user.getDisplayName().isBlank() ? txt(user.getUsername()) : user.getDisplayName();
    }

'''
if 'private Map<Long, User> loadUserMap' not in text:
    text = text.replace(helpers_anchor, helpers + helpers_anchor)
text = text.replace('    }\n}', '    }\n\n    private int nz(Object value) {\n        if (value instanceof Number number) return number.intValue();\n        if (value == null) return 0;\n        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception ignored) { return 0; }\n    }\n\n    private String txt(Object value) {\n        return value == null ? "" : String.valueOf(value);\n    }\n}')
p.write_text(text, encoding='utf-8')
