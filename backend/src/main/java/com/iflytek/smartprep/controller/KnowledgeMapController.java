package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.dto.KnowledgeMapNode;
import com.iflytek.smartprep.dto.KnowledgeMapRequest;
import com.iflytek.smartprep.rag.service.KnowledgeMapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 知识图谱控制器
 */
@RestController
@RequestMapping("/api/knowledge-map")
public class KnowledgeMapController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeMapController.class);

    private final KnowledgeMapService knowledgeMapService;

    public KnowledgeMapController(KnowledgeMapService knowledgeMapService) {
        this.knowledgeMapService = knowledgeMapService;
    }

    /**
     * 生成知识图谱
     *
     * @param request 请求参数
     * @return 知识图谱根节点
     */
    @PostMapping("/generate")
    public ResponseEntity<KnowledgeMapNode> generateKnowledgeMap(@RequestBody KnowledgeMapRequest request) {
        log.info("收到知识图谱生成请求: {}", request);

        try {
            // 设置默认值
            if (request.getMaxDepth() == null || request.getMaxDepth() <= 0) {
                request.setMaxDepth(3);
            }

            // 生成知识图谱
            KnowledgeMapNode rootNode = knowledgeMapService.generateKnowledgeMap(
                request.getDocumentId(),
                request.getMaxDepth()
            );

            return ResponseEntity.ok(rootNode);
        } catch (Exception e) {
            log.error("生成知识图谱失败", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取文档的简单大纲（快速版本）
     *
     * @param documentId 文档ID
     * @return 文档大纲
     */
    @GetMapping("/outline/{documentId}")
    public ResponseEntity<KnowledgeMapNode> getDocumentOutline(@PathVariable Long documentId) {
        log.info("获取文档大纲: {}", documentId);

        try {
            // 使用较浅的深度快速生成大纲
            KnowledgeMapNode outline = knowledgeMapService.generateKnowledgeMap(documentId, 2);
            return ResponseEntity.ok(outline);
        } catch (Exception e) {
            log.error("获取文档大纲失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
