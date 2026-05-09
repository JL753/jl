package com.iflytek.smartprep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识图谱节点
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeMapNode {
    /**
     * 节点ID
     */
    private String id;

    /**
     * 节点名称（知识点标题）
     */
    private String name;

    /**
     * 节点类型：root（根节点）、chapter（章节）、section（小节）、concept（概念）
     */
    private String type;

    /**
     * 节点描述
     */
    private String description;

    /**
     * 重要程度（1-5）
     */
    private Integer importance;

    /**
     * 关联页码
     */
    private List<Integer> pages;

    /**
     * 子节点
     */
    private List<KnowledgeMapNode> children;

    /**
     * 扩展属性（用于前端渲染）
     */
    private Object extra;
}
