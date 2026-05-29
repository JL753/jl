package com.iflytek.smartprep.service;

import java.util.List;
import java.util.Map;

public interface KnowledgeGraphService {

    /**
     * 获取完整知识依赖图（所有知识点 + 依赖边）
     * @return {nodes: [{id, name, difficultyLevel, tags, lessonId}], edges: [{from, to, relationType}]}
     */
    Map<String, Object> getFullGraph();

    /**
     * 获取用户各知识点掌握度
     * @return [{knowledgePointId, name, mastery, practiceCount, difficultyLevel, tags}]
     */
    List<Map<String, Object>> getUserProgress(Long userId);

    /**
     * 推荐用户下一个应学习的知识点
     * 条件：所有前驱知识点已掌握（mastery >= 0.6），但自身未掌握
     */
    List<Map<String, Object>> getNextRecommended(Long userId);

    /**
     * 答题后更新用户对某知识点的掌握度
     * 使用 ELO 类算法更新 mastery
     */
    void updateMastery(Long userId, Long knowledgePointId, boolean correct);

    /**
     * 获取某知识点的前驱依赖链（追溯到根）
     */
    List<Map<String, Object>> getPrerequisiteChain(Long knowledgePointId);

    /**
     * 获取某知识点的所有练习题
     */
    List<Map<String, Object>> getExercisesByKnowledgePoint(Long knowledgePointId);
}
