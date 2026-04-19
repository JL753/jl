package com.iflytek.smartprep.service;

import com.iflytek.smartprep.domain.StudyPath;
import com.iflytek.smartprep.dto.StudyPathRequest;

import java.util.List;
import java.util.Map;

public interface StudyPathService {
    StudyPath generate(Long userId, StudyPathRequest request);
    List<StudyPath> list(Long userId);
    Map<String, Object> latestOverview(Long userId);
}
