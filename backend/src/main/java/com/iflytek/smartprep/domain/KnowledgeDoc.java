package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sp_knowledge_doc")
public class KnowledgeDoc {
    private Long id;
    private String course;
    private String title;
    private String content;
    private String tag;
}
