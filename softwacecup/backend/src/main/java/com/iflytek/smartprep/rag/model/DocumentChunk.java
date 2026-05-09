package com.iflytek.smartprep.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档分块模型
 * 用于存储文档切分后的文本块及其元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    /**
     * 文档ID（对应数据库中的knowledge_doc.id）
     */
    private Long documentId;

    /**
     * 文档标题
     */
    private String documentTitle;

    /**
     * 课程名称
     */
    private String course;

    /**
     * 分块ID（同一文档内的唯一标识）
     */
    private Integer chunkIndex;

    /**
     * 文本内容
     */
    private String content;

    /**
     * 章节信息（如："第一章 机器学习基础"）
     */
    private String chapter;

    /**
     * 页码（PDF文档）
     */
    private Integer pageNumber;

    /**
     * 段落编号（Word文档）
     */
    private Integer paragraphNumber;

    /**
     * 标签（如："基础"、"难点"、"案例"）
     */
    private String tag;

    /**
     * 字符起始位置（在原文档中的位置）
     */
    private Integer startPosition;

    /**
     * 字符结束位置
     */
    private Integer endPosition;

    /**
     * 向量ID（存储在向量数据库中的ID）
     */
    private String vectorId;

    /**
     * 创建时间戳
     */
    private Long createdAt;

    /**
     * 获取元数据摘要（用于日志和调试）
     */
    public String getMetadataSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("文档: ").append(documentTitle);
        if (chapter != null) {
            sb.append(" | 章节: ").append(chapter);
        }
        if (pageNumber != null) {
            sb.append(" | 第").append(pageNumber).append("页");
        }
        if (paragraphNumber != null) {
            sb.append(" | 第").append(paragraphNumber).append("段");
        }
        return sb.toString();
    }

    /**
     * 获取引用标注（用于前端显示）
     */
    public String getCitationLabel() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(documentTitle);
        if (chapter != null && !chapter.isEmpty()) {
            sb.append(" - ").append(chapter);
        }
        if (pageNumber != null) {
            sb.append(" 第").append(pageNumber).append("页");
        }
        sb.append("]");
        return sb.toString();
    }
}
