package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sp_knowledge_point")
public class KnowledgePoint {
    private Long id;
    private Long subChapterId;
    private String name;
    private String description;
    private Integer difficultyLevel;
    private String tags;
}
