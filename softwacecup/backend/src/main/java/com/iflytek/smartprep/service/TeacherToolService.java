package com.iflytek.smartprep.service;

import com.iflytek.smartprep.dto.PaperCreateRequest;
import com.iflytek.smartprep.dto.QuestionItemRequest;
import com.iflytek.smartprep.dto.TeachingPptGenerateRequest;

import java.util.List;
import java.util.Map;

public interface TeacherToolService {
    List<Map<String, Object>> questionBank(Long userId);
    Map<String, Object> addQuestion(Long userId, QuestionItemRequest request);
    Map<String, Object> createPaper(Long userId, PaperCreateRequest request);
    List<Map<String, Object>> pptTemplates();
    Map<String, Object> generateTeachingPpt(Long userId, TeachingPptGenerateRequest request);
}
