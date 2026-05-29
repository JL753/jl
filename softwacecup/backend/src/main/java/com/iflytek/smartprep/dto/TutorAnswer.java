package com.iflytek.smartprep.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TutorAnswer {
    private String markdown;
    private List<String> diagrams;
    private List<String> shortVideoTips;
    private List<String> references;
    private List<String> streamChunks;
    private List<String> safetyTips;

    /**
     * RAG 引用来源列表
     * 格式：["[人工智能导论 - 第一章 第3页]", "[机器学习基础 - 第二章 第5页]"]
     */
    private List<String> citations;

    /**
     * RAG 检索到的原始文档块数量
     */
    private Integer retrievedChunksCount;
}
