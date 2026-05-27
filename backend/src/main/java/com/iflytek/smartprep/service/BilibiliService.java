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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
    private static final String SEARCH_API = "https://api.bilibili.com/x/web-interface/wbi/search/type?search_type=video&keyword=";
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

    /** 解析多P视频的指定分P，pageNum从1开始 */
    public BilibiliVideoMeta parseVideoWithPage(String bvid, int pageNum) {
        if (pageNum <= 1) return parseVideo("https://www.bilibili.com/video/" + bvid);
        // 先获取视频全量信息（含pages数组）
        BilibiliVideoMeta video = fetchFromApi(bvid);
        if (video == null) {
            video = fetchFromPage(bvid);
        }
        if (video == null) return null;

        // 获取分P列表
        List<BilibiliVideoMeta> pages = fetchVideoPages(bvid);
        if (pages.isEmpty() && video.getCid() != null && video.getCid() > 0) {
            pages.add(video); // 单P视频
        }
        if (pageNum <= pages.size()) {
            BilibiliVideoMeta page = pages.get(pageNum - 1);
            // 继承视频级别的描述、标签、作者等
            page.setDescription(video.getDescription());
            if (page.getTags() == null || page.getTags().isEmpty()) page.setTags(video.getTags());
            if (page.getAuthorName() == null) page.setAuthorName(video.getAuthorName());
            return page;
        }
        return video;
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
                String html = readBodyAsGbk(response);
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
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .addHeader("Referer", "https://search.bilibili.com/")
                    .addHeader("Origin", "https://search.bilibili.com")
                    .addHeader("Accept-Encoding", "identity")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return result;
                byte[] rawBytes = response.body().bytes();
                // 检查响应是否被 gzip 压缩（B站可能忽略 identity 请求）
                String body;
                if (rawBytes.length >= 2 && rawBytes[0] == 0x1f && rawBytes[1] == (byte) 0x8b) {
                    // gzip magic bytes — 手动解压
                    try (java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(rawBytes);
                         java.util.zip.GZIPInputStream gis = new java.util.zip.GZIPInputStream(bis);
                         java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
                        byte[] buf = new byte[4096];
                        int n;
                        while ((n = gis.read(buf)) > 0) bos.write(buf, 0, n);
                        body = bos.toString("UTF-8");
                    }
                } else {
                    body = new String(rawBytes, java.nio.charset.StandardCharsets.UTF_8);
                }
                if (body == null || body.isEmpty()) return result;

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
                        String pic = item.path("pic").asText("");
                        meta.setCoverUrl(pic.startsWith("//") ? "https:" + pic : pic);
                        String dur = item.path("duration").asText("");
                        meta.setDuration(parseDuration(dur));
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

        // 1. 尝试解析 space.bilibili.com 合集链接
        Matcher m = MID_SID_PATTERN.matcher(url);
        if (m.find()) {
            String mid = m.group(1);
            String sid = m.group(2);
            items = fetchSeriesArchives(mid, sid);
            if (!items.isEmpty()) return items;
        }

        // 2. 尝试解析多P视频（BVxxx?p=1 或直接 BVxxx）
        String bvid = extractBvid(url);
        if (bvid != null) {
            items = fetchVideoPages(bvid);
            if (!items.isEmpty()) return items;
        }

        if (items.isEmpty()) {
            log.warn("合集或多P视频均未找到视频: {}", url);
        }
        return items;
    }

    private List<BilibiliVideoMeta> fetchSeriesArchives(String mid, String sid) {
        List<BilibiliVideoMeta> items = new ArrayList<>();
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

    private List<BilibiliVideoMeta> fetchVideoPages(String bvid) {
        List<BilibiliVideoMeta> items = new ArrayList<>();
        try {
            Request request = new Request.Builder()
                    .url(VIEW_API + bvid)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .addHeader("Referer", "https://www.bilibili.com")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return items;
                String body = response.body().string();
                JsonNode root = objectMapper.readTree(body);
                int code = root.path("code").asInt(-1);
                if (code != 0) {
                    // API 失败，回退到页面解析
                    BilibiliVideoMeta single = fetchFromPage(bvid);
                    if (single != null) items.add(single);
                    return items;
                }

                JsonNode data = root.path("data");
                JsonNode pages = data.path("pages");
                if (pages.isArray() && pages.size() > 1) {
                    int pageNum = 1;
                    for (JsonNode page : pages) {
                        BilibiliVideoMeta meta = new BilibiliVideoMeta();
                        meta.setBvid(bvid + "_p" + pageNum);  // 唯一标识：BV1xx_p1, BV1xx_p2
                        meta.setTitle(page.path("part").asText(""));
                        meta.setDescription(data.path("desc").asText(""));
                        meta.setDuration(page.path("duration").asInt(0));
                        meta.setCoverUrl(data.path("pic").asText(""));
                        meta.setAuthorName(data.path("owner").path("name").asText(""));
                        meta.setCid(page.path("cid").asLong(0));
                        meta.setPageUrl("https://www.bilibili.com/video/" + bvid + "?p=" + pageNum);
                        items.add(meta);
                        pageNum++;
                    }
                } else {
                    BilibiliVideoMeta meta = parseVideoData(data, bvid);
                    if (meta != null) {
                        meta.setBvid(bvid + "_p1");
                        meta.setPageUrl("https://www.bilibili.com/video/" + bvid);
                        items.add(meta);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("获取分P列表失败 for bvid={}: {}", bvid, e.getMessage());
        }
        return items;
    }

    static String extractBvid(String url) {
        if (url == null) return null;
        Matcher m = BV_PATTERN.matcher(url);
        return m.find() ? m.group() : null;
    }

    private static String readBodyUtf8(Response response) throws IOException {
        if (response.body() == null) return "";
        byte[] bytes = response.body().bytes();
        try {
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new String(bytes, java.nio.charset.Charset.forName("GBK"));
        }
    }

    /**
     * 抓取B站视频的真实字幕/CC字幕
     */
    public String fetchSubtitles(String bvid) {
        try {
            // 1. 获取视频的cid和字幕列表
            String playerApi = "https://api.bilibili.com/x/player/v2?bvid=" + bvid;
            Request req = new Request.Builder()
                    .url(playerApi)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .addHeader("Referer", "https://www.bilibili.com")
                    .get().build();
            try (Response resp = httpClient.newCall(req).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) return null;
                JsonNode root = objectMapper.readTree(resp.body().string());
                JsonNode subtitleNode = root.path("data").path("subtitle");
                if (subtitleNode.isMissingNode()) return null;

                JsonNode subtitles = subtitleNode.path("subtitles");
                if (!subtitles.isArray() || subtitles.size() == 0) return null;

                // 优先选中文简体字幕，其次中文，最后第一个
                String subtitleUrl = null;
                for (JsonNode s : subtitles) {
                    String lang = s.path("lan_doc").asText("");
                    if (lang.contains("中文") || lang.contains("简体")) {
                        subtitleUrl = s.path("subtitle_url").asText("");
                        if (!subtitleUrl.isEmpty()) break;
                    }
                }
                if (subtitleUrl == null || subtitleUrl.isEmpty()) {
                    subtitleUrl = subtitles.get(0).path("subtitle_url").asText("");
                }
                if (subtitleUrl == null || subtitleUrl.isEmpty()) return null;

                // 2. 下载字幕JSON
                String url = subtitleUrl.startsWith("//") ? "https:" + subtitleUrl : subtitleUrl;
                if (!url.startsWith("http")) url = "https:" + url;
                Request subReq = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .get().build();
                try (Response subResp = httpClient.newCall(subReq).execute()) {
                    if (!subResp.isSuccessful() || subResp.body() == null) return null;
                    JsonNode subRoot = objectMapper.readTree(subResp.body().string());
                    JsonNode body = subRoot.path("body");
                    if (!body.isArray()) return null;

                    StringBuilder sb = new StringBuilder();
                    for (JsonNode item : body) {
                        double from = item.path("from").asDouble(0);
                        String content = item.path("content").asText("");
                        if (content.isEmpty()) continue;
                        int mins = (int) from / 60;
                        int secs = (int) from % 60;
                        sb.append(String.format("[%02d:%02d] %s\n", mins, secs, content));
                    }
                    return sb.length() > 0 ? sb.toString() : null;
                }
            }
        } catch (Exception e) {
            log.warn("获取B站字幕失败 for bvid={}: {}", bvid, e.getMessage());
            return null;
        }
    }

    private static String readBodyAsGbk(Response response) throws IOException {
        if (response.body() == null) return "";
        byte[] bytes = response.body().bytes();
        return new String(bytes, java.nio.charset.Charset.forName("GBK"));
    }
}
