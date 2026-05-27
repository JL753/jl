package com.iflytek.smartprep.rag.service;

import com.iflytek.smartprep.domain.KnowledgeDoc;
import com.iflytek.smartprep.mapper.KnowledgeDocMapper;
import com.iflytek.smartprep.rag.model.DocumentChunk;
import com.iflytek.smartprep.rag.model.RetrievalResult;
import com.iflytek.smartprep.rag.util.DocumentChunker;
import com.iflytek.smartprep.rag.util.DocumentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 服务层
 * 整合文档解析、分块、向量化和检索功能
 */
@Service
public class RAGService {

    private static final Logger log = LoggerFactory.getLogger(RAGService.class);

    private final DocumentParser documentParser;
    private final DocumentChunker documentChunker;
    private final VectorStoreService vectorStoreService;
    private final KnowledgeDocMapper knowledgeDocMapper;

    public RAGService(DocumentParser documentParser,
                     DocumentChunker documentChunker,
                     VectorStoreService vectorStoreService,
                     KnowledgeDocMapper knowledgeDocMapper) {
        this.documentParser = documentParser;
        this.documentChunker = documentChunker;
        this.vectorStoreService = vectorStoreService;
        this.knowledgeDocMapper = knowledgeDocMapper;
    }

    /**
     * 上传并处理文档
     *
     * @param file 上传的文件
     * @param course 课程名称
     * @param tag 标签
     * @return 文档ID
     */
    public Long uploadAndProcessDocument(MultipartFile file, String course, String tag) throws IOException {
        log.info("开始处理文档: {}, 课程: {}, 标签: {}", file.getOriginalFilename(), course, tag);

        // 1. 解析文档
        DocumentParser.ParseResult parseResult = documentParser.parseDocument(file);
        String content = parseResult.getContent();
        String title = parseResult.getFilename();

        log.info("文档解析完成，内容长度: {} 字符", content.length());

        // 2. 保存到数据库
        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setId(System.currentTimeMillis()); // 简单的ID生成策略
        doc.setCourse(course);
        doc.setTitle(title);
        doc.setContent(content);
        doc.setTag(tag);

        knowledgeDocMapper.insert(doc);
        log.info("文档已保存到数据库，ID: {}", doc.getId());

        // 3. 文档分块
        List<DocumentChunk> chunks = documentChunker.chunkDocument(
            doc.getId(),
            title,
            course,
            content,
            tag
        );

        log.info("文档分块完成，共 {} 个块", chunks.size());

        // 4. 向量化并存储
        int addedCount = vectorStoreService.addDocumentChunks(chunks);
        log.info("向量化完成，成功添加 {} 个块到向量数据库", addedCount);

        return doc.getId();
    }

    /**
     * 基于问题检索相关文档
     *
     * @param question 用户问题
     * @param course 课程名称（可选，用于过滤）
     * @param topK 返回前K个结果
     * @return 检索结果列表
     */
    public List<RetrievalResult> retrieveRelevantDocuments(String question, String course, int topK) {
        log.info("检索相关文档，问题: {}, 课程: {}, topK: {}", question, course, topK);

        try {
            List<DocumentChunk> chunks = vectorStoreService.searchSimilarChunks(question, topK, course);

            // 转换 DocumentChunk 为 RetrievalResult
            List<RetrievalResult> results = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = chunks.get(i);
                results.add(new RetrievalResult(chunk, 0.0, i + 1));
            }

            log.info("检索完成，找到 {} 个相关文档块", results.size());
            return results;
        } catch (Exception e) {
            log.error("检索文档失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 构建 RAG 上下文
     * 将检索到的文档块拼接成上下文字符串
     *
     * @param results 检索结果
     * @return 上下文字符串
     */
    public String buildRAGContext(List<RetrievalResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("以下是从知识库中检索到的相关内容：\n\n");

        for (int i = 0; i < results.size(); i++) {
            RetrievalResult result = results.get(i);
            context.append("【参考资料 ").append(i + 1).append("】\n");
            context.append(result.getContextSummary());
            context.append("\n\n");
        }

        context.append("请基于以上参考资料回答用户问题，并在回答中标注引用来源。");

        return context.toString();
    }

    /**
     * 提取引用信息
     * 从检索结果中提取引用标签列表
     *
     * @param results 检索结果
     * @return 引用标签列表
     */
    public List<String> extractCitations(List<RetrievalResult> results) {
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }

        return results.stream()
            .map(RetrievalResult::getFormattedCitation)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * 删除文档及其所有分块
     *
     * @param documentId 文档ID
     */
    public void deleteDocument(Long documentId) {
        log.info("删除文档: {}", documentId);

        try {
            // 从向量数据库删除
            vectorStoreService.deleteDocumentChunks(documentId);

            // 从MySQL删除
            knowledgeDocMapper.deleteById(documentId);

            log.info("文档删除成功: {}", documentId);
        } catch (Exception e) {
            log.error("删除文档失败: {}", documentId, e);
            throw new RuntimeException("删除文档失败", e);
        }
    }

    /**
     * 重新索引所有文档
     * 用于数据迁移或重建索引
     */
    public void reindexAllDocuments() {
        log.info("开始重新索引所有文档");

        try {
            // 获取所有文档
            List<KnowledgeDoc> allDocs = knowledgeDocMapper.selectList(null);
            log.info("找到 {} 个文档需要重新索引", allDocs.size());

            int successCount = 0;
            for (KnowledgeDoc doc : allDocs) {
                try {
                    // 分块
                    List<DocumentChunk> chunks = documentChunker.chunkDocument(
                        doc.getId(),
                        doc.getTitle(),
                        doc.getCourse(),
                        doc.getContent(),
                        doc.getTag()
                    );

                    // 向量化
                    vectorStoreService.addDocumentChunks(chunks);
                    successCount++;
                } catch (Exception e) {
                    log.error("重新索引文档失败: {}", doc.getId(), e);
                }
            }

            log.info("重新索引完成，成功: {}, 失败: {}", successCount, allDocs.size() - successCount);
        } catch (Exception e) {
            log.error("重新索引所有文档失败", e);
            throw new RuntimeException("重新索引失败", e);
        }
    }

    /**
     * 获取文档的所有分块（用于知识图谱生成）
     *
     * @param documentId 文档ID
     * @return 文档分块列表
     */
    public List<DocumentChunk> getDocumentChunks(Long documentId) {
        log.info("获取文档分块: {}", documentId);

        try {
            KnowledgeDoc doc = knowledgeDocMapper.selectById(documentId);
            if (doc == null) {
                throw new RuntimeException("文档不存在: " + documentId);
            }

            // 重新分块（因为向量数据库中的chunk可能不完整）
            List<DocumentChunk> chunks = documentChunker.chunkDocument(
                doc.getId(),
                doc.getTitle(),
                doc.getCourse(),
                doc.getContent(),
                doc.getTag()
            );

            log.info("获取到 {} 个文档分块", chunks.size());
            return chunks;
        } catch (Exception e) {
            log.error("获取文档分块失败: {}", documentId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取文档统计信息
     */
    public DocumentStats getDocumentStats() {
        try {
            Long totalDocsLong = knowledgeDocMapper.selectCount(null);
            int totalDocs = totalDocsLong != null ? totalDocsLong.intValue() : 0;
            int totalChunks = 0; // 暂时返回0，因为VectorStoreService没有getCollectionSize方法

            return new DocumentStats(totalDocs, totalChunks);
        } catch (Exception e) {
            log.error("获取文档统计信息失败", e);
            return new DocumentStats(0, 0);
        }
    }

    /**
     * 文档统计信息
     */
    public static class DocumentStats {
        private final int totalDocuments;
        private final int totalChunks;

        public DocumentStats(int totalDocuments, int totalChunks) {
            this.totalDocuments = totalDocuments;
            this.totalChunks = totalChunks;
        }

        public int getTotalDocuments() {
            return totalDocuments;
        }

        public int getTotalChunks() {
            return totalChunks;
        }
    }
}
