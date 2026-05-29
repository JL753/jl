# B站学习视频一键导入 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 教师/管理员一键从B站导入学习视频，AI自动分类+生成讲义练习，创建完整课时

**Architecture:** 后端新增 BilibiliController（4端点）+ BilibiliService（B站API抓取）+ VideoImportPipeline（AI编排），前端新增 BilibiliImportModal.vue（三Tab弹窗），集成到 ContentManagement.vue

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + OkHttp + Jackson + Vue3 + Vue Router

---

## 文件结构

```
backend/src/main/java/com/iflytek/smartprep/
├── controller/
│   └── BilibiliController.java       # 新建：4个REST端点
├── service/
│   ├── BilibiliService.java          # 新建：B站API/页面解析
│   └── VideoImportPipeline.java      # 新建：AI分类+生成+创建课时
└── dto/
    └── BilibiliImportRequest.java    # 新建：导入请求DTO

frontend/src/
├── components/
│   └── BilibiliImportModal.vue       # 新建：三Tab导入弹窗
├── views/teacher/
│   └── ContentManagement.vue         # 修改：添加[B站导入]按钮
└── api/
    └── index.js                      # 修改：添加4个API函数
```

---

### Task 1: 后端 DTO — BilibiliImportRequest

**Files:** Create: `softwacecup/backend/src/main/java/com/iflytek/smartprep/dto/BilibiliImportRequest.java`

- [ ] **Step 1: 创建 BilibiliImportRequest DTO**

```java
package com.iflytek.smartprep.dto;

import lombok.Data;
import java.util.List;

@Data
public class BilibiliImportRequest {
    private List<String> bvids;
    private boolean autoGenerate = true;
}
```

- [ ] **Step 2: 同样创建 3 个响应 DTO（VideoMeta、SearchResult、ImportResult）作为 BilibiliService 的内部类或独立记录**

这些 DTO 将在 Task 2 的 BilibiliService 中以内部静态类形式定义，或单独创建。为了保持文件聚焦，在 `dto/` 包下创建：

```java
// BilibiliVideoMeta.java
package com.iflytek.smartprep.dto;

import lombok.Data;

@Data
public class BilibiliVideoMeta {
    private String bvid;
    private String title;
    private String description;
    private Integer duration;
    private String coverUrl;
    private String authorName;
    private List<String> tags;
    private Long cid;
    private String partTitle;
}

// BilibiliSearchResult.java
package com.iflytek.smartprep.dto;

import lombok.Data;
import java.util.List;

@Data
public class BilibiliSearchResult {
    private int total;
    private List<BilibiliVideoMeta> items;
}

// BilibiliImportResult.java
package com.iflytek.smartprep.dto;

import lombok.Data;
import java.util.List;

@Data
public class BilibiliImportResult {
    private List<BilibiliImportResultItem> results;

    @Data
    public static class BilibiliImportResultItem {
        private String bvid;
        private Long lessonId;
        private String lessonName;
        private String subjectName;
        private String unitName;
        private boolean generated;
        private String error;
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/iflytek/smartprep/dto/
git commit -m "feat: 添加B站导入相关DTO类 — BilibiliImportRequest + VideoMeta + SearchResult + ImportResult"
```

---

### Task 2: BilibiliService — B站视频信息抓取

**Files:** Create: `softwacecup/backend/src/main/java/com/iflytek/smartprep/service/BilibiliService.java`

- [ ] **Step 1: 创建 BilibiliService**

Service 封装三个功能：解析单个视频、搜索视频、解析合集。

```java
package com.iflytek.smartprep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.smartprep.dto.BilibiliVideoMeta;
import com.iflytek.smartprep.dto.BilibiliSearchResult;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BilibiliService {

    private static final Logger log = LoggerFactory.getLogger(BilibiliService.class);
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    private static final String VIEW_API = "https://api.bilibili.com/x/web-interface/view?bvid=";
    private static final String SEARCH_API = "https://api.bilibili.com/x/web-interface/search/type?search_type=video&keyword=";
    private static final String SERIES_API = "https://api.bilibili.com/x/series/archives?mid=%s&series_id=%s";
    private static final String VIDEO_PAGE_URL = "https://www.bilibili.com/video/";

    private static final Pattern INITIAL_STATE_PATTERN =
            Pattern.compile("window\\.__INITIAL_STATE__\\s*=\\s*(\\{.*?\\})\\s*;", Pattern.DOTALL);
    private static final Pattern BV_PATTERN =
            Pattern.compile("BV[a-zA-Z0-9]{10}");
    private static final Pattern MID_SID_PATTERN =
            Pattern.compile("space\\.bilibili\\.com/(\\d+).*sid=(\\d+)");

    public BilibiliService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // ==================== 解析单个视频 ====================

    public BilibiliVideoMeta parseVideo(String url) {
        String bvid = extractBvid(url);
        if (bvid == null) {
            log.warn("无法从URL提取BVID: {}", url);
            return null;
        }

        // 主方案：公开API
        BilibiliVideoMeta meta = fetchFromApi(bvid);
        if (meta != null) return meta;

        // 回退：页面解析
        return fetchFromPage(bvid);
    }

    private BilibiliVideoMeta fetchFromApi(String bvid) {
        try {
            Request request = new Request.Builder()
                    .url(VIEW_API + bvid)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .addHeader("Referer", "https://www.bilibili.com")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return null;
                String body = response.body().string();
                JsonNode root = objectMapper.readTree(body);
                int code = root.path("code").asInt(-1);
                if (code != 0) {
                    log.warn("B站API返回code={} for bvid={}", code, bvid);
                    return null;
                }
                JsonNode data = root.path("data");
                return parseVideoData(data, bvid);
            }
        } catch (IOException e) {
            log.warn("B站API请求失败 for bvid={}: {}", bvid, e.getMessage());
            return null;
        }
    }

    private BilibiliVideoMeta fetchFromPage(String bvid) {
        try {
            Request request = new Request.Builder()
                    .url(VIDEO_PAGE_URL + bvid)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return null;
                String html = response.body().string();
                Matcher m = INITIAL_STATE_PATTERN.matcher(html);
                if (!m.find()) {
                    log.warn("未找到__INITIAL_STATE__ for bvid={}", bvid);
                    return null;
                }
                JsonNode root = objectMapper.readTree(m.group(1));
                JsonNode data = root.path("videoData");
                return parseVideoData(data, bvid);
            }
        } catch (IOException e) {
            log.warn("页面解析失败 for bvid={}: {}", bvid, e.getMessage());
            return null;
        }
    }

    private BilibiliVideoMeta parseVideoData(JsonNode data, String bvid) {
        BilibiliVideoMeta meta = new BilibiliVideoMeta();
        meta.setBvid(bvid);
        meta.setTitle(data.path("title").asText(""));
        meta.setDescription(data.path("desc").asText(""));
        meta.setDuration(data.path("duration").asInt(0));
        meta.setCoverUrl(data.path("pic").asText(""));
        meta.setAuthorName(data.path("owner").path("name").asText(""));
        meta.setCid(data.path("cid").asLong(0));

        // 解析标签
        List<String> tags = new ArrayList<>();
        // API返回的 tags 可能是数组，页面解析可能是 tag name
        JsonNode tagNode = data.path("tags");
        if (tagNode.isArray()) {
            for (JsonNode t : tagNode) {
                tags.add(t.path("tag_name").asText());
            }
        }
        meta.setTags(tags);

        // 分P标题（合集内视频可能有）
        JsonNode pages = data.path("pages");
        if (pages.isArray() && pages.size() > 0) {
            for (JsonNode p : pages) {
                if (p.path("cid").asLong() == (meta.getCid() != null ? meta.getCid() : 0)) {
                    meta.setPartTitle(p.path("part").asText(""));
                    break;
                }
            }
        }
        return meta;
    }

    // ==================== 搜索视频 ====================

    public BilibiliSearchResult searchVideos(String keyword, int page, int pageSize) {
        BilibiliSearchResult result = new BilibiliSearchResult();
        result.setItems(new ArrayList<>());
        result.setTotal(0);

        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = SEARCH_API + encodedKeyword + "&page=" + page + "&page_size=" + pageSize;

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .addHeader("Referer", "https://www.bilibili.com")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return result;
                String body = response.body().string();
                JsonNode root = objectMapper.readTree(body);
                int code = root.path("code").asInt(-1);
                if (code != 0) return result;

                JsonNode data = root.path("data");
                result.setTotal(data.path("numResults").asInt(0));

                JsonNode items = data.path("result");
                if (items.isArray()) {
                    for (JsonNode item : items) {
                        BilibiliVideoMeta meta = new BilibiliVideoMeta();
                        meta.setBvid(item.path("bvid").asText(""));
                        meta.setTitle(item.path("title").asText("").replaceAll("<[^>]+>", ""));
                        meta.setCoverUrl("https:" + item.path("pic").asText(""));
                        meta.setDuration(parseDuration(item.path("duration").asText("")));
                        meta.setAuthorName(item.path("author").asText(""));
                        meta.setPlayCount(item.path("play").asInt(0));
                        result.getItems().add(meta);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("B站搜索失败: {}", e.getMessage());
        }
        return result;
    }

    private int parseDuration(String duration) {
        // B站搜索返回格式如 "45:32" 或 "03:15"
        try {
            String[] parts = duration.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    // ==================== 解析合集/播放列表 ====================

    public List<BilibiliVideoMeta> parsePlaylist(String url) {
        List<BilibiliVideoMeta> items = new ArrayList<>();
        Matcher m = MID_SID_PATTERN.matcher(url);
        if (!m.find()) {
            log.warn("无法从合集URL提取mid/sid: {}", url);
            return items;
        }

        String mid = m.group(1);
        String sid = m.group(2);

        try {
            String apiUrl = String.format(SERIES_API, mid, sid);
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .addHeader("Referer", "https://space.bilibili.com")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return items;
                String body = response.body().string();
                JsonNode root = objectMapper.readTree(body);

                JsonNode archives = root.path("data").path("archives");
                if (archives.isArray()) {
                    for (JsonNode archive : archives) {
                        BilibiliVideoMeta meta = new BilibiliVideoMeta();
                        meta.setBvid(archive.path("bvid").asText(""));
                        meta.setTitle(archive.path("title").asText(""));
                        meta.setCoverUrl(archive.path("cover").asText(""));
                        meta.setDuration(archive.path("duration").asInt(0));
                        items.add(meta);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("解析合集失败: {}", e.getMessage());
        }
        return items;
    }

    // ==================== 工具方法 ====================

    static String extractBvid(String url) {
        if (url == null) return null;
        Matcher m = BV_PATTERN.matcher(url);
        return m.find() ? m.group() : null;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/iflytek/smartprep/service/BilibiliService.java
git commit -m "feat: 添加BilibiliService — B站视频解析/搜索/合集抓取"
```

---

### Task 3: VideoImportPipeline — AI编排+课时创建

**Files:** Create: `softwacecup/backend/src/main/java/com/iflytek/smartprep/service/VideoImportPipeline.java`

- [ ] **Step 1: 创建 VideoImportPipeline**

```java
package com.iflytek.smartprep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final KpDependencyMapper kpDependencyMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BilibiliImportResult importVideos(List<String> bvids, boolean autoGenerate) {
        BilibiliImportResult result = new BilibiliImportResult();
        result.setResults(new ArrayList<>());

        for (String bvid : bvids) {
            BilibiliImportResult.BilibiliImportResultItem item =
                    new BilibiliImportResult.BilibiliImportResultItem();
            item.setBvid(bvid);

            try {
                // 1. 获取视频元数据
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
                    // 2. AI 分类
                    ClassificationResult cr = classifyVideo(meta);
                    subjectId = cr.subjectId;
                    unitId = cr.unitId;
                    if (unitId == null) {
                        // 自动创建新单元
                        unitId = createUnit(subjectId, cr.unitName);
                    }
                    item.setSubjectName(cr.subjectName);
                    item.setUnitName(cr.unitName);

                    // 3. AI 生成讲义 + 练习 + 知识点
                    GenerationResult gr = generateContent(meta);
                    content = gr.content;
                    exercises = gr.exercises;
                    kpNames = gr.knowledgePoints;
                    item.setGenerated(true);
                }

                // 4. 创建课时
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
                lessonMapper.insert(lesson);

                item.setLessonId(lesson.getId());

                // 5. 创建练习题
                if (exercises != null) {
                    for (Exercise ex : exercises) {
                        ex.setId(System.currentTimeMillis() + (int)(Math.random() * 1000));
                        ex.setLessonId(lesson.getId());
                        exerciseMapper.insert(ex);
                    }
                }

                // 6. 关联知识点
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
            // 获取现有学科/单元结构
            List<Subject> subjects = subjectMapper.selectList(null);
            StringBuilder treeStr = new StringBuilder();
            for (Subject s : subjects) {
                treeStr.append("学科[").append(s.getId()).append("]: ").append(s.getName()).append("\n");
                List<Unit> units = unitMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Unit>()
                                .eq(Unit::getSubjectId, s.getId()));
                for (Unit u : units) {
                    treeStr.append("  单元[").append(u.getId()).append("]: ").append(u.getName()).append("\n");
                }
            }

            String tags = meta.getTags() != null ? String.join(", ", meta.getTags()) : "";
            String desc = meta.getDescription() != null ? meta.getDescription().substring(0, Math.min(200, meta.getDescription().length())) : "";

            String systemPrompt = "你是课程分类助手。根据给定的学科/单元结构，将视频归入最合适的单元。\n"
                    + "返回JSON格式: {\"subjectId\": 数字, \"subjectName\": \"学科名\", \"unitId\": 数字, \"unitName\": \"单元名\", \"confidence\": 0.0-1.0, \"reason\": \"理由\"}\n"
                    + "如果找不到匹配的单元，unitId填null，unitName填建议的新单元名称。";

            String userPrompt = "现有课程结构:\n" + treeStr.toString() + "\n"
                    + "视频标题: " + meta.getTitle() + "\n"
                    + "标签: " + tags + "\n"
                    + "简介: " + desc;

            // 使用 LLMClient.chatForJson
            JsonNode node = llmClient.chatForJson(systemPrompt, userPrompt, JsonNode.class);
            if (node != null) {
                result.subjectId = node.path("subjectId").asLong();
                result.subjectName = node.path("subjectName").asText("");
                result.unitId = node.has("unitId") && !node.get("unitId").isNull() ? node.path("unitId").asLong() : null;
                result.unitName = node.path("unitName").asText("");
                result.confidence = node.path("confidence").asDouble(0.5);
            }
        } catch (Exception e) {
            log.warn("AI分类失败: {}", e.getMessage());
        }

        // 如果 AI 分类失败，使用默认第一个学科/单元
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

            String desc = meta.getDescription() != null ? meta.getDescription().substring(0, Math.min(300, meta.getDescription().length())) : "";

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
                        exercise.setQuestion(ex.path("question").asText(""));
                        exercise.setType("choice");
                        exercise.setOptions(ex.path("options").toString());
                        exercise.setAnswer(ex.path("answer").asInt(0));
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

    // 内部类
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
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/iflytek/smartprep/service/VideoImportPipeline.java
git commit -m "feat: 添加VideoImportPipeline — AI分类+生成+创建课时"
```

---

### Task 4: BilibiliController — REST端点

**Files:** Create: `softwacecup/backend/src/main/java/com/iflytek/smartprep/controller/BilibiliController.java`

- [ ] **Step 1: 创建 BilibiliController**

```java
package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.RequireRole;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.dto.BilibiliImportRequest;
import com.iflytek.smartprep.dto.BilibiliImportResult;
import com.iflytek.smartprep.dto.BilibiliSearchResult;
import com.iflytek.smartprep.dto.BilibiliVideoMeta;
import com.iflytek.smartprep.service.BilibiliService;
import com.iflytek.smartprep.service.VideoImportPipeline;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bilibili")
@RequiredArgsConstructor
public class BilibiliController {

    private final BilibiliService bilibiliService;
    private final VideoImportPipeline videoImportPipeline;

    /**
     * 解析单个视频链接 → 元数据
     */
    @PostMapping("/parse")
    @RequireRole({"teacher", "admin"})
    public ApiResponse<BilibiliVideoMeta> parse(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        if (url == null || url.isBlank()) {
            return ApiResponse.fail("请提供视频链接");
        }
        BilibiliVideoMeta meta = bilibiliService.parseVideo(url);
        if (meta == null) {
            return ApiResponse.fail("视频解析失败，请检查链接格式");
        }
        return ApiResponse.ok(meta);
    }

    /**
     * 关键词搜索B站视频
     */
    @PostMapping("/search")
    @RequireRole({"teacher", "admin"})
    public ApiResponse<BilibiliSearchResult> search(@RequestBody Map<String, Object> body) {
        String keyword = (String) body.getOrDefault("keyword", "");
        int page = (int) body.getOrDefault("page", 1);
        int pageSize = (int) body.getOrDefault("pageSize", 10);

        if (keyword.isBlank()) {
            return ApiResponse.fail("请输入搜索关键词");
        }
        if (pageSize > 50) pageSize = 50;

        BilibiliSearchResult result = bilibiliService.searchVideos(keyword, page, pageSize);
        return ApiResponse.ok(result);
    }

    /**
     * 解析合集/播放列表
     */
    @PostMapping("/playlist")
    @RequireRole({"teacher", "admin"})
    public ApiResponse<List<BilibiliVideoMeta>> playlist(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        if (url == null || url.isBlank()) {
            return ApiResponse.fail("请提供合集链接");
        }
        List<BilibiliVideoMeta> items = bilibiliService.parsePlaylist(url);
        if (items.isEmpty()) {
            return ApiResponse.fail("合集解析失败，请检查链接格式");
        }
        return ApiResponse.ok(items);
    }

    /**
     * 一键导入视频 → 创建课时
     */
    @PostMapping("/import")
    @RequireRole({"teacher", "admin"})
    public ApiResponse<BilibiliImportResult> doImport(@RequestBody BilibiliImportRequest request) {
        if (request.getBvids() == null || request.getBvids().isEmpty()) {
            return ApiResponse.fail("请选择至少一个视频");
        }
        if (request.getBvids().size() > 50) {
            return ApiResponse.fail("单次导入上限50个视频");
        }

        BilibiliImportResult result = videoImportPipeline.importVideos(
                request.getBvids(), request.isAutoGenerate());

        return ApiResponse.ok(result);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/iflytek/smartprep/controller/BilibiliController.java
git commit -m "feat: 添加BilibiliController — 4个REST端点 /parse /search /playlist /import"
```

---

### Task 5: 前端 API 函数 & 路由

**Files:** Modify: `softwacecup/frontend/src/api/index.js`

- [ ] **Step 1: 在 index.js 末尾添加 4 个 API 函数**

```javascript
// ==================== B站视频导入 ====================
export const apiBilibiliParse = (url) => http.post('/bilibili/parse', { url })
export const apiBilibiliSearch = (keyword, page = 1, pageSize = 10) => http.post('/bilibili/search', { keyword, page, pageSize })
export const apiBilibiliPlaylist = (url) => http.post('/bilibili/playlist', { url })
export const apiBilibiliImport = (bvids, autoGenerate = true) => http.post('/bilibili/import', { bvids, autoGenerate })
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/api/index.js
git commit -m "feat: 添加B站导入4个前端API函数"
```

---

### Task 6: BilibiliImportModal.vue — 前端弹窗组件

**Files:** Create: `softwacecup/frontend/src/components/BilibiliImportModal.vue`

- [ ] **Step 1: 创建 BilibiliImportModal.vue**

```vue
<template>
  <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
    <div class="glass-card import-modal">
      <div class="im-header">
        <h3>B站视频导入</h3>
        <button class="im-close" @click="$emit('close')">&times;</button>
      </div>

      <!-- Tab Bar -->
      <div class="im-tabs">
        <button class="im-tab" :class="{ active: tab === 'link' }" @click="tab = 'link'">粘贴链接</button>
        <button class="im-tab" :class="{ active: tab === 'search' }" @click="tab = 'search'">搜索视频</button>
        <button class="im-tab" :class="{ active: tab === 'playlist' }" @click="tab = 'playlist'">合集导入</button>
      </div>

      <!-- Tab 1: Link paste -->
      <div v-if="tab === 'link'" class="im-body">
        <p class="im-hint">粘贴一个或多个B站视频链接，每行一个</p>
        <textarea v-model="linkText" class="im-textarea" rows="4" placeholder="https://www.bilibili.com/video/BV1xx411c7mD"></textarea>
        <button class="im-btn primary" @click="parseLinks" :disabled="parsing">解析</button>

        <div v-if="parsedVideos.length > 0" class="im-results">
          <div class="im-subtitle">解析结果 ({{ parsedVideos.length }} 个)</div>
          <div v-for="v in parsedVideos" :key="v.bvid" class="im-video-row">
            <div class="im-cover-wrap">
              <img v-if="v.coverUrl" :src="v.coverUrl" class="im-cover" />
              <div v-else class="im-cover-placeholder"></div>
            </div>
            <div class="im-video-info">
              <div class="im-video-title">{{ v.title }}</div>
              <div class="im-video-meta">{{ v.bvid }} · {{ formatDuration(v.duration) }} · {{ v.authorName }}</div>
            </div>
          </div>
          <button class="im-btn primary" @click="doImport" :disabled="importing">
            {{ importing ? '导入中...' : '一键导入 ' + parsedVideos.length + ' 个视频' }}
          </button>
        </div>
      </div>

      <!-- Tab 2: Search -->
      <div v-if="tab === 'search'" class="im-body">
        <div class="im-search-row">
          <input v-model="searchKeyword" class="im-search-input" placeholder="输入关键词搜索B站视频..." @keydown.enter="doSearch" />
          <button class="im-btn primary" @click="doSearch" :disabled="searching">搜索</button>
        </div>

        <div v-if="searchItems.length > 0" class="im-results">
          <div class="im-subtitle">搜索结果 · 共 {{ searchTotal }} 个</div>
          <div v-for="v in searchItems" :key="v.bvid" class="im-video-row">
            <input type="checkbox" :value="v.bvid" v-model="selectedBvids" class="im-checkbox" />
            <div class="im-cover-wrap">
              <img v-if="v.coverUrl" :src="v.coverUrl" class="im-cover" />
              <div v-else class="im-cover-placeholder"></div>
            </div>
            <div class="im-video-info">
              <div class="im-video-title">{{ v.title }}</div>
              <div class="im-video-meta">{{ formatDuration(v.duration) }} · {{ formatPlayCount(v.playCount) }}播放 · {{ v.authorName }}</div>
            </div>
          </div>
          <button class="im-btn primary" @click="doImportSelected" :disabled="importing || selectedBvids.length === 0">
            {{ importing ? '导入中...' : '导入选中视频（' + selectedBvids.length + '）' }}
          </button>
        </div>
      </div>

      <!-- Tab 3: Playlist -->
      <div v-if="tab === 'playlist'" class="im-body">
        <p class="im-hint">粘贴B站合集/播放列表链接</p>
        <div class="im-search-row">
          <input v-model="playlistUrl" class="im-search-input" placeholder="https://space.bilibili.com/xxx/channel/seriesdetail?sid=xxx" @keydown.enter="parsePlaylist" />
          <button class="im-btn primary" @click="parsePlaylist" :disabled="parsingPlaylist">解析</button>
        </div>

        <div v-if="playlistItems.length > 0" class="im-results">
          <div class="im-subtitle">共 {{ playlistItems.length }} 个视频</div>
          <div v-for="v in playlistItems" :key="v.bvid" class="im-video-row">
            <input type="checkbox" :value="v.bvid" v-model="selectedBvids" checked class="im-checkbox" />
            <div class="im-cover-wrap">
              <img v-if="v.coverUrl" :src="v.coverUrl" class="im-cover" />
              <div v-else class="im-cover-placeholder"></div>
            </div>
            <div class="im-video-info">
              <div class="im-video-title">{{ v.title }}</div>
              <div class="im-video-meta">{{ formatDuration(v.duration) }}</div>
            </div>
          </div>
          <button class="im-btn primary" @click="doImportSelected" :disabled="importing || selectedBvids.length === 0">
            {{ importing ? '导入中...' : '一键导入全部 ' + selectedBvids.length + ' 个视频' }}
          </button>
        </div>
      </div>

      <!-- Result summary -->
      <div v-if="importResults.length > 0" class="im-results im-import-summary">
        <div class="im-subtitle">导入完成</div>
        <div v-for="r in importResults" :key="r.bvid" class="im-result-row" :class="{ error: r.error }">
          <span v-if="r.error" class="im-result-icon">✗</span>
          <span v-else class="im-result-icon">✓</span>
          <div class="im-video-info">
            <div>{{ r.lessonName || r.bvid }}</div>
            <div class="im-video-meta" v-if="!r.error">{{ r.subjectName }} › {{ r.unitName }}</div>
            <div class="im-video-meta error" v-else>{{ r.error }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { apiBilibiliParse, apiBilibiliSearch, apiBilibiliPlaylist, apiBilibiliImport } from '../api'

const props = defineProps({ visible: Boolean })
const emit = defineEmits(['close'])

const tab = ref('link')

// Tab 1 state
const linkText = ref('')
const parsing = ref(false)
const parsedVideos = ref([])

// Tab 2 state
const searchKeyword = ref('')
const searching = ref(false)
const searchItems = ref([])
const searchTotal = ref(0)

// Tab 3 state
const playlistUrl = ref('')
const parsingPlaylist = ref(false)
const playlistItems = ref([])

// Shared
const selectedBvids = ref([])
const importing = ref(false)
const importResults = ref([])

async function parseLinks() {
  const urls = linkText.value.split('\n').filter(l => l.trim())
  if (urls.length === 0) return
  parsing.value = true
  parsedVideos.value = []
  importResults.value = []

  for (const url of urls) {
    try {
      const res = await apiBilibiliParse(url.trim())
      if (res.data) {
        parsedVideos.value.push(res.data)
        selectedBvids.value.push(res.data.bvid)
      }
    } catch (e) {
      // 该条解析失败，跳过
    }
  }
  parsing.value = false
}

async function doSearch() {
  if (!searchKeyword.value.trim()) return
  searching.value = true
  searchItems.value = []
  importResults.value = []
  try {
    const res = await apiBilibiliSearch(searchKeyword.value.trim(), 1, 10)
    searchItems.value = res.data?.items || []
    searchTotal.value = res.data?.total || 0
    selectedBvids.value = searchItems.value.map(v => v.bvid)
  } catch (e) { /* ignore */ }
  searching.value = false
}

async function parsePlaylist() {
  if (!playlistUrl.value.trim()) return
  parsingPlaylist.value = true
  playlistItems.value = []
  importResults.value = []
  try {
    const res = await apiBilibiliPlaylist(playlistUrl.value.trim())
    playlistItems.value = res.data || []
    selectedBvids.value = playlistItems.value.map(v => v.bvid)
  } catch (e) { /* ignore */ }
  parsingPlaylist.value = false
}

async function doImport() {
  if (selectedBvids.value.length === 0) return
  importing.value = true
  try {
    const res = await apiBilibiliImport(selectedBvids.value, true)
    importResults.value = res.data?.results || []
  } catch (e) { /* ignore */ }
  importing.value = false
}

async function doImportSelected() {
  await doImport()
}

function formatDuration(seconds) {
  if (!seconds) return '--:--'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

function formatPlayCount(n) {
  if (!n) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return String(n)
}
</script>

<style scoped>
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.6); z-index: 300;
  display: flex; align-items: center; justify-content: center;
}
.import-modal {
  width: 680px; max-height: 85vh; overflow-y: auto; padding: 0; border-radius: 14px;
}
.im-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border-bottom: 1px solid rgba(255,255,255,0.06);
}
.im-header h3 { font-size: 16px; font-weight: 600; }
.im-close { background: none; border: none; color: rgba(255,255,255,0.4); font-size: 20px; cursor: pointer; }
.im-tabs { display: flex; border-bottom: 1px solid rgba(255,255,255,0.06); padding: 0 20px; }
.im-tab {
  padding: 10px 16px; font-size: 12px; color: rgba(255,255,255,0.4);
  background: none; border: none; border-bottom: 2px solid transparent; cursor: pointer; font-family: inherit;
}
.im-tab.active { color: #60d9fa; border-bottom-color: #60d9fa; font-weight: 600; }
.im-body { padding: 20px; }
.im-hint { font-size: 11px; color: rgba(255,255,255,0.4); margin-bottom: 8px; }
.im-textarea {
  width: 100%; padding: 10px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.04); color: #fff; font-size: 12px; font-family: inherit;
  resize: vertical; margin-bottom: 10px; outline: none;
}
.im-btn {
  padding: 8px 20px; border-radius: 8px; border: none; font-size: 12px; cursor: pointer; font-family: inherit;
}
.im-btn.primary { background: linear-gradient(135deg,#3b82f6,#2563eb); color: #fff; }
.im-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.im-search-row { display: flex; gap: 8px; margin-bottom: 16px; }
.im-search-input {
  flex: 1; padding: 10px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.04); color: #fff; font-size: 12px; font-family: inherit; outline: none;
}
.im-subtitle { font-size: 10px; text-transform: uppercase; letter-spacing: 1px; color: rgba(255,255,255,0.3); margin-bottom: 10px; }
.im-results { margin-top: 16px; display: flex; flex-direction: column; gap: 8px; }
.im-video-row {
  display: flex; align-items: center; gap: 10px; padding: 8px;
  background: rgba(255,255,255,0.03); border-radius: 8px; border: 1px solid rgba(255,255,255,0.06);
}
.im-cover-wrap { width: 80px; min-width: 80px; height: 46px; border-radius: 4px; overflow: hidden; }
.im-cover { width: 100%; height: 100%; object-fit: cover; }
.im-cover-placeholder { width: 100%; height: 100%; background: rgba(255,255,255,0.05); }
.im-video-info { flex: 1; min-width: 0; }
.im-video-title { font-size: 12px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.im-video-meta { font-size: 10px; color: rgba(255,255,255,0.4); margin-top: 2px; }
.im-video-meta.error { color: #ef4444; }
.im-checkbox { accent-color: #3b82f6; width: 16px; height: 16px; flex-shrink: 0; }
.im-import-summary { border-top: 1px solid rgba(255,255,255,0.06); padding-top: 12px; margin-top: 12px; }
.im-result-row { display: flex; align-items: center; gap: 8px; padding: 4px 0; }
.im-result-row.error { opacity: 0.6; }
.im-result-icon { font-size: 12px; width: 16px; text-align: center; }

/* Glass card */
.glass-card {
  background: rgba(8, 13, 31, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(16px);
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/components/BilibiliImportModal.vue
git commit -m "feat: 添加BilibiliImportModal — 三Tab B站视频导入弹窗"
```

---

### Task 7: 集成到 ContentManagement.vue

**Files:** Modify: `softwacecup/frontend/src/views/teacher/ContentManagement.vue`

- [ ] **Step 1: 在 ContentManagement.vue 中添加导入按钮和弹窗**

在 `<h1>内容管理</h1>` 后面添加：

```vue
<button class="import-btn" @click="showBiliImport = true">B站导入</button>
```

在 `</div>` (最外层) 之前添加：

```vue
<BilibiliImportModal :visible="showBiliImport" @close="showBiliImport = false" />
```

在 script 中添加：

```javascript
import BilibiliImportModal from '../../components/BilibiliImportModal.vue'
const showBiliImport = ref(false)
```

在 style 中添加按钮样式：

```css
.import-btn {
  padding: 8px 16px; border-radius: 8px; border: 1px solid rgba(251, 114, 153, 0.3);
  background: rgba(251, 114, 153, 0.1); color: #fda4af; cursor: pointer;
  font-size: 13px; font-family: inherit; margin-bottom: 16px;
}
.import-btn:hover { background: rgba(251, 114, 153, 0.15); }
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/views/teacher/ContentManagement.vue
git commit -m "feat: ContentManagement集成B站导入入口"
```

---

### Task 8: 构建验证

- [ ] **Step 1: 后端编译**

```bash
cd backend && mvn compile -q 2>&1 | tail -10
```
预期: BUILD SUCCESS

- [ ] **Step 2: 前端构建**

```bash
cd frontend && npx vite build 2>&1 | tail -10
```
预期: built in Xs

- [ ] **Step 3: 确认所有导入无报错**

检查：BilibiliController 中的 `@RequireRole` 注解是否与现有 auth 体系兼容（查看 `config/RequireRole.java` 已存在，确认 "teacher" "admin" 角色值正确）。

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "chore: B站视频导入构建验证通过"
```

---

## 任务依赖

```
Task 1 (DTOs) ──→ Task 2 (BilibiliService) ──→ Task 3 (VideoImportPipeline) ──→ Task 4 (Controller)
                                                                                        │
Task 5 (前端API) ──→ Task 6 (BilibiliImportModal) ──→ Task 7 (ContentManagement集成) ──┤
                                                                                        │
                                                                              Task 8 (验证)
```

Tasks 1-4 后端顺序执行。Tasks 5-7 前端顺序执行。后端与前端可并行。
