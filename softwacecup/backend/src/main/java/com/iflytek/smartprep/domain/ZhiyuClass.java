package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_class")
public class ZhiyuClass {
    private Long id;
    private String name;
    private Long teacherId;
    private String inviteCode;
    private String description;
    private LocalDateTime createdAt;
}
