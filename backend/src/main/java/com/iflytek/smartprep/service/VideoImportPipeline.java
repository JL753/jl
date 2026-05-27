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
    private final SubChapterMapper subChapterMapper;
    private final ChapterMapper chapterMapper;
    private final CourseMapper courseMapper;
    private final SubjectMapper subjectMapper;
    private final ExerciseMapper exerciseMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 合集导入：整个合集 = 1 门 Course，每个视频 = 1 个 SubChapter（无 Chapter）
     */
    public BilibiliImportResult importPlaylist(List<String> bvids, String courseName, boolean autoGenerate, Long userId) {
        BilibiliImportResult result = new BilibiliImportResult();
        result.setResults(new ArrayList<>());

        if (bvids.isEmpty()) return result;

        // 1. 用第一个视频确定学科
        String firstBvid = bvids.get(0);
        String firstBaseBvid = extractBaseBvid(firstBvid);
        int firstPageNum = 1;
        if (firstBvid.contains("_p")) {
            try { firstPageNum = Integer.parseInt(firstBvid.substring(firstBvid.indexOf("_p") + 2)); } catch (Exception ignored) {}
        }
        BilibiliVideoMeta firstMeta = bilibiliService.parseVideoWithPage(firstBaseBvid, firstPageNum);
        if (firstMeta == null) {
            firstMeta = new BilibiliVideoMeta();
            firstMeta.setTitle(courseName != null ? courseName : "合集");
        }

        ClassificationResult cr = classifyVideo(firstMeta);
        Long subjectId = cr.subjectId;
        if (subjectId == null) {
            Subject fallback = subjectMapper.selectOne(
                    new LambdaQueryWrapper<Subject>().eq(Subject::getName, "我的导入"));
            if (fallback == null) {
                List<Subject> subjects = subjectMapper.selectList(null);
                if (!subjects.isEmpty()) fallback = subjects.get(0);
            }
            if (fallback != null) subjectId = fallback.getId();
        }

        // 2. 创建 Course
        if (courseName == null || courseName.isBlank()) {
            courseName = firstMeta.getTitle() != null ? firstMeta.getTitle() : "新建课程";
        }
        Course course = new Course();
        course.setId(System.currentTimeMillis());
        course.setSubjectId(subjectId);
        course.setTitle(courseName);
        course.setDescription("");
        course.setStatus("已发布");
        course.setCreatedAt(java.time.LocalDateTime.now());
        course.setUpdatedAt(java.time.LocalDateTime.now());
        courseMapper.insert(course);

        // 3. 遍历每个视频，生成SubChapter
        int sortOrder = 0;
        for (String bvid : bvids) {
            BilibiliImportResult.BilibiliImportResultItem item =
                    new BilibiliImportResult.BilibiliImportResultItem();
            item.setBvid(bvid);

            try {
                String baseBvid = extractBaseBvid(bvid);
                int pageNum = 1;
                if (bvid.contains("_p")) {
                    try { pageNum = Integer.parseInt(bvid.substring(bvid.indexOf("_p") + 2)); } catch (Exception ignored) {}
                }
                BilibiliVideoMeta meta = bilibiliService.parseVideoWithPage(baseBvid, pageNum);
                if (meta == null) {
                    meta = new BilibiliVideoMeta();
                    meta.setTitle("视频 " + (sortOrder + 1));
                }

                item.setLessonName(meta.getTitle());
                String content = null;
                List<Exercise> exercises = null;
                List<String> kpNames = null;

                if (autoGenerate) {
                    GenerationResult gr = generateContent(meta);
                    if (gr != null) {
                        content = gr.content;
                        exercises = gr.exercises;
                        kpNames = gr.knowledgePoints;
                    }
                    item.setGenerated(true);
                }

                long now = System.currentTimeMillis() + sortOrder;
                String videoUrl = "https://www.bilibili.com/video/" + baseBvid;
                if (pageNum > 1) videoUrl += "?p=" + pageNum;

                SubChapter sc = new SubChapter();
                sc.setId(now);
                sc.setChapterId(null);
                sc.setCourseId(course.getId());
                sc.setTitle(meta.getTitle());
                sc.setType("video");
                sc.setVideoUrl(videoUrl);
                sc.setContent(content);
                sc.setDuration(meta.getDuration());
                sc.setStatus("published");
                sc.setSortOrder(sortOrder);
                sc.setUserId(userId);
                sc.setCoverUrl(meta.getCoverUrl());
                subChapterMapper.insert(sc);

                item.setLessonId(sc.getId());

                if (exercises != null) {
                    for (Exercise ex : exercises) {
                        ex.setId(System.currentTimeMillis() + (int)(Math.random() * 1000));
                        ex.setLessonId(sc.getId());
                        exerciseMapper.insert(ex);
                    }
                }
                if (kpNames != null) {
                    for (String kpName : kpNames) {
                        KnowledgePoint kp = new KnowledgePoint();
                        kp.setId(System.currentTimeMillis() + (int)(Math.random() * 1000));
                        kp.setName(kpName);
                        kp.setLessonId(sc.getId());
                        kp.setDescription("");
                        knowledgePointMapper.insert(kp);
                    }
                }
            } catch (Exception e) {
                log.error("合集视频导入失败 bvid={}: {}", bvid, e.getMessage());
                item.setError("导入失败: " + e.getMessage());
            }
            result.getResults().add(item);
            sortOrder++;
        }

        return result;
    }

    private String extractBaseBvid(String bvid) {
        if (bvid.contains("_p")) return bvid.substring(0, bvid.indexOf("_p"));
        return bvid;
    }

    public BilibiliImportResult importVideos(List<String> bvids, boolean autoGenerate, Long userId) {
        BilibiliImportResult result = new BilibiliImportResult();
        result.setResults(new ArrayList<>());

        for (String bvid : bvids) {
            BilibiliImportResult.BilibiliImportResultItem item =
                    new BilibiliImportResult.BilibiliImportResultItem();
            item.setBvid(bvid);

            try {
                // 处理多P视频标识：BV1xx_p3 → BV1xx + ?p=3
                String baseBvid = bvid;
                String pageParam = "";
                if (bvid.contains("_p")) {
                    int idx = bvid.indexOf("_p");
                    pageParam = bvid.substring(idx + 1); // "p3"
                    baseBvid = bvid.substring(0, idx);    // "BV1xx"
                }
                String videoUrl = "https://www.bilibili.com/video/" + baseBvid;
                if (!pageParam.isEmpty()) {
                    videoUrl += "?p=" + pageParam.substring(1); // "?p=3"
                }
                BilibiliVideoMeta meta = bilibiliService.parseVideo(videoUrl);
                if (meta == null) {
                    item.setError("视频信息获取失败");
                    result.getResults().add(item);
                    continue;
                }

                item.setLessonName(meta.getTitle());

                Long subjectId = null;
                Long chapterId = null;
                String content = null;
                List<Exercise> exercises = null;
                List<String> kpNames = null;

                if (autoGenerate) {
                    ClassificationResult cr = classifyVideo(meta);
                    subjectId = cr.subjectId;
                    chapterId = cr.chapterId;
                    if (chapterId == null) {
                        chapterId = createChapter(subjectId, cr.subjectName, cr.chapterName);
                    }
                    item.setSubjectName(cr.subjectName);
                    item.setUnitName(cr.chapterName);

                    GenerationResult gr = generateContent(meta);
                    if (gr != null) {
                        content = gr.content;
                        exercises = gr.exercises;
                        kpNames = gr.knowledgePoints;
                    }
                    item.setGenerated(true);
                }

                long now = System.currentTimeMillis();
                SubChapter sc = new SubChapter();
                sc.setId(now);
                sc.setChapterId(chapterId);
                sc.setTitle(meta.getTitle());
                sc.setType("video");
                sc.setVideoUrl(videoUrl);
                sc.setContent(content);
                sc.setDuration(meta.getDuration());
                sc.setStatus("published");
                sc.setSortOrder(0);
                sc.setUserId(userId);
                sc.setCoverUrl(meta.getCoverUrl());
                subChapterMapper.insert(sc);

                item.setLessonId(sc.getId());

                if (exercises != null) {
                    for (Exercise ex : exercises) {
                        ex.setId(System.currentTimeMillis() + (int)(Math.random() * 1000));
                        ex.setLessonId(sc.getId());
                        exerciseMapper.insert(ex);
                    }
                }

                if (kpNames != null) {
                    for (String kpName : kpNames) {
                        KnowledgePoint kp = new KnowledgePoint();
                        kp.setId(System.currentTimeMillis() + (int)(Math.random() * 1000));
                        kp.setName(kpName);
                        kp.setLessonId(sc.getId());
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
                List<Course> courses = courseMapper.selectList(
                        new LambdaQueryWrapper<Course>().eq(Course::getSubjectId, s.getId()));
                for (Course c : courses) {
                    treeStr.append("  课程[").append(c.getId()).append("]: ").append(c.getTitle()).append("\n");
                    List<Chapter> chapters = chapterMapper.selectList(
                            new LambdaQueryWrapper<Chapter>().eq(Chapter::getCourseId, c.getId()));
                    for (Chapter ch : chapters) {
                        treeStr.append("    章节[").append(ch.getId()).append("]: ").append(ch.getTitle()).append("\n");
                    }
                }
            }

            String tags = meta.getTags() != null ? String.join(", ", meta.getTags()) : "";
            String desc = meta.getDescription() != null && meta.getDescription().length() > 200
                    ? meta.getDescription().substring(0, 200) : meta.getDescription() != null ? meta.getDescription() : "";

            String systemPrompt = "你是课程分类助手。根据给定的学科/课程/章节结构，将视频归入最合适的章节。\n"
                    + "返回JSON格式: {\"subjectId\": 数字或null, \"subjectName\": \"学科名\", \"chapterId\": 数字或null, \"chapterName\": \"章节名\", \"confidence\": 0.0-1.0, \"reason\": \"理由\"}\n"
                    + "重要：如果视频内容与所有学科都不匹配（如纯娱乐、生活vlog等），subjectId填null，confidence填0.1。\n"
                    + "如果勉强能匹配但不自信，confidence填0.3以下。只有明确匹配才给0.6以上的confidence。";

            String userPrompt = "现有课程结构:\n" + treeStr + "\n"
                    + "视频标题: " + meta.getTitle() + "\n"
                    + "标签: " + tags + "\n"
                    + "简介: " + desc;

            JsonNode node = llmClient.chatForJson(systemPrompt, userPrompt, JsonNode.class);
            if (node != null) {
                result.subjectId = node.has("subjectId") && !node.get("subjectId").isNull() ? node.path("subjectId").asLong() : null;
                result.subjectName = node.path("subjectName").asText("");
                result.chapterId = node.has("chapterId") && !node.get("chapterId").isNull() ? node.path("chapterId").asLong() : null;
                result.chapterName = node.path("chapterName").asText("");
                result.confidence = node.path("confidence").asDouble(0.5);
                // 低置信度视为无法归类
                if (result.confidence < 0.5) {
                    result.subjectId = null;
                }
            }
        } catch (Exception e) {
            log.warn("AI分类失败: {}", e.getMessage());
        }

        if (result.subjectId == null) {
            Subject fallback = subjectMapper.selectOne(
                    new LambdaQueryWrapper<Subject>().eq(Subject::getName, "我的导入"));
            if (fallback == null) {
                List<Subject> subjects = subjectMapper.selectList(null);
                if (!subjects.isEmpty()) fallback = subjects.get(0);
            }
            if (fallback != null) {
                result.subjectId = fallback.getId();
                result.subjectName = fallback.getName();
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

    private Long createChapter(Long subjectId, String subjectName, String chapterName) {
        if (chapterName == null || chapterName.isBlank()) {
            chapterName = "新建章节";
        }
        // Find or create a Course under this Subject
        Course course = null;
        if (subjectId != null) {
            List<Course> courses = courseMapper.selectList(
                    new LambdaQueryWrapper<Course>().eq(Course::getSubjectId, subjectId).last("LIMIT 1"));
            if (!courses.isEmpty()) course = courses.get(0);
        }
        if (course == null) {
            course = new Course();
            course.setId(System.currentTimeMillis());
            course.setSubjectId(subjectId);
            course.setTitle(subjectName != null ? subjectName : "默认课程");
            course.setDescription("");
            course.setStatus("已发布");
            course.setCreatedAt(java.time.LocalDateTime.now());
            course.setUpdatedAt(java.time.LocalDateTime.now());
            courseMapper.insert(course);
        }
        // Create chapter
        Chapter chapter = new Chapter();
        chapter.setId(System.currentTimeMillis() + 1);
        chapter.setCourseId(course.getId());
        chapter.setTitle(chapterName);
        chapter.setDescription("");
        chapter.setSortOrder(999);
        chapter.setCreatedAt(java.time.LocalDateTime.now());
        chapter.setUpdatedAt(java.time.LocalDateTime.now());
        chapterMapper.insert(chapter);
        return chapter.getId();
    }

    private static class ClassificationResult {
        Long subjectId;
        String subjectName;
        Long chapterId;
        String chapterName;
        double confidence;
    }

    private static class GenerationResult {
        String content;
        List<Exercise> exercises;
        List<String> knowledgePoints;
    }
}
