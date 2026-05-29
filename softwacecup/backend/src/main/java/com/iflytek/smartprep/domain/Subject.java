package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sp_subject")
public class Subject {
    private Long id;
    private String name;
    private String icon;
    private String color;
    private String description;
    private Integer sortOrder;
}
