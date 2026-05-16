package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sp_kp_dependency")
public class KpDependency {
    private Long id;
    private Long prerequisiteId;
    private Long successorId;
    private String relationType;
}
