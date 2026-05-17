package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.service.BaiduSearchService;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
public class GraphController {

    private final Driver neo4jDriver;
    private final BaiduSearchService baiduSearchService;

    /**
     * 从 Neo4j 读取全图数据
     */
    @GetMapping("/neo4j")
    public ApiResponse<Map<String, Object>> getNeo4jGraph() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        Set<String> nodeIds = new HashSet<>();

        try (Session session = neo4jDriver.session()) {
            // 查询所有 Entity 节点
            Result nodeResult = session.run(
                "MATCH (n) " +
                "OPTIONAL MATCH (n)-[r]-(m) " +
                "RETURN n, collect(DISTINCT {type: type(r), target: id(m), targetName: m.name}) as relations"
            );
            while (nodeResult.hasNext()) {
                Record record = nodeResult.next();
                var node = record.get("n").asNode();
                String nodeId = String.valueOf(node.id());
                if (!nodeIds.add(nodeId)) continue;

                Map<String, Object> nodeMap = new HashMap<>();
                nodeMap.put("id", nodeId);
                nodeMap.put("name", node.get("name").asString(""));
                nodeMap.put("type", node.containsKey("type") ? node.get("type").asString("Entity") : "Entity");
                nodeMap.put("mastery", node.containsKey("mastery") ? node.get("mastery").asDouble(0) : 0);
                nodeMap.put("subjectId", node.containsKey("subjectId") ? node.get("subjectId").asString("") : "");
                nodeMap.put("lessonId", node.containsKey("lessonId") ? node.get("lessonId").asString("") : "");
                nodeMap.put("color", node.containsKey("color") ? node.get("color").asString("#334155") : "#334155");
                nodes.add(nodeMap);

                // 边
                var relations = record.get("relations");
                if (!relations.isNull()) {
                    for (var rel : relations.asList(org.neo4j.driver.Value::asMap)) {
                        Map<String, Object> edgeMap = new HashMap<>();
                        edgeMap.put("source", nodeId);
                        edgeMap.put("target", String.valueOf(rel.get("target")));
                        edgeMap.put("type", rel.get("type") != null ? rel.get("type").toString() : "RELATED_TO");
                        edges.add(edgeMap);
                    }
                }
            }

            // 如果没有 Neo4j 数据，回退到空结果
            if (nodes.isEmpty()) {
                result.put("nodes", List.of());
                result.put("edges", List.of());
                result.put("fallback", true);
                return ApiResponse.ok(result);
            }
        } catch (Exception e) {
            // Neo4j 不可用时返回空
            result.put("nodes", List.of());
            result.put("edges", List.of());
            result.put("fallback", true);
            return ApiResponse.ok(result);
        }

        result.put("nodes", nodes);
        result.put("edges", edges);
        result.put("fallback", false);
        return ApiResponse.ok(result);
    }

    /**
     * 百度搜索节点相关资源
     */
    @PostMapping("/search-resource")
    public ApiResponse<List<Map<String, String>>> searchResource(@RequestBody Map<String, String> body) {
        String nodeName = body.getOrDefault("nodeName", "");
        String subjectName = body.getOrDefault("subjectName", "");
        String query = (nodeName + " " + subjectName + " 教程 学习资源").trim();
        List<Map<String, String>> results = baiduSearchService.searchResources(query);
        return ApiResponse.ok(results);
    }
}
