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
                // 解析搜索结果
                JsonNode searchResults = root.path("results");
                if (searchResults.isArray()) {
                    for (JsonNode item : searchResults) {
                        Map<String, String> result = new HashMap<>();
                        result.put("title", item.path("title").asText(""));
                        result.put("url", item.path("url").asText(""));
                        result.put("snippet", item.path("snippet").asText("").substring(0, Math.min(200, item.path("snippet").asText("").length())));
                        results.add(result);
                        if (results.size() >= 5) break;
                    }
                }
                // 备用解析: choices[0].message.content
                if (results.isEmpty()) {
                    JsonNode choices = root.path("choices");
                    if (choices.isArray() && choices.size() > 0) {
                        String content = choices.get(0).path("message").path("content").asText("");
                        // 从AI回复中提取链接
                        String[] lines = content.split("\n");
                        for (String line : lines) {
                            if (line.contains("http") && results.size() < 5) {
                                Map<String, String> result = new HashMap<>();
                                result.put("title", line.replaceAll("https?://\\S+", "").trim());
                                result.put("url", line.replaceAll(".*?(https?://\\S+).*", "$1"));
                                result.put("snippet", "");
                                results.add(result);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.warn("百度搜索失败: {}", e.getMessage());
        }
        return results;
    }
}
