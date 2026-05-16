package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sp_lesson")
public class Lesson {
    private Long id;
    private Long unitId;
    private String name;
    private String type;
    private String videoUrl;
    private Integer duration;
    private String content;
    private String status;
    private Integer sortOrder;
    private Long userId;
    private String coverUrl;
}
