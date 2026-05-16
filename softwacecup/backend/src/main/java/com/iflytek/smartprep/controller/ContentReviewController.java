package com.iflytek.smartprep.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.config.RequireRole;
import com.iflytek.smartprep.domain.ContentReview;
import com.iflytek.smartprep.domain.Lesson;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.mapper.ContentReviewMapper;
import com.iflytek.smartprep.mapper.LessonMapper;
import com.iflytek.smartprep.service.LLMClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/content-review")
@RequiredArgsConstructor
public class ContentReviewController {

    private final ContentReviewMapper reviewMapper;
    private final LessonMapper lessonMapper;
    private final LLMClient llmClient;

    @GetMapping("/pending")
    @RequireRole({"teacher"})
    public ApiResponse<List<ContentReview>> pending() {
        return ApiResponse.ok(reviewMapper.selectList(
                new LambdaQueryWrapper<ContentReview>().eq(ContentReview::getStatus, "pending")));
    }

    @GetMapping("/{id}")
    @RequireRole({"teacher"})
    public ApiResponse<ContentReview> getReview(@PathVariable Long id) {
        ContentReview r = reviewMapper.selectById(id);
        return r != null ? ApiResponse.ok(r) : ApiResponse.fail("审核记录不存在");
    }

    @PostMapping("/{id}/approve")
    @RequireRole({"teacher"})
    public ApiResponse<String> approve(@PathVariable Long id) {
        ContentReview r = reviewMapper.selectById(id);
        if (r == null) return ApiResponse.fail("审核记录不存在");

        r.setStatus("approved");
        r.setReviewerId(LoginUserHolder.get().getUserId());
        r.setReviewedAt(LocalDateTime.now());
        reviewMapper.updateById(r);

        Lesson lesson = lessonMapper.selectById(r.getLessonId());
        if (lesson != null) {
            lesson.setStatus("published");
            lessonMapper.updateById(lesson);
        }
        return ApiResponse.ok("审核通过，课时已发布");
    }

    @PostMapping("/{id}/reject")
    @RequireRole({"teacher"})
    public ApiResponse<String> reject(@PathVariable Long id, @RequestBody(required = false) String reason) {
        ContentReview r = reviewMapper.selectById(id);
        if (r == null) return ApiResponse.fail("审核记录不存在");

        r.setStatus("rejected");
        r.setReviewerId(LoginUserHolder.get().getUserId());
        r.setReviewedAt(LocalDateTime.now());
        reviewMapper.updateById(r);
        return ApiResponse.ok("已拒绝");
    }

    @PostMapping("/lessons/{id}/ai-generate-content")
    @RequireRole({"teacher"})
    public ApiResponse<ContentReview> aiGenerateContent(@PathVariable Long id) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) return ApiResponse.fail("课时不存在");

        String prompt = "你是一位教学专家。请为以下课时生成讲义内容。\n"
                + "课时名称：" + lesson.getName() + "\n"
                + "要求：包含3-5个知识点段落，每段标注类型（重点/难点/易错点/普通）。"
                + "返回JSON格式：{\"sections\":[{\"type\":\"text\",\"content\":\"...\",\"tags\":[\"重点\"]}]}";

        try {
            String aiContent = llmClient.chat(prompt);

            ContentReview review = new ContentReview();
            review.setId(System.currentTimeMillis());
            review.setLessonId(id);
            review.setAiDraftJson(aiContent);
            review.setStatus("pending");
            reviewMapper.insert(review);

            return ApiResponse.ok(review);
        } catch (Exception e) {
            return ApiResponse.fail("AI生成失败: " + e.getMessage());
        }
    }
}
