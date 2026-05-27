package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
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

    @PostMapping("/parse")
    @RequireRole({"teacher", "admin", "student"})
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

    @PostMapping("/search")
    @RequireRole({"teacher", "admin", "student"})
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

    @PostMapping("/playlist")
    @RequireRole({"teacher", "admin", "student"})
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

    @PostMapping("/import")
    @RequireRole({"teacher", "admin", "student"})
    public ApiResponse<BilibiliImportResult> doImport(@RequestBody BilibiliImportRequest request) {
        if (request.getBvids() == null || request.getBvids().isEmpty()) {
            return ApiResponse.fail("请选择至少一个视频");
        }
        if (request.getBvids().size() > 50) {
            return ApiResponse.fail("单次导入上限50个视频");
        }

        String role = LoginUserHolder.get().getRole();
        Long userId = "student".equals(role) ? LoginUserHolder.get().getUserId() : null;
        BilibiliImportResult result = videoImportPipeline.importVideos(
                request.getBvids(), request.isAutoGenerate(), userId);

        return ApiResponse.ok(result);
    }

    @PostMapping("/import-playlist")
    @RequireRole({"teacher", "admin", "student"})
    public ApiResponse<BilibiliImportResult> importPlaylist(@RequestBody BilibiliImportRequest request) {
        if (request.getBvids() == null || request.getBvids().isEmpty()) {
            return ApiResponse.fail("请选择至少一个视频");
        }
        String role = LoginUserHolder.get().getRole();
        Long userId = "student".equals(role) ? LoginUserHolder.get().getUserId() : null;
        BilibiliImportResult result = videoImportPipeline.importPlaylist(
                request.getBvids(), request.getCourseName(), request.isAutoGenerate(), userId);
        return ApiResponse.ok(result);
    }

    @PostMapping("/subtitles")
    @RequireRole({"teacher", "admin", "student"})
    public ApiResponse<String> subtitles(@RequestBody Map<String, String> body) {
        String bvid = body.get("bvid");
        if (bvid == null || bvid.isBlank()) {
            return ApiResponse.fail("请提供bvid");
        }
        String subtitles = bilibiliService.fetchSubtitles(bvid);
        if (subtitles == null || subtitles.isEmpty()) {
            return ApiResponse.fail("该视频无字幕或字幕获取失败");
        }
        return ApiResponse.ok(subtitles);
    }
}
