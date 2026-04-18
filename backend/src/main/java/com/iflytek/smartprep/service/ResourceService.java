package com.iflytek.smartprep.service;

import com.iflytek.smartprep.domain.LearningResource;
import com.iflytek.smartprep.dto.ResourceGenerateRequest;

import java.util.List;
import java.util.Map;

public interface ResourceService {
    List<LearningResource> generate(Long userId, ResourceGenerateRequest request);
    List<LearningResource> listByUser(Long userId);
    List<Map<String, Object>> teacherLibrary();
    Map<String, Object> progress(Long userId);
    Map<String, Object> recommendation(Long userId, String prompt);
    Map<String, Object> latestOverview(Long userId);
}
