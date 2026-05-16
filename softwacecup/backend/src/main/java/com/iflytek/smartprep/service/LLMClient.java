package com.iflytek.smartprep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 统一 LLM 调用客户端
 * 支持 OpenAI 兼容 API（DeepSeek / GLM / 等）
 */
@Service
public class LLMClient {

    private static final Logger log = LoggerFactory.getLogger(LLMClient.class);

    @Value("${smartprep.llm.api-key:}")
    private String apiKey;

    @Value("${smartprep.llm.base-url:}")
    private String baseUrl;

    @Value("${smartprep.llm.model:deepseek-v4-flash}")
    private String defaultModel;

    @Value("${smartprep.llm.temperature:0.7}")
    private double defaultTemperature;

    @Value("${smartprep.llm.max-tokens:4096}")
    private int defaultMaxTokens;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LLMClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // ==================== 非流式调用 ====================

    /**
     * 简单对话（仅用户消息）
     */
    public String chat(String userPrompt) {
        return chat(null, userPrompt, defaultModel, defaultTemperature, defaultMaxTokens);
    }

    /**
     * 系统提示 + 用户消息
     */
    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, defaultModel, defaultTemperature, defaultMaxTokens);
    }

    /**
     * 完整参数对话
     */
    public String chat(String systemPrompt, String userPrompt, String model, double temperature, int maxTokens) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userPrompt != null ? userPrompt : ""));
        return chatMessages(messages, model, temperature, maxTokens);
    }

    /**
     * 多轮对话（自定义 messages 列表）
     */
    public String chatMessages(List<Map<String, String>> messages) {
        return chatMessages(messages, defaultModel, defaultTemperature, defaultMaxTokens);
    }

    /**
     * 多轮对话 + 完整参数
     */
    public String chatMessages(List<Map<String, String>> messages, String model, double temperature, int maxTokens) {
        if (apiKey == null || apiKey.isBlank() || baseUrl == null || baseUrl.isBlank()) {
            log.warn("LLM API 未配置 (apiKey/baseUrl 为空)，返回 null");
            return null;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", messages);
            body.put("temperature", temperature);
            body.put("max_tokens", maxTokens);
            body.put("stream", false);

            String json = objectMapper.writeValueAsString(body);
            Request request = new Request.Builder()
                    .url(buildUrl())
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.parse("application/json; charset=utf-8")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "";
                    log.error("LLM API 返回错误 {}: {}", response.code(), errBody);
                    return null;
                }

                String respBody = response.body().string();
                JsonNode root = objectMapper.readTree(respBody);
                JsonNode content = root.path("choices").path(0).path("message").path("content");
                return content.isMissingNode() ? null : content.asText();
            }
        } catch (Exception e) {
            log.error("LLM 调用失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 流式调用（SSE） ====================

    /**
     * 流式对话，通过 Consumer 回调每个 delta chunk
     * @return 拼接后的完整文本
     */
    public String chatStream(String systemPrompt, String userPrompt, Consumer<String> onChunk) {
        return chatStream(systemPrompt, userPrompt, defaultModel, defaultTemperature, defaultMaxTokens, onChunk);
    }

    /**
     * 流式对话 + 完整参数
     */
    public String chatStream(String systemPrompt, String userPrompt,
                             String model, double temperature, int maxTokens,
                             Consumer<String> onChunk) {
        if (apiKey == null || apiKey.isBlank() || baseUrl == null || baseUrl.isBlank()) {
            log.warn("LLM API 未配置，无法流式调用");
            return null;
        }

        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userPrompt != null ? userPrompt : ""));

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", messages);
            body.put("temperature", temperature);
            body.put("max_tokens", maxTokens);
            body.put("stream", true);

            String json = objectMapper.writeValueAsString(body);
            Request request = new Request.Builder()
                    .url(buildUrl())
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.parse("application/json; charset=utf-8")))
                    .build();

            StringBuilder fullText = new StringBuilder();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("LLM 流式调用返回错误 {}", response.code());
                    return null;
                }

                ResponseBody responseBody = response.body();
                if (responseBody == null) return null;

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(responseBody.byteStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if ("[DONE]".equals(data)) break;

                            try {
                                JsonNode root = objectMapper.readTree(data);
                                JsonNode delta = root.path("choices").path(0).path("delta").path("content");
                                if (!delta.isMissingNode()) {
                                    String chunk = delta.asText();
                                    fullText.append(chunk);
                                    if (onChunk != null) onChunk.accept(chunk);
                                }
                            } catch (Exception ignored) {
                                // 跳过解析失败的行
                            }
                        }
                    }
                }
            }
            return fullText.toString();
        } catch (Exception e) {
            log.error("LLM 流式调用失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== JSON 模式 ====================

    /**
     * 调用 LLM 并解析为 JSON 对象
     */
    public <T> T chatForJson(String systemPrompt, String userPrompt, Class<T> clazz) {
        String text = chat(systemPrompt, userPrompt);
        if (text == null) return null;
        try {
            // 尝试提取 JSON（可能被 markdown 代码块包裹）
            String json = extractJson(text);
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("解析 LLM JSON 响应失败: {}", e.getMessage());
            log.debug("原始响应: {}", text);
            return null;
        }
    }

    // ==================== 工具方法 ====================

    private String buildUrl() {
        String url = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        if (url.endsWith("/chat/completions") || url.endsWith("/chat/completions/")) {
            return url.replaceAll("/$", "");
        }
        return url + "chat/completions";
    }

    /**
     * 从 LLM 响应中提取 JSON（去除 markdown 代码块包裹）
     */
    public static String extractJson(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        // 去除 ```json ... ``` 包裹
        if (trimmed.startsWith("```json")) {
            int start = trimmed.indexOf("\n") + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) return trimmed.substring(start, end).trim();
        }
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf("\n") + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) return trimmed.substring(start, end).trim();
        }
        // 尝试找到 JSON 对象的起止位置
        int braceStart = trimmed.indexOf('{');
        int braceEnd = trimmed.lastIndexOf('}');
        if (braceStart >= 0 && braceEnd > braceStart) {
            return trimmed.substring(braceStart, braceEnd + 1);
        }
        int bracketStart = trimmed.indexOf('[');
        int bracketEnd = trimmed.lastIndexOf(']');
        if (bracketStart >= 0 && bracketEnd > bracketStart) {
            return trimmed.substring(bracketStart, bracketEnd + 1);
        }
        return trimmed;
    }
}
