package com.iflytek.smartprep.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.dto.BilibiliImportResult;
import com.iflytek.smartprep.dto.BilibiliVideoMeta;
import com.iflytek.smartprep.mapper.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoImportPipeline {

    private static final Logger log = LoggerFactory.getLogger(VideoImportPipeline.class);
    private final BilibiliService bilibiliService;
    private final LLMClient llmClient;
    private final LessonMapper lessonMapper;
    private final UnitMapper unitMapper;
    private final SubjectMapper subjectMapper;
    private final ExerciseMapper exerciseMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BilibiliImportResult importVideos(List<String> bvids, boolean autoGenerate, Long userId) {
        BilibiliImportResult result = new BilibiliImportResult();
        result.setResults(new ArrayList<>());

        for (String bvid : bvids) {
            BilibiliImportResult.BilibiliImportResultItem item =
                    new BilibiliImportResult.BilibiliImportResultItem();
            item.setBvid(bvid);

            try {
                BilibiliVideoMeta meta = bilibiliService.parseVideo("https://www.bilibili.com/video/" + bvid);
                if (meta == null) {
                    item.setError("视频信息获取失败");
                    result.getResults().add(item);
                    continue;
                }

                item.setLessonName(meta.getTitle());

                Long subjectId = null;
                Long unitId = null;
                String content = null;
                List<Exercise> exercises = null;
                List<String> kpNames = null;

                if (autoGenerate) {
                    ClassificationResult cr = classifyVideo(meta);
                    subjectId = cr.subjectId;
                    unitId = cr.unitId;
                    if (unitId == null) {
                        unitId = createUnit(subjectId, cr.unitName);
                    }
                    item.setSubjectName(cr.subjectName);
                    item.setUnitName(cr.unitName);

                    GenerationResult gr = generateContent(meta);
                    if (gr != null) {
                        content = gr.content;
                        exercises = gr.exercises;
                        kpNames = gr.knowledgePoints;
                    }
                    item.setGenerated(true);
                }

                long now = System.currentTimeMillis();
                Lesson lesson = new Lesson();
                lesson.setId(now);
                lesson.setUnitId(unitId);
                lesson.setName(meta.getTitle());
                lesson.setType("video");
                lesson.setVideoUrl("https://www.bilibili.com/video/" + bvid);
                lesson.setContent(content);
                lesson.setDuration(meta.getDuration());
                lesson.setStatus("draft");
                lesson.setSortOrder(0);
                lesson.setUserId(userId);
                lessonMapper.insert(lesson);

                item.setLessonId(lesson.getId());

                if (exercises != null) {
                    for (Exercise ex : exercises) {
                        ex.setId(System.currentTimeMillis() + (int)(Math.random() * 1000));
                        ex.setLessonId(lesson.getId());
                        exerciseMapper.insert(ex);
                    }
                }

                if (kpNames != null) {
                    for (String kpName : kpNames) {
                        KnowledgePoint kp = new KnowledgePoint();
                        kp.setId(System.currentTimeMillis() + (int)(Math.random() * 1000));
                        kp.setName(kpName);
                        kp.setLessonId(lesson.getId());
                        kp.setDescription("");
                        knowledgePointMapper.insert(kp);
                    }
                }

            } catch (Exception e) {
                log.error("导入视频失败 bvid={}: {}", bvid, e.getMessage());
                item.setError("导入失败: " + e.getMessage());
            }

            result.getResults().add(item);
        }

        return result;
    }

    private ClassificationResult classifyVideo(BilibiliVideoMeta meta) {
        ClassificationResult result = new ClassificationResult();

        try {
            List<Subject> subjects = subjectMapper.selectList(null);
            StringBuilder treeStr = new StringBuilder();
            for (Subject s : subjects) {
                treeStr.append("学科[").append(s.getId()).append("]: ").append(s.getName()).append("\n");
                List<Unit> units = unitMapper.selectList(
                        new LambdaQueryWrapper<Unit>().eq(Unit::getSubjectId, s.getId()));
                for (Unit u : units) {
                    treeStr.append("  单元[").append(u.getId()).append("]: ").append(u.getName()).append("\n");
                }
            }

            String tags = meta.getTags() != null ? String.join(", ", meta.getTags()) : "";
            String desc = meta.getDescription() != null && meta.getDescription().length() > 200
                    ? meta.getDescription().substring(0, 200) : meta.getDescription() != null ? meta.getDescription() : "";

            String systemPrompt = "你是课程分类助手。根据给定的学科/单元结构，将视频归入最合适的单元。\n"
                    + "返回JSON格式: {\"subjectId\": 数字, \"subjectName\": \"学科名\", \"unitId\": 数字, \"unitName\": \"单元名\", \"confidence\": 0.0-1.0, \"reason\": \"理由\"}\n"
                    + "如果找不到匹配的单元，unitId填null，unitName填建议的新单元名称。";

            String userPrompt = "现有课程结构:\n" + treeStr + "\n"
                    + "视频标题: " + meta.getTitle() + "\n"
                    + "标签: " + tags + "\n"
                    + "简介: " + desc;

            JsonNode node = llmClient.chatForJson(systemPrompt, userPrompt, JsonNode.class);
            if (node != null) {
                result.subjectId = node.has("subjectId") && !node.get("subjectId").isNull() ? node.path("subjectId").asLong() : null;
                result.subjectName = node.path("subjectName").asText("");
                result.unitId = node.has("unitId") && !node.get("unitId").isNull() ? node.path("unitId").asLong() : null;
                result.unitName = node.path("unitName").asText("");
                result.confidence = node.path("confidence").asDouble(0.5);
            }
        } catch (Exception e) {
            log.warn("AI分类失败: {}", e.getMessage());
        }

        if (result.subjectId == null) {
            List<Subject> subjects = subjectMapper.selectList(null);
            if (!subjects.isEmpty()) {
                result.subjectId = subjects.get(0).getId();
                result.subjectName = subjects.get(0).getName();
            }
        }

        return result;
    }

    private GenerationResult generateContent(BilibiliVideoMeta meta) {
        GenerationResult result = new GenerationResult();

        try {
            String systemPrompt = "你是课程内容生成助手。根据视频标题和描述，生成一份完整的课时讲义（Markdown格式，含重点难点标注），"
                    + "3道课后练习题（选择题，含4个选项、正确答案索引、解析），"
                    + "以及3-5个知识点标签。\n"
                    + "返回JSON格式: {\"content\": \"Markdown讲义\", \"exercises\": [{\"question\": \"问题\", \"options\": [\"A\",\"B\",\"C\",\"D\"], \"answer\": 0, \"explanation\": \"解析\"}], \"knowledgePoints\": [\"kp1\",\"kp2\"]}";

            String desc = meta.getDescription() != null && meta.getDescription().length() > 300
                    ? meta.getDescription().substring(0, 300) : meta.getDescription() != null ? meta.getDescription() : "";

            String userPrompt = "视频标题: " + meta.getTitle() + "\n"
                    + "视频简介: " + desc + "\n"
                    + "时长: " + (meta.getDuration() != null ? meta.getDuration() / 60 + "分钟" : "未知") + "\n"
                    + "作者: " + (meta.getAuthorName() != null ? meta.getAuthorName() : "未知");

            JsonNode node = llmClient.chatForJson(systemPrompt, userPrompt, JsonNode.class);
            if (node != null) {
                result.content = node.path("content").asText("");

                JsonNode exArr = node.path("exercises");
                result.exercises = new ArrayList<>();
                if (exArr.isArray()) {
                    for (JsonNode ex : exArr) {
                        Exercise exercise = new Exercise();
                        exercise.setType("choice");
                        // Bundle question and options into contentJson
                        ObjectNode contentNode = objectMapper.createObjectNode();
                        contentNode.put("question", ex.path("question").asText(""));
                        contentNode.set("options", ex.path("options"));
                        exercise.setContentJson(contentNode.toString());
                        exercise.setAnswer(String.valueOf(ex.path("answer").asInt(0)));
                        exercise.setExplanation(ex.path("explanation").asText(""));
                        result.exercises.add(exercise);
                    }
                }

                JsonNode kpArr = node.path("knowledgePoints");
                result.knowledgePoints = new ArrayList<>();
                if (kpArr.isArray()) {
                    for (JsonNode kp : kpArr) {
                        result.knowledgePoints.add(kp.asText());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("AI生成内容失败: {}", e.getMessage());
        }

        return result;
    }

    private Long createUnit(Long subjectId, String unitName) {
        if (unitName == null || unitName.isBlank()) {
            unitName = "新建单元";
        }
        Unit unit = new Unit();
        unit.setId(System.currentTimeMillis());
        unit.setSubjectId(subjectId);
        unit.setName(unitName);
        unit.setDescription("");
        unit.setSortOrder(999);
        unitMapper.insert(unit);
        return unit.getId();
    }

    private static class ClassificationResult {
        Long subjectId;
        String subjectName;
        Long unitId;
        String unitName;
        double confidence;
    }

    private static class GenerationResult {
        String content;
        List<Exercise> exercises;
        List<String> knowledgePoints;
    }
}
