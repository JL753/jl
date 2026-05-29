package com.iflytek.smartprep.service;

import java.util.List;
import java.util.Map;

public interface CourseService {
    List<Map<String, Object>> getChapterTree(Long courseId);
    Map<String, Object> getSubChapterDetail(Long subChapterId);
}
