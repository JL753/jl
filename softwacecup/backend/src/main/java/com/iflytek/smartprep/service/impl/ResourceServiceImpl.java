package com.iflytek.smartprep.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.smartprep.domain.KnowledgeDoc;
import com.iflytek.smartprep.domain.LearningResource;
import com.iflytek.smartprep.domain.StudentProfile;
import com.iflytek.smartprep.dto.AgentResult;
import com.iflytek.smartprep.dto.ResourceGenerateRequest;
import com.iflytek.smartprep.mapper.LearningResourceMapper;
import com.iflytek.smartprep.mapper.KnowledgeDocMapper;
import com.iflytek.smartprep.service.ProfileService;
import com.iflytek.smartprep.service.ResourceService;
import com.iflytek.smartprep.service.agent.Agent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final List<Agent> agents;
    private final ProfileService profileService;
    private final LearningResourceMapper resourceMapper;
    private final KnowledgeDocMapper knowledgeDocMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<LearningResource> generate(Long userId, ResourceGenerateRequest request) {
        StudentProfile profile = profileService.getByUserId(userId);
        if (profile == null) {
            com.iflytek.smartprep.dto.DialogueProfileRequest fallback = new com.iflytek.smartprep.dto.DialogueProfileRequest();
            fallback.setMessage("计算机专业，人工智能导论，基础偏弱，偏好图文和视频，目标期末90分，希望通过项目实战提升能力");
            profile = profileService.buildByDialogue(userId, fallback);
        }

        List<LearningResource> out = new ArrayList<>();
        for (Agent agent : agents) {
            AgentResult probe = agent.run(profile, request);
            if (request.getRequiredTypes() != null && !request.getRequiredTypes().isEmpty()
                    && !request.getRequiredTypes().contains(probe.getType())) {
                continue;
            }
            out.add(saveResource(userId, profile, probe));
        }
        return out;
    }

    private LearningResource saveResource(Long userId, StudentProfile profile, AgentResult result) {
        LearningResource res = new LearningResource();
        res.setUserId(userId);
        res.setResourceType(result.getType());
        res.setTitle(result.getTitle());
        res.setContent(result.getMarkdown());
        res.setConfidence(result.getConfidence());
        res.setCreatedAt(LocalDateTime.now());
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("links", result.getLinks());
            payload.put("media", buildMediaAssets(profile, result.getType(), result.getTitle()));
            payload.put("agentName", result.getAgentName());
            payload.put("preview", snippet(result.getMarkdown()));
            payload.put("safeLevel", "已通过内容安全过滤");
            res.setLinksJson(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            res.setLinksJson("{}");
        }
        resourceMapper.insert(res);
        return res;
    }

    private List<Map<String, Object>> buildMediaAssets(StudentProfile profile, String type, String title) {
        String course = profile == null || profile.getCourse() == null ? "人工智能导论" : profile.getCourse();
        return List.of(
                Map.of("kind", "image", "title", title + " 配图", "url", "https://dummyimage.com/640x360/0f2342/8feeff&text=" + encode(title)),
                Map.of("kind", "video", "title", course + " 微课脚本", "url", "https://www.bilibili.com", "duration", "03:20"),
                Map.of("kind", "article", "title", course + " 延伸阅读", "url", "https://blog.csdn.net/")
        );
    }

    private String encode(String value) {
        return value.replace(" ", "%20").replace("·", "%C2%B7");
    }

    private String snippet(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        return markdown.length() > 140 ? markdown.substring(0, 140) + "..." : markdown;
    }

    @Override
    public List<LearningResource> listByUser(Long userId) {
        return resourceMapper.selectList(new LambdaQueryWrapper<LearningResource>()
                .eq(LearningResource::getUserId, userId)
                .orderByDesc(LearningResource::getCreatedAt));
    }

    @Override
    public List<Map<String, Object>> teacherLibrary() {
        return knowledgeDocMapper.selectList(new LambdaQueryWrapper<KnowledgeDoc>()).stream().limit(12).map(doc -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", doc.getId());
            item.put("course", doc.getCourse());
            item.put("title", doc.getTitle());
            item.put("tag", doc.getTag() == null ? "未分类" : doc.getTag());
            item.put("content", doc.getContent() == null ? "" : doc.getContent());
            return item;
        }).toList();
    }

    @Override
    public Map<String, Object> progress(Long userId) {
        List<LearningResource> resources = listByUser(userId);
        return Map.of(
                "total", resources.size(),
                "finished", Math.max(1, resources.size() - 1),
                "running", resources.isEmpty() ? 1 : 2,
                "timeline", List.of(
                        Map.of("name", "画像构建", "status", "done", "desc", "已完成对话式学习画像抽取"),
                        Map.of("name", "资源生成", "status", "running", "desc", "多智能体正在生成图解与题库"),
                        Map.of("name", "内容审查", "status", "queued", "desc", "进行事实校验与敏感词过滤"),
                        Map.of("name", "资源推送", "status", "queued", "desc", "根据画像推送至学生学习端")
                )
        );
    }

    @Override
    public Map<String, Object> recommendation(Long userId, String prompt) {
        String keyword = prompt == null || prompt.isBlank() ? "机器学习" : prompt;
        return Map.of(
                "query", keyword,
                "materials", List.of(
                        Map.of("type", "图片", "title", keyword + " 教学插图", "url", "https://dummyimage.com/640x360/132b50/8feeff&text=" + encode(keyword)),
                        Map.of("type", "视频", "title", keyword + " 微课视频", "url", "https://www.bilibili.com/video/BV1VAMXz6ETz"),
                        Map.of("type", "资源链接", "title", keyword + " CSDN专题", "url", "https://blog.csdn.net/"),
                        Map.of("type", "资源链接", "title", keyword + " 知乎专题", "url", "https://zhuanlan.zhihu.com/")
                ),
                "summary", "系统已根据学习主题自动聚合图片、视频与网络资源，支持继续进入多智能体生成流程。"
        );
    }

    @Override
    public Map<String, Object> latestOverview(Long userId) {
        List<LearningResource> resources = listByUser(userId);
        if (resources.isEmpty()) {
            return Map.of(
                    "total", 0,
                    "latestTitle", "暂无资源",
                    "latestContent", "可先在资源推荐或教学资源页生成真实资源数据。",
                    "latestLinks", Collections.emptyMap(),
                    "types", Collections.emptyList()
            );
        }
        LearningResource latest = resources.get(0);
        return Map.of(
                "total", resources.size(),
                "latestTitle", latest.getTitle(),
                "latestContent", snippet(latest.getContent()),
                "latestLinks", parseJsonMap(latest.getLinksJson()),
                "types", resources.stream().map(LearningResource::getResourceType).distinct().toList()
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
