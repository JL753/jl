package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.domain.StudentProfile;
import com.iflytek.smartprep.dto.AgentResult;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.service.ProfileService;
import com.iflytek.smartprep.service.agent.WebCrawlerAgent;
import com.iflytek.smartprep.dto.ResourceGenerateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crawl")
@RequiredArgsConstructor
public class WebCrawlController {

    private final WebCrawlerAgent webCrawlerAgent;
    private final ProfileService profileService;

    @PostMapping("/search")
    public ApiResponse<AgentResult> search(@RequestBody CrawlRequest request) {
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return ApiResponse.fail("搜索关键词不能为空");
        }
        Long uid = LoginUserHolder.get().getUserId();
        StudentProfile profile = profileService.getByUserId(uid);
        if (profile == null) {
            profile = new StudentProfile();
        }

        // 手动构建，绕过 @NotBlank Bean Validation（不走 @Valid）
        ResourceGenerateRequest genReq = new ResourceGenerateRequest();
        genReq.setTopic(request.getQuery().trim());
        genReq.setCourse(request.getCourse() != null && !request.getCourse().isBlank()
                ? request.getCourse() : "通用课程");
        genReq.setMajor(profile.getMajor() != null && !profile.getMajor().isBlank()
                ? profile.getMajor() : "通用专业");

        AgentResult result = webCrawlerAgent.run(profile, genReq);
        return ApiResponse.ok(result);
    }

    public static class CrawlRequest {
        private String query;
        private String course;

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public String getCourse() { return course; }
        public void setCourse(String course) { this.course = course; }
    }
}
