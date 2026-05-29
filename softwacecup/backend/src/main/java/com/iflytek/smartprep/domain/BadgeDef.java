package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sp_badge_def")
public class BadgeDef {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private String color;
    private String unlockRule;
    private Integer sortOrder;
}
