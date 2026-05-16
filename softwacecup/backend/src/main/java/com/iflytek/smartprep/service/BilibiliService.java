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

    public BilibiliVideoMeta parseVideo(String url) {
        String bvid = extractBvid(url);
        if (bvid == null) {
            log.warn("无法从URL提取BVID: {}", url);
            return null;
        }
        BilibiliVideoMeta meta = fetchFromApi(bvid);
        if (meta != null) return meta;
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
                return parseVideoData(root.path("data"), bvid);
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
                return parseVideoData(root.path("videoData"), bvid);
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

        List<String> tags = new ArrayList<>();
        JsonNode tagNode = data.path("tags");
        if (tagNode.isArray()) {
            for (JsonNode t : tagNode) {
                String tagName = t.path("tag_name").asText();
                if (!tagName.isEmpty()) tags.add(tagName);
            }
        }
        meta.setTags(tags);

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
        try {
            String[] parts = duration.split(":");
            if (parts.length == 2) return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
            if (parts.length == 3) return Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]);
        } catch (Exception e) { /* ignore */ }
        return 0;
    }

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

    static String extractBvid(String url) {
        if (url == null) return null;
        Matcher m = BV_PATTERN.matcher(url);
        return m.find() ? m.group() : null;
    }
}
