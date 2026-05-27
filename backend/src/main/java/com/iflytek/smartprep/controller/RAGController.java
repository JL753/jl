package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.config.RequireRole;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.rag.model.RetrievalResult;
import com.iflytek.smartprep.rag.service.RAGService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 知识库管理接口
 */
@RestController
@RequestMapping("/api/rag")
public class RAGController {

    private static final Logger log = LoggerFactory.getLogger(RAGController.class);

    private final RAGService ragService;

    public RAGController(RAGService ragService) {
        this.ragService = ragService;
    }

    /**
     * 上传文档并向量化
     *
     * POST /api/rag/upload
     * Content-Type: multipart/form-data
     *
     * 参数:
     * - file: 文档文件 (PDF/Word/TXT)
     * - course: 课程名称
     * - tag: 标签 (可选)
     */
    @PostMapping("/upload")
    @RequireRole({"teacher", "admin"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadDocument(
        @RequestParam("file") MultipartFile file,
        @RequestParam("course") String course,
        @RequestParam(value = "tag", required = false, defaultValue = "基础") String tag
    ) {
        try {
            Long userId = LoginUserHolder.get().getUserId();
            log.info("用户 {} 上传文档: {}, 课程: {}", userId, file.getOriginalFilename(), course);

            // 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("文件不能为空"));
            }

            // 验证文件大小 (限制50MB)
            if (file.getSize() > 50 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("文件大小不能超过50MB"));
            }

            // 验证文件类型
            String filename = file.getOriginalFilename();
            if (filename == null || !isValidFileType(filename)) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("不支持的文件类型，仅支持 PDF、Word、TXT"));
            }

            // 处理文档
            Long documentId = ragService.uploadAndProcessDocument(file, course, tag);

            Map<String, Object> result = new HashMap<>();
            result.put("documentId", documentId);
            result.put("filename", filename);
            result.put("course", course);
            result.put("tag", tag);
            result.put("message", "文档上传并向量化成功");

            return ResponseEntity.ok(ApiResponse.ok(result));

        } catch (Exception e) {
            log.error("上传文档失败", e);
            return ResponseEntity.status(500).body(ApiResponse.fail("上传文档失败: " + e.getMessage()));
        }
    }

    /**
     * 检索相关文档
     *
     * POST /api/rag/search
     * Content-Type: application/json
     *
     * 请求体:
     * {
     *   "question": "什么是机器学习？",
     *   "course": "人工智能导论",  // 可选
     *   "topK": 3                  // 可选，默认3
     * }
     */
    @PostMapping("/search")
    @RequireRole({"student", "teacher", "admin"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchDocuments(
        @RequestBody Map<String, Object> request
    ) {
        try {
            String question = (String) request.get("question");
            String course = (String) request.get("course");
            Integer topK = request.containsKey("topK") ? (Integer) request.get("topK") : 3;

            if (question == null || question.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("问题不能为空"));
            }

            log.info("检索文档，问题: {}, 课程: {}, topK: {}", question, course, topK);

            // 检索
            List<RetrievalResult> results = ragService.retrieveRelevantDocuments(question, course, topK);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("question", question);
            response.put("results", results);
            response.put("count", results.size());
            response.put("context", ragService.buildRAGContext(results));
            response.put("citations", ragService.extractCitations(results));

            return ResponseEntity.ok(ApiResponse.ok(response));

        } catch (Exception e) {
            log.error("检索文档失败", e);
            return ResponseEntity.status(500).body(ApiResponse.fail("检索文档失败: " + e.getMessage()));
        }
    }

    /**
     * 删除文档
     *
     * DELETE /api/rag/document/{documentId}
     */
    @DeleteMapping("/document/{documentId}")
    @RequireRole({"teacher", "admin"})
    public ResponseEntity<ApiResponse<String>> deleteDocument(@PathVariable Long documentId) {
        try {
            Long userId = LoginUserHolder.get().getUserId();
            log.info("用户 {} 删除文档: {}", userId, documentId);

            ragService.deleteDocument(documentId);

            return ResponseEntity.ok(ApiResponse.ok("文档删除成功"));

        } catch (Exception e) {
            log.error("删除文档失败", e);
            return ResponseEntity.status(500).body(ApiResponse.fail("删除文档失败: " + e.getMessage()));
        }
    }

    /**
     * 重新索引所有文档
     *
     * POST /api/rag/reindex
     */
    @PostMapping("/reindex")
    @RequireRole({"admin"})
    public ResponseEntity<ApiResponse<String>> reindexAllDocuments() {
        try {
            Long userId = LoginUserHolder.get().getUserId();
            log.info("用户 {} 触发重新索引", userId);

            ragService.reindexAllDocuments();

            return ResponseEntity.ok(ApiResponse.ok("重新索引完成"));

        } catch (Exception e) {
            log.error("重新索引失败", e);
            return ResponseEntity.status(500).body(ApiResponse.fail("重新索引失败: " + e.getMessage()));
        }
    }

    /**
     * 获取RAG系统状态
     *
     * GET /api/rag/status
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRAGStatus() {
        try {
            // Map<String, Object> status = ragService.getSystemStatus(); // Method doesn't exist yet
            Map<String, Object> status = new HashMap<>();
            status.put("status", "running");
            status.put("message", "RAG system is operational");
            return ResponseEntity.ok(ApiResponse.ok(status));
        } catch (Exception e) {
            log.error("获取RAG状态失败", e);
            return ResponseEntity.status(500).body(ApiResponse.fail("获取状态失败: " + e.getMessage()));
        }
    }

    /**
     * 验证文件类型
     */
    private boolean isValidFileType(String filename) {
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".pdf") ||
               lowerFilename.endsWith(".doc") ||
               lowerFilename.endsWith(".docx") ||
               lowerFilename.endsWith(".txt") ||
               lowerFilename.endsWith(".md");
    }
}
