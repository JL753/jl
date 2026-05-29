package com.iflytek.smartprep.service.agent;

import com.iflytek.smartprep.domain.ResourceRecommendation;
import com.iflytek.smartprep.mapper.ResourceRecommendationMapper;
import com.iflytek.smartprep.service.LLMClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSearchAgent {

    private final LLMClient llmClient;
    private final ResourceRecommendationMapper recommendationMapper;

    /**
     * 根据学习内容和薄弱维度推荐互联网资源
     * @param lessonTitle 当前课时标题
     * @param weakDimension 薄弱维度描述
     * @param userId 用户ID
     * @param lessonId 课时ID
     * @return 推荐结果 JSON 字符串
     */
    public String recommend(String lessonTitle, String weakDimension, Long userId, Long lessonId) {
        String prompt = String.format(
            "你是一个学习资源推荐助手。学生正在学习「%s」，薄弱维度是「%s」。\n" +
            "请推荐3-5个优质学习资源，包括B站视频、知乎文章、CSDN博客、GitHub项目等。\n" +
            "格式为JSON数组，不要其他文字：\n" +
            "[{\"title\":\"资源标题\",\"url\":\"https://...\",\"platform\":\"B站/知乎/CSDN/GitHub\"," +
            "\"type\":\"视频/文章/项目\",\"description\":\"简介\",\"reason\":\"推荐理由\"}]",
            lessonTitle, weakDimension);

        try {
            String response = llmClient.chat(prompt);
            response = response.replaceAll("```json|```", "").trim();

            ResourceRecommendation rec = new ResourceRecommendation();
            rec.setId(System.currentTimeMillis());
            rec.setUserId(userId);
            rec.setLessonId(lessonId);
            rec.setResourcesJson(response);
            rec.setCreatedAt(LocalDateTime.now());
            recommendationMapper.insert(rec);

            return response;
        } catch (Exception e) {
            log.warn("WebSearchAgent 推荐失败：{}", e.getMessage());
            return "[{\"title\":\"推荐服务暂时不可用\",\"url\":\"\",\"platform\":\"\"," +
                    "\"type\":\"\",\"description\":\"请稍后再试\",\"reason\":\"\"}]";
        }
    }
}
