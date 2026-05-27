package com.iflytek.smartprep.service;

import com.iflytek.smartprep.domain.LearningAssessment;
import com.iflytek.smartprep.dto.AssessmentRequest;

import java.util.List;
import java.util.Map;

public interface AssessmentService {
    LearningAssessment evaluate(Long userId, AssessmentRequest request);
    List<LearningAssessment> list(Long userId);
    Map<String, Object> latestOverview(Long userId);
}
