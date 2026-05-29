package com.iflytek.smartprep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class BaiduSearchService {

    private static final Logger log = LoggerFactory.getLogger(BaiduSearchService.class);
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${smartprep.baidu.search-token:}")
    private String searchToken;

    private static final String SEARCH_URL = "https://qianfan.baidubce.com/v2/ai_search/web_search";

    public BaiduSearchService() {
        this.httpClient = new OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(15, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public List<Map<String, String>> searchResources(String query) {
        List<Map<String, String>> results = new ArrayList<>();
        if (searchToken == null || searchToken.isBlank()) {
            log.warn("百度搜索 token 未配置");
            return results;
        }

        try {
            String jsonBody = objectMapper.writeValueAsString(Map.of(
                "messages", List.of(Map.of("role", "user", "content", query + " 学习资源")),
                "edition", "standard",
                "search_source", "baidu_search_v2",
                "search_recency_filter", "month"
            ));

            Request request = new Request.Builder()
                    .url(SEARCH_URL)
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + searchToken)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return results;
                String body = response.body().string();
                JsonNode root = objectMapper.readTree(body);
                // 解析搜索结果: references 数组
                JsonNode searchResults = root.path("references");
                if (!searchResults.isArray()) searchResults = root.path("results");
                if (searchResults.isArray()) {
                    for (JsonNode item : searchResults) {
                        Map<String, String> result = new HashMap<>();
                        result.put("title", item.path("title").asText(""));
                        result.put("url", item.path("url").asText(""));
                        String snippet = item.path("snippet").asText("");
                        if (snippet.isEmpty()) snippet = item.path("content").asText("");
                        result.put("snippet", snippet.substring(0, Math.min(200, snippet.length())));
                        results.add(result);
                        if (results.size() >= 5) break;
                    }
                }
            }
        } catch (IOException e) {
            log.warn("百度搜索失败: {}", e.getMessage());
        }
        return results;
    }
}
