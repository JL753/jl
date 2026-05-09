package com.iflytek.smartprep.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.smartprep.domain.StudentProfile;
import com.iflytek.smartprep.dto.DialogueProfileRequest;
import com.iflytek.smartprep.mapper.StudentProfileMapper;
import com.iflytek.smartprep.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final StudentProfileMapper profileMapper;
    private final ObjectMapper objectMapper;

    @Override
    public StudentProfile buildByDialogue(Long userId, DialogueProfileRequest request) {
        StudentProfile profile = getByUserId(userId);
        if (profile == null) {
            profile = new StudentProfile();
            profile.setUserId(userId);
        }
        String text = request.getMessage() == null ? "" : request.getMessage();
        Map<String, String> dims = extractDimensions(text);

        profile.setMajor(dims.getOrDefault("major", nv(profile.getMajor(), "计算机科学与技术")));
        profile.setCourse(dims.getOrDefault("course", nv(profile.getCourse(), "人工智能导论")));
        profile.setKnowledgeBase(dims.getOrDefault("knowledgeBase", nv(profile.getKnowledgeBase(), "中等")));
        profile.setCognitiveStyle(dims.getOrDefault("cognitiveStyle", nv(profile.getCognitiveStyle(), "图文结合")));
        profile.setWeakPoints(dims.getOrDefault("weakPoints", nv(profile.getWeakPoints(), "数学推导、模型评估")));
        profile.setInterestPreference(dims.getOrDefault("interestPreference", nv(profile.getInterestPreference(), "工程实践")));
        profile.setPacePreference(dims.getOrDefault("pacePreference", nv(profile.getPacePreference(), "每周3次")));
        profile.setExamGoal(dims.getOrDefault("examGoal", nv(profile.getExamGoal(), "85分以上")));
        return saveOrUpdateProfile(userId, profile);
    }

    @Override
    public StudentProfile getByUserId(Long userId) {
        return profileMapper.selectOne(new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, userId));
    }

    @Override
    public StudentProfile saveOrUpdateProfile(Long userId, StudentProfile profile) {
        profile.setUserId(userId);
        profile.setUpdatedAt(LocalDateTime.now());
        try {
            Map<String, Object> dims = new LinkedHashMap<>();
            dims.put("major", nv(profile.getMajor(), "-"));
            dims.put("course", nv(profile.getCourse(), "-"));
            dims.put("knowledgeBase", nv(profile.getKnowledgeBase(), "-"));
            dims.put("cognitiveStyle", nv(profile.getCognitiveStyle(), "-"));
            dims.put("weakPoints", nv(profile.getWeakPoints(), "-"));
            dims.put("interestPreference", nv(profile.getInterestPreference(), "-"));
            dims.put("pacePreference", nv(profile.getPacePreference(), "-"));
            dims.put("examGoal", nv(profile.getExamGoal(), "-"));
            profile.setProfileJson(objectMapper.writeValueAsString(dims));
        } catch (Exception e) {
            profile.setProfileJson("{}");
        }
        if (profile.getId() == null) {
            profileMapper.insert(profile);
        } else {
            profileMapper.updateById(profile);
        }
        return profile;
    }

    @Override
    public Map<String, Object> summary(Long userId) {
        StudentProfile profile = getByUserId(userId);
        if (profile == null) {
            return Map.of(
                    "major", "未建立画像",
                    "course", "未建立画像",
                    "knowledgeBase", "-",
                    "cognitiveStyle", "-",
                    "weakPoints", "-",
                    "interestPreference", "-",
                    "pacePreference", "-",
                    "examGoal", "-",
                    "dimensions", Collections.emptyMap()
            );
        }
        Map<String, Object> dimensions = parseJsonMap(profile.getProfileJson());
        return Map.of(
                "major", nv(profile.getMajor(), "-"),
                "course", nv(profile.getCourse(), "-"),
                "knowledgeBase", nv(profile.getKnowledgeBase(), "-"),
                "cognitiveStyle", nv(profile.getCognitiveStyle(), "-"),
                "weakPoints", nv(profile.getWeakPoints(), "-"),
                "interestPreference", nv(profile.getInterestPreference(), "-"),
                "pacePreference", nv(profile.getPacePreference(), "-"),
                "examGoal", nv(profile.getExamGoal(), "-"),
                "dimensions", dimensions
        );
    }

    private Map<String, String> extractDimensions(String text) {
        Map<String, String> d = new LinkedHashMap<>();
        String lower = text.toLowerCase();
        d.put("major", containsAny(text, "计算机", "软件", "人工智能", "电子信息") ? "计算机科学与技术" : "人工智能" );
        d.put("course", containsAny(text, "深度学习") ? "深度学习" : "人工智能导论");
        d.put("knowledgeBase", containsAny(text, "基础差", "薄弱", "跟不上") ? "偏弱" : containsAny(text, "基础好", "熟悉") ? "较强" : "中等");
        d.put("cognitiveStyle", containsAny(text, "视频", "动画") ? "视频优先" : containsAny(text, "图", "导图") ? "图解优先" : "图文结合");
        d.put("weakPoints", containsAny(text, "公式", "数学", "推导") ? "数学推导与公式理解" : containsAny(text, "代码", "编程") ? "代码实现与调试" : "模型评估与知识迁移");
        d.put("interestPreference", containsAny(text, "项目", "实战", "案例") ? "项目实战" : "理论精讲");
        d.put("pacePreference", containsAny(text, "冲刺", "快") ? "高强度冲刺" : containsAny(text, "慢", "循序渐进") ? "渐进巩固" : "每周3次");
        d.put("examGoal", containsAny(text, "考研", "90", "高分") ? "90分以上" : "80分以上");
        d.put("learningGoal", containsAny(text, "项目", "就业") ? "完成课程并具备项目实战能力" : "通过课程考核并构建知识体系");
        d.put("resourcePreference", containsAny(text, "ppt", "讲义") ? "讲义/PPT" : containsAny(text, "题", "刷题") ? "题库/测评" : "讲义+视频+练习混合");
        d.put("riskTag", containsAny(text, "拖延", "时间少") ? "学习节奏风险" : "稳定");
        d.put("dialogueSummary", "已从自然语言中抽取10个画像维度，可持续随学随新。");
        if (lower.contains("python")) {
            d.put("interestPreference", "Python 实战");
        }
        return d;
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

    private boolean containsAny(String text, String... items) {
        for (String item : items) {
            if (text.contains(item)) {
                return true;
            }
        }
        return false;
    }

    private String nv(String v, String def) {
        return v == null || v.isBlank() ? def : v;
    }
}
