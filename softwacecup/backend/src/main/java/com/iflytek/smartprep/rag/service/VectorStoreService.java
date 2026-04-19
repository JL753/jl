package com.iflytek.smartprep.rag.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.iflytek.smartprep.rag.model.DocumentChunk;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 向量数据库服务
 * 集成 Chroma 向量数据库，支持文档向量化存储和相似度检索
 *
 * 部署说明：
 * 1. 使用 Docker 启动 Chroma：
 *    docker run -d -p 8000:8000 chromadb/chroma
 *
 * 2. 或使用 Python 启动：
 *    pip install chromadb
 *    chroma run --host 0.0.0.0 --port 8000
 */
@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    @Value("${smartprep.rag.chroma.url:http://localhost:8000}")
    private String chromaUrl;

    @Value("${smartprep.rag.chroma.collection:smartprep_knowledge}")
    private String collectionName;

    @Value("${smartprep.rag.embedding.api-key:}")
    private String embeddingApiKey;

    @Value("${smartprep.rag.embedding.base-url:}")
    private String embeddingBaseUrl;

    @Value("${smartprep.rag.embedding.model:text-embedding-ada-002}")
    private String embeddingModel;

    private final OkHttpClient httpClient;
    private final Gson gson;

    public VectorStoreService() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
    }

    /**
     * 初始化集合（如果不存在则创建）
     */
    public void initializeCollection() {
        try {
            // 检查集合是否存在
            if (!collectionExists()) {
                createCollection();
                log.info("向量数据库集合已创建: {}", collectionName);
            } else {
                log.info("向量数据库集合已存在: {}", collectionName);
            }
        } catch (Exception e) {
            log.error("初始化向量数据库集合失败", e);
        }
    }

    /**
     * 检查集合是否存在
     */
    private boolean collectionExists() throws IOException {
        Request request = new Request.Builder()
            .url(chromaUrl + "/api/v1/collections/" + collectionName)
            .get()
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    /**
     * 创建集合
     */
    private void createCollection() throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("name", collectionName);
        body.addProperty("metadata", gson.toJson(Map.of("description", "SmartPrep Knowledge Base")));

        RequestBody requestBody = RequestBody.create(
            body.toString(),
            MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
            .url(chromaUrl + "/api/v1/collections")
            .post(requestBody)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("创建集合失败: " + response.body().string());
            }
        }
    }

    /**
     * 添加文档块到向量数据库
     *
     * @param chunks 文档块列表
     * @return 成功添加的数量
     */
    public int addDocumentChunks(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return 0;
        }

        try {
            // 批量生成向量
            List<float[]> embeddings = batchGenerateEmbeddings(
                chunks.stream().map(DocumentChunk::getContent).toList()
            );

            // 构建 Chroma 请求
            JsonObject body = new JsonObject();

            JsonArray ids = new JsonArray();
            JsonArray documents = new JsonArray();
            JsonArray metadatas = new JsonArray();
            JsonArray embeddingsArray = new JsonArray();

            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = chunks.get(i);
                float[] embedding = embeddings.get(i);

                // 生成唯一ID
                String id = generateChunkId(chunk);
                chunk.setVectorId(id);

                ids.add(id);
                documents.add(chunk.getContent());

                // 元数据
                JsonObject metadata = new JsonObject();
                metadata.addProperty("documentId", chunk.getDocumentId());
                metadata.addProperty("documentTitle", chunk.getDocumentTitle());
                metadata.addProperty("course", chunk.getCourse());
                metadata.addProperty("chunkIndex", chunk.getChunkIndex());
                if (chunk.getChapter() != null) {
                    metadata.addProperty("chapter", chunk.getChapter());
                }
                if (chunk.getPageNumber() != null) {
                    metadata.addProperty("pageNumber", chunk.getPageNumber());
                }
                if (chunk.getTag() != null) {
                    metadata.addProperty("tag", chunk.getTag());
                }
                metadatas.add(metadata);

                // 向量
                JsonArray embeddingArray = new JsonArray();
                for (float value : embedding) {
                    embeddingArray.add(value);
                }
                embeddingsArray.add(embeddingArray);
            }

            body.add("ids", ids);
            body.add("documents", documents);
            body.add("metadatas", metadatas);
            body.add("embeddings", embeddingsArray);

            // 发送请求
            RequestBody requestBody = RequestBody.create(
                body.toString(),
                MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                .url(chromaUrl + "/api/v1/collections/" + collectionName + "/add")
                .post(requestBody)
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.info("成功添加 {} 个文档块到向量数据库", chunks.size());
                    return chunks.size();
                } else {
                    log.error("添加文档块失败: {}", response.body().string());
                    return 0;
                }
            }

        } catch (Exception e) {
            log.error("添加文档块到向量数据库失败", e);
            return 0;
        }
    }

    /**
     * 检索相关文档块
     *
     * @param query 查询文本
     * @param topK 返回前K个结果
     * @param course 课程过滤（可选）
     * @return 相关文档块列表
     */
    public List<DocumentChunk> searchSimilarChunks(String query, int topK, String course) {
        try {
            // 生成查询向量
            float[] queryEmbedding = generateEmbedding(query);

            // 构建查询请求
            JsonObject body = new JsonObject();

            JsonArray queryEmbeddings = new JsonArray();
            JsonArray embeddingArray = new JsonArray();
            for (float value : queryEmbedding) {
                embeddingArray.add(value);
            }
            queryEmbeddings.add(embeddingArray);
            body.add("query_embeddings", queryEmbeddings);
            body.addProperty("n_results", topK);

            // 添加过滤条件
            if (course != null && !course.isEmpty()) {
                JsonObject where = new JsonObject();
                where.addProperty("course", course);
                body.add("where", where);
            }

            RequestBody requestBody = RequestBody.create(
                body.toString(),
                MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                .url(chromaUrl + "/api/v1/collections/" + collectionName + "/query")
                .post(requestBody)
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("检索失败: {}", response.body().string());
                    return new ArrayList<>();
                }

                String responseBody = response.body().string();
                return parseSearchResults(responseBody);
            }

        } catch (Exception e) {
            log.error("检索相关文档块失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 生成单个文本的向量
     */
    private float[] generateEmbedding(String text) throws IOException {
        List<float[]> embeddings = batchGenerateEmbeddings(List.of(text));
        return embeddings.isEmpty() ? new float[0] : embeddings.get(0);
    }

    /**
     * 批量生成文本向量
     * 支持 OpenAI API 兼容的 Embedding 服务
     */
    private List<float[]> batchGenerateEmbeddings(List<String> texts) throws IOException {
        // 如果没有配置 Embedding API，使用模拟向量
        if (embeddingApiKey == null || embeddingApiKey.isEmpty() ||
            embeddingBaseUrl == null || embeddingBaseUrl.isEmpty()) {
            log.warn("未配置 Embedding API，使用模拟向量");
            return generateMockEmbeddings(texts);
        }

        try {
            String url = embeddingBaseUrl.endsWith("/") ?
                embeddingBaseUrl + "embeddings" :
                embeddingBaseUrl + "/embeddings";

            JsonObject body = new JsonObject();
            body.addProperty("model", embeddingModel);

            JsonArray input = new JsonArray();
            for (String text : texts) {
                // 截断过长文本
                String truncated = text.length() > 8000 ? text.substring(0, 8000) : text;
                input.add(truncated);
            }
            body.add("input", input);

            RequestBody requestBody = RequestBody.create(
                body.toString(),
                MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + embeddingApiKey)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("生成向量失败: {}", response.body().string());
                    return generateMockEmbeddings(texts);
                }

                String responseBody = response.body().string();
                return parseEmbeddingResponse(responseBody);
            }

        } catch (Exception e) {
            log.error("调用 Embedding API 失败，使用模拟向量", e);
            return generateMockEmbeddings(texts);
        }
    }

    /**
     * 解析 Embedding API 响应
     */
    private List<float[]> parseEmbeddingResponse(String responseBody) {
        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
        JsonArray data = json.getAsJsonArray("data");

        List<float[]> embeddings = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            JsonObject item = data.get(i).getAsJsonObject();
            JsonArray embeddingArray = item.getAsJsonArray("embedding");

            float[] embedding = new float[embeddingArray.size()];
            for (int j = 0; j < embeddingArray.size(); j++) {
                embedding[j] = embeddingArray.get(j).getAsFloat();
            }
            embeddings.add(embedding);
        }

        return embeddings;
    }

    /**
     * 生成模拟向量（用于演示和测试）
     * 基于文本哈希生成确定性向量
     */
    private List<float[]> generateMockEmbeddings(List<String> texts) {
        List<float[]> embeddings = new ArrayList<>();
        Random random = new Random();

        for (String text : texts) {
            // 使用文本哈希作为随机种子，确保相同文本生成相同向量
            random.setSeed(text.hashCode());

            float[] embedding = new float[384]; // 使用384维向量（常见的小型模型维度）
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] = (float) (random.nextGaussian() * 0.1);
            }

            // 归一化
            float norm = 0;
            for (float v : embedding) {
                norm += v * v;
            }
            norm = (float) Math.sqrt(norm);
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] /= norm;
            }

            embeddings.add(embedding);
        }

        return embeddings;
    }

    /**
     * 解析检索结果
     */
    private List<DocumentChunk> parseSearchResults(String responseBody) {
        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
        JsonArray ids = json.getAsJsonArray("ids").get(0).getAsJsonArray();
        JsonArray documents = json.getAsJsonArray("documents").get(0).getAsJsonArray();
        JsonArray metadatas = json.getAsJsonArray("metadatas").get(0).getAsJsonArray();
        JsonArray distances = json.getAsJsonArray("distances").get(0).getAsJsonArray();

        List<DocumentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            JsonObject metadata = metadatas.get(i).getAsJsonObject();

            DocumentChunk chunk = DocumentChunk.builder()
                .vectorId(ids.get(i).getAsString())
                .content(documents.get(i).getAsString())
                .documentId(metadata.get("documentId").getAsLong())
                .documentTitle(metadata.get("documentTitle").getAsString())
                .course(metadata.get("course").getAsString())
                .chunkIndex(metadata.get("chunkIndex").getAsInt())
                .chapter(metadata.has("chapter") ? metadata.get("chapter").getAsString() : null)
                .pageNumber(metadata.has("pageNumber") ? metadata.get("pageNumber").getAsInt() : null)
                .tag(metadata.has("tag") ? metadata.get("tag").getAsString() : null)
                .build();

            chunks.add(chunk);
        }

        return chunks;
    }

    /**
     * 生成文档块唯一ID
     */
    private String generateChunkId(DocumentChunk chunk) {
        return String.format("doc_%d_chunk_%d_%d",
            chunk.getDocumentId(),
            chunk.getChunkIndex(),
            System.currentTimeMillis());
    }

    /**
     * 删除文档的所有块
     */
    public void deleteDocumentChunks(Long documentId) {
        try {
            JsonObject body = new JsonObject();
            JsonObject where = new JsonObject();
            where.addProperty("documentId", documentId);
            body.add("where", where);

            RequestBody requestBody = RequestBody.create(
                body.toString(),
                MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                .url(chromaUrl + "/api/v1/collections/" + collectionName + "/delete")
                .post(requestBody)
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.info("成功删除文档 {} 的所有块", documentId);
                } else {
                    log.error("删除文档块失败: {}", response.body().string());
                }
            }

        } catch (Exception e) {
            log.error("删除文档块失败", e);
        }
    }
}
