package com.iflytek.smartprep.rag.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.iflytek.smartprep.domain.KnowledgeDoc;
import com.iflytek.smartprep.dto.KnowledgeMapNode;
import com.iflytek.smartprep.mapper.KnowledgeDocMapper;
import com.iflytek.smartprep.rag.model.DocumentChunk;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知识图谱生成服务
 * 利用大模型从文档中提取知识结构并生成思维导图数据
 */
@Service
public class KnowledgeMapService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeMapService.class);

    @Value("${smartprep.llm.base-url:}")
    private String llmApiUrl;

    @Value("${smartprep.llm.api-key:}")
    private String llmApiKey;

    @Value("${smartprep.llm.model:glm-4}")
    private String llmModel;

    private final KnowledgeDocMapper knowledgeDocMapper;
    private final RAGService ragService;
    private final Gson gson;
    private final OkHttpClient httpClient;

    public KnowledgeMapService(KnowledgeDocMapper knowledgeDocMapper, RAGService ragService) {
        this.knowledgeDocMapper = knowledgeDocMapper;
        this.ragService = ragService;
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 生成知识图谱
     *
     * @param documentId 文档ID
     * @param maxDepth 最大深度
     * @return 知识图谱根节点
     */
    public KnowledgeMapNode generateKnowledgeMap(Long documentId, Integer maxDepth) {
        log.info("开始生成知识图谱，文档ID: {}, 最大深度: {}", documentId, maxDepth);

        // 1. 获取文档信息
        KnowledgeDoc doc = knowledgeDocMapper.selectById(documentId);
        if (doc == null) {
            throw new RuntimeException("文档不存在: " + documentId);
        }

        // 2. 获取文档的所有分块
        List<DocumentChunk> chunks = ragService.getDocumentChunks(documentId);
        if (chunks.isEmpty()) {
            throw new RuntimeException("文档未进行向量化处理");
        }

        // 3. 构建文档摘要（取前10个chunk）
        StringBuilder contentSummary = new StringBuilder();
        int chunkLimit = Math.min(10, chunks.size());
        for (int i = 0; i < chunkLimit; i++) {
            contentSummary.append(chunks.get(i).getContent()).append("\n\n");
        }

        // 4. 调用大模型提取知识结构
        String prompt = buildKnowledgeExtractionPrompt(doc.getTitle(), contentSummary.toString(), maxDepth);
        String llmResponse = callLLM(prompt);

        // 5. 解析大模型返回的JSON
        KnowledgeMapNode rootNode = parseKnowledgeMapFromLLM(llmResponse, doc.getTitle());

        log.info("知识图谱生成完成，根节点: {}", rootNode.getName());
        return rootNode;
    }

    /**
     * 构建知识提取提示词
     */
    private String buildKnowledgeExtractionPrompt(String title, String content, Integer maxDepth) {
        return String.format("""
                你是一位专业的知识结构分析专家。请分析以下文档内容，提取其核心知识结构，并以JSON格式输出思维导图数据。

                文档标题：%s

                文档内容摘要：
                %s

                要求：
                1. 提取文档的章节结构和核心知识点
                2. 最多生成 %d 层深度的知识树
                3. 每个节点包含：id、name、type（root/chapter/section/concept）、description、importance（1-5）、children
                4. 必须严格按照以下JSON格式输出，不要添加任何其他文字：

                {
                  "id": "root",
                  "name": "文档标题",
                  "type": "root",
                  "description": "文档简介",
                  "importance": 5,
                  "children": [
                    {
                      "id": "chapter1",
                      "name": "第一章标题",
                      "type": "chapter",
                      "description": "章节描述",
                      "importance": 4,
                      "children": [
                        {
                          "id": "section1-1",
                          "name": "小节标题",
                          "type": "section",
                          "description": "小节描述",
                          "importance": 3,
                          "children": []
                        }
                      ]
                    }
                  ]
                }

                请直接输出JSON，不要包含任何markdown标记或其他说明文字。
                """, title, content, maxDepth);
    }

    /**
     * 调用大模型API
     */
    private String callLLM(String prompt) {
        try {
            // 构建请求体（适配智谱GLM格式）
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", llmModel);

            JsonArray messages = new JsonArray();
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);
            messages.add(message);
            requestBody.add("messages", messages);

            // 添加参数
            requestBody.addProperty("temperature", 0.3);
            requestBody.addProperty("max_tokens", 4000);

            RequestBody body = RequestBody.create(
                    gson.toJson(requestBody),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(llmApiUrl)
                    .addHeader("Authorization", "Bearer " + llmApiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("LLM API调用失败: " + response.code());
                }

                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                // 解析响应（适配智谱GLM格式）
                if (jsonResponse.has("choices")) {
                    JsonArray choices = jsonResponse.getAsJsonArray("choices");
                    if (choices.size() > 0) {
                        JsonObject choice = choices.get(0).getAsJsonObject();
                        JsonObject messageObj = choice.getAsJsonObject("message");
                        return messageObj.get("content").getAsString();
                    }
                }

                throw new RuntimeException("LLM返回格式异常");
            }
        } catch (Exception e) {
            log.error("调用LLM失败", e);
            throw new RuntimeException("知识图谱生成失败: " + e.getMessage());
        }
    }

    /**
     * 解析大模型返回的知识图谱JSON
     */
    private KnowledgeMapNode parseKnowledgeMapFromLLM(String llmResponse, String docTitle) {
        try {
            // 清理可能的markdown代码块标记
            String cleanedJson = llmResponse.trim();
            if (cleanedJson.startsWith("```json")) {
                cleanedJson = cleanedJson.substring(7);
            }
            if (cleanedJson.startsWith("```")) {
                cleanedJson = cleanedJson.substring(3);
            }
            if (cleanedJson.endsWith("```")) {
                cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
            }
            cleanedJson = cleanedJson.trim();

            // 解析JSON
            KnowledgeMapNode rootNode = gson.fromJson(cleanedJson, KnowledgeMapNode.class);

            // 验证并修复根节点
            if (rootNode == null) {
                rootNode = createFallbackKnowledgeMap(docTitle);
            }
            if (rootNode.getName() == null || rootNode.getName().isEmpty()) {
                rootNode.setName(docTitle);
            }
            if (rootNode.getType() == null) {
                rootNode.setType("root");
            }
            if (rootNode.getChildren() == null) {
                rootNode.setChildren(new ArrayList<>());
            }

            return rootNode;
        } catch (Exception e) {
            log.error("解析知识图谱JSON失败，使用降级方案", e);
            return createFallbackKnowledgeMap(docTitle);
        }
    }

    /**
     * 创建降级知识图谱（当LLM调用失败时）
     */
    private KnowledgeMapNode createFallbackKnowledgeMap(String docTitle) {
        KnowledgeMapNode root = KnowledgeMapNode.builder()
                .id("root")
                .name(docTitle)
                .type("root")
                .description("文档知识结构")
                .importance(5)
                .children(new ArrayList<>())
                .build();

        // 添加示例章节
        KnowledgeMapNode chapter1 = KnowledgeMapNode.builder()
                .id("chapter1")
                .name("核心概念")
                .type("chapter")
                .description("文档的核心知识点")
                .importance(4)
                .children(new ArrayList<>())
                .build();

        KnowledgeMapNode chapter2 = KnowledgeMapNode.builder()
                .id("chapter2")
                .name("实践应用")
                .type("chapter")
                .description("知识的实际应用")
                .importance(3)
                .children(new ArrayList<>())
                .build();

        root.getChildren().add(chapter1);
        root.getChildren().add(chapter2);

        return root;
    }
}
