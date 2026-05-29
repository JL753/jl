package com.iflytek.smartprep.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG 检索结果
 * 包含检索到的文档块和相似度分数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalResult {

    /**
     * 文档块
     */
    private DocumentChunk chunk;

    /**
     * 相似度分数（0-1之间，越高越相似）
     */
    private Double score;

    /**
     * 排名（1表示最相关）
     */
    private Integer rank;

    /**
     * 获取格式化的引用信息
     */
    public String getFormattedCitation() {
        if (chunk == null) {
            return "";
        }
        return chunk.getCitationLabel();
    }

    /**
     * 获取上下文摘要（用于拼接到提示词）
     */
    public String getContextSummary() {
        if (chunk == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【来源：").append(chunk.getDocumentTitle());

        if (chunk.getChapter() != null && !chunk.getChapter().isEmpty()) {
            sb.append(" - ").append(chunk.getChapter());
        }

        if (chunk.getPageNumber() != null) {
            sb.append(" 第").append(chunk.getPageNumber()).append("页");
        }

        sb.append("】\n");
        sb.append(chunk.getContent());

        return sb.toString();
    }
}
