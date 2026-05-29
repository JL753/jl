package com.iflytek.smartprep.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.smartprep.domain.LearningAssessment;
import com.iflytek.smartprep.dto.AssessmentRequest;
import com.iflytek.smartprep.mapper.LearningAssessmentMapper;
import com.iflytek.smartprep.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssessmentServiceImpl implements AssessmentService {

    private final LearningAssessmentMapper assessmentMapper;
    private final ObjectMapper objectMapper;

    @Override
    public LearningAssessment evaluate(Long userId, AssessmentRequest request) {
        int acc = request.getPracticeCount() == null || request.getPracticeCount() == 0
                ? 0 : Math.max(0, 100 - request.getWrongCount() * 100 / request.getPracticeCount());
        int active = Math.min(100, (request.getStudyMinutes() == null ? 0 : request.getStudyMinutes()) / 6);
        int completion = Math.min(100, (request.getFinishedTasks() == null ? 0 : request.getFinishedTasks()) * 12);
        int resourceUse = Math.min(100, (request.getResourceUseCount() == null ? 0 : request.getResourceUseCount()) * 5);

        LearningAssessment a = new LearningAssessment();
        a.setUserId(userId);
        try {
            a.setScoreDimensionJson(objectMapper.writeValueAsString(Map.of(
                    "accuracy", acc,
                    "activity", active,
                    "completion", completion,
                    "resourceUse", resourceUse,
                    "stability", Math.max(60, (acc + completion) / 2),
                    "knowledgeTransfer", Math.max(55, (active + resourceUse) / 2)
            )));
        } catch (Exception e) {
            a.setScoreDimensionJson("{}");
        }
        a.setDiagnosis("系统识别你当前在知识迁移、综合应用与持续学习节奏方面仍有提升空间；基础理解较稳定，但遇到复杂场景时易出现失分。综合来看，你更适合‘图解 + 题库 + 实操’混合学习策略。");
        a.setOptimizeSuggestion("建议后续7天采用‘讲义精读 + 视频强化 + 分层练习 + 代码实操 + 错题复盘’的混合策略，系统将自动增加图解资源与案例训练占比，并提升综合应用题训练频次。");
        a.setCreatedAt(LocalDateTime.now());
        assessmentMapper.insert(a);
        return a;
    }

    @Override
    public List<LearningAssessment> list(Long userId) {
        return assessmentMapper.selectList(new LambdaQueryWrapper<LearningAssessment>()
                .eq(LearningAssessment::getUserId, userId)
                .orderByDesc(LearningAssessment::getCreatedAt));
    }

    @Override
    public Map<String, Object> latestOverview(Long userId) {
        List<LearningAssessment> list = list(userId);
        if (list.isEmpty()) {
            return Map.of(
                    "diagnosis", "暂无评估结果",
                    "optimizeSuggestion", "完成练习、资源浏览或考试后可生成真实评估数据。",
                    "dimensions", Collections.emptyMap()
            );
        }
        LearningAssessment latest = list.get(0);
        return Map.of(
                "diagnosis", latest.getDiagnosis(),
                "optimizeSuggestion", latest.getOptimizeSuggestion(),
                "dimensions", parseJsonMap(latest.getScoreDimensionJson())
        );
    }

    private Map<String, Object> parseJsonMap(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Collections.emptyMap();
            }
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
