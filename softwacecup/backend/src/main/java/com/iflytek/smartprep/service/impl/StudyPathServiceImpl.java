package com.iflytek.smartprep.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.smartprep.domain.StudyPath;
import com.iflytek.smartprep.dto.StudyPathRequest;
import com.iflytek.smartprep.mapper.StudyPathMapper;
import com.iflytek.smartprep.service.StudyPathService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StudyPathServiceImpl implements StudyPathService {

    private final StudyPathMapper studyPathMapper;
    private final ObjectMapper objectMapper;

    @Override
    public StudyPath generate(Long userId, StudyPathRequest request) {
        StudyPath p = new StudyPath();
        p.setUserId(userId);
        p.setTitle("个性化学习路径（多智能体协同版）");
        try {
            p.setStepsJson(objectMapper.writeValueAsString(List.of(
                    Map.of("step", 1, "name", "对话诊断画像", "goal", "抽取学习基础、风格、短板与目标", "duration", "第1天", "agent", "画像智能体"),
                    Map.of("step", 2, "name", "基础讲义精学", "goal", "通过精讲文档夯实核心概念", "duration", "第2-3天", "agent", "课程讲解智能体"),
                    Map.of("step", 3, "name", "思维导图梳理", "goal", "构建知识框架与章节关系", "duration", "第4天", "agent", "思维导图智能体"),
                    Map.of("step", 4, "name", "分层题库训练", "goal", "围绕易错点进行分层刷题", "duration", "第5-7天", "agent", "题库智能体"),
                    Map.of("step", 5, "name", "视频/动画微课", "goal", "借助多模态讲解强化抽象知识理解", "duration", "第8天", "agent", "多模态智能体"),
                    Map.of("step", 6, "name", "代码实操案例", "goal", "完成最小可运行案例，强化迁移应用", "duration", "第9-11天", "agent", "实操项目智能体"),
                    Map.of("step", 7, "name", "综合复盘评估", "goal", "根据测评结果动态调整路径", "duration", "第12天", "agent", "评估智能体")
            )));
            p.setPushJson(objectMapper.writeValueAsString(List.of(
                    Map.of("type", "document", "title", "讲义精读包", "reason", "适合当前基础与考试目标"),
                    Map.of("type", "mindmap", "title", "知识脉络导图", "reason", "帮助形成结构化理解"),
                    Map.of("type", "question", "title", "分层练习题", "reason", "针对易错点进行突破"),
                    Map.of("type", "media", "title", "短视频/动画脚本", "reason", "适配视频优先的认知风格"),
                    Map.of("type", "coding", "title", "代码实操任务单", "reason", "促进知识迁移与项目能力提升")
            )));
        } catch (Exception e) {
            p.setStepsJson("[]");
            p.setPushJson("[]");
        }
        p.setCreatedAt(LocalDateTime.now());
        studyPathMapper.insert(p);
        return p;
    }

    @Override
    public List<StudyPath> list(Long userId) {
        return studyPathMapper.selectList(new LambdaQueryWrapper<StudyPath>()
                .eq(StudyPath::getUserId, userId)
                .orderByDesc(StudyPath::getCreatedAt));
    }

    @Override
    public Map<String, Object> latestOverview(Long userId) {
        List<StudyPath> paths = list(userId);
        if (paths.isEmpty()) {
            return Map.of(
                    "title", "暂无学习路径",
                    "steps", Collections.emptyList(),
                    "pushResources", Collections.emptyList()
            );
        }
        StudyPath latest = paths.get(0);
        return Map.of(
                "title", latest.getTitle(),
                "steps", parseList(latest.getStepsJson()),
                "pushResources", parseList(latest.getPushJson())
        );
    }

    private List<Map<String, Object>> parseList(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
