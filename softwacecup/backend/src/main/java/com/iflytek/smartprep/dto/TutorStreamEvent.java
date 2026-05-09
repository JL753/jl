package com.iflytek.smartprep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorStreamEvent {
    private String type;
    private String content;

    /**
     * RAG 引用来源（仅在 type="citations" 时使用）
     */
    private List<String> citations;
}
