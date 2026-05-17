package com.iflytek.smartprep.service;

import com.iflytek.smartprep.domain.CourseAnswer;
import com.iflytek.smartprep.domain.CourseQuestion;
import java.util.List;
import java.util.Map;

public interface CourseQaService {
    List<Map<String, Object>> getQuestions(Long courseId, int page, int pageSize);
    CourseQuestion askQuestion(Long courseId, Long userId, String title, String content);
    List<Map<String, Object>> getAnswers(Long questionId);
    CourseAnswer postAnswer(Long questionId, Long userId, String content);
    CourseAnswer postAiAnswer(Long questionId, String content);
}
