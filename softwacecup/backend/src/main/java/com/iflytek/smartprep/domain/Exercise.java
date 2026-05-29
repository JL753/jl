package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sp_exercise")
public class Exercise {
    private Long id;
    private Long knowledgePointId;
    private Long lessonId;
    private String type;
    private Integer difficulty;
    private String contentJson;
    private String answer;
    private String explanation;
}
