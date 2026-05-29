package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sp_unit")
public class Unit {
    private Long id;
    private Long subjectId;
    private String name;
    private String description;
    private Integer sortOrder;
    private Long prerequisiteUnitId;
}
