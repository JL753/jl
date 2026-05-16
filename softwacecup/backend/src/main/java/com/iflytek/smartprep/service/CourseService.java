package com.iflytek.smartprep.service;

import java.util.List;
import java.util.Map;

public interface CourseService {

    /**
     * 获取学科树（学科 → 单元 → 课时）
     */
    List<Map<String, Object>> getSubjectTree();

    /**
     * 获取课时详情（包含视频地址、markdown内容、关联知识点、关联练习）
     */
    Map<String, Object> getLessonDetail(Long lessonId);

    /**
     * 获取单元下的所有课时列表
     */
    List<Map<String, Object>> getLessonsByUnit(Long unitId);

    /**
     * 获取课时关联的知识点列表
     */
    List<Map<String, Object>> getKnowledgePointsByLesson(Long lessonId);
}
